package alektas.telecomapp.domain.entities.configs

data class ChannelsConfig(
    val channelCount: Int,
    val carrierFrequency: Double,
    val dataSpeed: Double,
    val channelsCodesType: Int,
    val channelsCodesLength: Int,
    val frameLength: Int,
    val isDataCodingEnabled: Boolean,
    val dataCodesType: Int,
    val dataCodesLength: Int
)