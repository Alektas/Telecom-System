package alektas.telecomapp.domain.entities.contracts

import alektas.telecomapp.data.CodeGenerator

class CdmaContract {

    companion object {
        const val DEFAULT_FRAME_SIZE = 100
        const val DEFAULT_CODE_SIZE = 2
        const val DEFAULT_CHANNEL_COUNT = 1
        const val DEFAULT_CODE_TYPE = CodeGenerator.WALSH
    }
}