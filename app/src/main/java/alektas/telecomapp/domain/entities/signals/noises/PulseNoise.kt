package alektas.telecomapp.domain.entities.signals.noises

import alektas.telecomapp.domain.entities.Simulator
import java.util.*
import kotlin.math.abs
import kotlin.math.pow

/**
 * Хаотичные импульсные помехи (ХИП).
 *
 * @param sparseness коэффициент разреженности импульсов, при 0 имеет вид АБГШ
 * (обратнопропорционален частоте появления импульсов)
 * @param snr отношение сигнал/шум (signal/noise rate) в дБ
 * @param signalMagnitude среднеквадратическая амплитуда полезного сигнала
 */
class PulseNoise(
    private val sparseness: Double = 1.2,
    private val snr: Double,
    signalMagnitude: Double
) :
    BaseNoise(snr) {

    init {
        val deviation = signalMagnitude / 10.0.pow(snr / 20)
        data = Simulator.simulate {
            val rand = deviation * Random().nextGaussian()
            if (abs(rand) > sparseness * deviation) rand else 0.0
        }
    }
}