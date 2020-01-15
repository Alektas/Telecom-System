package alektas.telecomapp.domain.entities.signals.noises

import alektas.telecomapp.domain.entities.Simulator
import java.util.*
import kotlin.math.pow

/**
 * Аддитивный белый гауссовский шум (АБГШ).
 *
 * @param snr отношение сигнал/шум (signal/noise rate) в дБ.
 * @param signalPower мощность полезного сигнала, Вт
 */
class WhiteNoise(private val snr: Double, signalPower: Double): BaseNoise(snr) {
    init {
        val deviation = if (signalPower <= 0) 0.0 else signalPower / 10.0.pow(snr / 10)
        data = Simulator.simulate { deviation * Random().nextGaussian() }
    }
}