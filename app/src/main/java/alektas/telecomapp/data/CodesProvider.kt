package alektas.telecomapp.data

import android.util.Log
import kotlin.random.Random

class CodesProvider(val type: Int) {
    private var codeCounter = 0

    fun generateCode(length: Int): Array<Boolean> {
        when (type) {
            WALLSH -> {
                if (codeCounter == 4) {
                    Log.e("CodesProvider", "Used more then CodesProvider can generate!",
                        IndexOutOfBoundsException())
                }
                return WALLSH_MATRIX[codeCounter++]
            }
            else -> {
                val r = Random.Default
                return Array(length) { r.nextBoolean() }
            }
        }
    }

    companion object {
        const val WALLSH = 1
        private val WALLSH_MATRIX = arrayOf(
            arrayOf(true, true, true, true),
            arrayOf(true, true, false, false),
            arrayOf(true, false, false, true),
            arrayOf(true, false, true, false)
        )

        fun generateCode(length: Int): Array<Boolean> {
            val r = Random.Default
            return Array(length) { r.nextBoolean() }
        }
    }
}