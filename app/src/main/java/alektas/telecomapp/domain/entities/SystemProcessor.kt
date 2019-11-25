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
import alektas.telecomapp.domain.entities.demodulators.QpskDemodulator
import alektas.telecomapp.domain.entities.generators.SignalGenerator
import alektas.telecomapp.domain.entities.modulators.QpskModulator
import alektas.telecomapp.domain.entities.signals.BaseSignal
import alektas.telecomapp.domain.entities.signals.DigitalSignal
import alektas.telecomapp.domain.entities.signals.Signal
import alektas.telecomapp.domain.entities.signals.noises.Noise
import alektas.telecomapp.domain.entities.signals.noises.WhiteNoise
import android.annotation.SuppressLint
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import io.reactivex.rxkotlin.toObservable
import io.reactivex.rxkotlin.zipWith
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import javax.inject.Inject
import javax.inject.Named
import kotlin.math.max

private const val BER_POINTS_COUNT = 20

class SystemProcessor {
    @Inject
    lateinit var storage: Repository
    private var codedGroupData: DoubleArray? = null
    private var transmittedChannels: List<ChannelData>? = null
    private var decodedChannels: List<ChannelData>? = null
    @JvmField
    @field:[Inject Named("sourceSnr")]
    var noiseSnr: Double? = null
    private var decodingThreshold = QpskContract.DEFAULT_SIGNAL_THRESHOLD
    private var disposable = CompositeDisposable()
    val berProcess = BehaviorSubject.create<Int>()

    init {
        App.component.inject(this)

        if (noiseSnr != null) {
            setNoise(noiseSnr ?: QpskContract.DEFAULT_SIGNAL_NOISE_RATE)
        }

        disposable.addAll(
            storage.observeChannels()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    transmittedChannels = it
                    generateChannelsSignal(it)
                },

            storage.observeDemodulatorConfig()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { demodulate(it) },

            storage.observeDemodulatedSignal()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { s ->
                    codedGroupData = s.dataValues
                    decodedChannels?.let { updateDecodedChannels(it, s.dataValues) }
                },

            storage.observeDecodedChannels()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    decodedChannels = it
                },

            storage.observeNoise()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { noiseSnr = it.snr() },

            Observable.combineLatest(
                storage.observeChannels(),
                storage.observeDecodedChannels(),
                BiFunction<List<ChannelData>, List<ChannelData>, Map<BooleanArray, List<Int>>> { origin, decoded ->
                    diffChannels(origin, decoded)
                })
                .subscribeOn(Schedulers.computation())
                .subscribe { storage.setSimulatedChannelsErrors(it) }
        )
    }

    fun setAdcFrequency(frequency: Double) {
        val freq = frequency * 1.0e6 // МГц -> Гц
        Simulator.samplingRate = freq
        noiseSnr?.let { n -> setNoise(n) }
        val filterConfig = storage.getDemodulatorFilterConfig().apply { samplingRate = freq }
        storage.setDemodulatorFilterConfig(filterConfig)
        transmittedChannels?.let { generateChannelsSignal(it) }
    }

    @SuppressLint("CheckResult")
    fun generateChannels(
        count: Int,
        carrierFrequency: Double, // МГц
        dataSpeed: Double, // кБит/с
        codeLength: Int,
        frameLength: Int,
        codesType: Int = CodeGenerator.WALSH
    ) {
        Single.create<List<ChannelData>> {
            val channels = mutableListOf<ChannelData>()
            val codeGen = CodeGenerator()
            val codes = when (codesType) {
                CodeGenerator.WALSH -> codeGen.generateWalshMatrix(max(codeLength, count))
                else -> codeGen.generateRandomCodes(count, codeLength)
            }

            val bitTime = 1.0e-3 / dataSpeed // скорость в период бита (в секундах)

            for (i in 0 until count) {
                val frameData = UserDataProvider.generateData(frameLength)
                val channel = ChannelData(
                    "${i + 1}",
                    carrierFrequency * 1.0e6, // МГц -> Гц
                    frameData,
                    bitTime,
                    codes[i],
                    codesType
                )
                channels.add(channel)
            }

            val dataTime = frameLength * codes[0].size * bitTime
            Simulator.simulationTime = dataTime
            noiseSnr?.let { n -> setNoise(n) }

            it.onSuccess(channels)
        }
            .subscribeOn(Schedulers.computation())
            .subscribe { channels: List<ChannelData> -> storage.setChannels(channels) }
    }

    @SuppressLint("CheckResult")
    fun generateChannelsSignal(channels: List<ChannelData>) {
        if (channels.isEmpty()) {
            storage.setChannelsSignal(BaseSignal())
            return
        }

        Single.create<Signal> {
            val groupData = aggregate(channels)

            val carrier = SignalGenerator().cos(frequency = channels[0].carrierFrequency)
            val signal = QpskModulator(channels[0].bitTime).modulate(carrier, groupData)

            it.onSuccess(signal)
        }
            .subscribeOn(Schedulers.computation())
            .subscribe { signal: Signal -> storage.setChannelsSignal(signal) }
    }

    private fun aggregate(channels: List<ChannelData>): DoubleArray {
        return channels
            .map { channel ->
                // Преобразование однополярных двоичных данных в биполярные
                val code = channel.code.toBipolar()
                val data = channel.data.toBipolar()
                CdmaDecimalCoder().encode(code, data)
            }
            .reduce { acc, data ->
                data.mapIndexed { i, value -> acc[i] + value }.toDoubleArray()
            }
    }

    fun removeChannel(channel: ChannelData) {
        storage.removeChannel(channel)
    }

    @SuppressLint("CheckResult")
    fun setNoise(snr: Double, singleThread: Boolean = false) {
        Single.create<Noise> {
            val noise = WhiteNoise(snr, QpskContract.DEFAULT_SIGNAL_MAGNITUDE)
            it.onSuccess(noise)
        }
            .subscribeOn(if (singleThread) Schedulers.single() else Schedulers.computation())
            .subscribe { noise: Noise -> storage.setNoise(noise) }
    }

    fun disableNoise() {
        storage.disableNoise()
    }


    fun enableNoise() {
        storage.enableNoise(true)
    }

    @SuppressLint("CheckResult")
    fun demodulate(config: DemodulatorConfig) {
        val demodulator = QpskDemodulator(config)

        Single.create<DigitalSignal> {
            val demodSignal = demodulator.demodulate(config.inputSignal)
            it.onSuccess(demodSignal)
        }
            .subscribeOn(Schedulers.computation())
            .subscribe { signal: DigitalSignal ->
                storage.setDemodulatedSignal(signal)
                storage.setChannelI(demodulator.sigI)
                storage.setFilteredChannelI(demodulator.filteredSigI)
                storage.setChannelQ(demodulator.sigQ)
                storage.setFilteredChannelQ(demodulator.filteredSigQ)
                storage.setDemodulatedSignalConstellation(demodulator.constellation)
            }
    }

    fun updateDecodedChannels(channels: List<ChannelData>, groupData: DoubleArray) {
        val newChannels = mutableListOf<ChannelData>()

        for (c in channels) {
            val frameData =
                CdmaDecimalCoder(decodingThreshold).decode(c.code.toBipolar(), groupData)
            val errors = mutableListOf<Int>()
            frameData.forEachIndexed { i, d -> if (d == 0.0) errors.add(i) }
            c.errors = errors
            c.data = frameData.toUnipolar()
            newChannels.add(c)
        }

        storage.setDecodedChannels(channels)
    }

    fun addDecodedChannel(code: BooleanArray, threshold: Float) {
        decodingThreshold = threshold.toDouble()

        codedGroupData?.let {
            val frameData = CdmaDecimalCoder(decodingThreshold).decode(code.toBipolar(), it)
            val errors = mutableListOf<Int>()
            frameData.forEachIndexed { i, d -> if (d == 0.0) errors.add(i) }
            val channel = ChannelData(data = frameData.toUnipolar(), code = code)
            channel.errors = errors
            storage.addDecodedChannel(channel)
        }
    }

    @SuppressLint("CheckResult")
    fun setDecodedChannels(count: Int, codeLength: Int, codesType: Int, threshold: Float) {
        decodingThreshold = threshold.toDouble()

        Single.create<List<ChannelData>> { emitter ->
            codedGroupData?.let {
                val channels = mutableListOf<ChannelData>()
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
                    val channel = ChannelData(data = frameData.toUnipolar(), code = codes[i])
                    channel.errors = errors
                    channels.add(channel)
                }

                emitter.onSuccess(channels)
            }
        }
            .subscribeOn(Schedulers.computation())
            .subscribe { channels: List<ChannelData> -> storage.setDecodedChannels(channels) }
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
        transmitted: List<ChannelData>,
        received: List<ChannelData>
    ): Map<BooleanArray, List<Int>> {
        val channelsErrorBits = mutableMapOf<BooleanArray, List<Int>>()

        if (transmitted.isEmpty()) {
            return channelsErrorBits
        }
        if (received.isEmpty()) {
            transmitted.forEach { channelsErrorBits[it.code] = it.data.indices.toList() }
            return channelsErrorBits
        }

        for (transCh in transmitted) {
            val recCh = received.find { it.code.contentEquals(transCh.code) }
            if (recCh == null) {
                channelsErrorBits[transCh.code] = transCh.data.indices.toList()
            } else {
                diff(transCh.data, recCh.data).let {
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

    fun calculateBer(fromSnr: Double, toSnr: Double) {
        val step = (toSnr - fromSnr) / BER_POINTS_COUNT
        val snrs = DoubleArray(BER_POINTS_COUNT) { fromSnr + it * step }
        val isNoiseWasEnabled = storage.isNoiseEnabled()

        disposable.add(snrs.toObservable()
            .skip(1) // Первое SNR вручную запускается в doOnSubscribe, поэтому пропускаем
            .zipWith(storage.observeBer()) { snr, _ -> snr } // Ждем вычисления BER, затем запускаем следующее SNR
            .doOnSubscribe {
                berProcess.onNext(0)
                if (!isNoiseWasEnabled) storage.enableNoise(false)
                setNoise(fromSnr, true)
            }
            .subscribe({
                val index = snrs.indexOf(it)
                if (index > 0) {
                    val progress = (index / BER_POINTS_COUNT.toDouble() * 100).toInt()
                    berProcess.onNext(progress)
                }
                setNoise(it, true)
            }, {
                berProcess.onNext(100)
            }, {
                if (!isNoiseWasEnabled) disableNoise() // Восстановить исходное состояние
                berProcess.onNext(100)
            })
        )
    }

}