package alektas.telecomapp.domain.entities.coders

interface Coder<T> {
    fun encode(code: T, data: T): T
    fun decode(code: T, codedData: T): T
}