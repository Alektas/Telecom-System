package alektas.telecomapp.domain.processes

import alektas.telecomapp.App
import alektas.telecomapp.data.CodeGenerator
import alektas.telecomapp.data.UserDataProvider
import alektas.telecomapp.domain.Repository
import alektas.telecomapp.domain.entities.Channel
import alektas.telecomapp.domain.entities.coders.CdmaDecimalCoder
import alektas.telecomapp.domain.entities.coders.toBipolar
import alektas.telecomapp.domain.entities.configs.DecoderConfig
import alektas.telecomapp.domain.entities.configs.DemodulatorConfig
import alektas.telecomapp.domain.entities.contracts.QpskContract
import alektas.telecomapp.domain.entities.demodulators.QpskDemodulator
import alektas.telecomapp.domain.entities.generators.SignalGenerator
import alektas.telecomapp.domain.entities.modulators.QpskModulator
import alektas.telecomapp.domain.entities.signals.Signal
import alektas.telecomapp.domain.entities.signals.noises.Noise
import alektas.telecomapp.domain.entities.signals.noises.WhiteNoise
import alektas.telecomapp.utils.L
import alektas.telecomapp.utils.format
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.toFlowable
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject
import kotlin.math.log2
import kotlin.math.pow

class CalculateCharacteristicsProcess(
    private val transmittingChannels: List<Channel>,
    private val decodingChannels: List<Channel>,
    private val demodulatorConfig: DemodulatorConfig,
    private val decoderConfig: DecoderConfig
) {
    @Inject
    lateinit var storage: Repository
    private val disposable = CompositeDisposable()
    private val state = ProcessState(CHARACTERISTICS_KEY, CHARACTERISTICS_NAME)
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
                    state.removeSubStates()
                    val ber = calculateBer(
                        transmittingChannels,
                        snr,
                        demodulatorConfig,
                        decodingChannels,
                        decoderConfig
                    ) { progress(state.apply { setSubState(it) }) }
                    val capacity = calculateCapacity(
                        snr,
                        transmittingChannels.first().bitTime
                    ) { progress(state.apply { setSubState(it) }) }
                    Triple(snr, ber, capacity)
                }
                .subscribeOn(Schedulers.computation())
                .observeOn(Schedulers.io())
                .doOnSubscribe {
                    progress(state.apply {
                        state = ProcessState.STARTED
                    })
                }
                .doFinally {
                    progress(state.apply {
                        state = ProcessState.FINISHED
                        this.progress = 100
                    })
                }
                .subscribe({
                    pointsCalculated++
                    val p = (pointsCalculated / snrs.size.toDouble() * 100).toInt()
                    progress(state.apply {
                        this.progress = p
                    })
                    storage.setBerByNoise(it.first to it.second)
                    storage.setCapacityByNoise(it.first to it.third)
                }, {
                    it.printStackTrace()
                    progress(state.apply {
                        state = ProcessState.ERROR
                        this.progress = 100
                    })
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
        snr: Double,
        demodulatorConfig: DemodulatorConfig,
        decoderChannels: List<Channel>,
        decoderConfig: DecoderConfig,
        progress: (ProcessState) -> Unit
    ): Double {
        val state = ProcessState(BER_CALC_KEY, BER_CALC_NAME, ProcessState.STARTED)
        progress(state)

        val signal = createSignal(transmittingChannels) {
            progress(state.apply { setSubState(it) })
        }

        val noise = createNoise(snr) {
            progress(state.apply { setSubState(it) })
        }

        val ether = createEther(signal, noise) {
            progress(state.apply { setSubState(it) })
        }

        val groupData = demodulate(ether, demodulatorConfig) {
            progress(state.apply { setSubState(it) })
        }

        val channels = if (decoderConfig.isAutoDetection) {
            detectChannels(
                decoderConfig,
                groupData,
                decoderConfig.threshold ?: QpskContract.DEFAULT_SIGNAL_THRESHOLD
            ) { progress(state.apply { setSubState(it) }) }
        } else {
            decoderChannels
        }
        val bitsWithErrors = bitsAndErrors(channels, decoderConfig, groupData) {
            progress(state.apply { setSubState(it) })
        }
        // вероятность битовой ошибки в процентах
        val ber = bitsWithErrors.second / bitsWithErrors.first.toDouble() * 100.0
        L.d(
            "Ber calculation",
            "Bits=${bitsWithErrors.first}, Errors=${bitsWithErrors.second}, BER=${ber}%, SNR=${snr}дБ"
        )

        progress(state.apply { this.state = ProcessState.FINISHED })
        return ber
    }

    private fun calculateCapacity(
        snr: Double,
        bitTime: Double,
        progress: (ProcessState) -> Unit
    ): Double {
        progress(ProcessState(CAPACITY_CALC_KEY, CAPACITY_CALC_NAME, ProcessState.STARTED))

        val bandwidth = 1 / bitTime
        val linearSnr = 10.0.pow(snr / 10)
        val capacity = bandwidth * log2(1 + linearSnr) * 1.0e-3 // кБит/с
        L.d(
            "Capacity calculation",
            "Bandwidth=${(bandwidth * 1.0e-3).format(3)}кГц, SNR=${snr.format(3)}дБ, linSNR=${linearSnr.format(
                3
            )}, Capacity=${capacity.format(3)}кБит/с"
        )
        Thread.sleep(200)
        progress(ProcessState(CAPACITY_CALC_KEY, CAPACITY_CALC_NAME, ProcessState.FINISHED))
        return capacity
    }

    private fun createSignal(channels: List<Channel>, progress: (ProcessState) -> Unit): Signal {
        progress(ProcessState(CREATE_SIGNAL_KEY, CREATE_SIGNAL_NAME, ProcessState.STARTED))
        val dataChannels = channels.map { c ->
            val channel = c.copy()
            if (channel.frameData.isEmpty()) {
                channel.apply { frameData = UserDataProvider.generateData(frameLength) }
            } else {
                channel
            }
        }
        val groupData = aggregate(dataChannels)
        val carrier = SignalGenerator().cos(frequency = dataChannels[0].carrierFrequency)
        val signal = QpskModulator(dataChannels[0].bitTime).modulate(carrier, groupData)

        progress(ProcessState(CREATE_SIGNAL_KEY, CREATE_SIGNAL_NAME, ProcessState.FINISHED))
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
        progress(ProcessState(CREATE_NOISE_KEY, CREATE_NOISE_NAME, ProcessState.STARTED))
        val noise = WhiteNoise(snr, QpskContract.DEFAULT_SIGNAL_POWER)
        progress(ProcessState(CREATE_NOISE_KEY, CREATE_NOISE_NAME, ProcessState.FINISHED))

        return noise
    }

    private fun createEther(signal: Signal, noise: Noise, progress: (ProcessState) -> Unit): Signal {
        progress(ProcessState(CREATE_ETHER_KEY, CREATE_ETHER_NAME, ProcessState.STARTED))
        val ether = signal + noise
        progress(ProcessState(CREATE_ETHER_KEY, CREATE_ETHER_NAME, ProcessState.FINISHED))

        return ether
    }

    private fun demodulate(ether: Signal, config: DemodulatorConfig, progress: (ProcessState) -> Unit): DoubleArray {
        progress(ProcessState(DEMODULATE_KEY, DEMODULATE_NAME, ProcessState.STARTED))
        val demod = QpskDemodulator(config).demodulateFrame(ether).dataValues
        progress(ProcessState(DEMODULATE_KEY, DEMODULATE_NAME, ProcessState.FINISHED))

        return demod
    }

    private fun detectChannels(
        decoderConfig: DecoderConfig,
        groupData: DoubleArray,
        threshold: Float,
        progress: (ProcessState) -> Unit
    ): List<Channel> {
        progress(ProcessState(DETECT_CHANNELS_KEY, DETECT_CHANNELS_NAME, ProcessState.STARTED))

        val codeGen = CodeGenerator()
        val codes = when (decoderConfig.codeType) {
            CodeGenerator.WALSH -> codeGen.generateWalshMatrix(
                decoderConfig.codeLength ?: 0
            )
            else -> codeGen.generateWalshMatrix(decoderConfig.codeLength ?: 0)
        }
        val channels = mutableListOf<Channel>()
        for (i in codes.indices) {
            val decoder = CdmaDecimalCoder(threshold)
            val code = codes[i].toBipolar()
            if (decoder.detectChannel(code, groupData)) {
                channels.add(Channel(code = codes[i]))
            }
        }

        progress(ProcessState(DETECT_CHANNELS_KEY, DETECT_CHANNELS_NAME, ProcessState.FINISHED))
        return channels
    }

    private fun bitsAndErrors(
        channels: List<Channel>,
        decoderConfig: DecoderConfig,
        groupData: DoubleArray,
        progress: (ProcessState) -> Unit
    ): Pair<Int, Int> {
        progress(ProcessState(DECODE_KEY, DECODE_NAME, ProcessState.STARTED))
        val bitsAndErrors = channels.fold(0 to 0) { acc, channel ->
            val data = decode(
                groupData,
                channel.code,
                decoderConfig.threshold ?: QpskContract.DEFAULT_SIGNAL_THRESHOLD
            )
            val bits = data.size
            val errors = countErrors(data)
            (acc.first + bits) to (acc.second + errors)
        }
        progress(ProcessState(DECODE_KEY, DECODE_NAME, ProcessState.FINISHED))

        return bitsAndErrors
    }

    private fun decode(
        groupData: DoubleArray,
        code: BooleanArray,
        threshold: Float
    ): DoubleArray {
        return CdmaDecimalCoder(threshold).decode(code.toBipolar(), groupData)
    }

    private fun countErrors(data: DoubleArray): Int {
        return data.count { it == 0.0 }
    }

}