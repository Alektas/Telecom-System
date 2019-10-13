package alektas.telecomapp.domain.entities.signals

import alektas.telecomapp.domain.entities.Simulator
import kotlin.math.sin

/**
 * Гармоническое колебание, со временем распространяющиеся в пространстве.
 * Спектр сигнала состоит из одной частоты.
 *
 * @param magnitude амплитуда гармоники в вольтах/амперах/ваттах
 * @param frequency частота гармоники в Гц
 * @param phase начальная фаза сигнала в радианах
 */
class HarmonicSignal(
    var magnitude: Double,
    var frequency: Double,
    var phase: Double
) : BaseSignal() {

    init {
        calculateData()
    }

    /**
     * Повернуть фазу сигнала на <code>radian</code>
     */
    fun shiftPhaseBy(radian: Double): Signal {
        phase += radian
        calculateData()
        return this
    }

    private fun calculateData() {
        data = Simulator.simulate { magnitude * sin(2 * Math.PI * frequency * it + phase) }
    }

}