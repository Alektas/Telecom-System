package alektas.telecomapp.domain.entities.signals.noises

import alektas.telecomapp.domain.entities.Simulator
import alektas.telecomapp.domain.entities.signals.BaseSignal
import java.util.*

class WhiteNoise(power: Double = 1.0): BaseSignal() {

    init {
        data = Simulator.simulate { power * Random().nextGaussian() }
    }
}