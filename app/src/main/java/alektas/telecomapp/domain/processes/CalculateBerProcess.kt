package alektas.telecomapp.domain.processes

import alektas.telecomapp.App
import alektas.telecomapp.domain.Repository
import alektas.telecomapp.domain.entities.Channel
import alektas.telecomapp.domain.entities.coders.CdmaDecimalCoder
import alektas.telecomapp.domain.entities.coders.toBipolar
import alektas.telecomapp.domain.entities.configs.DemodulatorConfig
import alektas.telecomapp.domain.entities.contracts.QpskContract
import alektas.telecomapp.domain.entities.demodulators.QpskDemodulator
import alektas.telecomapp.domain.entities.signals.Signal
import alektas.telecomapp.domain.entities.signals.noises.Noise
import alektas.telecomapp.domain.entities.signals.noises.WhiteNoise
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.toFlowable
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

class CalculateBerProcess(
    private val signal: Signal,
    private val demodulatorConfig: DemodulatorConfig,
    private val channels: List<Channel>,
    private val threshold: Double
) {
    @Inject
    lateinit var storage: Repository
    private val disposable = CompositeDisposable()

    init {
        App.component.inject(this)
    }

    private fun createNoise(snr: Double): Noise {
        return WhiteNoise(snr, QpskContract.DEFAULT_SIGNAL_MAGNITUDE)
    }

    private fun createEther(signal: Signal, noise: Noise): Signal {
        return signal + noise
    }

    private fun demodulate(ether: Signal, config: DemodulatorConfig): DoubleArray {
        return QpskDemodulator(config).demodulateFrame(ether).dataValues
    }

    private fun decode(groupData: DoubleArray, code: BooleanArray, threshold: Double): DoubleArray {
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
        snr: Double,
        signal: Signal,
        demodulatorConfig: DemodulatorConfig,
        channels: List<Channel>,
        threshold: Double
    ): Pair<Double, Double> {
        val noise = createNoise(snr)
        val ether = createEther(signal, noise)
        val groupData = demodulate(ether, demodulatorConfig)
        val bitsWithErrors = channels.fold(0 to 0) { acc, channel ->
            val data = decode(groupData, channel.code, threshold)
            val bits = data.size
            val errors = countErrors(data)
            (acc.first + bits) to (acc.second + errors)
        }
        // вероятность битовой ошибки в процентах
        val ber = bitsWithErrors.second / bitsWithErrors.first.toDouble() * 100.0
        return snr to ber
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
                .parallel()
                .runOn(Schedulers.computation())
                .map { snr ->
                    calculateBer(snr, signal, demodulatorConfig, channels, threshold)
                }
                .sequential()
                .observeOn(Schedulers.io())
                .doOnSubscribe { progress(0) }
                .subscribe({
                    pointsCalculated++
                    val p = (pointsCalculated / snrs.size.toDouble() * 100).toInt()
                    progress(p)
                    storage.setBerByNoise(it)
                },
                    { progress(100) },
                    { progress(100) })
        )
    }

    fun cancel() {
        disposable.dispose()
    }
}