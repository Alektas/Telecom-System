package alektas.telecomapp.data

import org.junit.Assert.*
import org.junit.Test

class UserDataProviderTest {

    @Test
    fun generateData_length_isCorrect() {
        val data = UserDataProvider.generateData(3)
        assertTrue(3 == data.size)
    }
}