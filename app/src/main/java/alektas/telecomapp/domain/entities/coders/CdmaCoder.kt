package alektas.telecomapp.domain.entities.coders

class CdmaCoder : Coder<BooleanArray> {

    /**
     * Кодирование информации путем сложения каждого бита информации с кодом по модулю 2 (XOR)
     * (0 + 1 = 1 + 0 = 1; 0 + 0 = 0; 1 + 1 = 0).
     * Информационная посылка удлиняется в {@link CdmaContract#SPREAD_RATIO} раз.
     *
     * @return массив битов. Если код равен 0, то возвращается исходная информация.
     * Если информация отсутствует, то возвращается 0.
     */
    override fun encode(code: BooleanArray, data: BooleanArray): BooleanArray {
        if (code.isEmpty() || data.isEmpty()) return data

        val spreadData = mutableListOf<Boolean>()
        data.forEach { bit -> repeat(code.size) { spreadData.add(bit.xor(code[it])) } }

        return spreadData.toBooleanArray()
    }

    override fun decode(code: BooleanArray, codedData: BooleanArray): BooleanArray {
        return codedData.asList()
            .chunked(code.size) {
                average(it.mapIndexed { i, bit -> bit.xor(code[i]) })
            }
            .toBooleanArray()
    }

    private fun average(data: List<Boolean>): Boolean {
        return data.count { it }.let { it > data.size - it }
    }

}