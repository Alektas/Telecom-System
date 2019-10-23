package alektas.telecomapp.data

import kotlin.random.Random

class CodeGenerator {

    companion object {
        const val RANDOM = 0
        const val WALSH = 1
    }

    fun generateRandomCodes(count: Int, length: Int): Array<BooleanArray> {
        val codes = Array(count) { BooleanArray(length) }
        for (i in 0 until count) {
            codes[i] = generateRandomCode(length)
        }
        return codes
    }

    fun generateRandomCode(length: Int): BooleanArray {
        val r = Random.Default
        return BooleanArray(length) { r.nextBoolean() }
    }

    /**
     * Генерирует матрицу кодов Уолша.
     * Матрицы Уолша могут быть только размера, кратного степени 2, поэтому размер матрицы может
     * превышать количество требуемых кодов <code>codeCount</code>, но никода не будет меньше.
     */
    fun generateWalshMatrix(codeCount: Int): Array<BooleanArray> {
        return transformHadamardToWalsh(generateHadamardMatrix(codeCount))
    }

    private fun transformHadamardToWalsh(hadamardMatrix: Array<BooleanArray>): Array<BooleanArray> {
        val walshMatrix = Array(hadamardMatrix.size) { BooleanArray(hadamardMatrix.size) }
        hadamardMatrix.forEachIndexed { y, raw ->
            val inv = y.inv()
            val gray = inv.xor(inv.shr(1))
            walshMatrix[gray] = raw
        }
        return walshMatrix
    }

    /**
     * Генерирует матрицу кодов Адамара.
     * Матрицы Адамара могут быть только размера, кратного степени 2, поэтому размер матрицы может
     * превышать количество требуемых кодов <code>codeCount</code>, но никода не будет меньше.
     */
    fun generateHadamardMatrix(codeCount: Int): Array<BooleanArray> {
        val hadamardMatrixCore = arrayOf(
            booleanArrayOf(true, true),
            booleanArrayOf(true, false)
        )

        return generateHadamardMatrix(codeCount, hadamardMatrixCore)
    }

    private fun generateHadamardMatrix(codeCount: Int, core: Array<BooleanArray>): Array<BooleanArray> {
        if (core.size >= codeCount) return core

        val size = 2 * core.size
        val matrix =  Array(size) { y ->
            val raw = BooleanArray(size)
            for (x in 0 until size) {
                raw[x] =
                    when {
                        x < core.size && y < core.size -> core[y][x]
                        y < core.size -> core[y][x - core.size]
                        x < core.size -> core[y - core.size][x]
                        else -> core[y - core.size][x - core.size].xor(true)
                    }
            }
            raw
        }

        return generateHadamardMatrix(codeCount, matrix)
    }


}