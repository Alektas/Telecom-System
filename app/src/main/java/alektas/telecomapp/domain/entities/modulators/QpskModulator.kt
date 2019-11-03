package alektas.telecomapp.domain.entities.modulators

import alektas.telecomapp.domain.entities.signals.BinarySignal
import alektas.telecomapp.domain.entities.signals.HarmonicSignal
import alektas.telecomapp.domain.entities.signals.Signal

class QpskModulator(val bitTime: Double) : Modulator<BooleanArray> {

    /**
     * @param carrier несущая гармоника. Для соответствия общепринятой конвенциии необходимо
     * использовать сигнал вида косинус
     * @param data массив битов данных
     */
    override fun modulate(carrier: HarmonicSignal, data: BooleanArray): Signal {
        val dataI = data.filterIndexed { index, _ -> index % 2 == 0 }.toBooleanArray()
        val signalI = BinarySignal(dataI, bitTime * 2)

        val dataQ = data.filterIndexed { index, _ -> index % 2 != 0 }.toBooleanArray()
        val signalQ = BinarySignal(dataQ, bitTime * 2)

        return carrier * signalI - carrier.shiftPhaseBy(Math.PI / 2) * signalQ
    }

}