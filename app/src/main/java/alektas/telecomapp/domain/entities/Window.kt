package alektas.telecomapp.domain.entities

import alektas.telecomapp.domain.entities.signals.BaseSignal
import alektas.telecomapp.domain.entities.signals.Signal
import java.util.*
import kotlin.math.cos
import kotlin.math.exp

class Window(val type: Int) {

    fun applyTo(signal: Signal): Signal {
        val points = signal.getPoints()
        val values = calculate(
            points.values.toList(),
            when (type) {
                GAUSSE -> ::gausse
                HAMMING -> ::hamming
                HANN -> ::hann
                BLACKMANN -> ::blackmannHarris
                else -> ::square
            }
        )

        val d = TreeMap<Double, Double>()
        for ((i, time) in points.keys.withIndex()) {
            d[time] = values[i]
        }

        return BaseSignal(d)
    }

    fun calculate(n: Int, frameSize: Int): Double {
        return when (type) {
            GAUSSE -> gausse(n, frameSize)
            HAMMING -> hamming(n, frameSize)
            HANN -> hann(n, frameSize)
            BLACKMANN -> blackmannHarris(n, frameSize)
            else -> square(n, frameSize)
        }
    }

    private fun calculate(
        values: List<Double>,
        func: (Int, Int) -> Double
    ): List<Double> {
        val frameSize = values.size
        return values.mapIndexed { i, value -> value * func(i, frameSize) }
    }

    companion object {
        const val GAUSSE = 0
        const val HAMMING = 1
        const val HANN = 2
        const val BLACKMANN = 3
        const val SQUARE = 999
        val windowNames = mapOf(
            GAUSSE to "Гаусс",
            HANN to "Ханн",
            HAMMING to "Хамминг",
            BLACKMANN to "Блэкмэн",
            SQUARE to "Равномерная"
        )

        fun getName(id: Int): String {
            return windowNames[id] ?: id.toString()
        }

        fun getIdBy(windowName: String): Int {
            return windowNames.filterValues { it == windowName }.keys.first()
        }

        fun square(n: Int, frameSize: Int): Double {
            return 1.0
        }

        fun gausse(n: Int, frameSize: Int): Double {
            val a = (frameSize - 1) / 2
            var t = (n - a) / (0.5 * a)
            t *= t
            return exp(-t / 2)
        }

        fun hamming(n: Int, frameSize: Int): Double {
            return 0.54 - 0.46 * cos((2 * Math.PI * n) / (frameSize - 1))
        }

        fun hann(n: Int, frameSize: Int): Double {
            return 0.5 * (1 - cos((2 * Math.PI * n) / (frameSize - 1)))
        }

        fun blackmannHarris(n: Int, frameSize: Int): Double {
            return 0.35875 -
                    (0.48829 * cos((2 * Math.PI * n) / (frameSize - 1))) +
                    (0.14128 * cos((4 * Math.PI * n) / (frameSize - 1))) -
                    (0.01168 * cos((4 * Math.PI * n) / (frameSize - 1)))
        }
    }

}

