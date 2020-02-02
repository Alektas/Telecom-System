package alektas.telecomapp.domain.entities.coders

class DataCodesContract {

    companion object {
        const val DEFAULT_IS_CODING_ENABLED = false
        const val DEFAULT_CODE_WORD_LENGTH = 7
        const val HAMMING = 0
        val codeNames = mapOf(
            HAMMING to "Хэмминга"
        )

        fun getCodeTypeId(codeTypeName: String): Int {
            return codeNames.filterValues { it == codeTypeName }.let {
                if (it.isEmpty()) return -1
                else it.keys.first()
            }
        }

        fun getCodeName(id: Int): String? {
            return codeNames[id]
        }
    }
}