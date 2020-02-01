package alektas.telecomapp.domain.entities.configs

data class DecoderConfig(
    var isAutoDetection: Boolean,
    var channelCount: Int? = null,
    var channelsCodeLength: Int? = null,
    var channelsCodeType: Int? = null,
    var threshold: Float? = null
) {
    fun update(config: DecoderConfig) {
        isAutoDetection = config.isAutoDetection
        config.channelCount?.let { channelCount = it }
        config.channelsCodeLength?.let { channelsCodeLength = it }
        config.channelsCodeType?.let { channelsCodeType = it }
        config.threshold?.let { threshold = it }
    }
}