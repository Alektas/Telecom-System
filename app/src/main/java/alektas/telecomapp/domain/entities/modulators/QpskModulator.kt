package alektas.telecomapp.domain.entities.modulators

import alektas.telecomapp.domain.entities.QpskContract
import alektas.telecomapp.domain.entities.signals.BinarySignal
import alektas.telecomapp.domain.entities.signals.HarmonicSignal
import alektas.telecomapp.domain.entities.signals.Signal

class QpskModulator : Modulator<Array<Boolean>> {

    /**
     * @param carrier несущая гармоника. Для соответствия общепринятой конвенциии необходимо
     * использовать сигнал вида косинус
     * @param data массив битов данных
     */
    override fun modulate(carrier: HarmonicSignal, data: Array<Boolean>): Signal {
        val dataI = data.filterIndexed { index, _ -> index % 2 == 0 }.toTypedArray()
        val signalI = BinarySignal(dataI, QpskContract.SYMBOL_TIME)

        val dataQ = data.filterIndexed { index, _ -> index % 2 != 0 }.toTypedArray()
        val signalQ = BinarySignal(dataQ, QpskContract.SYMBOL_TIME)

        return carrier * signalI - carrier.shiftPhaseBy(Math.PI / 2) * signalQ
    }

    /**
     * @param carrier несущая гармоника. Для соответствия общепринятой конвенциии необходимо
     * использовать сигнал вида косинус
     * @param data два массива с квадратурными составляющими: первый - I, второй - Q
     */
    fun modulate(carrier: HarmonicSignal, data: Pair<Array<Boolean>, Array<Boolean>>): Signal {
        val signalI = BinarySignal(data.first, QpskContract.SYMBOL_TIME)
        val signalQ = BinarySignal(data.second, QpskContract.SYMBOL_TIME)

        return carrier * signalI - carrier.shiftPhaseBy(Math.PI / 2) * signalQ
    }
}