package alektas.telecomapp.domain.entities.converters

import org.junit.Test

import org.junit.Assert.*

class ValueConverterTest {
    // 8-разрядный конвертер оперирует значениями от 0 до 255
    private val bitDepth = 8
    private val converter = ValueConverter(bitDepth)

    @Test
    fun convertToBitString_zeroValue_isCorrect() {
        val data = doubleArrayOf(0.0)
        val string = converter.convertToBitString(data)

        assertEquals("00000000", string)
    }

    @Test
    fun convertToBitString_minAndMaxValues_isCorrect() {
        val data = doubleArrayOf(0.0, 1.0)
        val string = converter.convertToBitString(data)

        assertEquals("0000000011111111", string)
    }

    @Test
    fun convertToBitString_severalBipolarValues_isCorrect() {
        val data = doubleArrayOf(0.0, 1.0, -1.0)
        val string = converter.convertToBitString(data)

        assertEquals("011111111111111100000000", string)
    }

    @Test
    fun convertToValues_zero_isCorrect() {
        val actual = converter.convertToBipolarNormalizedValues("00000000")
        val expected = doubleArrayOf(-1.0)

        assertArrayEquals(expected, actual, 0.0)
    }

    @Test
    fun convertToValues_minAndMaxValues_isBipolar() {
        val actual = converter.convertToBipolarNormalizedValues("0000000011111111")
        val expected = doubleArrayOf(-1.0, 1.0)

        assertArrayEquals(expected, actual, 0.0)
    }

    @Test
    fun convertToValues_severalValues_isBipolar() {
        val actual = converter.convertToBipolarNormalizedValues("011111111111111100000000")
        val expected = doubleArrayOf(0.0, 1.0, -1.0)

        assertArrayEquals(expected, actual, 1 / converter.maxLevel.toDouble())
    }
}