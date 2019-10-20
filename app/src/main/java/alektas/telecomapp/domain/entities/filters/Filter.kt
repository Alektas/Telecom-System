package alektas.telecomapp.domain.entities.filters

interface Filter {
    fun filter(data: DoubleArray): DoubleArray
    fun impulseResponse(): DoubleArray
}