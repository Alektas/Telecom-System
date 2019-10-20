package alektas.telecomapp.domain.entities

import alektas.telecomapp.domain.entities.signals.BaseSignal
import alektas.telecomapp.domain.entities.signals.Signal
import alektas.telecomapp.domain.entities.signals.noises.WhiteNoise

class RadioChannel(val ether: Signal) {

    class Builder {
        private var ether: Signal = BaseSignal()

        fun addSignal(signal: Signal): Builder {
            ether += signal
            return this
        }

        fun addWhiteNoise(power: Double = 1.0): Builder {
            ether += WhiteNoise(power)
            return this
        }

        fun build(): RadioChannel {
            return RadioChannel(ether)
        }
    }
}