package alektas.telecomapp.domain.entities.coders

import org.junit.Assert.*
import org.junit.Test

class HammingCoderTest {
    val coder7 = HammingCoder(7)

    @Test
    fun encode_1011_is0110011() {
        val actual = coder7.encode(booleanArrayOf(true, false, true, true))
        val expected = booleanArrayOf(false, true, true, false, false, true, true)

        println("actual=${actual.contentToString()}")
        println("expected=${expected.contentToString()}")
        assertArrayEquals(expected, actual)
    }

    @Test
    fun decode_0110011_is1011() {
        val actual = coder7.decode(booleanArrayOf(false, true, true, false, false, true, true))
        val expected = booleanArrayOf(true, false, true, true)

        println("actual=${actual.contentToString()}")
        println("expected=${expected.contentToString()}")
        assertArrayEquals(expected, actual)
    }

    @Test
    fun getExtraBitsCount_whenDataSizeIs11_is1() {
        assertEquals(1, coder7.getExtraBitsCount(11))
    }

    @Test
    fun getExtraBitsCount_whenDataSizeIs3_is1() {
        assertEquals(1, coder7.getExtraBitsCount(3))
    }

    @Test
    fun getExtraBitsCount_whenDataSizeIs8_is0() {
        assertEquals(0, coder7.getExtraBitsCount(8))
    }

}