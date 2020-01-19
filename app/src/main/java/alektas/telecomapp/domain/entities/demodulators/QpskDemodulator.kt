package alektas.telecomapp.domain.entities.demodulators

import alektas.telecomapp.domain.entities.Simulator
import alektas.telecomapp.domain.entities.configs.DemodulatorConfig
import alektas.telecomapp.domain.entities.filters.DummyFilter
import alektas.telecomapp.domain.entities.filters.Filter
import alektas.telecomapp.domain.entities.filters.FilterConfig
import alektas.telecomapp.domain.entities.filters.FirFilter
import alektas.telecomapp.domain.entities.generators.SignalGenerator
import alektas.telecomapp.domain.entities.signals.BaseSignal
import alektas.telecomapp.domain.entities.signals.DigitalSignal
import alektas.telecomapp.domain.entities.signals.Signal

class QpskDemodulator(config: DemodulatorConfig) : Demodulator<DigitalSignal> {
    private val filter: Filter = when (config.filterConfig.type) {
        FilterConfig.FIR -> FirFilter(config.filterConfig)
        else -> DummyFilter()
    }
    private val frameLength = config.frameLength
    private val codeLength = config.codeLength
    private val bitTime = config.bitTime
    private val symbolTime = bitTime * 2
    private val dataTime = bitTime * frameLength * codeLength
    private val timeOffset = bitTime * config.delayCompensation
    private val carrierFrequency = config.carrierFrequency
    var sigI: Signal = BaseSignal()
    var filteredSigI: Signal = BaseSignal()
    var sigQ: Signal = BaseSignal()
    var filteredSigQ: Signal = BaseSignal()

    /**
     * Демодуляция одного фрейма QPSK сигнала.
     * В процессе демодуляции в объекте демодулятора сохраняются промежуточные сигналы.
     *
     * @param signal ФМн-4 (QPSK) сигнал фрейма
     * @return цифровой сигнал
     */
    override fun demodulate(signal: Signal): DigitalSignal {
        if (signal.isEmpty()) return DigitalSignal(doubleArrayOf(), bitTime)

        val gen = SignalGenerator()
        val cos = gen.cos(frequency = carrierFrequency)
        val sin = gen.sin(frequency = carrierFrequency)
        sigI = signal * cos
        sigQ = signal * sin
        filteredSigI = filter.filter(sigI)
        filteredSigQ = filter.filter(sigQ)

        val dataI = integrate(
            filteredSigI,
            timeOffset,
            dataTime + timeOffset,
            Simulator.samplesFor(symbolTime)
        )

        val dataQ = integrate(
            filteredSigQ,
            timeOffset,
            dataTime + timeOffset,
            Simulator.samplesFor(symbolTime)
        )

        val data = mutableListOf<Double>()
        dataI.forEachIndexed { i, bitI ->
            data.add(bitI)
            data.add(dataQ[i])
        }

        return DigitalSignal(data.toDoubleArray(), bitTime)
    }

    /**
     * Сглаживает сигнал и возвращает массив значений сигнала на интервалах <code>interval</code>.
     *
     * @param from с какого времени начинать извлекать информацию, в секундах
     * @param to до какого времени извлекать информацию, в секундах
     * @param interval с какой периодичностью усреднять информацию, в сэмплах
     */
    private fun integrate(
        signal: Signal,
        from: Double,
        to: Double,
        interval: Int
    ): List<Double> {
        return signal.getValues(from, to).toList()
            .windowed(interval, interval, true) { it.average() }
    }

}