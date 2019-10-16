package alektas.telecomapp.domain.entities

import alektas.telecomapp.domain.entities.coders.CdmaCoder
import org.junit.Test

import org.junit.Assert.*
import org.junit.Before

class CdmaCoderTest {
    private lateinit var coder: CdmaCoder

    @Before
    fun prepare() {
        coder = CdmaCoder()
    }

    /**
     * Каждый бит информации должен складываться по модулю 2 с кодом
     * Длина закодированных данных в битах должна быть: L = S * D, где
     * S - коэффициент расширения, равный количеству битов кода, приходящихся на 1 бит информации
     * D - длина данных в битах
     */
    @Test
    fun encode_isCorrect() {
        val code = arrayOf(true, false)
        val data = arrayOf(false, true, true)

        val encodedData = coder.encode(code, data)

        assertArrayEquals(
            arrayOf(true, false, false, true, false, true),
            encodedData)
    }

    /**
     * Длина закодированных данных в битах должна быть: L = S * D, где
     * S - коэффициент расширения, равный количеству битов кода, приходящихся на 1 бит информации
     * D - длина данных в битах
     */
    @Test
    fun encode_length_isCorrect() {
        val code = arrayOf(true, false, true)
        val data = arrayOf(false, true, true, false)

        val size = coder.encode(code, data).size

        assertTrue(12 == size)
    }

    /**
     * Каждый бит информации должен складываться по модулю 2 с кодом.
     * Длина закодированных данных в битах должна быть: L = S * D, где
     * S - коэффициент расширения, равный количеству битов кода, приходящихся на 1 бит информации
     * D - длина данных в битах
     */
    @Test
    fun decode_isCorrect() {
        val code = arrayOf(true, false)
        val encodedData = arrayOf(false, true, true, false, false, true)

        val decodedData = coder.decode(code, encodedData)

        assertArrayEquals(arrayOf(true, false, true), decodedData)
    }

    /**
     * Длина закодированных данных в битах должна быть: L = S * D, где
     * S - коэффициент расширения, равный количеству битов кода, приходящихся на 1 бит информации
     * D - длина данных в битах
     */
    @Test
    fun decode_length_isCorrect() {
        val code = arrayOf(true, false, true)
        val encodedData = arrayOf(false, true, true, false, false, false, true, true, true)

        val size = coder.decode(code, encodedData).size

        assertTrue("expected: 3, actual: $size", 3 == size)
    }
}