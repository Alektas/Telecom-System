package alektas.telecomapp.domain.entities.signals

import alektas.telecomapp.domain.entities.Simulator

class BinarySignal(
    val bits: BooleanArray,
    val bitTime: Double,
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