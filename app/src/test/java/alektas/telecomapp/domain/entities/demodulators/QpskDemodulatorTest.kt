package alektas.telecomapp.domain.entities.demodulators

import alektas.telecomapp.domain.entities.filters.FilterConfig
import alektas.telecomapp.domain.entities.modulators.QpskModulator
import alektas.telecomapp.domain.entities.signals.BinarySignal
import alektas.telecomapp.domain.entities.signals.HarmonicSignal
import org.junit.Test
import org.junit.Assert.*

class QpskDemodulatorTest {
    // количество битов должно быть четным (особенности QPSK демодуляции)
    private val inData = booleanArrayOf(true, false, true, false)
    private val carrier = HarmonicSignal(1.0, 1000.0, 0.0)
    private val dummyFilterConfig = FilterConfig(type = FilterConfig.NONE)
    private val inSig = QpskModulator().modulate(carrier, inData)

    @Test
    fun demodulate_withDummyFilter_isCorrect() {
        val config = DemodulatorConfig(inSig, carrier.frequency, inData.size, dummyFilterConfig)
        val demodulator = QpskDemodulator(config)
        val outSig = demodulator.demodulate(inSig)

        assertArrayEquals(inData, outSig.bits)
    }

    @Test
    fun getConstellation() {
    }

    @Test
    fun getConstellation1() {
    }
}