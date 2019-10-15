package alektas.telecomapp.domain.entities.modulators

import alektas.telecomapp.domain.entities.QpskContract
import alektas.telecomapp.domain.entities.signals.DigitalSignal
import alektas.telecomapp.domain.entities.signals.HarmonicSignal
import alektas.telecomapp.domain.entities.signals.Signal

class QpskModulator : Modulator<Array<Boolean>> {

    /**
     * @param carrier несущая гармоника. Для соответствия общепринятой конвенциии необходимо
     * использовать сигнал вида косинус
     */
    override fun modulate(carrier: HarmonicSignal, data: Array<Boolean>): Signal {
        val dataI = data.filterIndexed { index, _ -> index % 2 == 0 }.toTypedArray()
        val signalI = DigitalSignal(dataI, QpskContract.SYMBOL_TIME)

        val dataQ = data.filterIndexed { index, _ -> index % 2 != 0 }.toTypedArray()
        val signalQ = DigitalSignal(dataQ, QpskContract.SYMBOL_TIME)

        return carrier * signalI - carrier.shiftPhaseBy(Math.PI / 2) * signalQ
    }
}