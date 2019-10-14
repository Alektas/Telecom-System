package alektas.telecomapp.domain.entities.signals

import alektas.telecomapp.domain.entities.Simulator

class DigitalSignal(
    val bits: Array<Boolean>,
    bitTime: Double,
    magnitude: Double = 1.0,
    bipolar: Boolean = true
) : BaseSignal() {

    init {
        data = Simulator.simulate { time ->
            val i = (time / bitTime).toInt()
            if (i >= bits.size || (!bits[i] && !bipolar)) 0.0
            else if (bits[i]) magnitude else -magnitude
        }
    }
}