package alektas.telecomapp.domain.processes

import alektas.telecomapp.App
import alektas.telecomapp.data.UserDataProvider
import alektas.telecomapp.domain.Repository
import alektas.telecomapp.domain.entities.Channel
import alektas.telecomapp.domain.entities.coders.CdmaDecimalCoder
import alektas.telecomapp.domain.entities.coders.toBipolar
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
    private val threshold: Float
) {
    @Inject
    lateinit var storage: Repository
    private val disposable = CompositeDisposable()

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
        progress: (Int) -> Unit = {}
    ) {
        val step = (toSnr - fromSnr) / pointsCount
        val snrs = DoubleArray(pointsCount) { fromSnr + it * step }
        var pointsCalculated = 0

        disposable.add(
            snrs.toFlowable()
                .map { snr ->
                    val ber = calculateBer(transmittingChannels, snr, demodulatorConfig, decodingChannels, threshold)
                    val capacity = calculateCapacity(snr, decodingChannels.first().bitTime)
                    Triple(snr, ber, capacity)
                }
                .subscribeOn(Schedulers.computation())
                .observeOn(Schedulers.io())
                .doOnSubscribe {
                    progress(0)
                }
                .subscribe({
                    pointsCalculated++
                    val p = (pointsCalculated / snrs.size.toDouble() * 100).toInt()
                    progress(p)
                    storage.setBerByNoise(it.first to it.second)
                    storage.setCapacityByNoise(it.first to it.third)
                }, {
                    progress(100)
                }, {
                    progress(100)
                })
        )
    }

    fun cancel() {
        disposable.dispose()
    }

    private fun createSignal(channels: List<Channel>): Signal {
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
        return QpskModulator(dataChannels[0].bitTime).modulate(carrier, groupData)
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

    private fun createNoise(snr: Double): Noise {
        return WhiteNoise(snr, QpskContract.DEFAULT_SIGNAL_POWER)
    }

    private fun createEther(signal: Signal, noise: Noise): Signal {
        return signal + noise
    }

    private fun demodulate(ether: Signal, config: DemodulatorConfig): DoubleArray {
        return QpskDemodulator(config).demodulateFrame(ether).dataValues
    }

    private fun decode(groupData: DoubleArray, code: BooleanArray, threshold: Float): DoubleArray {
        return CdmaDecimalCoder(threshold).decode(code.toBipolar(), groupData)
    }

    private fun countErrors(data: DoubleArray): Int {
        return data.count { it == 0.0 }
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
        decodingChannels: List<Channel>,
        threshold: Float
    ): Double {
        val signal = createSignal(transmittingChannels)
        val noise = createNoise(snr)
        val ether = createEther(signal, noise)
        val groupData = demodulate(ether, demodulatorConfig)
        val bitsWithErrors = decodingChannels.fold(0 to 0) { acc, channel ->
            val data = decode(groupData, channel.code, threshold)
            val bits = data.size
            val errors = countErrors(data)
            (acc.first + bits) to (acc.second + errors)
        }
        // вероятность битовой ошибки в процентах
        val ber = bitsWithErrors.second / bitsWithErrors.first.toDouble() * 100.0
        L.d(
            "Ber calculation",
            "Bits=${bitsWithErrors.first}, Errors=${bitsWithErrors.second}, BER=${ber}%, SNR=${snr}дБ"
        )
        return ber
    }

    private fun calculateCapacity(
        snr: Double,
        bitTime: Double
    ): Double {
        val bandwidth = 1 / bitTime
        val linearSnr = 10.0.pow(snr / 10)
        val capacity = bandwidth * log2(1 + linearSnr) * 1.0e-3 // кБит/с
        L.d(
            "Capacity calculation",
            "Bandwidth=${(bandwidth * 1.0e-3).format(3)}кГц, SNR=${snr.format(3)}дБ, linSNR=${linearSnr.format(
                3
            )}, Capacity=${capacity.format(3)}кБит/с"
        )
        return capacity
    }

}