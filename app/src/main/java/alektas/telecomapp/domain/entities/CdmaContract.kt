package alektas.telecomapp.domain.entities

class CdmaContract {

    companion object {
        const val MAX_CHANNEL_COUNT = 5
        const val DATA_FRAME_LENGTH = 10
        const val CODE_BIT_TIME = 1.0e-5
        const val CODE_LENGTH = MAX_CHANNEL_COUNT
        const val SPREAD_RATIO = CODE_LENGTH
        const val DATA_BIT_TIME = SPREAD_RATIO * CODE_BIT_TIME
        const val SPREAD_DATA_LENGTH = SPREAD_RATIO * DATA_FRAME_LENGTH
    }
}