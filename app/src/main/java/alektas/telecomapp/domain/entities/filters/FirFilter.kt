package alektas.telecomapp.domain.entities.filters

import alektas.telecomapp.domain.entities.Window
import alektas.telecomapp.domain.entities.signals.BaseSignal
import alektas.telecomapp.domain.entities.signals.Signal
import kotlin.math.PI
import kotlin.math.sin

/**
 * Фильтр с конечной импульсной характеристикой - КИХ (Finite Impulse Response - FIR).
 *
 * @param order порядок (длина) фильтра
 * @param bandwidth Ширина полосы пропускания
 * @param suppressBand Ширина полосы затухания
 * @param samplingRate Частота дискретизации сигнала
 */
class FirFilter(
    val order: Int,
    val bandwidth: Double,
    val suppressBand: Double,
    val samplingRate: Double,
    val windowType: Int = Window.HAMMING
) : Filter {

    constructor(config: FilterConfig) : this(
        config.order,
        config.bandwidth,
        config.suppressBand,
        config.samplingRate,
        config.windowType
    )

    override fun filter(signal: Signal): Signal {
        return BaseSignal(signal.getTimes(), filter(signal.getValues()))
    }

    override fun filter(
        data: DoubleArray
    ): DoubleArray {
        val impResponse = DoubleArray(order) //Импульсная характеристика фильтра
        val impResponseIdeal = DoubleArray(order) //Идеальная импульсная характеристика
        val window = DoubleArray(order) //Весовая функция

        // Расчет импульсной характеристики фильтра
        val cutoffFrequency = (bandwidth + suppressBand) / (2 * samplingRate)

        for (i in 0 until order) {
            if (i == 0) impResponseIdeal[i] = 2 * PI * cutoffFrequency
            else impResponseIdeal[i] = sin(2 * PI * cutoffFrequency * i) / (PI * i)
            // весовая функция
            window[i] = Window(windowType).calculate(i, order)
            impResponse[i] = impResponseIdeal[i] * window[i]
        }

        //Нормировка импульсной характеристики
        val sum = impResponse.sum()
        for (i in 0 until order) impResponse[i] /= sum //сумма коэффициентов равна 1

        //Фильтрация входных данных
        val out = DoubleArray(data.size)
        for (i in data.indices) {
            out[i] = 0.0
            for (j in 0 until order - 1) {
                if (i - j >= 0) out[i] += impResponse[j] * data[i - j]
            }
        }

        return out
    }

    override fun impulseResponse(): DoubleArray {
        val impulse = DoubleArray(order)
        impulse[0] = 1.0
        return filter(impulse)
    }

}