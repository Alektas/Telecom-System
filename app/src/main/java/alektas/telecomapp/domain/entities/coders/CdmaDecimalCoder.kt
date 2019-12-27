package alektas.telecomapp.domain.entities.coders

import alektas.telecomapp.domain.entities.contracts.QpskContract

class CdmaDecimalCoder(private val threshold: Float = QpskContract.DEFAULT_SIGNAL_THRESHOLD) : Coder<DoubleArray> {

    /**
     * Кодирование биполярной информации.
     * Информационная посылка удлиняется в [code.size] раз.
     *
     * @param code биполярный код (массив из -1 и 1)
     * @param data биполярный массив данных (из -1 и 1)
     * @return массив кодированных данных (из -1 и 1). Если код пустой, то возвращается исходная информация.
     * Если информация отсутствует, то возвращается пустой массив.
     */
    override fun encode(code: DoubleArray, data: DoubleArray): DoubleArray {
        if (code.isEmpty() || data.isEmpty()) return data

        val spreadData = mutableListOf<Double>()
        data.forEach { value -> repeat(code.size) { spreadData.add(value * code[it]) } }

        return spreadData.toDoubleArray()
    }

    /**
     * Деодирование информации.
     * Информационная посылка в [code.size] меньше закодированных данных.
     *
     * @param code биполярный код (массив из -1 и 1)
     * @param codedData массив кодированных данных (любые значения)
     * @return информационная посылка из -1, 1 и 0, где 0 - это неопределенный бит (ошибка, либо отсутствие)
     * Если код пустой, то возвращается кодированная информация.
     * Если информация отсутствует, то возвращается пустой массив.
     */
    override fun decode(code: DoubleArray, codedData: DoubleArray): DoubleArray {
        if (code.isEmpty() || codedData.isEmpty()) return codedData

        return codedData.asList()
            .chunked(code.size) {
                it.foldIndexed(0.0) { i, acc, v -> acc + v * code[i] }
            }
            .map { normalize(it, threshold) }
            .toDoubleArray()
    }

    private fun normalize(value: Double, threshold: Float): Double {
        return when {
            value > threshold -> 1.0
            value < -threshold -> -1.0
            else -> 0.0
        }
    }

}

fun BooleanArray.toBipolar(): DoubleArray {
    return this.map { if (it) 1.0 else -1.0 }.toDoubleArray()
}

fun DoubleArray.toUnipolar(): BooleanArray {
    return this.map { it > 0 }.toBooleanArray()
}