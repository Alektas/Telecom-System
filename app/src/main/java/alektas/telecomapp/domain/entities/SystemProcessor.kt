package alektas.telecomapp.domain.entities

import alektas.telecomapp.App
import alektas.telecomapp.data.CodeGenerator
import alektas.telecomapp.data.UserDataProvider
import alektas.telecomapp.domain.Repository
import alektas.telecomapp.domain.entities.coders.CdmaDecimalCoder
import alektas.telecomapp.domain.entities.coders.toBipolar
import alektas.telecomapp.domain.entities.coders.toUnipolar
import alektas.telecomapp.domain.entities.configs.DecoderConfig
import alektas.telecomapp.domain.entities.contracts.QpskContract
import alektas.telecomapp.domain.entities.configs.DemodulatorConfig
import alektas.telecomapp.domain.entities.contracts.CdmaContract
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
import alektas.telecomapp.domain.processes.*
import alektas.telecomapp.utils.L
import android.annotation.SuppressLint
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.functions.BiFunction
import io.reactivex.rxkotlin.zipWith
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import javax.inject.Inject
import javax.inject.Named
import kotlin.math.max

class SystemProcessor {
    @Inject
    lateinit var storage: Repository
    @JvmField
    @field:[Inject Named("sourceSnr")]
    var noiseSnr: Double? = null
    @JvmField
    @field:[Inject Named("interferenceRate")]
    var interferenceRate: Double? = null
    @JvmField
    @field:[Inject Named("interferenceSparseness")]
    var interferenceSparseness: Double? = null
    private var disposable = CompositeDisposable()
    private var simulationSubscription: Disposable? = null
    private var transmitSubscription: Disposable? = null
    private var decodeSubscription: Disposable? = null
    private var characteristicsProcess: CalculateCharacteristicsProcess? = null
    val characteristicsProcessState = BehaviorSubject.create<ProcessState>()

    init {
        App.component.inject(this)
        App.component.channelsConfig().let {
            createChannels(
                it.channelCount,
                it.carrierFrequency,
                it.dataSpeed,
                it.codeLength,
                it.frameLength,
                it.codeType
            )
        }

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
                    generateChannelsFrameSignal(it)
                },

            Observable.combineLatest(
                storage.observeEther(),
                storage.observeDemodulatorConfig(),
                BiFunction { s: Signal, c: DemodulatorConfig -> s to c }
            )
                .subscribeOn(Schedulers.io())
                .subscribe { demodulate(it.first, it.second) },

            Observable.combineLatest(
                storage.observeDemodulatedSignal(),
                storage.observeDecoderConfig(),
                BiFunction { s: DigitalSignal, c: DecoderConfig -> s.dataValues to c }
            )
                .subscribeOn(Schedulers.io())
                .subscribe { (data, config) ->
                    L.d(this, "Decoding: new frame arrived or decoder configuration changed")
                    decodeSubscription?.dispose()
                    if (config.isAutoDetection) {
                        autoDecode(
                            data,
                            config.codeLength ?: 0,
                            config.codeType ?: 0,
                            config.threshold ?: QpskContract.DEFAULT_SIGNAL_THRESHOLD
                        )
                    } else {
                        decodeChannels(
                            storage.getDecoderChannels(),
                            data,
                            config.threshold ?: QpskContract.DEFAULT_SIGNAL_THRESHOLD
                        )
                    }
                },

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
                }
        )
    }

    /**
     * Установка частоты дискретизации АЦП системы.
     * Все сигналы после установки обрабатываются и генерируются с указанной частотой.
     */
    fun setAdcFrequency(frequency: Double) {
        val freq = frequency * 1.0e6 // МГц -> Гц
        Simulator.samplingRate = freq
        val filterConfig = storage.getDemodulatorFilterConfig().apply { samplingRate = freq }
        storage.setDemodulatorFilterConfig(filterConfig)
    }

    /**
     * Обработка данных сигнала, записанных в строковом виде. Единицы и нули из строки
     * преобразуются в последовательность сигналов [Signal] с длительностями одного фрейма.
     *
     * @param dataString строка данных из нулей и единиц
     * @param adcResolution разрядность АЦП, оцифровавшего данные
     * @param adcSamplingRate частота дискретизации АЦП, оцифровавшего данные
     */
    fun processData(
        dataString: String,
        adcResolution: Int,
        adcSamplingRate: Double // МГц
    ) {
        cancelCurrentProcess()
        transmitSubscription = Single
            .create<DoubleArray> {
                storage.setTransmittingSubProcess(ProcessState(READ_FILE_KEY, READ_FILE_NAME, state = ProcessState.STARTED))
                val data =
                    ValueConverter(adcResolution).convertToBipolarNormalizedValues(dataString)
                storage.setTransmittingSubProcess(ProcessState(READ_FILE_KEY, READ_FILE_NAME, state = ProcessState.FINISHED))
                it.onSuccess(data)
            }
            .flatMapObservable { generateFrames(adcSamplingRate * 1.0e6, it) }
            .subscribeOn(Schedulers.io())
            .doOnSubscribe {
                storage.setTransmittingSubProcess(ProcessState(READ_FILE_KEY, READ_FILE_NAME))
                storage.startCountingStatistics()
            }
            .subscribe({
                storage.setFileSignal(it)
            }, {
                it.printStackTrace()
            })
    }

    /**
     * Создание источника сигналов фреймов из массива данных.
     */
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
            .doOnSubscribe {
                storage.setTransmittingSubProcess(ProcessState(CREATE_SIGNAL_KEY, CREATE_SIGNAL_NAME, state = ProcessState.STARTED))
            }
            .doFinally {
                storage.setTransmittingSubProcess(ProcessState(CREATE_SIGNAL_KEY, CREATE_SIGNAL_NAME, state = ProcessState.FINISHED))
            }
    }

    /**
     * Создание и установка каналов связи, которые затем можно использовать для хранения
     * и передачи фреймов.
     *
     * @param count количество создаваемых каналов
     * @param carrierFrequency частота несущей гармоники каналов, МГц
     * @param dataSpeed скорость передачи данных, кБит/с
     * @param codeLength длина генерируемого уникального кода канала
     * @param frameLength длина фреймов (массивов данных)
     * @param codesType семейство кодов каналов, см. [CodeGenerator]
     */
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

    /**
     * Генерация и передача фреймов в выделенных каналах.
     *
     * @param channels каналы связи, в которых будут передаваться фреймы
     * @param frameCount количество фреймов, которые нужно передать
     */
    fun transmitFrames(channels: List<Channel>, frameCount: Int) {
        cancelCurrentProcess()
        var framesTransmitted = 0
        val frameSize = if (channels.isEmpty()) 0 else channels.first().frameLength

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
            .zipWith(storage.observeDecoderChannels(false)) { next, prev ->
                storage.resetTransmittingSubProcesses()
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
    private fun generateChannelsFrameSignal(channels: List<Channel>) {
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
            .doOnSubscribe {
                storage.setTransmittingSubProcess(
                    ProcessState(
                        CREATE_SIGNAL_KEY,
                        CREATE_SIGNAL_NAME,
                        state = ProcessState.STARTED
                    )
                )
            }
            .doFinally {
                storage.setTransmittingSubProcess(
                    ProcessState(
                        CREATE_SIGNAL_KEY,
                        CREATE_SIGNAL_NAME,
                        state = ProcessState.FINISHED
                    )
                )
            }
            .subscribe { signal: Signal ->
                storage.setChannelsFrameSignal(signal)
            }
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
            val bitTime = try {
                storage.getSimulatedChannels().first().bitTime
            } catch (e: NoSuchElementException) {
                QpskContract.DEFAULT_DATA_BIT_TIME
            }
            val bitEnergy = QpskContract.DEFAULT_SIGNAL_POWER * bitTime
            val noise = WhiteNoise(snr, bitEnergy)
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
            val bitTime = try {
                storage.getSimulatedChannels().first().bitTime
            } catch (e: NoSuchElementException) {
                QpskContract.DEFAULT_DATA_BIT_TIME
            }
            val bitEnergy = QpskContract.DEFAULT_SIGNAL_POWER * bitTime
            val noise = PulseNoise(sparseness, snr, bitEnergy)
            it.onSuccess(noise)
        }
            .subscribeOn(if (singleThread) Schedulers.single() else Schedulers.computation())
            .observeOn(Schedulers.io())
            .subscribe { noise: Noise -> storage.setInterference(noise) }
    }

    @SuppressLint("CheckResult")
    fun setInterferenceSparseness(sparseness: Double) {
        Single.create<Noise> {
            val bitTime = try {
                storage.getSimulatedChannels().first().bitTime
            } catch (e: NoSuchElementException) {
                QpskContract.DEFAULT_DATA_BIT_TIME
            }
            val bitEnergy = QpskContract.DEFAULT_SIGNAL_POWER * bitTime
            val noise = PulseNoise(
                sparseness,
                interferenceRate ?: QpskContract.DEFAULT_SIGNAL_NOISE_RATE,
                bitEnergy
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
            val bitTime = try {
                storage.getSimulatedChannels().first().bitTime
            } catch (e: NoSuchElementException) {
                QpskContract.DEFAULT_DATA_BIT_TIME
            }
            val bitEnergy = QpskContract.DEFAULT_SIGNAL_POWER * bitTime
            val noise = PulseNoise(
                interferenceSparseness ?: QpskContract.DEFAULT_INTERFERENCE_SPARSENESS,
                rate,
                bitEnergy
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
    private fun demodulate(signal: Signal, config: DemodulatorConfig) {
        val demodulator = QpskDemodulator(config)

        Single.create<DigitalSignal> {
            val demodSignal = demodulator.demodulate(signal)
            it.onSuccess(demodSignal)
        }
            .subscribeOn(Schedulers.computation())
            .observeOn(Schedulers.io())
            .doOnSubscribe {
                storage.setTransmittingSubProcess(
                    ProcessState(
                        DEMODULATE_KEY,
                        DEMODULATE_NAME,
                        state = ProcessState.STARTED
                    )
                )
            }
            .doFinally {
                storage.setTransmittingSubProcess(
                    ProcessState(
                        DEMODULATE_KEY,
                        DEMODULATE_NAME,
                        state = ProcessState.FINISHED
                    )
                )
            }
            .subscribe { s: DigitalSignal ->
                storage.setDemodulatedSignal(s)
                storage.setChannelI(demodulator.sigI)
                storage.setFilteredChannelI(demodulator.filteredSigI)
                storage.setChannelQ(demodulator.sigQ)
                storage.setFilteredChannelQ(demodulator.filteredSigQ)
            }
    }

    fun applyConfig(config: DecoderConfig) {
        if (!config.isAutoDetection) {
            val chls = createDecoderChannels(
                config.channelCount ?: 0,
                config.codeLength ?: 0,
                config.codeType ?: CdmaContract.DEFAULT_CODE_TYPE
            )
            storage.setDecoderChannels(chls)
        }
        L.d(this, "Decoding: update decoder config")
        storage.updateDecoderConfig(config)
    }

    private fun createDecoderChannels(
        count: Int,
        codeLength: Int,
        codeType: Int
    ): List<Channel> {
        val channels = mutableListOf<Channel>()
        val codeGen = CodeGenerator()
        val codes = when (codeType) {
            CodeGenerator.WALSH -> codeGen.generateWalshMatrix(max(codeLength, count))
            else -> codeGen.generateWalshMatrix(max(codeLength, count))
        }

        for (i in 0 until count) {
            channels.add(Channel(code = codes[i]))
        }

        L.d(this, "Decoding: create channels")
        return channels
    }

    private fun decodeChannels(
        channels: List<Channel>,
        data: DoubleArray,
        threshold: Float
    ) {
        decodeSubscription = Single.create<List<Channel>> { emitter ->
            val decodedChannels = channels.map {
                it.copy().apply {
                    val frameData = CdmaDecimalCoder(threshold).decode(code.toBipolar(), data)
                    val errors = mutableListOf<Int>()
                    frameData.forEachIndexed { i, d -> if (d == 0.0) errors.add(i) }
                    this.errors = errors
                    this.frameData = frameData.toUnipolar()
                }
            }
            emitter.onSuccess(decodedChannels)
        }
            .subscribeOn(Schedulers.computation())
            .observeOn(Schedulers.io())
            .doOnSubscribe {
                storage.setTransmittingSubProcess(
                    ProcessState(
                        DECODE_KEY,
                        DECODE_NAME,
                        state = ProcessState.STARTED
                    )
                )
            }
            .doFinally {
                storage.setTransmittingSubProcess(
                    ProcessState(
                        DECODE_KEY,
                        DECODE_NAME,
                        state = ProcessState.FINISHED
                    )
                )
            }
            .subscribe { c: List<Channel> ->
                L.d(this, "Decoding: decode channels")
                storage.setDecoderChannels(c)
            }
    }

    fun addCustomDecoderChannel(code: BooleanArray) {
        val c = Channel(code = code).apply {
            name = "Custom-$id"
        }
        L.d(this, "Decoding: add custom channel")
        storage.addDecoderChannel(c)
    }

    /**
     * Корреляционное автоопределение и декодирование каналов.
     */
    private fun autoDecode(data: DoubleArray, codeLength: Int, codeType: Int, threshold: Float) {
        decodeSubscription = Single.create<List<Channel>> { emitter ->
            val channels = mutableListOf<Channel>()
            val codeGen = CodeGenerator()
            val codes = when (codeType) {
                CodeGenerator.WALSH -> codeGen.generateWalshMatrix(codeLength)
                else -> codeGen.generateWalshMatrix(codeLength)
            }

            for (i in codes.indices) {
                val decoder = CdmaDecimalCoder(threshold)
                val code = codes[i].toBipolar()
                if (decoder.detectChannel(code, data)) {
                    val errors = mutableListOf<Int>()
                    val frameData = decoder.decode(code, data).apply {
                        forEachIndexed { index, d -> if (d == 0.0) errors.add(index) }
                    }
                    val channel = Channel(
                        name = "${i + 1}",
                        frameData = frameData.toUnipolar(),
                        code = codes[i]
                    ).apply {
                        this.errors = errors
                    }
                    channels.add(channel)
                }
            }

            emitter.onSuccess(channels)
        }
            .subscribeOn(Schedulers.computation())
            .observeOn(Schedulers.io())
            .doOnSubscribe {
                storage.setTransmittingSubProcess(
                    ProcessState(
                        DECODE_KEY,
                        DECODE_NAME,
                        state = ProcessState.STARTED
                    )
                )
            }
            .doFinally {
                storage.setTransmittingSubProcess(
                    ProcessState(
                        DECODE_KEY,
                        DECODE_NAME,
                        state = ProcessState.FINISHED
                    )
                )
            }
            .subscribe { channels: List<Channel> ->
                L.d(this, "Decoding: auto decode")
                storage.setDecoderChannels(channels)
            }
    }

    /**
     * Запуск процесса расчета характеристик системы (BER, пропускная способность)
     * в зависимости от величины шумов (SNR).
     *
     * @param fromSnr начальное значение SNR для расчетов, в дБ
     * @param toSnr конечное значение SNR для расчетов, в дБ
     * @param pointsCount количество отсчётов измерений - при скольких значения SNR
     * рассчитывать характеристики
     */
    fun calculateCharacteristics(fromSnr: Double, toSnr: Double, pointsCount: Int) {
        cancelCurrentProcess()

        val transmittingChannels = storage.getSimulatedChannels()
        val demodConfig = storage.getCurrentDemodulatorConfig()
        val decoderConfig = storage.getDecoderConfiguration()
        val decodingChannels = storage.getDecoderChannels()

        storage.clearBerByNoiseList()
        storage.clearCapacityByNoiseList()

        characteristicsProcess = CalculateCharacteristicsProcess(
            transmittingChannels,
            decodingChannels,
            demodConfig,
            decoderConfig
        )
        characteristicsProcess?.execute(fromSnr, toSnr, pointsCount) {
            characteristicsProcessState.onNext(it)
        }
    }

    fun getCharacteristicsProcessRange(): Pair<Double, Double> {
        val fromSnr = characteristicsProcess?.currentStartSnr ?: 0.0
        val toSnr = characteristicsProcess?.currentFinishSnr ?: 5.0
        return fromSnr to toSnr
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
    fun diffChannels(
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

    fun cancelCurrentProcess() {
        transmitSubscription?.let {
            it.dispose()
            storage.endCountingStatistics()
        }
        characteristicsProcess?.cancel()
    }

}