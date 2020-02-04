package alektas.telecomapp.ui.datasource.simulation

import alektas.telecomapp.App
import alektas.telecomapp.domain.entities.generators.ChannelCodesGenerator
import alektas.telecomapp.domain.Repository
import alektas.telecomapp.domain.entities.SystemProcessor
import alektas.telecomapp.domain.entities.coders.DataCodesContract
import alektas.telecomapp.domain.entities.configs.ChannelsConfig
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.disposables.CompositeDisposable
import javax.inject.Inject

class ChannelsSettingsViewModel : ViewModel() {
    @Inject
    lateinit var storage: Repository
    @Inject
    lateinit var processor: SystemProcessor
    private val disposable = CompositeDisposable()
    val carrierFrequency = MutableLiveData<Double>()
    val dataSpeed = MutableLiveData<Double>()
    val channelCount = MutableLiveData<Int>()
    val channelCodeType = MutableLiveData<Int>()
    val channelCodeLength = MutableLiveData<Int>()
    val frameSize = MutableLiveData<Int>()
    val dataCodeType = MutableLiveData<Int>()
    val isSettingsChanged = MutableLiveData<Boolean>()

    init {
        App.component.inject(this)
    }

    fun setupChannelsConfig(
        countString: String,
        carrierFrequencyString: String,
        dataSpeedString: String,
        channelCodeTypeString: String,
        channelCodeLengthString: String,
        frameLengthString: String,
        isDataDecoding: Boolean,
        dataCodesTypeString: String
    ): Boolean {
        try {
            val config = buildConfig(
                countString,
                carrierFrequencyString,
                dataSpeedString,
                channelCodeTypeString,
                channelCodeLengthString,
                frameLengthString,
                isDataDecoding,
                dataCodesTypeString
            )
            processor.applyConfig(config)
            return true
        } catch (e: Exception) {
            return false
        }
    }

    private fun buildConfig(
        countString: String?,
        carrierFrequencyString: String?,
        dataSpeedString: String?,
        channelCodeTypeString: String?,
        channelCodeLengthString: String?,
        frameLengthString: String?,
        isDataDecoding: Boolean?,
        dataCodesTypeString: String?
    ): ChannelsConfig {
        val channelCount = countString?.let { parseChannelCount(it) }
        val freq = carrierFrequencyString?.let { parseFrequency(it) }
        val dataSpeed = dataSpeedString?.let { parseDataspeed(it) }
        val channelCodeLength = channelCodeLengthString?.let { parseFrameLength(it) }
        val channelCodeType = channelCodeTypeString?.let { ChannelCodesGenerator.getCodeTypeId(it) }
        val frameLength = frameLengthString?.let { parseFrameLength(it) }
        val dataCodesType = dataCodesTypeString?.let { DataCodesContract.getCodeTypeId(it) }

        if (channelCodeType != null && channelCodeType < 0
            || dataCodesType != null && dataCodesType < 0
        ) {
            throw NumberFormatException()
        }

        saveSettings(
            channelCount,
            freq,
            dataSpeed,
            channelCodeType,
            channelCodeLength,
            frameLength,
            dataCodesType
        )
        isSettingsChanged.value = false

        return ChannelsConfig(
            channelCount,
            freq,
            dataSpeed,
            channelCodeType,
            channelCodeLength,
            frameLength,
            isDataDecoding,
            dataCodesType
        )
    }

    fun setSettingsChanged() {
        isSettingsChanged.value = true
    }

    private fun saveSettings(
        channelCount: Int? = null,
        freq: Double? = null,
        dataSpeed: Double? = null,
        channelCodeType: Int? = null,
        channelCodeLength: Int? = null,
        frameLength: Int? = null,
        dataCodesType: Int? = null
    ) {
        channelCount?.let { this.channelCount.value = it }
        freq?.let { this.carrierFrequency.value = it }
        dataSpeed?.let { this.dataSpeed.value = it }
        channelCodeLength?.let { this.channelCodeLength.value = it }
        channelCodeType?.let { this.channelCodeType.value = it }
        frameLength?.let { this.frameSize.value = it }
        dataCodesType?.let { this.dataCodeType.value = it }
    }

    fun parseChannelCount(count: String): Int {
        val c = count.toInt()
        if (c <= 0) throw NumberFormatException()
        return c
    }

    fun parseFrequency(freqString: String): Double {
        val c = freqString.toDouble()
        if (c <= 0) throw NumberFormatException()
        return c
    }

    fun parseDataspeed(speed: String): Double {
        val c = speed.toDouble()
        if (c <= 0) throw NumberFormatException()
        return c
    }

    fun parseFrameLength(length: String): Int {
        val c = length.toInt()
        if (c <= 0) throw NumberFormatException()
        return c
    }

    override fun onCleared() {
        disposable.dispose()
        super.onCleared()
    }
}
