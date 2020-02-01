package alektas.telecomapp.domain.entities.contracts

import alektas.telecomapp.domain.entities.generators.ChannelCodesGenerator

class CdmaContract {

    companion object {
        const val DEFAULT_FRAME_SIZE = 100
        const val DEFAULT_CHANNEL_CODE_SIZE = 4
        const val DEFAULT_CHANNEL_COUNT = 4
        const val DEFAULT_FRAME_COUNT = 1
        const val DEFAULT_CHANNEL_CODE_TYPE = ChannelCodesGenerator.WALSH
    }
}