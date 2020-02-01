package alektas.telecomapp.domain.entities.configs

data class ChannelsConfig(
    val channelCount: Int,
    val carrierFrequency: Double,
    val dataSpeed: Double,
    val channelsCodeLength: Int,
    val frameLength: Int,
    val channelsCodeType: Int
)