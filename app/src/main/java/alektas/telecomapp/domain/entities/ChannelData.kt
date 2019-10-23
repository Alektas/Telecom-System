package alektas.telecomapp.domain.entities

data class ChannelData(
    val id: Int,
    val name: String,
    val data: BooleanArray,
    val code: BooleanArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ChannelData

        if (!name.contentEquals(other.name)) return false
        if (!data.contentEquals(other.data)) return false
        if (!code.contentEquals(other.code)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + data.contentHashCode()
        result = 31 * result + code.contentHashCode()
        return result
    }

    fun getDataString(): String {
        return data.joinToString { if (it) "1" else "0" }
    }

    fun getCodeString(): String {
        return code.joinToString { if (it) "1" else "0" }
    }

}