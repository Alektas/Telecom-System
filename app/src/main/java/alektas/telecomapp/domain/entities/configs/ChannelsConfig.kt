package alektas.telecomapp.domain.entities.configs

data class ChannelsConfig(
    var channelCount: Int? = null,
    var carrierFrequency: Double? = null, // МГц
    var dataSpeed: Double? = null, // кБит/с
    var channelsCodesType: Int? = null,
    var channelsCodesLength: Int? = null,
    var frameLength: Int? = null,
    var isDataCoding: Boolean? = null,
    var dataCodesType: Int? = null
) {
    fun update(config: ChannelsConfig) {
        config.channelCount?.let { channelCount = it }
        config.carrierFrequency?.let { carrierFrequency = it }
        config.dataSpeed?.let { dataSpeed = it }
        config.channelsCodesType?.let { channelsCodesType = it }
        config.channelsCodesLength?.let { channelsCodesLength = it }
        config.frameLength?.let { frameLength = it }
        config.isDataCoding?.let { isDataCoding = it }
        config.dataCodesType?.let { dataCodesType = it }
    }
}