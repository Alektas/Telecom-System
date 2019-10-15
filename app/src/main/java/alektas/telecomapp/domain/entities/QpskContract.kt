package alektas.telecomapp.domain.entities

class QpskContract {

    companion object {
        const val DATA_BIT_TIME = CdmaContract.CODE_BIT_TIME
        const val SYMBOL_TIME = DATA_BIT_TIME * 2
        const val CARRIER_FREQUENCY = 5.0e5
    }
}