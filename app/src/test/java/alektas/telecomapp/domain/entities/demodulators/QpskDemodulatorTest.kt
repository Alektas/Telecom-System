package alektas.telecomapp.domain.entities.demodulators

import alektas.telecomapp.domain.entities.configs.DemodulatorConfig
import alektas.telecomapp.domain.entities.filters.FilterConfig
import alektas.telecomapp.domain.entities.generators.SignalGenerator
import alektas.telecomapp.domain.entities.modulators.QpskModulator
import org.junit.Test
import org.junit.Assert.*

class QpskDemodulatorTest {
    // количество битов должно быть четным (особенности QPSK демодуляции)
    private val inData = booleanArrayOf(true, true, false, false, true, false, false, true)
    private val carrier = SignalGenerator().cos(frequency = 100000.0)
    private val dummyFilterConfig = FilterConfig(type = FilterConfig.NONE)
    private val inSig = QpskModulator().modulate(carrier, inData)
    private val config =
        DemodulatorConfig(
            inSig, carrier.frequency, 4, 2,
            filterConfig = dummyFilterConfig
        )
    private val demodulator = QpskDemodulator(config)

    @Test
    fun demodulate_size_isCorrect() {
        val outSig = demodulator.demodulate(inSig)

        println("expected: ${inData.size}")
        println("actual: ${outSig.bits.size}")
        assertEquals(inData.size, outSig.bits.size)
    }

    @Test
    fun demodulate_isCorrect() {
        val outSig = demodulator.demodulate(inSig)

        println("expected: ${inData.joinToString { it.toString() }}")
        println("actual: ${outSig.bits.joinToString { it.toString() }}")
        assertArrayEquals(inData, outSig.bits)
    }

    @Test
    fun getConstellation_size_isCorrect() {
        val points = demodulator.getConstellation(inSig)

        println("expected: ${inData.size / 2}")
        println("actual: ${points.size}")
        assertEquals(points.size, inData.size / 2)
    }

    @Test
    fun getConstellation_isCorrect() {
        val points = demodulator.getConstellation(inSig)
        val data = BooleanArray(inData.size)
        points.forEachIndexed { i, it ->
            data[i * 2] = it.first > 0
            data[i * 2 + 1] = it.second > 0
        }

        println("expected: ${inData.joinToString { it.toString() }}")
        println("actual: ${data.joinToString { it.toString() }}")
        assertArrayEquals(inData, data)
    }

}