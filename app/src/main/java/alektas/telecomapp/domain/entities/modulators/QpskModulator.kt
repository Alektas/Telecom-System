package alektas.telecomapp.domain.entities.modulators

import alektas.telecomapp.domain.entities.QpskContract
import alektas.telecomapp.domain.entities.signals.BinarySignal
import alektas.telecomapp.domain.entities.signals.HarmonicSignal
import alektas.telecomapp.domain.entities.signals.Signal

class QpskModulator : Modulator<BooleanArray> {

    /**
     * @param carrier несущая гармоника. Для соответствия общепринятой конвенциии необходимо
     * использовать сигнал вида косинус
     * @param data массив битов данных
     */
    override fun modulate(carrier: HarmonicSignal, data: BooleanArray): Signal {
        val dataI = data.filterIndexed { index, _ -> index % 2 == 0 }.toBooleanArray()
        val signalI = BinarySignal(dataI, QpskContract.SYMBOL_TIME)

        val dataQ = data.filterIndexed { index, _ -> index % 2 != 0 }.toBooleanArray()
        val signalQ = BinarySignal(dataQ, QpskContract.SYMBOL_TIME)

        return carrier * signalI - carrier.shiftPhaseBy(Math.PI / 2) * signalQ
    }

    /**
     * @param carrier несущая гармоника. Для соответствия общепринятой конвенциии необходимо
     * использовать сигнал вида косинус
     * @param data два массива с квадратурными составляющими: первый - I, второй - Q
     */
    fun modulate(carrier: HarmonicSignal, data: Pair<BooleanArray, BooleanArray>): Signal {
        val signalI = BinarySignal(data.first, QpskContract.SYMBOL_TIME)
        val signalQ = BinarySignal(data.second, QpskContract.SYMBOL_TIME)

        return carrier * signalI - carrier.shiftPhaseBy(Math.PI / 2) * signalQ
    }
}