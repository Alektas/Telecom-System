package alektas.telecomapp.domain.entities.configs

data class DecoderConfig(
    val channelCount: Int,
    val codeLength: Int,
    val codeType: Int,
    val threshold: Float
)