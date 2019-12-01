package alektas.telecomapp.domain.entities.converters

import kotlin.math.pow

class ValueConverter(private val bitDepth: Int) {
    private val levelsCount: Int = 2.0.pow(bitDepth.toDouble()).toInt() - 1

    /**
     * Конвертировать значения в строковое битовое представление.
     * При этом максимальное значение в целочисленном виде представляет собой
     * <code>2^bitDepth - 1</code>, а минимальное равно нулю.
     */
    fun convertToBitString(values: DoubleArray): String {
        val max = values.max() ?: 0.0
        val min = values.min() ?: 0.0
        val factor = levelsCount / (max - min)
        val sb = StringBuilder()
        values.forEach {
            sb.append(((it - min) * factor).toInt().toString(2))
        }
        return sb.toString()
    }

    /**
     * Конвертировать строковое битовое представление в массив десятичных значений от -1 до 1.
     */
    fun convertToValues(bitString: String): DoubleArray {
        val values = bitString.chunked(bitDepth) {
            var value = 0
            it.forEachIndexed { i, c ->
                value = value.or(c.toInt())
                if (i != bitDepth - 1) value = value.shl(1)
            }
            value
        }

        val halfLevels = levelsCount / 2.0
        return values.map { (it - halfLevels) / halfLevels }.toDoubleArray()
    }
}