package alektas.telecomapp.domain.entities.coders

import alektas.telecomapp.domain.entities.CdmaContract

class CdmaCoder : Coder<Array<Boolean>> {

    /**
     * Сложение двух двоичных чисел (кода и информации) по модулю 2 (XOR)
     * (0 + 1 = 1 + 0 = 1; 0 + 0 = 0; 1 + 1 = 0).
     *
     * Информационная посылка удлиняется в {@link CdmaContract#SPREAD_RATIO} раз.
     * То есть если {@link CdmaContract#SPREAD_RATIO} = 2,
     * то информация [1, 0, 1] -> [1, 1, 0, 0, 1, 1].
     *
     * Если длина кода меньше, чем длина информации, то код увеличивается до размера информации
     * путем циклического повторения. То есть если информация [1, 0, 1, 1, 0], то код [0, 1, 1] ->
     * [0, 1, 1, 0, 1].
     *
     * @return двоичное число. Если код равен 0, то возвращается исходная информация.
     * Если информация отсутствует, то возвращается 0.
     */
    override fun encode(
        code: Array<Boolean>,
        data: Array<Boolean>
    ): Array<Boolean> {
        if (code.isEmpty() || data.isEmpty()) return data

        val spreadData = mutableListOf<Boolean>()
        data.forEach { bit -> repeat(CdmaContract.SPREAD_RATIO) { spreadData.add(bit) } }

        val sum = mutableListOf<Boolean>()
        var j = 0
        for (element in spreadData) {
            if (j == code.size) j = 0
            sum.add(element.xor(code[j]))
            j++
        }

        return sum.toTypedArray()
    }

    override fun decode(code: Array<Boolean>): Array<Boolean> {
        return arrayOf()
    }

}