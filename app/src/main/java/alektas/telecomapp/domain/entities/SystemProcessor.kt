package alektas.telecomapp.domain.entities

import alektas.telecomapp.App
import alektas.telecomapp.data.CodeGenerator
import alektas.telecomapp.data.UserDataProvider
import alektas.telecomapp.domain.Repository
import alektas.telecomapp.domain.entities.coders.CdmaDecimalCoder
import alektas.telecomapp.domain.entities.coders.toBipolar
import alektas.telecomapp.domain.entities.coders.toUnipolar
import alektas.telecomapp.domain.entities.contracts.QpskContract
import alektas.telecomapp.domain.entities.configs.DemodulatorConfig
import alektas.telecomapp.domain.entities.converters.ValueConverter
import alektas.telecomapp.domain.entities.demodulators.QpskDemodulator
import alektas.telecomapp.domain.entities.generators.SignalGenerator
import alektas.telecomapp.domain.entities.modulators.QpskModulator
import alektas.telecomapp.domain.entities.signals.BaseSignal
import alektas.telecomapp.domain.entities.signals.DigitalSignal
import alektas.telecomapp.domain.entities.signals.Signal
import alektas.telecomapp.domain.entities.signals.noises.Noise
import alektas.telecomapp.domain.entities.signals.noises.PulseNoise
import alektas.telecomapp.domain.entities.signals.noises.WhiteNoise
import alektas.telecomapp.utils.L
import android.annotation.SuppressLint
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.functions.BiFunction
import io.reactivex.rxkotlin.toObservable
import io.reactivex.rxkotlin.zipWith
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import javax.inject.Inject
import javax.inject.Named
import kotlin.math.max

class SystemProcessor {
    @Inject
    lateinit var storage: Repository
    private var codedGroupData: DoubleArray? = null
    private var simulatedChannels: List<Channel>? = null
    private var decodedChannels: List<Channel>? = null
    @JvmField
    @field:[Inject Named("sourceSnr")]
    var noiseSnr: Double? = null
    @JvmField
    @field:[Inject Named("interferenceRate")]
    var interferenceRate: Double? = null
    @JvmField
    @field:[Inject Named("interferenceSparseness")]
    var interferenceSparseness: Double? = null
    private var decodingThreshold = QpskContract.DEFAULT_SIGNAL_THRESHOLD
    private var disposable = CompositeDisposable()
    private var simulationSubscription: Disposable? = null
    private var transmitSubscription: Disposable? = null
    val berProcess = BehaviorSubject.create<Int>()

    init {
        App.component.inject(this)

        if (noiseSnr != null) {
            setNoise(noiseSnr ?: QpskContract.DEFAULT_SIGNAL_NOISE_RATE)
        }

        if (interferenceRate != null && interferenceSparseness != null) {
            setInterference(
                interferenceSparseness ?: QpskContract.DEFAULT_INTERFERENCE_SPARSENESS,
                interferenceRate ?: QpskContract.DEFAULT_SIGNAL_NOISE_RATE
            )
        }

        disposable.addAll(
            storage.observeSimulatedChannels()
                .subscribeOn(Schedulers.io())
                .subscribe {
                    simulatedChannels = it
                    generateChannelsFrameSignal(it)
                },

            storage.observeDemodulatorConfig()
                .subscribeOn(Schedulers.io())
                .subscribe { demodulate(it) },

            storage.observeDemodulatedSignal()
                .subscribeOn(Schedulers.io())
                .subscribe { s ->
                    codedGroupData = s.dataValues
                    decodedChannels?.let { updateDecodedChannels(it, s.dataValues) }
                },

            storage.observeDecodedChannels()
                .subscribeOn(Schedulers.io())
                .subscribe { decodedChannels = it },

            storage.observeNoise()
                .subscribeOn(Schedulers.io())
                .subscribe { noiseSnr = it.rate() },

            storage.observeInterference()
                .subscribeOn(Schedulers.io())
                .subscribe {
                    if (it is PulseNoise) {
                        interferenceRate = it.rate()
                        interferenceSparseness = it.sparseness
                    }
                },

            Observable.combineLatest(
                storage.observeSimulatedChannels(),
                storage.observeDecodedChannels(),
                BiFunction<List<Channel>, List<Channel>, Map<BooleanArray, List<Int>>> { origin, decoded ->
                    diffChannels(origin, decoded)
                })
                .subscribeOn(Schedulers.computation())
                .observeOn(Schedulers.io())
                .subscribe { storage.setSimulatedChannelsErrors(it) }
        )
    }

    fun setAdcFrequency(frequency: Double) {
        val freq = frequency * 1.0e6 // МГц -> Гц
        Simulator.samplingRate = freq
        val filterConfig = storage.getDemodulatorFilterConfig().apply { samplingRate = freq }
        storage.setDemodulatorFilterConfig(filterConfig)
    }

    fun processData(
        dataString: String,
        adcResolution: Int,
        adcSamplingRate: Double // МГц
    ) {
        transmitSubscription?.dispose()
        transmitSubscription = Single
            .create<DoubleArray> {
                val data =
                    ValueConverter(adcResolution).convertToBipolarNormalizedValues(dataString)
                it.onSuccess(data)
            }
            .flatMapObservable { generateFrames(adcSamplingRate * 1.0e6, it) }
            .doOnSubscribe { storage.startCountingStatistics() }
            .subscribeOn(Schedulers.io())
            .subscribe {
                storage.setEther(it)
            }
    }

    private fun generateFrames(samplingRate: Double, data: DoubleArray): Observable<Signal> {
        return Observable.create<Signal> { subscriber ->
            val dc = storage.getCurrentDemodulatorConfig()
            val frameTime = dc.bitTime * dc.frameLength * dc.codeLength
            val framePoints = Simulator.samplesFor(frameTime)

            // Так как продолжительность данных могла измениться, обновляем время симуляции
            Simulator.simulationTime = frameTime

            val frameTimeline = Simulator.getTimeline(samplingRate, framePoints)
            val frames = data.toList().chunked(framePoints)
            storage.setExpectedFrameCount(frames.size)

            frames.forEach {
                val frameSignal = BaseSignal(frameTimeline, it.toDoubleArray())
                subscriber.onNext(frameSignal)
            }

            subscriber.onComplete()
        }
    }

    @SuppressLint("CheckResult")
    fun createChannels(
        count: Int,
        carrierFrequency: Double, // МГц
        dataSpeed: Double, // кБит/с
        codeLength: Int,
        frameLength: Int,
        codesType: Int
    ) {
        simulationSubscription?.dispose()
        simulationSubscription =
            generateChannels(count, carrierFrequency, dataSpeed, codeLength, frameLength, codesType)
                .subscribeOn(Schedulers.computation())
                .observeOn(Schedulers.io())
                .subscribe { channels: List<Channel> ->
                    storage.setChannels(channels)
                }
    }

    private fun generateChannels(
        count: Int,
        carrierFrequency: Double, // МГц
        dataSpeed: Double, // кБит/с
        codeLength: Int,
        frameLength: Int,
        codesType: Int
    ): Single<List<Channel>> {
        return Single.create<List<Channel>> {
            val channels = mutableListOf<Channel>()
            val codeGen = CodeGenerator()
            val codes = when (codesType) {
                CodeGenerator.WALSH -> codeGen.generateWalshMatrix(max(codeLength, count))
                else -> codeGen.generateRandomCodes(count, codeLength)
            }

            val bitTime = 1.0e-3 / dataSpeed // скорость в период бита (в секундах)

            for (i in 0 until count) {
                val channel = Channel(
                    name = "${i + 1}",
                    carrierFrequency = carrierFrequency * 1.0e6, // МГц -> Гц
                    frameLength = frameLength,
                    bitTime = bitTime,
                    code = codes[i],
                    codeType = codesType
                )
                channels.add(channel)
            }

            // Так как продолжительность данных изменилась, обновляем время симуляции
            val dataTime = frameLength * codes[0].size * bitTime
            changeSimulationTime(dataTime)

            it.onSuccess(channels)
        }
    }

    private fun changeSimulationTime(time: Double) {
        if (Simulator.simulationTime == time) return

        Simulator.simulationTime = time
        noiseSnr?.let { n -> setNoise(n) } // генерируем новый шум с обновленной продолжительностью
        interferenceRate?.let { rate ->
            interferenceSparseness?.let { sp ->
                setInterference(sp, rate)
            }
        } // генерируем новые помехи с обновленной продолжительностью
    }

    fun transmitFrames(channels: List<Channel>, frameCount: Int) {
        var framesTransmitted = 0
        val frameSize = if (channels.isEmpty()) 0 else channels.first().frameLength

        transmitSubscription?.dispose()
        transmitSubscription = Observable
            .create<List<Channel>> {
                // генерировать на 1 фрейм меньше, так как первый фрейм отправляется в startWith
                for (i in 1 until frameCount) {
                    val chls = channels.map { c ->
                        c.copy().apply { frameData = UserDataProvider.generateData(frameSize) }
                    }
                    it.onNext(chls)
                }

                // Сгенерировать дополнительный пустой фрейм, означающий конец передачи.
                // Нужен для того, чтобы дождаться декодирования последнего фрейма для отображения
                // индикации прогресса передачи.
                val chls = channels.map { c ->
                    c.copy().apply { frameData = booleanArrayOf() }
                }
                it.onNext(chls)
                it.onComplete()
            }
            .zipWith(storage.observeDecodedChannels(false)) { next, prev ->
                framesTransmitted++
                next
            } // дожидаться декодирования
            .startWith(channels.map { c ->
                c.apply { frameData = UserDataProvider.generateData(frameSize) }
            }) // первый фрейм запускает цикл передачи (нужно для срабатывания zipWith)
            .subscribeOn(Schedulers.io())
            .doOnSubscribe {
                storage.startCountingStatistics()
                storage.setExpectedFrameCount(frameCount)
            }
            .doFinally { storage.endCountingStatistics() }
            .subscribe {
                if (framesTransmitted >= frameCount) {
                    transmitSubscription?.dispose()
                    return@subscribe
                }

                storage.setChannels(it) // передача очередного фрейма всеми каналами
            }
    }

    @SuppressLint("CheckResult")
    fun generateChannelsFrameSignal(channels: List<Channel>) {
        if (channels.isEmpty()) {
            storage.setChannelsFrameSignal(BaseSignal())
            return
        }

        Single.create<Signal> {
            val groupData = aggregate(channels)

            val carrier = SignalGenerator().cos(frequency = channels[0].carrierFrequency)
            val signal = QpskModulator(channels[0].bitTime).modulate(carrier, groupData)

            it.onSuccess(signal)
        }
            .subscribeOn(Schedulers.computation())
            .observeOn(Schedulers.io())
            .subscribe { signal: Signal -> storage.setChannelsFrameSignal(signal) }
    }

    private fun aggregate(channels: List<Channel>): DoubleArray {
        return channels
            .map { channel ->
                // Преобразование однополярных двоичных данных в биполярные
                val code = channel.code.toBipolar()
                val data = channel.frameData.toBipolar()
                CdmaDecimalCoder().encode(code, data)
            }
            .reduce { acc, data ->
                data.mapIndexed { i, value -> acc[i] + value }.toDoubleArray()
            }
    }

    fun removeChannel(channel: Channel) {
        storage.removeChannel(channel)
    }

    @SuppressLint("CheckResult")
    fun setNoise(snr: Double, singleThread: Boolean = false) {
        Single.create<Noise> {
            val noise = WhiteNoise(snr, QpskContract.DEFAULT_SIGNAL_MAGNITUDE)
            it.onSuccess(noise)
        }
            .subscribeOn(if (singleThread) Schedulers.single() else Schedulers.computation())
            .observeOn(Schedulers.io())
            .subscribe { noise: Noise -> storage.setNoise(noise) }
    }

    fun disableNoise() {
        storage.disableNoise()
    }

    fun enableNoise() {
        storage.enableNoise(true)
    }

    @SuppressLint("CheckResult")
    fun setInterference(sparseness: Double, snr: Double, singleThread: Boolean = false) {
        Single.create<Noise> {
            val noise = PulseNoise(sparseness, snr, QpskContract.DEFAULT_SIGNAL_MAGNITUDE)
            it.onSuccess(noise)
        }
            .subscribeOn(if (singleThread) Schedulers.single() else Schedulers.computation())
            .observeOn(Schedulers.io())
            .subscribe { noise: Noise -> storage.setInterference(noise) }
    }

    @SuppressLint("CheckResult")
    fun setInterferenceSparseness(sparseness: Double) {
        Single.create<Noise> {
            val noise = PulseNoise(
                sparseness,
                interferenceRate ?: QpskContract.DEFAULT_SIGNAL_NOISE_RATE,
                QpskContract.DEFAULT_SIGNAL_MAGNITUDE
            )
            it.onSuccess(noise)
        }
            .subscribeOn(Schedulers.computation())
            .observeOn(Schedulers.io())
            .subscribe { noise: Noise -> storage.setInterference(noise) }
    }

    @SuppressLint("CheckResult")
    fun setInterferenceRate(rate: Double) {
        Single.create<Noise> {
            val noise = PulseNoise(
                interferenceSparseness ?: QpskContract.DEFAULT_INTERFERENCE_SPARSENESS,
                rate,
                QpskContract.DEFAULT_SIGNAL_MAGNITUDE
            )
            it.onSuccess(noise)
        }
            .subscribeOn(Schedulers.computation())
            .observeOn(Schedulers.io())
            .subscribe { noise: Noise -> storage.setInterference(noise) }
    }

    fun disableInterference() {
        storage.disableInterference()
    }

    fun enableInterference() {
        storage.enableInterference(true)
    }

    @SuppressLint("CheckResult")
    fun demodulate(config: DemodulatorConfig) {
        val demodulator = QpskDemodulator(config)

        Single.create<DigitalSignal> {
            val demodSignal = demodulator.demodulateFrame(config.inputSignal)
            it.onSuccess(demodSignal)
        }
            .subscribeOn(Schedulers.computation())
            .observeOn(Schedulers.io())
            .subscribe { signal: DigitalSignal ->
                storage.setDemodulatedSignal(signal)
                storage.setChannelI(demodulator.sigI)
                storage.setFilteredChannelI(demodulator.filteredSigI)
                storage.setChannelQ(demodulator.sigQ)
                storage.setFilteredChannelQ(demodulator.filteredSigQ)
            }
    }

    private fun updateDecodedChannels(channels: List<Channel>, groupData: DoubleArray) {
        val chls = channels.map {
            it.copy().apply {
                val frameData =
                    CdmaDecimalCoder(decodingThreshold).decode(code.toBipolar(), groupData)
                val errors = mutableListOf<Int>()
                frameData.forEachIndexed { i, d -> if (d == 0.0) errors.add(i) }
                this.errors = errors
                this.frameData = frameData.toUnipolar()
            }
        }

        L.d(this, "setDecodedChannels: update")
        storage.setDecodedChannels(chls)
    }

    fun addDecodedChannel(code: BooleanArray, threshold: Float) {
        decodingThreshold = threshold.toDouble()

        codedGroupData?.let {
            val frameData = CdmaDecimalCoder(decodingThreshold).decode(code.toBipolar(), it)
            val errors = mutableListOf<Int>()
            frameData.forEachIndexed { i, d -> if (d == 0.0) errors.add(i) }
            val channel = Channel(frameData = frameData.toUnipolar(), code = code)
            channel.errors = errors
            storage.addDecodedChannel(channel)
        }
    }

    @SuppressLint("CheckResult")
    fun createDecodedChannels(count: Int, codeLength: Int, codesType: Int, threshold: Float) {
        decodingThreshold = threshold.toDouble()

        Single.create<List<Channel>> { emitter ->
            codedGroupData?.let {
                val channels = mutableListOf<Channel>()
                val codeGen = CodeGenerator()
                val codes = when (codesType) {
                    CodeGenerator.WALSH -> codeGen.generateWalshMatrix(max(codeLength, count))
                    else -> codeGen.generateRandomCodes(count, codeLength)
                }

                for (i in 0 until count) {
                    val frameData =
                        CdmaDecimalCoder(decodingThreshold).decode(codes[i].toBipolar(), it)
                    val errors = mutableListOf<Int>()
                    frameData.forEachIndexed { index, d -> if (d == 0.0) errors.add(index) }
                    val channel = Channel(frameData = frameData.toUnipolar(), code = codes[i])
                    channel.errors = errors
                    channels.add(channel)
                }

                emitter.onSuccess(channels)
            }
        }
            .subscribeOn(Schedulers.computation())
            .observeOn(Schedulers.io())
            .subscribe { channels: List<Channel> ->
                L.d(this, "setDecodedChannels: create")
                storage.setDecodedChannels(channels)
            }
    }

    fun calculateBer(fromSnr: Double, toSnr: Double, pointsCount: Int) {
        val step = (toSnr - fromSnr) / pointsCount
        val snrs = DoubleArray(pointsCount) { fromSnr + it * step }
        val isNoiseWasEnabled = storage.isNoiseEnabled()
        val isInterferenceWasEnabled = storage.isInterferenceEnabled()

        disposable.add(snrs.toObservable()
            .skip(1) // Первое SNR вручную запускается в doOnSubscribe, поэтому пропускаем
            .zipWith(storage.observeBerByNoise()) { snr, _ -> snr } // Ждем вычисления BER, затем запускаем следующее SNR
            .doOnSubscribe {
                berProcess.onNext(0)
                if (isInterferenceWasEnabled) storage.disableInterference()
                if (!isNoiseWasEnabled) storage.enableNoise(false)
                setNoise(fromSnr, true)
            }
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .subscribe({
                val index = snrs.indexOf(it)
                if (index > 0) {
                    val progress = (index / pointsCount.toDouble() * 100).toInt()
                    berProcess.onNext(progress)
                }
                setNoise(it, true)
            }, {
                berProcess.onNext(100)
            }, {
                // Восстановить исходное состояние
                if (!isNoiseWasEnabled) disableNoise()
                if (isInterferenceWasEnabled) enableInterference()
                berProcess.onNext(100)
            })
        )
    }

    /**
     * Определение ошибочно принятых битов каналов.
     * Декодированные каналы, которые не передавались источником, не учитываются.
     * Если передаваемые каналы не декодировались, то все данные этих каналов
     * считются неправильно принятыми.
     *
     * @return словарь, где ключ - код канала, значение - список индексов несовпадающих битов канала
     */
    @SuppressLint("CheckResult")
    private fun diffChannels(
        transmitted: List<Channel>,
        received: List<Channel>
    ): Map<BooleanArray, List<Int>> {
        val channelsErrorBits = mutableMapOf<BooleanArray, List<Int>>()

        if (transmitted.isEmpty()) {
            return channelsErrorBits
        }
        if (received.isEmpty()) {
            transmitted.forEach { channelsErrorBits[it.code] = it.frameData.indices.toList() }
            return channelsErrorBits
        }

        for (transCh in transmitted) {
            val recCh = received.find { it.code.contentEquals(transCh.code) }
            if (recCh == null) {
                channelsErrorBits[transCh.code] = transCh.frameData.indices.toList()
            } else {
                diff(transCh.frameData, recCh.frameData).let {
                    if (it.isNotEmpty()) channelsErrorBits[transCh.code] = it
                }
            }
        }

        return channelsErrorBits
    }

    /**
     * Поиск несовпадающих битов в двух массивах.
     * Если размер одного из массивов больше другого, то "лишние" биты считются несовпадающими.
     *
     * @return список индексов несовпадающих битов
     */
    private fun diff(first: BooleanArray, second: BooleanArray): List<Int> {
        if (first.isEmpty()) return second.indices.toList()
        if (second.isEmpty()) return first.indices.toList()

        val indices = mutableListOf<Int>()

        val maxSize = max(first.size, second.size)
        for (i in 0 until maxSize) {
            try {
                if (first[i] != second[i]) indices.add(i)
            } catch (e: IndexOutOfBoundsException) {
                indices.add(i)
            }
        }

        return indices
    }

}