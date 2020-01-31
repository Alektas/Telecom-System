package alektas.telecomapp.domain.entities.coders

import org.junit.Assert.*
import org.junit.Test

class HammingCoderTest {
    val coder = HammingCoder()

    @Test
    fun encode_1011_is0110011() {
        val actual = coder.encode(booleanArrayOf(true, false, true, true))
        val expected = booleanArrayOf(false, true, true, false, false, true, true)

        println("actual=${actual.contentToString()}")
        println("expected=${expected.contentToString()}")
        assertArrayEquals(expected, actual)
    }

    @Test
    fun decode_0110011_is1011() {
        val actual = coder.decode(booleanArrayOf(false, true, true, false, false, true, true))
        val expected = booleanArrayOf(true, false, true, true)

        println("actual=${actual.contentToString()}")
        println("expected=${expected.contentToString()}")
        assertArrayEquals(expected, actual)
    }

    @Test
    fun getParityBits_0110011_is010() {
        val actual = coder.getParityBits(booleanArrayOf(false, true, true, false, false, true, true))
        val expected = booleanArrayOf(false, true, false)

        println("actual=${actual.contentToString()}")
        println("expected=${expected.contentToString()}")
        assertArrayEquals(expected, actual)
    }


    @Test
    fun getParityBits_111_is11() {
        val actual = coder.getParityBits(booleanArrayOf(true, true, true))
        val expected = booleanArrayOf(true, true)

        println("actual=${actual.contentToString()}")
        println("expected=${expected.contentToString()}")
        assertArrayEquals(expected, actual)
    }

    @Test
    fun calcParityBits_0110011_is010() {
        val actual = coder.calcParityBits(booleanArrayOf(false, true, true, false, false, true, true))
        val expected = booleanArrayOf(false, false, false)

        println("actual=${actual.contentToString()}")
        println("expected=${expected.contentToString()}")
        assertArrayEquals(expected, actual)
    }

    @Test
    fun parityBitsCount_of16_is5() {
        assertEquals(5, coder.parityBitsCount(16))
    }

    @Test
    fun parityBitsCount_of15_is4() {
        assertEquals(4, coder.parityBitsCount(15))
    }

    @Test
    fun parityBitsCount_of8_is4() {
        assertEquals(4, coder.parityBitsCount(8))
    }

    @Test
    fun parityBitsCount_of7_is3() {
        assertEquals(3, coder.parityBitsCount(7))
    }

    @Test
    fun parityBitsCount_of4_is3() {
        assertEquals(3, coder.parityBitsCount(4))
    }

    @Test
    fun parityBitsCount_of3_is2() {
        assertEquals(2, coder.parityBitsCount(3))
    }

}