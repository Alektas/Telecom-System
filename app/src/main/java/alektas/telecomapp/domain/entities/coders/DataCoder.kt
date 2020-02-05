package alektas.telecomapp.domain.entities.coders

interface DataCoder {
    fun encode(data: BooleanArray): BooleanArray
    fun decode(codedData: BooleanArray): BooleanArray
}