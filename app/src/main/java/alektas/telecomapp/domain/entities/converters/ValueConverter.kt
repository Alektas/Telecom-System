package alektas.telecomapp.domain.entities.converters

import kotlin.math.pow

class ValueConverter(private val bitDepth: Int) {
    val maxLevel: Int = 2.0.pow(bitDepth.toDouble()).toInt() - 1

    /**
     * Конвертировать значения в строковое битовое представление.
     * При этом максимальное значение в целочисленном виде представляет собой
     * {@code 2^bitDepth - 1}, а минимальное равно нулю.
     */
    fun convertToBitString(values: DoubleArray): String {
        val max = values.max() ?: 0.0
        val min = values.min() ?: 0.0
        val factor = if (max == min) 0.0 else maxLevel / (max - min)
        val sb = StringBuilder()
        values.forEach {
            val v = ((it - min) * factor).toInt()
            sb.append(toBinaryString(v))
        }
        return sb.toString()
    }

    /**
     * Конвертировать строковое битовое представление в массив десятичных значений от -1 до 1.
     */
    fun convertToBipolarNormalizedValues(bitString: String): DoubleArray {
        val values = bitString.chunked(bitDepth) {
            var value = 0
            it.forEachIndexed { i, c ->
                val bit = if (c == '0') 0 else 1
                value = value.or(bit)
                if (i != bitDepth - 1) value = value.shl(1)
            }
            value
        }

        val halfLevel = maxLevel / 2.0
        return values.map { (it - halfLevel) / halfLevel }.toDoubleArray()
    }

    /**
     * Преобразование целого числа в двоичную строку.
     * Если полученное двоичное число содержит бит меньше, чем [bitDepth],
     * то строка спереди дополняется нулями.
     */
    fun toBinaryString(int: Int): String =
        Integer.toBinaryString(int).padStart(bitDepth, '0')
}