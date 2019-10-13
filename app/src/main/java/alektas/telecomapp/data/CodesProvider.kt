package alektas.telecomapp.data

import kotlin.random.Random

class CodesProvider {

    companion object {

        fun generateCode(length: Int): Array<Boolean> {
            val r = Random.Default
            return Array(length) { r.nextBoolean() }
        }
    }
}