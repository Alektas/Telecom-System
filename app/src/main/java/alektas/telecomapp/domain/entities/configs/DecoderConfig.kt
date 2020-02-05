package alektas.telecomapp.domain.entities.configs

data class DecoderConfig(
    var isAutoDetection: Boolean? = null,
    var channelCount: Int? = null,
    var channelsCodeType: Int? = null,
    var channelsCodeLength: Int? = null,
    var threshold: Float? = null,
    var isDataCoding: Boolean? = null,
    var dataCodesType: Int? = null
) {
    fun update(config: DecoderConfig) {
        config.isAutoDetection?.let { isAutoDetection = it }
        config.channelCount?.let { channelCount = it }
        config.channelsCodeType?.let { channelsCodeType = it }
        config.channelsCodeLength?.let { channelsCodeLength = it }
        config.threshold?.let { threshold = it }
        config.isDataCoding?.let { isDataCoding = it }
        config.dataCodesType?.let { dataCodesType = it }
    }
}