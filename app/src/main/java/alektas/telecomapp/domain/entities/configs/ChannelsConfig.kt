package alektas.telecomapp.domain.entities.configs

data class ChannelsConfig(
    val channelCount: Int,
    val carrierFrequency: Double,
    val dataSpeed: Double,
    val codeLength: Int,
    val frameLength: Int,
    val codeType: Int
)