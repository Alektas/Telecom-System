package alektas.telecomapp.domain.entities.modulators

import alektas.telecomapp.domain.entities.QpskContract
import alektas.telecomapp.domain.entities.signals.DigitSignal
import alektas.telecomapp.domain.entities.signals.HarmonicSignal
import alektas.telecomapp.domain.entities.signals.Signal

class QpskModulator : Modulator<Array<Boolean>> {

    override fun modulate(carrier: HarmonicSignal, data: Array<Boolean>): Signal {
        val dataI = data.filterIndexed { index, _ -> index % 2 == 0 }.toTypedArray()
        val signalI = DigitSignal(dataI, QpskContract.SYMBOL_TIME)

        val dataQ = data.filterIndexed { index, _ -> index % 2 != 0 }.toTypedArray()
        val signalQ = DigitSignal(dataQ, QpskContract.SYMBOL_TIME)

        return carrier * signalI - carrier.shiftPhaseBy(Math.PI / 2) * signalQ
    }
}