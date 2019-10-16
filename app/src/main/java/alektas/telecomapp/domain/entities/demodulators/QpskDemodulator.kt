package alektas.telecomapp.domain.entities.demodulators

import alektas.telecomapp.BuildConfig
import alektas.telecomapp.domain.entities.CdmaContract
import alektas.telecomapp.domain.entities.QpskContract
import alektas.telecomapp.domain.entities.Simulator
import alektas.telecomapp.domain.entities.generators.SignalGenerator
import alektas.telecomapp.domain.entities.signals.BaseSignal
import alektas.telecomapp.domain.entities.signals.DigitalSignal
import alektas.telecomapp.domain.entities.signals.Signal
import java.util.*
import kotlin.collections.HashMap
import kotlin.math.abs

class QpskDemodulator : Demodulator<Signal> {
    private val isBit: (Double) -> Boolean = { abs(it) > QpskContract.SIGNAL_THRESHOLD }

    /**
     * Демодуляция QPSK сигнала.
     *
     * @return двоичный биполярный сигнал из -1 и 1
     */
    override fun demodulate(signal: Signal): Signal {
        val gen = SignalGenerator()
        val cos = gen.cos(frequency = QpskContract.CARRIER_FREQUENCY)
        val sin = gen.sin(frequency = QpskContract.CARRIER_FREQUENCY)

        val dataI = extractBinaryData(
            signal * cos,
            0.0,
            QpskContract.SYMBOL_TIME * CdmaContract.SPREAD_DATA_LENGTH / 2.0,
            Simulator.samplesFor(QpskContract.SYMBOL_TIME),
            isBit
        ).first

        val dataQ = extractBinaryData(
            signal * sin,
            0.0,
            QpskContract.SYMBOL_TIME * CdmaContract.SPREAD_DATA_LENGTH / 2.0,
            Simulator.samplesFor(QpskContract.SYMBOL_TIME),
            isBit
        ).first

        val data = mutableListOf<Boolean>()
        dataI.forEachIndexed { i, bitI ->
            data.add(bitI)
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

        val dataI = extractDigitalData(
            signal * cos,
            0.0,
            QpskContract.SYMBOL_TIME * CdmaContract.SPREAD_DATA_LENGTH / 2.0,
            Simulator.samplesFor(QpskContract.SYMBOL_TIME)
        )

        val dataQ = extractDigitalData(
            signal * sin,
            0.0,
            QpskContract.SYMBOL_TIME * CdmaContract.SPREAD_DATA_LENGTH / 2.0,
            Simulator.samplesFor(QpskContract.SYMBOL_TIME)
        )

        val data = mutableListOf<Pair<Double, Double>>()
        dataI.forEachIndexed { i, value ->
            data.add(Pair(value, dataQ[i]))
        }

        return data
    }

    fun averageDigitalSignal(signal: Signal, interval: Int): Signal {
        val averageValues = mutableListOf<Double>()
        signal.getValues().toList()
            .windowed(interval, interval, false) { it.average() }
            .forEach { value -> repeat(interval) { averageValues.add(value) } }

        val data = signal.getPoints().keys.zip(averageValues)

        val d = TreeMap<Double, Double>()
        d.putAll(data)

        return BaseSignal(d)
    }

    /**
     * Сглаживает сигнал и возвращает массив битов в соответствии с уровнем сигнала
     * на интервалах <code>interval</code>.
     * Если сигнал больше порога {@link QpskContract#SIGNAL_THRESHOLD}, то бит = 1
     * Если сигнал меньше порога {@link QpskContract#SIGNAL_TRESHOLD_NEG}, то бит = 0
     * Если сигнал между пороговыми значениями, то информация отсутствует, бит не записывается.
     *
     * @param from с какого времени начинать извлекать информацию, в секундах
     * @param to до какого времени извлекать информацию, в секундах
     * @param interval с какой периодичностью усреднять информацию, в сэмплах
     */
    private fun extractBinaryData(
        signal: Signal,
        from: Double,
        to: Double,
        interval: Int,
        isBit: (Double) -> Boolean
    ): Pair<BooleanArray, Map<Int, Double>> {
        val errorBits = HashMap<Int, Double>()
        val bits = signal.getValues(from, to).toList()
            .also { if (BuildConfig.DEBUG) println("Signal values count: ${it.size}") }
            .windowed(interval, interval, false) { it.average() }
            .also {
                it.forEachIndexed { i, value -> if (!isBit(value)) errorBits[i] = value }
            }
            .map { it < 0 }
            .toBooleanArray()

        return Pair(bits, errorBits)
    }

    /**
     * Сглаживает сигнал и возвращает массив значений сигнала на интервалах <code>interval</code>.
     *
     * @param from с какого времени начинать извлекать информацию, в секундах
     * @param to до какого времени извлекать информацию, в секундах
     * @param interval с какой периодичностью усреднять информацию, в сэмплах
     */
    private fun extractDigitalData(
        signal: Signal,
        from: Double,
        to: Double,
        interval: Int
    ): List<Double> {
        return signal.getValues(from, to).toList()
            .windowed(interval, interval, false) { it.average() }
    }
}