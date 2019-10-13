package alektas.telecomapp.data

import kotlin.random.Random

class UserDataProvider {

    companion object {

        fun generateData(length: Int): Array<Boolean> {
            val r = Random.Default
            return Array(length) { r.nextBoolean() }
        }
    }
}