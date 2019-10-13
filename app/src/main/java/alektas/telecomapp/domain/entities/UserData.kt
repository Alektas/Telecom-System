package alektas.telecomapp.domain.entities

import alektas.telecomapp.data.CodesProvider
import alektas.telecomapp.data.UserDataProvider

data class UserData(
    val data: Array<Boolean> = UserDataProvider.generateData(CdmaContract.DATA_FRAME_LENGTH),
    val code: Array<Boolean> = CodesProvider.generateCode(CdmaContract.CODE_LENGTH)
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UserData

        if (!data.contentEquals(other.data)) return false
        if (!code.contentEquals(other.code)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = data.contentHashCode()
        result = 31 * result + code.contentHashCode()
        return result
    }
}