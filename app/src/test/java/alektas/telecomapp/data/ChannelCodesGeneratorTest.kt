package alektas.telecomapp.data

import alektas.telecomapp.domain.entities.generators.ChannelCodesGenerator
import org.junit.Assert.*
import org.junit.Test

class ChannelCodesGeneratorTest {

    @Test
    fun generateRandomCode_length_isCorrect() {
        val code = ChannelCodesGenerator()
            .generateRandomCode(3)
        assertTrue(3 == code.size)
    }

    @Test
    fun generateWalshMatrix_size_isCorrect() {
        val code = ChannelCodesGenerator()
            .generateWalshMatrix(5)
        assertTrue(8 == code.size)
    }

    @Test
    fun generateHadamardMatrix_size_isCorrect() {
        val code = ChannelCodesGenerator()
            .generateHadamardMatrix(5)
        assertTrue(8 == code.size)
    }

    @Test
    fun generateRandomCodes_size_isCorrect() {
        val code = ChannelCodesGenerator()
            .generateRandomCodes(3, 4)
        assertTrue(3 == code.size)
    }
}