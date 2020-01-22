package alektas.telecomapp.domain.entities.demodulators

import alektas.telecomapp.domain.entities.coders.toBipolar
import alektas.telecomapp.domain.entities.configs.DemodulatorConfig
import alektas.telecomapp.domain.entities.contracts.QpskContract
import alektas.telecomapp.domain.entities.filters.FilterConfig
import alektas.telecomapp.domain.entities.generators.SignalGenerator
import alektas.telecomapp.domain.entities.modulators.QpskModulator
import org.junit.Test
import org.junit.Assert.*

class QpskDemodulatorTest {
    // количество битов должно быть четным (особенности QPSK демодуляции)
    private val inData =
        booleanArrayOf(true, true, false, false, true, false, false, true).toBipolar()
    private val carrier = SignalGenerator().cos(frequency = 100000.0)
    private val dummyFilterConfig = FilterConfig(type = FilterConfig.NONE)
    private val inSig = QpskModulator(QpskContract.DEFAULT_DATA_BIT_TIME).modulate(carrier, inData)
    private val config =
        DemodulatorConfig(
            0.1f, carrier.frequency, 4, 2,
            filterConfig = dummyFilterConfig
        )
    private val demodulator = QpskDemodulator(config)

    @Test
    fun demodulate_size_isCorrect() {
        val outSig = demodulator.demodulate(inSig)

        println("expected: ${inData.size}")
        println("actual: ${outSig.dataValues.size}")
        assertEquals(inData.size, outSig.dataValues.size)
    }

    @Test
    fun demodulate_isCorrect() {
        val outSig = demodulator.demodulate(inSig)
        val actual = outSig.dataValues.map { if (it > 0.0) 1.0 else -1.0 }

        assertArrayEquals(inData.toTypedArray(), actual.toTypedArray())
    }

}