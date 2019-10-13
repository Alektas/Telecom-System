package alektas.telecomapp.domain.entities

class CdmaContract {

    companion object {
        const val CODE_BIT_TIME = 1.0e-5
        const val SPREAD_RATIO = 4
        const val DATA_BIT_TIME = SPREAD_RATIO * CODE_BIT_TIME
        const val DATA_FRAME_LENGTH = 100
        const val CODE_LENGTH = DATA_FRAME_LENGTH * SPREAD_RATIO
    }
}