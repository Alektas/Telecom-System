package alektas.telecomapp.domain.entities.signals

import alektas.telecomapp.domain.entities.Simulator

class DigitalSignal(
    val values: Array<Double>,
    bitTime: Double
) : BaseSignal() {

    init {
        data = Simulator.simulate { time ->
            val i = (time / bitTime).toInt()
            if (i >= values.size) 0.0 else values[i]
        }
    }
}