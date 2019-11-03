package alektas.telecomapp.domain.entities

class QpskContract {

    companion object {
        const val DEFAULT_DATA_BIT_TIME = 1.0e-5
        const val DEFAULT_SYMBOL_TIME = DEFAULT_DATA_BIT_TIME * 2.0
        const val DEFAULT_CARRIER_FREQUENCY = 5.0e4
        const val DEFAULT_SIGNAL_MAGNITUDE = 0.707
        const val DEFAULT_SIGNAL_THRESHOLD = 0.3
    }
}