package alektas.telecomapp.data

import kotlin.random.Random

class UserDataProvider {

    companion object {

        fun generateData(length: Int): BooleanArray {
            val r = Random.Default
            return BooleanArray(length) { r.nextBoolean() }
        }
    }
}