package alektas.telecomapp.domain.entities.signals

import alektas.telecomapp.domain.entities.Simulator

class ConstantSignal(value: Double = 1.0): BaseSignal() {

    init {
        data = Simulator.simulate { value }
    }
}