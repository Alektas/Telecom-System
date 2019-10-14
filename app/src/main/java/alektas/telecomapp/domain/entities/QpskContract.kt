package alektas.telecomapp.domain.entities

class QpskContract {

    companion object {
        const val DATA_BIT_TIME = 1.0e-5
        const val SYMBOL_TIME = DATA_BIT_TIME * 2
        const val CARRIER_FREQUENCY = 5.0e5
    }
}