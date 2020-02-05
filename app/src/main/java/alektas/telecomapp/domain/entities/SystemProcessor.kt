package alektas.telecomapp.domain.entities

import alektas.telecomapp.App
import alektas.telecomapp.domain.entities.generators.ChannelCodesGenerator
import alektas.telecomapp.data.UserDataProvider
import alektas.telecomapp.domain.Repository
import alektas.telecomapp.domain.entities.coders.*
import alektas.telecomapp.domain.entities.configs.ChannelsConfig
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
    val characteristicsProcessState = BehaviorSubject.create<ProcessState>()
    private var disposable = CompositeDisposable()
    private var simulationSubscription: Disposable? = null
    private var transmitSubscription: Disposable? = null
    private var errorsCountingSubscription: Disposable? = null
    private var decodeSubscription: Disposable? = null
    private var characteristicsProcess: CalculateCharacteristicsProcess? = null
    private var sourceDataCodesType = DataCodesContract.HAMMING
    private var isDataCodingEnabled = DataCodesContract.DEFAULT_IS_CODING_ENABLED

    init {
        App.component.inject(this)
        App.component.channelsConfig().let { applyConfig(it) }
        App.component.decoderConfig().let { applyConfig(it) }

        noiseSnr?.let { setNoise(it) }
        interferenceRate?.let { r -> interferenceSparseness?.let { setInterference(it, r) } }

        disposable.addAll(
            storage.observeSimulationChannelsConfig()
                .subscribeOn(Schedulers.io())
                .subscribe { config ->
                    createChannels(
                        config.channelCount ?: CdmaContract.DEFAULT_CHANNEL_COUNT,
                        config.carrierFrequency ?: 1.0e-6 * QpskContract.DEFAULT_CARRIER_FREQUENCY,
                        config.dataSpeed ?: 1.0e-3 / QpskContract.DEFAULT_DATA_BIT_TIME,
                        config.channelsCodesType ?: CdmaContract.DEFAULT_CHANNEL_CODE_TYPE,
                        config.channelsCodesLength ?: CdmaContract.DEFAULT_CHANNEL_CODE_SIZE,
                        config.frameLength ?: CdmaContract.DEFAULT_FRAME_SIZE,
                        config.isDataCoding ?: DataCodesContract.DEFAULT_IS_CODING_ENABLED,
                        config.dataCodesType ?: DataCodesContract.HAMMING
                    )
                },

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
                    val isAuto =
                        config.isAutoDetection ?: CdmaContract.DEFAULT_IS_AUTO_DETECTION_ENABLED
                    val threshold = config.threshold ?: QpskContract.DEFAULT_SIGNAL_THRESHOLD
                    val isDataDecoding = config.isDataCoding ?: DataCodesContract.DEFAULT_IS_CODING_ENABLED
                    val dataCodesType = config.dataCodesType ?: DataCodesContract.HAMMING
                    val codeLength = config.channelsCodeLength ?: CdmaContract.DEFAULT_CHANNEL_CODE_SIZE
                    val channelsCodesType = config.channelsCodeType ?: CdmaContract.DEFAULT_CHANNEL_CODE_TYPE
                    if (isAuto) {
                        autoDecode(
                            data,
                            codeLength,
                            channelsCodesType,
                            threshold,
                            isDataDecoding,
                            dataCodesType
                        )
                    } else {
                        decodeChannels(
                            storage.getDecoderChannels(),
                            data,
                            threshold,
                            isDataDecoding,
                            dataCodesType
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
                storage.setTransmittingSubProcess(
                    ProcessState(
                        READ_FILE_KEY,
                        READ_FILE_NAME,
                        state = ProcessState.STARTED
                    )
                )
                val data =
                    ValueConverter(adcResolution).convertToBipolarNormalizedValues(dataString)
                storage.setTransmittingSubProcess(
                    ProcessState(
                        READ_FILE_KEY,
                        READ_FILE_NAME,
                        state = ProcessState.FINISHED
                    )
                )
                it.onSuccess(data)
            }
            .flatMapObservable { generateFrames(adcSamplingRate * 1.0e6, it) }
            .subscribeOn(Schedulers.io())
            .doOnSubscribe {
                storage.setTransmittingSubProcess(ProcessState(READ_FILE_KEY, READ_FILE_NAME))
                storage.startFileProcessingMode()
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
    }

    fun applyConfig(config: ChannelsConfig) {
        L.d(this, "Decoding: update simulation channels config")
        storage.updateSimulationChannelsConfig(config)
    }

    /**
     * Создание и установка каналов связи, которые затем можно использовать для хранения
     * и передачи фреймов.
     *
     * @param count количество создаваемых каналов
     * @param carrierFrequency частота несущей гармоники каналов, МГц
     * @param dataSpeed скорость передачи данных, кБит/с
     * @param channelsCodeType семейство кодов каналов, см. [ChannelCodesGenerator]
     * @param channelsCodeLength длина генерируемого уникального кода канала
     * @param frameLength длина фреймов (массивов данных)
     * @param isDataDecoding кодируются ли данные источника
     * @param dataCodesType семейство кодов источника данных, см. [DataCodesContract]
     */
    @SuppressLint("CheckResult")
    fun createChannels(
        count: Int,
        carrierFrequency: Double, // МГц
        dataSpeed: Double, // кБит/с
        channelsCodeType: Int,
        channelsCodeLength: Int,
        frameLength: Int,
        isDataDecoding: Boolean,
        dataCodesType: Int
    ) {
        this.isDataCodingEnabled = isDataDecoding
        this.sourceDataCodesType = dataCodesType

        simulationSubscription?.dispose()
        simulationSubscription =
            generateChannels(
                count,
                carrierFrequency,
                dataSpeed,
                channelsCodeType,
                channelsCodeLength,
                frameLength
            )
                .subscribeOn(Schedulers.computation())
                .observeOn(Schedulers.io())
                .subscribe { channels: List<Channel> ->
                    storage.setSimulatedChannels(channels)
                }
    }

    private fun generateChannels(
        count: Int,
        carrierFrequency: Double, // МГц
        dataSpeed: Double, // кБит/с
        channelsCodeType: Int,
        codeLength: Int,
        frameLength: Int
    ): Single<List<Channel>> {
        L.d(this, "Generate simulation channels")
        return Single.create<List<Channel>> {
            val channels = mutableListOf<Channel>()
            val channelCodesGen = ChannelCodesGenerator()
            val channelsCodes = when (channelsCodeType) {
                ChannelCodesGenerator.WALSH -> channelCodesGen.generateWalshMatrix(
                    max(
                        codeLength,
                        count
                    )
                )
                else -> channelCodesGen.generateRandomCodes(count, codeLength)
            }

            val bitTime = 1.0e-3 / dataSpeed // скорость в период бита (в секундах)

            for (i in 0 until count) {
                val channel = Channel(
                    name = "${i + 1}",
                    carrierFrequency = carrierFrequency * 1.0e6, // МГц -> Гц
                    frameLength = frameLength,
                    bitTime = bitTime,
                    code = channelsCodes[i],
                    channelCodeType = channelsCodeType
                )
                channels.add(channel)
            }

            // Так как продолжительность данных изменилась, обновляем время симуляции
            val dataTime = frameLength * channelsCodes[0].size * bitTime
            changeSimulationTime(dataTime)

            it.onSuccess(channels)
        }
    }

    private fun changeSimulationTime(time: Double) {
        if (Simulator.simulationTime == time) return

        L.d(this, "Change simulation time")

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
    fun transmitFrames(
        channels: List<Channel>,
        frameCount: Int
    ) {
        if (channels.isEmpty()) return

        cancelCurrentProcess()
        val coder = if (!isDataCodingEnabled) Repeater() else
            when (sourceDataCodesType) {
                DataCodesContract.HAMMING -> HammingCoder(
                    channels.firstOrNull()?.frameLength ?: CdmaContract.DEFAULT_FRAME_SIZE
                )
                else -> Repeater()
            }

        L.d(this, "Transmitting: start transmitting $frameCount frames")

        errorsCountingSubscription = storage.observeSimulatedChannels(withLast = false)
            .zipWith(storage.observeDecoderChannels(withLast = false)) { trans, rec ->
                diffChannelsData(trans, rec)
            }
            .subscribeOn(Schedulers.io())
            .subscribe {
                L.d(this, "Statistics: computed transmitted and received data difference")
                storage.setChannelsDataErrors(it)
            }

        transmitSubscription = Observable
            .create<List<Channel>> {
                for (i in 0 until frameCount) {
                    val chls = channels.map { c -> setupWithData(c, coder) }
                    it.onNext(chls)
                    try {
                        Thread.sleep(1000)
                    } catch (e: InterruptedException) {
                        cancelCurrentProcess()
                        it.onComplete()
                    }
                }
                it.onComplete()
            }
            .subscribeOn(Schedulers.io())
            .doOnSubscribe {
                storage.startCountingStatistics()
                storage.setExpectedFrameCount(frameCount)
            }
            .subscribe({
                storage.setSimulatedChannels(it) // передача очередного фрейма всеми каналами
            }, {
                it.printStackTrace()
            })
    }

    private fun setupWithData(channel: Channel, coder: DataCoder): Channel {
        val dataLength = if (coder is HammingCoder) coder.dataBitsInWord else channel.frameLength
        val data = UserDataProvider.generateData(dataLength)
        val codedData = coder.encode(data)
        return channel.copy().apply {
            frameData = codedData
            sourceData = data
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
        storage.removeSimulatedChannel(channel)
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
        L.d(this, "Demodulation: start frame demodulation")
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
        val isAuto = config.isAutoDetection ?: CdmaContract.DEFAULT_IS_AUTO_DETECTION_ENABLED
        if (!isAuto) {
            val chls = createDecoderChannels(
                config.channelCount ?: 0,
                config.channelsCodeLength ?: 0,
                config.channelsCodeType ?: CdmaContract.DEFAULT_CHANNEL_CODE_TYPE
            )
            storage.setDecoderChannels(chls)
        }
        L.d(this, "Decoding: update decoder config")
        storage.updateDecoderConfig(config)
    }

    fun addCustomDecoderChannel(code: BooleanArray) {
        val c = Channel(code = code).apply {
            name = "Custom-$id"
        }
        L.d(this, "Decoding: add custom channel")
        storage.addDecoderChannel(c)
    }

    private fun createDecoderChannels(
        count: Int,
        codeLength: Int,
        channelsCodeType: Int
    ): List<Channel> {
        val channels = mutableListOf<Channel>()
        val codeGen =
            ChannelCodesGenerator()
        val codes = when (channelsCodeType) {
            ChannelCodesGenerator.WALSH -> codeGen.generateWalshMatrix(max(codeLength, count))
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
        threshold: Float,
        isDataDecoding: Boolean,
        dataCodesType: Int
    ) {
        decodeSubscription = Single.create<List<Channel>> { emitter ->
            val decodedChannels = channels.map {
                it.copy().apply {
                    val frame = CdmaDecimalCoder(threshold).decode(code.toBipolar(), data)
                    val errors = mutableListOf<Int>()
                    frame.forEachIndexed { i, d -> if (d == 0.0) errors.add(i) }
                    this.errors = errors
                    this.frameData = frame.toUnipolar()
                    this.sourceData = extractData(frameData, isDataDecoding, dataCodesType)
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

    /**
     * Корреляционное автоопределение и декодирование каналов.
     */
    private fun autoDecode(
        data: DoubleArray,
        codeLength: Int,
        channelsCodeType: Int,
        threshold: Float,
        isDataDecoding: Boolean,
        dataCodesType: Int
    ) {
        decodeSubscription = Single.create<List<Channel>> { emitter ->
            val channels = mutableListOf<Channel>()
            val codeGen = ChannelCodesGenerator()
            val codes = when (channelsCodeType) {
                ChannelCodesGenerator.WALSH -> codeGen.generateWalshMatrix(codeLength)
                else -> codeGen.generateWalshMatrix(codeLength)
            }

            for (i in codes.indices) {
                val decoder = CdmaDecimalCoder(threshold)
                val code = codes[i].toBipolar()
                if (decoder.detectChannel(code, data)) {
                    val errors = mutableListOf<Int>()
                    val frame = decoder.decode(code, data).apply {
                        forEachIndexed { index, d -> if (d == 0.0) errors.add(index) }
                    }.toUnipolar()
                    val channel = Channel(
                        name = "${i + 1}",
                        frameData = frame,
                        sourceData = extractData(frame, isDataDecoding, dataCodesType),
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

    private fun extractData(
        frame: BooleanArray,
        isDataDecoding: Boolean,
        dataCodesType: Int
    ): BooleanArray {
        var frameData = frame
        if (isDataDecoding) {
            val coder = when (dataCodesType) {
                DataCodesContract.HAMMING -> HammingCoder(frame.size)
                else -> Repeater()
            }
            frameData = coder.decode(frameData)
        }
        return frameData
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
        val sourceConfig = storage.getSimulatedChannelsConfiguration()
        val demodConfig = storage.getCurrentDemodulatorConfig()
        val decoderConfig = storage.getDecoderConfiguration()
        val decodingChannels = storage.getDecoderChannels()

        storage.clearBerByNoiseList()
        storage.clearTheoreticBerByNoiseList()
        storage.clearCapacityByNoiseList()
        storage.clearDataSpeedByNoiseList()

        characteristicsProcess = CalculateCharacteristicsProcess(
            transmittingChannels,
            sourceConfig,
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

    fun cancelCurrentProcess() {
        errorsCountingSubscription?.dispose()
        transmitSubscription?.let {
            it.dispose()
            storage.endCountingStatistics()
        }
        characteristicsProcess?.cancel()
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
    fun diffChannelsFrames(
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

    private fun diffChannelsData(
        transmitted: List<Channel>,
        received: List<Channel>
    ): Map<BooleanArray, List<Int>> {
        val channelsErrorBits = mutableMapOf<BooleanArray, List<Int>>()

        if (transmitted.isEmpty()) {
            return channelsErrorBits
        }
        if (received.isEmpty()) {
            transmitted.forEach { channelsErrorBits[it.code] = it.sourceData.indices.toList() }
            return channelsErrorBits
        }

        for (transCh in transmitted) {
            val recCh = received.find { it.code.contentEquals(transCh.code) }
            if (recCh == null) {
                channelsErrorBits[transCh.code] = transCh.sourceData.indices.toList()
            } else {
                diff(transCh.sourceData, recCh.sourceData).let {
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