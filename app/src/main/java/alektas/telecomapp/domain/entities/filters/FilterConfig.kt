package alektas.telecomapp.domain.entities.filters

import alektas.telecomapp.domain.entities.Simulator
import alektas.telecomapp.domain.entities.Window

class FilterConfig(
    var type: Int = NONE,
    var order: Int = 256,
    var bandwidth: Double = 1.0e3,
    var suppressBand: Double = 1.0e1,
    var samplingRate: Double = Simulator.SAMPLING_RATE,
    var windowType: Int = Window.HAMMING) {

    companion object {
        const val NONE = 0
        const val FIR = 1
    }
}