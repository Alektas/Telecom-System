package alektas.telecomapp.domain.entities.demodulators

import alektas.telecomapp.domain.entities.signals.Signal

interface Demodulator<T> {
    fun demodulate(signal: Signal): T
}