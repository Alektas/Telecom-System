package alektas.telecomapp.domain.entities.signals.noises

import alektas.telecomapp.domain.entities.Simulator
import alektas.telecomapp.domain.entities.signals.BaseSignal
import java.util.*
import kotlin.math.pow

/**
 * Аддитивный белый гауссовский шум (АБГШ).
 *
 * @param snr отношение сигнал/шум (signal/noise rate) в дБ.
 * @param signalMagnitude среднеквадратическая амплитуда полезного сигнала
 */
class WhiteNoise(snr: Double, signalMagnitude: Double): BaseSignal() {

    init {
        val deviation = signalMagnitude / 10.0.pow(snr / 20)
        data = Simulator.simulate { deviation * Random().nextGaussian() }
    }
}