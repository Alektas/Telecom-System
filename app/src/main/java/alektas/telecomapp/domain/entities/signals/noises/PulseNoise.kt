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
 * @param rate отношение энергии бита к спектральной мощности шума (Eb/N0 rate) в дБ.
 * @param bitEnergy энергия, приходящаяся на один бит сигнала (Eb), Вт * сек
 */
class PulseNoise(
    val sparseness: Double,
    val rate: Double,
    bitEnergy: Double
) : BaseNoise(rate) {

    init {
        val linSnr = 10.0.pow(rate / 10)
        val n0 = bitEnergy / linSnr // Спектральная плотность мощности шума - N0, Вт/Гц
        val n = n0 * Simulator.samplingRate // Мощность шума, Вт
        val deviation = if (bitEnergy <= 0) 0.0 else sqrt(n)
        data = Simulator.simulate {
            val rand = deviation * Random().nextGaussian()
            if (abs(rand) > sparseness * deviation) rand else 0.0
        }
    }

}