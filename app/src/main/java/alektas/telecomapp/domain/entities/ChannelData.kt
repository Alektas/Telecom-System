package alektas.telecomapp.domain.entities

import alektas.telecomapp.data.CodeGenerator

data class ChannelData(
    val name: String = "${channelsCount + 1}",
    var data: BooleanArray = booleanArrayOf(),
    val bitTime: Double = QpskContract.DEFAULT_DATA_BIT_TIME,
    val code: BooleanArray = booleanArrayOf(),
    val codeType: Int = CodeGenerator.WALSH
) {
    private val id: Int

    init {
        channelsCount++
        id = channelsCount
    }

    companion object {
        private var channelsCount: Int = 0
    }

    fun getDataString(): String {
        return data.joinToString { if (it) "1" else "0" }
    }

    fun getCodeString(): String {
        return code.joinToString { if (it) "1" else "0" }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ChannelData) return false

        if (name != other.name) return false
        if (!data.contentEquals(other.data)) return false
        if (!code.contentEquals(other.code)) return false
        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + data.contentHashCode()
        result = 31 * result + code.contentHashCode()
        result = 31 * result + id
        return result
    }

}