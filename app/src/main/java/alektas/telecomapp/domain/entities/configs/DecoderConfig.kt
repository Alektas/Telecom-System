package alektas.telecomapp.domain.entities.configs

data class DecoderConfig(
    var isAutoDetection: Boolean,
    var channelCount: Int? = null,
    var codeLength: Int? = null,
    var codeType: Int? = null,
    var threshold: Float? = null
) {
    fun update(config: DecoderConfig) {
        isAutoDetection = config.isAutoDetection
        config.channelCount?.let { channelCount = it }
        config.codeLength?.let { codeLength = it }
        config.codeType?.let { codeType = it }
        config.threshold?.let { threshold = it }
    }
}