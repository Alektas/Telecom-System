package alektas.telecomapp.domain.entities.demodulators

import alektas.telecomapp.domain.entities.QpskContract
import alektas.telecomapp.domain.entities.Simulator
import alektas.telecomapp.domain.entities.generators.SignalGenerator
import alektas.telecomapp.domain.entities.signals.BaseSignal
import alektas.telecomapp.domain.entities.signals.DigitalSignal
import alektas.telecomapp.domain.entities.signals.Signal
import java.util.*

class QpskDemodulator : Demodulator<Signal> {

    /**
     * Демодуляция QPSK сигнала.
     *
     * @return двоичный биполярный сигнал из -1 и 1
     */
    override fun demodulate(signal: Signal): Signal {
        val gen = SignalGenerator()
        val cos = gen.cos(frequency = QpskContract.CARRIER_FREQUENCY)
        val sin = gen.sin(frequency = QpskContract.CARRIER_FREQUENCY)

        val dataI = extractBinaryData(signal * cos, Simulator.samplesFor(QpskContract.SYMBOL_TIME))
        val dataQ = extractBinaryData(signal * sin, Simulator.samplesFor(QpskContract.SYMBOL_TIME))

        val data = mutableListOf<Boolean>()
        dataI.forEachIndexed { i, bit ->
            data.add(bit)
            data.add(dataQ[i])
        }

        return DigitalSignal(data.toTypedArray(), QpskContract.DATA_BIT_TIME)
    }

    /**
     * Возвращает "созвездие" QPSK сигнала <code>signal</code> в виде массива попарно:
     * первое значение - I-компонента сигнала, второе - Q-компонента.
     */
    fun getConstellation(signal: Signal): List<Pair<Double, Double>> {
        val gen = SignalGenerator()
        val cos = gen.cos(frequency = QpskContract.CARRIER_FREQUENCY)
        val sin = gen.sin(frequency = QpskContract.CARRIER_FREQUENCY)

        val dataI = extractDigitalData(signal * cos, Simulator.samplesFor(QpskContract.SYMBOL_TIME))
        val dataQ = extractDigitalData(signal * sin, Simulator.samplesFor(QpskContract.SYMBOL_TIME))

        val data = mutableListOf<Pair<Double, Double>>()
        dataI.forEachIndexed { i, value ->
            data.add(Pair(value, dataQ[i]))
        }

        return data
    }

    private fun averageDigitalSignal(signal: Signal, interval: Int): Signal {
        val averageValues = mutableListOf<Double>()
        signal.getValues().toList()
            .chunked(interval) { it.average() }
            .forEach { value -> repeat(interval) { averageValues.add(value) } }

        val data = signal.getPoints().keys.zip(averageValues)

        val d = TreeMap<Double, Double>()
        d.putAll(data)

        return BaseSignal(d)
    }

    /**
     * Сглаживает сигнал и возвращает массив битов в соответствии с уровнем сигнала
     * на интервалах <code>interval</code>.
     * Если сигнал больше 0, то бит = 1
     * Если сигнал меньше или равен 0, то бит = 0
     */
    private fun extractBinaryData(signal: Signal, interval: Int): BooleanArray {
        return signal.getValues().toList()
            .chunked(interval) { it.average() }
            .map { it > 0 }
            .toBooleanArray()
    }

    /**
     * Сглаживает сигнал и возвращает массив значений сигнала на интервалах <code>interval</code>.
     */
    private fun extractDigitalData(signal: Signal, interval: Int): List<Double> {
        return signal.getValues().toList()
            .chunked(interval) { it.average() }
    }
}