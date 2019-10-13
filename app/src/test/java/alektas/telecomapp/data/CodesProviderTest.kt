package alektas.telecomapp.data

import org.junit.Assert.*
import org.junit.Test

class CodesProviderTest {

    @Test
    fun generateCode_length_isCorrect() {
        val code = CodesProvider.generateCode(3)
        assertTrue(3 == code.size)
    }
}