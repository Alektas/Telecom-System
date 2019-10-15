package alektas.telecomapp.domain.entities

class CdmaContract {

    companion object {
        const val DATA_FRAME_LENGTH = 10
        const val CODE_BIT_TIME = 1.0e-5
        const val CODE_LENGTH = 10
        const val SPREAD_RATIO = CODE_LENGTH
        const val DATA_BIT_TIME = SPREAD_RATIO * CODE_BIT_TIME
    }
}