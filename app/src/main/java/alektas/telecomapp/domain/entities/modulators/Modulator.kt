package alektas.telecomapp.domain.entities.modulators

import alektas.telecomapp.domain.entities.signals.HarmonicSignal
import alektas.telecomapp.domain.entities.signals.Signal

interface Modulator<T> {
    fun modulate(carrier: HarmonicSignal, data: T): Signal
}