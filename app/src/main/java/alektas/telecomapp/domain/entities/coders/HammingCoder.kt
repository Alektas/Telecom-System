package alektas.telecomapp.domain.entities.coders

import alektas.telecomapp.utils.isPowerOfTwo
import androidx.annotation.VisibleForTesting
import kotlin.math.pow

class HammingCoder {

    fun encode(data: BooleanArray): BooleanArray {
        val codedData = data.spread()
        val parityBits = calcParityBits(codedData)
        codedData.fillBy(parityBits)
        return codedData
    }

    fun decode(codedData: BooleanArray): BooleanArray {
        val calculatedParityBits = calcParityBits(codedData)
        val invalidIndex = getInvalidBitIndex(calculatedParityBits)
        return codedData.extractValidData(invalidIndex)
    }

    private fun BooleanArray.spread(): BooleanArray {
        val codedData = mutableListOf<Boolean>()
        var dataIndex = 0
        var counter = 1
        while (dataIndex != this.size) {
            if (counter.isPowerOfTwo()) {
                codedData.add(false)
            } else {
                codedData.add(this[dataIndex])
                dataIndex++
            }
            counter++
        }
        return codedData.toBooleanArray()
    }

    /**
     * @return For encoding this method return parity bits.
     * For decoding returns array of zeros if there are no errors in the block. If there is
     * a error in the block, then returns inverted bits of the error position. To translate
     * the result to the error bit index use [getInvalidBitIndex].
     */
    @VisibleForTesting
    fun calcParityBits(block: BooleanArray): BooleanArray {
        val parityBitsCount = parityBitsCount(block.size)
        val parityBits = BooleanArray(parityBitsCount)
        for (i in block.indices) {
            if (!block[i]) continue // count only '1' bits
            var position = i + 1
            var k = 0 // parity bits array pointer
            while (position != 0) {
                // count only for the corresponding parity bits
                if (Integer.lowestOneBit(position) == 1) {
                    parityBits[k] = parityBits[k].xor(block[i])
                }
                k++
                position = position.ushr(1)
            }
        }
        return parityBits
    }

    private fun BooleanArray.fillBy(parityBits: BooleanArray) {
        parityBits.forEachIndexed { p, b ->
            val position = 2.0.pow(p).toInt()
            this[position - 1] = b
        }
    }

    @VisibleForTesting
    fun getParityBits(block: BooleanArray): BooleanArray {
        val size = parityBitsCount(block.size)
        val parityBits = BooleanArray(size)
        var blockPointer = 1
        var i = 0
        while (blockPointer < block.size) {
            parityBits[i] = block[blockPointer - 1]
            i++
            blockPointer = blockPointer.shl(1)
        }
        return parityBits
    }

    private fun BooleanArray.extractValidData(invalidIndex: Int): BooleanArray {
        val data = mutableListOf<Boolean>()
        this.forEachIndexed { i, b ->
            if (!(i + 1).isPowerOfTwo()) {
                data.add(if (i == invalidIndex) b.not() else b)
            }
        }
        return data.toBooleanArray()
    }

    private fun getInvalidBitIndex(
        calculatedParityBits: BooleanArray,
        receivedParityBits: BooleanArray
    ): Int {
        return calculatedParityBits.foldIndexed(0) { i, acc: Int, b ->
            if (b != receivedParityBits[i]) acc + i else acc
        }
    }

    /**
     * Translate parity bits to the error bit index in the data block.
     *
     * @return error bit index. If there is no errors, returns -1
     */
    private fun getInvalidBitIndex(parityBits: BooleanArray): Int {
        val invalidPosition = parityBits.foldRight(0) { b, acc ->
            acc.shl(1) + if (b) 1 else 0
        }
        return invalidPosition - 1
    }

    @VisibleForTesting
    fun parityBitsCount(blockSize: Int): Int {
        var x = blockSize
        var count = 0
        while (x != 0) {
            count++
            x = x.ushr(1)
        }
        return count
    }
}