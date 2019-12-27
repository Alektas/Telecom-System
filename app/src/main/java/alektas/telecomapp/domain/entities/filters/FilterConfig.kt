package alektas.telecomapp.domain.entities.filters

import alektas.telecomapp.domain.entities.Simulator
import alektas.telecomapp.domain.entities.Window

class FilterConfig(
    var type: Int = FIR,
    var order: Int = DEFAULT_ORDER,
    var bandwidth: Double = DEFAULT_BANDWIDTH,
    var suppressBand: Double = DEFAULT_SUPPRESS_BAND,
    var samplingRate: Double = DEFAULT_SAMPLING_RATE,
    var windowType: Int = DEFAULT_WINDOW_TYPE) {

    companion object {
        const val NONE = 0
        const val FIR = 1
        const val DEFAULT_ORDER = 10
        const val DEFAULT_BANDWIDTH = 1.0e3
        const val DEFAULT_SUPPRESS_BAND = 1.0e1
        const val DEFAULT_SAMPLING_RATE = Simulator.DEFAULT_SAMPLING_RATE
        const val DEFAULT_WINDOW_TYPE = Window.BLACKMANN
    }
}