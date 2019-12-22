package alektas.telecomapp.domain.entities.contracts

class QpskContract {

    companion object {
        const val DEFAULT_INTERFERENCE_SPARSENESS = 1.2
        const val DEFAULT_DATA_BIT_TIME = 1.0e-5
        const val DEFAULT_SYMBOL_TIME = DEFAULT_DATA_BIT_TIME * 2.0
        const val DEFAULT_CARRIER_FREQUENCY = 5.0e4
        const val DEFAULT_SIGNAL_MAGNITUDE = 0.707
        const val DEFAULT_SIGNAL_NOISE_RATE = 3.0e0
        const val DEFAULT_SIGNAL_THRESHOLD = 0.3
    }
}