package alektas.telecomapp.domain.entities.coders

import alektas.telecomapp.domain.entities.CdmaContract

class CdmaCoder : Coder<Array<Boolean>> {

    /**
     * Кодирование информации путем сложения каждого бита информации с кодом по модулю 2 (XOR)
     * (0 + 1 = 1 + 0 = 1; 0 + 0 = 0; 1 + 1 = 0).
     * Информационная посылка удлиняется в {@link CdmaContract#SPREAD_RATIO} раз.
     *
     * @return двоичное число. Если код равен 0, то возвращается исходная информация.
     * Если информация отсутствует, то возвращается 0.
     */
    override fun encode(code: Array<Boolean>, data: Array<Boolean>): Array<Boolean> {
        if (code.isEmpty() || data.isEmpty()) return data

        val spreadData = mutableListOf<Boolean>()
        data.forEach { bit -> repeat(CdmaContract.CODE_LENGTH) { spreadData.add(bit.xor(code[it])) } }

        return spreadData.toTypedArray()
    }

    override fun decode(code: Array<Boolean>, codedData: Array<Boolean>): Array<Boolean> {
        return codedData.asList()
            .chunked(code.size) {
                average(it.mapIndexed { i, bit -> bit.xor(code[i]) })
            }
            .toTypedArray()
    }

    private fun average(data: List<Boolean>): Boolean {
        return data.count { it }.let { it > data.size - it }
    }

}