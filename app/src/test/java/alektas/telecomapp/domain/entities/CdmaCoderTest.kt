package alektas.telecomapp.domain.entities

import alektas.telecomapp.data.CodesProvider
import alektas.telecomapp.data.UserDataProvider
import alektas.telecomapp.domain.entities.coders.CdmaCoder
import org.junit.Test

import org.junit.Assert.*
import org.junit.Before

class CdmaCoderTest {
    private lateinit var coder: CdmaCoder

    @Before
    fun prepare() {
        coder = CdmaCoder()
    }

    /**
     * Длина закодированных данных в битах должна быть: L = S * D, где
     * S - коэффициент расширения, равный количеству битов кода, приходящихся на 1 бит информации
     * D - длина данных в битах
     */
    @Test
    fun encode_length_isCorrect() {
        val code = CodesProvider.generateCode(CdmaContract.CODE_LENGTH)
        val data = UserDataProvider.generateData(CdmaContract.DATA_FRAME_LENGTH)

        val size = coder.encode(code, data).size

        assertTrue(CdmaContract.SPREAD_RATIO * CdmaContract.DATA_FRAME_LENGTH == size)
    }
}