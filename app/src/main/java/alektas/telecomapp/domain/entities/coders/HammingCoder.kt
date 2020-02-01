package alektas.telecomapp.domain.entities.coders

/**
 * Coder that transforms information data bits to the Hamming code and vice versa.
 * Hamming codes allows to fix one error bit in code word.
 * If the data is larger than the code word can contain, then it will be divided to the several words.
 *
 * @param wordSize size of the code words. Warning! It must be greater than 2!
 */
class HammingCoder(val wordSize: Int) {
    val parityBitsInWord = getParityBitsCount(wordSize)
    val dataBitsInWord = wordSize - parityBitsInWord
    /**
     * Characteristic of the coder that display fraction of extra parity bits in the each word.
     */
    val redundancy = parityBitsInWord / wordSize.toFloat()
    /**
     * Characteristic of the coder that display fraction of information bits in the each word.
     */
    val rate = dataBitsInWord / wordSize.toFloat()
    private val parityIndices = IntArray(parityBitsInWord) { 1.shl(it) - 1 }

    /**
     * Encode data by Hamming code.
     * It adds extra parity bits, so output data is larger than input.
     * If the data is not a multiple of [wordSize], extra zero bits adds to the end of the data
     * (to find out how much use [getExtraBitsCount] method).
     *
     * @param data arbitrary number of information bits
     * @return coded data (concatenation of the code words)
     */
    fun encode(data: BooleanArray): BooleanArray {
        if (data.isEmpty()) return booleanArrayOf()
        return data
            .toRowCodeWords()
            .map { encode(it).bits }
            .toFlatData()
    }

    /**
     * Decode coded data (concatenation of the code words) by Hamming code.
     *
     * @param codedData concatenation of the code words' bits
     * @return information bits, extracted from the coded data
     */
    fun decode(codedData: BooleanArray): BooleanArray {
        if (codedData.isEmpty()) return booleanArrayOf()
        return codedData
            .asCodeWords()
            .map { decode(it) }
            .toFlatData()
    }

    /**
     * Calculate extra bits count, that will be added to the end of the data while encoding
     * to fill the last code word.
     *
     * @param dataSize bits count of the data that will be or were encoded
     * @return extra bits count
     */
    fun getExtraBitsCount(dataSize: Int): Int {
        val reminder = dataSize % dataBitsInWord
        if (reminder == 0) return 0
        return dataBitsInWord - reminder
    }

    private fun encode(word: CodeWord): CodeWord {
        val parityBits = calculateParityBits(word)
        return word.with(parityBits)
    }

    private fun decode(word: CodeWord): BooleanArray {
        val parityBits = calculateParityBits(word)
        val invalidIndex = parityBits.toInvalidBitIndex()
        return word.extractValidData(invalidIndex)
    }

    /**
     * @return For encoding this method return parity bits.
     * For decoding returns array of zeros if there are no errors in the block. If there is
     * a error in the block, then returns inverted bits of the error position. To translate
     * the result to the error bit index use [ParityBits.toInvalidBitIndex].
     */
    private fun calculateParityBits(word: CodeWord): ParityBits {
        val bits = BooleanArray(parityBitsInWord)
        for (i in word.indices) {
            if (!word[i]) continue // count only '1' bits
            var position = i + 1
            var k = 0 // parity bits array pointer
            while (position != 0) {
                // count only for the corresponding parity bits
                if (Integer.lowestOneBit(position) == 1) {
                    bits[k] = bits[k].xor(word[i])
                }
                k++
                position = position.ushr(1)
            }
        }
        return ParityBits(bits)
    }

    private fun getParityBitsCount(wordSize: Int): Int {
        var x = wordSize
        var count = 0
        while (x != 0) {
            count++
            x = x.ushr(1)
        }
        return count
    }

    private fun List<BooleanArray>.toFlatData(): BooleanArray =
        this.foldIndexed(BooleanArray(this.size * this[0].size)) { i, acc, block ->
            block.copyInto(acc, i * this[0].size)
        }

    /**
     * Divide data flow to the code words (each of [wordSize] length) with empty parity bits
     */
    private fun BooleanArray.toRowCodeWords(): List<CodeWord> =
        this.toList().chunked(dataBitsInWord) { it.toRowCodeWord() }

    /**
     * Convert list of data bits to the code word with empty parity bits. If the data is lesser than
     * the code word, adds extra zero bits to fill the word.
     */
    private fun List<Boolean>.toRowCodeWord(): CodeWord {
        var pointer = 0 // pointer for the data array
        val data = BooleanArray(wordSize)
        for (i in data.indices) {
            if (parityIndices.contains(i) || pointer >= this.size) {
                data[i] = false
                continue
            }
            data[i] = this[pointer]
            pointer++
        }
        return CodeWord(data)
    }

    private fun BooleanArray.asCodeWords(): List<CodeWord> =
        this.asList().chunked(wordSize) { CodeWord(it.toBooleanArray()) }

    inner class CodeWord(val bits: BooleanArray) {
        val size = bits.size
        val indices = bits.indices

        operator fun get(index: Int): Boolean = bits[index]

        fun with(parityBits: ParityBits): CodeWord {
            parityBits.bits.forEachIndexed { p, b ->
                this.bits[1.shl(p) - 1] = b
            }
            return this
        }

        fun extractValidData(invalidIndex: Int): BooleanArray {
            val data = mutableListOf<Boolean>()
            bits.forEachIndexed { i, b ->
                if (!parityIndices.contains(i)) {
                    data.add(if (i == invalidIndex) b.not() else b)
                }
            }
            return data.toBooleanArray()
        }
    }

    inner class ParityBits(val bits: BooleanArray) {
        val size = bits.size

        operator fun get(index: Int): Boolean = bits[index]

        /**
         * Translate parity bits to the error bit index in the data block.
         *
         * @return error bit index. If there is no errors, returns -1
         */
        fun toInvalidBitIndex(): Int {
            val invalidPosition = bits.foldRight(0) { b, acc ->
                acc.shl(1) + if (b) 1 else 0
            }
            return invalidPosition - 1
        }
    }
}