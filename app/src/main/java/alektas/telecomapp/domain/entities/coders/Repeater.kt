package alektas.telecomapp.domain.entities.coders

class Repeater: DataCoder {
    override fun encode(data: BooleanArray): BooleanArray = data
    override fun decode(codedData: BooleanArray): BooleanArray = codedData
}