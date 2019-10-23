package alektas.telecomapp.data

import org.junit.Assert.*
import org.junit.Test

class CodeGeneratorTest {

    @Test
    fun generateRandomCode_length_isCorrect() {
        val code = CodeGenerator().generateRandomCode(3)
        assertTrue(3 == code.size)
    }

    @Test
    fun generateWalshMatrix_size_isCorrect() {
        val code = CodeGenerator().generateWalshMatrix(5)
        assertTrue(8 == code.size)
    }

    @Test
    fun generateHadamardMatrix_size_isCorrect() {
        val code = CodeGenerator().generateHadamardMatrix(5)
        assertTrue(8 == code.size)
    }

    @Test
    fun generateRandomCodes_size_isCorrect() {
        val code = CodeGenerator().generateRandomCodes(3, 4)
        assertTrue(3 == code.size)
    }
}