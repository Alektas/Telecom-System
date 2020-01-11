package alektas.telecomapp.domain.entities.filters

import alektas.telecomapp.domain.entities.signals.Signal

interface Filter {
    fun filter(signal: Signal): Signal
    fun filter(data: DoubleArray): DoubleArray
    fun impulseResponse(): DoubleArray
}