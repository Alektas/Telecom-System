package alektas.telecomapp.domain.entities.demodulators

import alektas.telecomapp.domain.entities.signals.Signal

class DemodulatorConfig {
    var inputSignal: Signal? = null

    companion object {
        val DEFAULT = DemodulatorConfig()
    }

}