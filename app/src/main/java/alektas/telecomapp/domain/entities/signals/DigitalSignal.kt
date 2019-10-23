package alektas.telecomapp.domain.entities.signals

import alektas.telecomapp.domain.entities.Simulator

class DigitalSignal(
    val dataValues: DoubleArray,
    bitTime: Double
) : BaseSignal() {

    init {
        data = Simulator.simulate { time ->
            val i = (time / bitTime).toInt()
            if (i >= dataValues.size) 0.0 else dataValues[i]
        }
    }
}