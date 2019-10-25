package alektas.telecomapp.domain.entities.filters

import alektas.telecomapp.domain.entities.signals.Signal

class DummyFilter: Filter {
    override fun filter(signal: Signal): Signal {
        return signal
    }

    override fun filter(data: DoubleArray): DoubleArray {
        return data
    }

    override fun impulseResponse(): DoubleArray {
        return doubleArrayOf()
    }
}