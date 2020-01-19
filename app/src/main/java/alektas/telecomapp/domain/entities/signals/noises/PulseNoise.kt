package alektas.telecomapp.domain.entities.signals.noises

import alektas.telecomapp.domain.entities.Simulator
import java.util.*
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Хаотичные импульсные помехи (ХИП).
 *
 * @param sparseness коэффициент разреженности импульсов, при 0 имеет вид АБГШ
 * (обратнопропорционален частоте появления импульсов)
 * @param rate отношение сигнал/шум (signal/noise rate) в дБ
 * @param signalPower мощность полезного сигнала, Вт
 */
class PulseNoise(
    val sparseness: Double,
    val rate: Double,
    signalPower: Double
) : BaseNoise(rate) {

    init {
        val linSnr = 10.0.pow(rate / 10)
        val deviation = if (signalPower <= 0) 0.0 else sqrt(0.5 * signalPower / linSnr)
        data = Simulator.simulate {
            val rand = deviation * Random().nextGaussian()
            if (abs(rand) > sparseness * deviation) rand else 0.0
        }
    }

}