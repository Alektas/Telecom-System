package alektas.telecomapp.domain.processes

import alektas.telecomapp.App
import alektas.telecomapp.domain.entities.generators.ChannelCodesGenerator
import alektas.telecomapp.data.UserDataProvider
import alektas.telecomapp.domain.Repository
import alektas.telecomapp.domain.entities.Channel
import alektas.telecomapp.domain.entities.SystemProcessor
import alektas.telecomapp.domain.entities.coders.*
import alektas.telecomapp.domain.entities.configs.ChannelsConfig
import alektas.telecomapp.domain.entities.configs.DecoderConfig
import alektas.telecomapp.domain.entities.configs.DemodulatorConfig
import alektas.telecomapp.domain.entities.contracts.CdmaContract
import alektas.telecomapp.domain.entities.contracts.QpskContract
import alektas.telecomapp.domain.entities.demodulators.QpskDemodulator
import alektas.telecomapp.domain.entities.generators.SignalGenerator
import alektas.telecomapp.domain.entities.modulators.QpskModulator
import alektas.telecomapp.domain.entities.signals.Signal
import alektas.telecomapp.domain.entities.signals.noises.Noise
import alektas.telecomapp.domain.entities.signals.noises.WhiteNoise
import alektas.telecomapp.utils.L
import alektas.telecomapp.utils.Math
import alektas.telecomapp.utils.format
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.toFlowable
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject
import kotlin.math.*

class CalculateCharacteristicsProcess(
    private val transmittingChannels: List<Channel>,
    private val sourceConfig: ChannelsConfig,
    private val decodingChannels: List<Channel>,
    private val demodulatorConfig: DemodulatorConfig,
    private val decoderConfig: DecoderConfig
) {
    @Inject
    lateinit var storage: Repository
    @Inject
    lateinit var processor: SystemProcessor
    private val disposable = CompositeDisposable()
    private val state = ProcessState(CHARACTERISTICS_KEY, CHARACTERISTICS_NAME)
    private val berState = ProcessState(BER_CALC_KEY, BER_CALC_NAME)
    private val theoreticBerState = ProcessState(THEORETIC_BER_CALC_KEY, THEORETIC_BER_CALC_NAME)
    private val capacityState = ProcessState(CAPACITY_CALC_KEY, CAPACITY_CALC_NAME)
    private val dataSpeedState = ProcessState(DATA_SPEED_CALC_KEY, DATA_SPEED_CALC_NAME)
    private val threshold = decoderConfig.threshold ?: QpskContract.DEFAULT_SIGNAL_THRESHOLD
    var currentStartSnr: Double? = null
    var currentFinishSnr: Double? = null

    init {
        App.component.inject(this)
    }

    /**
     * Запуск многопоточной задачи.
     *
     * @param progress коллбэк прогресса выполнения задачи (значения прогресса: от 0 до 100)
     */
    fun execute(
        fromSnr: Double,
        toSnr: Double,
        pointsCount: Int,
        progress: (ProcessState) -> Unit = {}
    ) {
        currentStartSnr = fromSnr
        currentFinishSnr = toSnr

        val step = (toSnr - fromSnr) / pointsCount
        val snrs = DoubleArray(pointsCount) { fromSnr + it * step }
        var pointsCalculated = 0

        disposable.add(
            snrs.toFlowable()
                .map { snr ->
                    progress(state.withResetedSubStates())

                    val frameLength = transmittingChannels.firstOrNull()?.frameLength ?: CdmaContract.DEFAULT_FRAME_SIZE
                    val isDataCoding = sourceConfig.isDataCoding ?: DataCodesContract.DEFAULT_IS_CODING_ENABLED
                    val coderType = sourceConfig.dataCodesType ?: DataCodesContract.HAMMING
                    val coder = buildCoder(frameLength, isDataCoding, coderType)

                    val ber = calculateBer(
                        transmittingChannels,
                        coder,
                        snr,
                        demodulatorConfig,
                        decodingChannels,
                        decoderConfig
                    ) { progress(state.withSubState(it)) }


                    val codeRate = if (coder is HammingCoder) coder.rate else 1.0f
                    val dataSpeed =
                        calculateDataSpeed(transmittingChannels.first().bitTime, ber, codeRate) {
                            progress(state.withSubState(it))
                        }

                    val theoreticBer =
                        calculateTheoreticBer(snr) { progress(state.withSubState(it)) }

                    val capacity = calculateCapacity(
                        snr,
                        transmittingChannels.first().bitTime
                    ) { progress(state.withSubState(it)) }

                    SystemCharacteristics(snr, ber, theoreticBer, capacity, dataSpeed)
                }
                .subscribeOn(Schedulers.computation())
                .observeOn(Schedulers.io())
                .doOnSubscribe {
                    progress(state.withState(ProcessState.STARTED))
                }
                .doFinally {
                    progress(state.with(ProcessState.FINISHED, 100))
                }
                .subscribe({
                    pointsCalculated++
                    val p = (pointsCalculated / snrs.size.toDouble() * 100).toInt()
                    progress(state.withProgress(p))
                    storage.setBerByNoise(it.snr to it.ber)
                    storage.setTheoreticBerByNoise(it.snr to it.theoreticBer)
                    storage.setCapacityByNoise(it.snr to it.capacity)
                    storage.setDataSpeedByNoise(it.snr to it.dataSpeed)
                }, {
                    it.printStackTrace()
                    progress(state.with(ProcessState.ERROR, 100))
                })
        )
    }

    fun cancel() {
        disposable.dispose()
    }

    /**
     * Расчет вероятности битовой ошибки (BER) при отношении сигнал/шум (С/Ш)
     *
     * @return ключ - отношение С/Ш, значение - расчитанная BER в процентах
     */
    private fun calculateBer(
        transmittingChannels: List<Channel>,
        coder: DataCoder,
        snr: Double,
        demodulatorConfig: DemodulatorConfig,
        decoderChannels: List<Channel>,
        decoderConfig: DecoderConfig,
        progress: (ProcessState) -> Unit
    ): Double {
        progress(berState.withState(ProcessState.STARTED))

        val dataLength =
            if (coder is HammingCoder) {
                coder.dataBitsInWord
            } else {
                transmittingChannels.first().frameLength
            }
        val dataChannels = generateData(transmittingChannels, dataLength) {
            progress(berState.withSubState(it))
        }

        val codedDataChannels = encodeData(dataChannels, coder) {
            progress(berState.withSubState(it))
        }

        val signal = createSignal(codedDataChannels) {
            progress(berState.withSubState(it))
        }

        val noise = createNoise(snr) {
            progress(berState.withSubState(it))
        }

        val ether = createEther(signal, noise) {
            progress(berState.withSubState(it))
        }

        val groupData = demodulate(ether, demodulatorConfig) {
            progress(berState.withSubState(it))
        }

        val isAuto = decoderConfig.isAutoDetection ?: CdmaContract.DEFAULT_IS_AUTO_DETECTION_ENABLED
        val channels = if (isAuto) {
            detectChannels(
                decoderConfig,
                groupData,
                threshold
            ) { progress(berState.withSubState(it)) }
        } else {
            decoderChannels
        }

        val decodeState = ProcessState(DECODE_KEY, DECODE_NAME, ProcessState.STARTED)
        progress(berState.withSubState(decodeState))
        val isDataDecoding = decoderConfig.isDataCoding ?: DataCodesContract.DEFAULT_IS_CODING_ENABLED
        val decoderType = decoderConfig.dataCodesType ?: DataCodesContract.HAMMING
        val frameLength = channels.firstOrNull()?.frameLength ?: CdmaContract.DEFAULT_FRAME_SIZE
        val decoder = buildCoder(frameLength, isDataDecoding, decoderType)
        val decodedChannels = decodeChannels(
            channels,
            groupData,
            threshold,
            decoder
        )
        progress(berState.withSubState(decodeState.withState(ProcessState.FINISHED)))

        val errorsState = ProcessState(FIND_ERRORS_KEY, FIND_ERRORS_NAME, ProcessState.STARTED)
        progress(berState.withSubState(errorsState))
        val bitCount = decodedChannels.fold(0) { acc, c ->
            acc + c.frameData.size
        }
        val errorMap = processor.diffChannelsFrames(dataChannels, decodedChannels)
        val errorCount = errorMap.values.flatten().size
        progress(berState.withSubState(errorsState.withState(ProcessState.FINISHED)))

        // вероятность битовой ошибки в процентах
        val ber = errorCount / bitCount.toDouble() * 100.0
        L.d(
            "Ber calculation",
            "Bits=${bitCount}, Errors=${errorCount}, BER=${ber}%, SNR=${snr}дБ"
        )

        progress(berState.withState(ProcessState.FINISHED))
        return ber
    }

    private fun buildCoder(frameLength: Int, isDataCoding: Boolean, coderType: Int): DataCoder {
        return if (isDataCoding && coderType == DataCodesContract.HAMMING) {
            HammingCoder(frameLength)
        } else {
            Repeater()
        }
    }

    private fun calculateTheoreticBer(
        snr: Double,
        progress: (ProcessState) -> Unit
    ): Double {
        progress(theoreticBerState.withState(ProcessState.STARTED))

        val linearSnr = 10.0.pow(snr / 10)
        val f = sqrt(2 * linearSnr)
        val ber = 100 / sqrt(2 * PI) * Math.integrate(f, 100.0, 100) { exp(-0.5 * it.pow(2)) }

        L.d(
            "Theoretic BER calculation",
            "SNR=${snr.format(3)}дБ, linSNR=${linearSnr.format(3)}, BER=${ber.format(6)}%"
        )

        progress(theoreticBerState.withState(ProcessState.FINISHED))
        return ber
    }

    private fun calculateCapacity(
        snr: Double,
        bitTime: Double,
        progress: (ProcessState) -> Unit
    ): Double {
        progress(capacityState.withState(ProcessState.STARTED))

        val bandwidth = 1 / bitTime
        val linearSnr = 10.0.pow(snr / 10)
        val capacity = bandwidth * log2(1 + linearSnr) * 1.0e-3 // кБит/с
        L.d(
            "Capacity calculation",
            "Bandwidth=${(bandwidth * 1.0e-3).format(3)}кГц, SNR=${snr.format(3)}дБ, linSNR=${linearSnr.format(
                3
            )}, Capacity=${capacity.format(3)}кБит/с"
        )

        progress(capacityState.withState(ProcessState.FINISHED))
        return capacity
    }

    /**
     * @param bitTime время бита, сек
     * @param ber верятность битовой ошибки, %
     */
    private fun calculateDataSpeed(
        bitTime: Double,
        ber: Double,
        codeRate: Float,
        progress: (ProcessState) -> Unit
    ): Double {
        progress(dataSpeedState.withState(ProcessState.STARTED))

        val dataSpeed = codeRate * 1.0e-3 / bitTime * (100 - ber) / 100
        L.d(
            "Data speed calculation",
            "BER=${ber.format(3)}%, Data speed=${dataSpeed.format(3)}кБит/с"
        )

        progress(dataSpeedState.withState(ProcessState.FINISHED))
        return dataSpeed
    }

    private fun generateData(
        channels: List<Channel>,
        dataLength: Int,
        progress: (ProcessState) -> Unit
    ): List<Channel> {
        val state = ProcessState(GENERATE_DATA_KEY, GENERATE_DATA_NAME, ProcessState.STARTED)
        progress(state)
        val dataChannels = channels.map { c ->
            c.copy().apply {
                frameData = UserDataProvider.generateData(dataLength)
            }
        }
        progress(state.withState(ProcessState.FINISHED))
        return dataChannels
    }

    private fun encodeData(
        channels: List<Channel>,
        coder: DataCoder,
        progress: (ProcessState) -> Unit
    ): List<Channel> {
        val state = ProcessState(ENCODE_DATA_KEY, ENCODE_DATA_NAME, ProcessState.STARTED)
        progress(state)
        val dataChannels = channels.map { c ->
            c.copy().apply { frameData = coder.encode(frameData) }
        }
        progress(state.withState(ProcessState.FINISHED))
        return dataChannels
    }

    private fun createSignal(channels: List<Channel>, progress: (ProcessState) -> Unit): Signal {
        val state = ProcessState(CREATE_SIGNAL_KEY, CREATE_SIGNAL_NAME, ProcessState.STARTED)
        progress(state)
        val groupData = aggregate(channels)
        val carrier = SignalGenerator().cos(frequency = channels[0].carrierFrequency)
        val signal = QpskModulator(channels[0].bitTime).modulate(carrier, groupData)

        progress(state.withState(ProcessState.FINISHED))
        return signal
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

    private fun createNoise(snr: Double, progress: (ProcessState) -> Unit): Noise {
        val state = ProcessState(CREATE_NOISE_KEY, CREATE_NOISE_NAME, ProcessState.STARTED)
        progress(state)
        val bitTime = try {
            storage.getSimulatedChannels().first().bitTime
        } catch (e: NoSuchElementException) {
            QpskContract.DEFAULT_DATA_BIT_TIME
        }
        val bitEnergy = QpskContract.DEFAULT_SIGNAL_POWER * bitTime
        val noise = WhiteNoise(snr, bitEnergy)
        progress(state.withState(ProcessState.FINISHED))
        return noise
    }

    private fun createEther(
        signal: Signal,
        noise: Noise,
        progress: (ProcessState) -> Unit
    ): Signal {
        val state = ProcessState(CREATE_ETHER_KEY, CREATE_ETHER_NAME, ProcessState.STARTED)
        progress(state)
        val ether = signal + noise
        progress(state.withState(ProcessState.FINISHED))
        return ether
    }

    private fun demodulate(
        ether: Signal,
        config: DemodulatorConfig,
        progress: (ProcessState) -> Unit
    ): DoubleArray {
        val state = ProcessState(DEMODULATE_KEY, DEMODULATE_NAME, ProcessState.STARTED)
        progress(state)
        val demod = QpskDemodulator(config).demodulate(ether).dataValues
        progress(state.withState(ProcessState.FINISHED))
        return demod
    }

    private fun detectChannels(
        decoderConfig: DecoderConfig,
        groupData: DoubleArray,
        threshold: Float,
        progress: (ProcessState) -> Unit
    ): List<Channel> {
        val state = ProcessState(DETECT_CHANNELS_KEY, DETECT_CHANNELS_NAME, ProcessState.STARTED)
        progress(state)

        val codeGen = ChannelCodesGenerator()
        val codes = when (decoderConfig.channelsCodeType) {
            ChannelCodesGenerator.WALSH -> codeGen.generateWalshMatrix(
                decoderConfig.channelsCodeLength ?: 0
            )
            else -> codeGen.generateWalshMatrix(decoderConfig.channelsCodeLength ?: 0)
        }
        val channels = mutableListOf<Channel>()
        for (i in codes.indices) {
            val decoder = CdmaDecimalCoder(threshold)
            val code = codes[i].toBipolar()
            if (decoder.detectChannel(code, groupData)) {
                channels.add(Channel(code = codes[i]))
            }
        }

        progress(state.withState(ProcessState.FINISHED))
        return channels
    }

    private fun decodeChannels(
        channels: List<Channel>,
        groupData: DoubleArray,
        threshold: Float,
        coder: DataCoder
    ): List<Channel> {
        return channels.map {
            it.apply {
                val frame = decodeChannel(groupData, it.code, threshold)
                val frameData = decodeFrame(frame, coder)
                this.frameData = frameData
            }
        }
    }

    private fun decodeChannel(
        groupData: DoubleArray,
        code: BooleanArray,
        threshold: Float
    ): DoubleArray {
        return CdmaDecimalCoder(threshold).decode(code.toBipolar(), groupData)
    }

    private fun decodeFrame(
        frame: DoubleArray,
        coder: DataCoder
    ): BooleanArray {
        return coder.decode(frame.toUnipolar())
    }

    data class SystemCharacteristics(
        val snr: Double,
        val ber: Double,
        val theoreticBer: Double,
        val capacity: Double,
        val dataSpeed: Double
    )

}