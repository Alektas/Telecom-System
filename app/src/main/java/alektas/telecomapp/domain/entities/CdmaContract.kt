package alektas.telecomapp.domain.entities

class CdmaContract {

    companion object {
        var DATA_FRAME_LENGTH = 10
        var CODE_BIT_TIME = 1.0e-5
        var CODE_LENGTH = 4
        var SPREAD_RATIO = CODE_LENGTH
        var DATA_BIT_TIME = SPREAD_RATIO * CODE_BIT_TIME
        var SPREAD_DATA_LENGTH = SPREAD_RATIO * DATA_FRAME_LENGTH
    }
}