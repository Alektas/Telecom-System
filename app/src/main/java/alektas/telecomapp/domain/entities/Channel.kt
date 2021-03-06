package alektas.telecomapp.domain.entities

import alektas.telecomapp.domain.entities.generators.ChannelCodesGenerator
import alektas.telecomapp.domain.entities.contracts.CdmaContract
import alektas.telecomapp.domain.entities.contracts.QpskContract

data class Channel(
    var name: String = "${channelsCount + 1}",
    val carrierFrequency: Double = QpskContract.DEFAULT_CARRIER_FREQUENCY,
    val frameLength: Int = CdmaContract.DEFAULT_FRAME_SIZE,
    var frameData: BooleanArray = booleanArrayOf(),
    var sourceData: BooleanArray = booleanArrayOf(),
    val bitTime: Double = QpskContract.DEFAULT_DATA_BIT_TIME,
    val code: BooleanArray = booleanArrayOf(),
    val channelCodeType: Int = ChannelCodesGenerator.WALSH
) {
    val id: Int
    var errors: List<Int>? = null

    init {
        channelsCount++
        id = channelsCount
    }

    companion object {
        private var channelsCount: Int = 0
    }

    fun getDataString(): String {
        return frameData.joinToString(separator = " ") { if (it) "1" else "0" }
    }

    fun getCodeString(): String {
        return code.joinToString(separator = " ") { if (it) "1" else "0" }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Channel) return false

        if (name != other.name) return false
        if (!frameData.contentEquals(other.frameData)) return false
        if (!code.contentEquals(other.code)) return false
        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + frameData.contentHashCode()
        result = 31 * result + code.contentHashCode()
        result = 31 * result + id
        return result
    }

}