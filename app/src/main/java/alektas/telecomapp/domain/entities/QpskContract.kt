package alektas.telecomapp.domain.entities

class QpskContract {

    companion object {
        const val DATA_BIT_TIME = 1.0e-5
        const val SYMBOL_TIME = DATA_BIT_TIME * 2.0
        const val CARRIER_FREQUENCY = 5.0e4
        const val SIGNAL_MAGNITUDE = 0.707
        const val SIGNAL_THRESHOLD = 0.3
    }
}