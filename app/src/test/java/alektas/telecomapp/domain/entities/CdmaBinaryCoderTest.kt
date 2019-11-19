package alektas.telecomapp.domain.entities

import alektas.telecomapp.domain.entities.coders.CdmaBinaryCoder
import org.junit.Test

import org.junit.Assert.*
import org.junit.Before

class CdmaBinaryCoderTest {
    private lateinit var coder: CdmaBinaryCoder

    @Before
    fun prepare() {
        coder = CdmaBinaryCoder()
    }

    /**
     * Каждый бит информации должен складываться по модулю 2 с кодом
     * Длина закодированных данных в битах должна быть: L = S * D, где
     * S - коэффициент расширения, равный количеству битов кода, приходящихся на 1 бит информации
     * D - длина данных в битах
     */
    @Test
    fun encode_isCorrect() {
        val code = booleanArrayOf(true, false)
        val data = booleanArrayOf(false, true, true)

        val encodedData = coder.encode(code, data)

        assertArrayEquals(
            booleanArrayOf(true, false, false, true, false, true),
            encodedData)
    }

    /**
     * Длина закодированных данных в битах должна быть: L = S * D, где
     * S - коэффициент расширения, равный количеству битов кода, приходящихся на 1 бит информации
     * D - длина данных в битах
     */
    @Test
    fun encode_length_isCorrect() {
        val code = booleanArrayOf(true, false, true)
        val data = booleanArrayOf(false, true, true, false)

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
        val code = booleanArrayOf(true, false)
        val encodedData = booleanArrayOf(false, true, true, false, false, true)

        val decodedData = coder.decode(code, encodedData)

        assertArrayEquals(booleanArrayOf(true, false, true), decodedData)
    }

    /**
     * Длина закодированных данных в битах должна быть: L = S * D, где
     * S - коэффициент расширения, равный количеству битов кода, приходящихся на 1 бит информации
     * D - длина данных в битах
     */
    @Test
    fun decode_length_isCorrect() {
        val code = booleanArrayOf(true, false, true)
        val encodedData = booleanArrayOf(false, true, true, false, false, false, true, true, true)

        val size = coder.decode(code, encodedData).size

        assertTrue("expected: 3, actual: $size", 3 == size)
    }
}