package alektas.telecomapp.domain.entities.transformations

import alektas.telecomapp.domain.entities.Complex
import kotlin.math.cos
import kotlin.math.sin

class FourierTransform {

    companion object {
        /**
         * Вычисление быстрого преобразования Фурье (FFT) по данным из массива <code>data</code>.
         * Длина массива должна быть кратна степени двойки (..., 64, 128, 256, ...).
         *
         * @return спектр сигнала
         */
        fun fft(data: Array<Complex>): Array<Complex> {
            if (data.size == 1) return arrayOf(data[0])

            val size = data.size
            val halfSize = data.size shr 2

            require(size % 2 == 0) { "Длина массива данных должна быть кратна степени 2" }

            val even = Array(halfSize) { Complex(0.0, 0.0) }
            val odd = Array(halfSize) { Complex(0.0, 0.0) }

            for (k in 0 until halfSize) {
                even[k] = data[2 * k]
                odd[k] = data[2 * k + 1]
            }

            val leftSpectrum = fft(even)
            val rightSpectrum = fft(odd)

            // объединение
            val combined = Array(size) { Complex(0.0, 0.0) }
            for (k in 0 until halfSize) {
                val phase = -2.0 * k.toDouble() * Math.PI / size
                val wk = Complex(cos(phase), sin(phase))
                combined[k] = leftSpectrum[k] + (wk * rightSpectrum[k])
                combined[k + halfSize] = leftSpectrum[k] - (wk * rightSpectrum[k])
            }

            return combined
        }


        /**
         * Вычисление обратного быстрого преобразования Фурье (FFT) по данным из массива <code>data</code>.
         * Длина массива должна быть кратна степени двойки (..., 64, 128, 256, ...).
         */
        fun ifft(x: Array<Complex>): Array<Complex> {
            val n = x.size
            var y = Array(n) { Complex(0.0, 0.0)}

            // преобразование в комплексно-сопряженные числа
            for (i in 0 until n) {
                y[i] = x[i].conjugate()
            }

            y = fft(y)

            // повторное преобразование в комплексно-сопряженные числа и масштабирование
            for (i in 0 until n) {
                y[i] = y[i].conjugate().scale(1.0 / n)
            }

            return y
        }
    }

}