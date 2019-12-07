package alektas.telecomapp.domain.entities.demodulators

import alektas.telecomapp.domain.entities.signals.Signal

interface Demodulator<T> {
    fun demodulateFrame(signal: Signal): T
}