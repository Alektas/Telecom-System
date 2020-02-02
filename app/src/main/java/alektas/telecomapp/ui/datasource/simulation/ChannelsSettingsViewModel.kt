package alektas.telecomapp.ui.datasource.simulation

import alektas.telecomapp.App
import alektas.telecomapp.domain.entities.generators.ChannelCodesGenerator
import alektas.telecomapp.domain.Repository
import alektas.telecomapp.domain.entities.SystemProcessor
import alektas.telecomapp.domain.entities.coders.DataCodesContract
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.disposables.CompositeDisposable
import javax.inject.Inject

private const val MINIMAL_CODE_WORD_LENGTH = 3

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
    val dataCodeLength = MutableLiveData<Int>()
    val isSettingsChanged = MutableLiveData<Boolean>()

    init {
        App.component.inject(this)
    }

    fun createChannels(
        countString: String,
        carrierFrequencyString: String,
        dataSpeedString: String,
        channelCodeLengthString: String,
        channelCodeTypeString: String,
        frameLengthString: String
    ): Boolean {
        val channelCount = parseChannelCount(countString)
        val freq = parseFrequency(carrierFrequencyString)
        val dataSpeed = parseDataspeed(dataSpeedString)
        val channelCodeLength = parseFrameLength(channelCodeLengthString)
        val channelCodeType = ChannelCodesGenerator.getCodeTypeId(channelCodeTypeString)
        val frameLength = parseFrameLength(frameLengthString)

        if (channelCount <= 0 || freq <= 0 || dataSpeed <= 0 || channelCodeLength <= 0
            || channelCodeType < 0 || frameLength <= 0
        ) return false

        saveChannelsSettings(
            channelCount,
            freq,
            dataSpeed,
            channelCodeType,
            channelCodeLength,
            frameLength
        )
        isSettingsChanged.value = false

        processor.createChannels(
            channelCount,
            freq,
            dataSpeed,
            channelCodeType,
            channelCodeLength,
            frameLength
        )

        return true
    }

    fun enableDataCoding(codesTypeString: String, codesLengthString: String): Boolean {
        val codesType = DataCodesContract.getCodeTypeId(codesTypeString)
        val codesLength = parseCodeWordLength(codesLengthString)

        if (codesType < 0 || codesLength < MINIMAL_CODE_WORD_LENGTH) return false

        this.dataCodeType.value = codesType
        this.dataCodeLength.value = codesLength
        processor.setDataCoding(codesType, codesLength)
        return true
    }

    fun disableDataCoding() {
        processor.disableDataCoding()
    }

    fun setSettingsChanged() {
        isSettingsChanged.value = true
    }

    private fun saveChannelsSettings(
        channelCount: Int,
        freq: Double,
        dataSpeed: Double,
        channelCodeType: Int,
        channelCodeLength: Int,
        frameLength: Int
    ) {
        this.channelCount.value = channelCount
        carrierFrequency.value = freq
        this.dataSpeed.value = dataSpeed
        this.channelCodeLength.value = channelCodeLength
        this.channelCodeType.value = channelCodeType
        frameSize.value = frameLength
    }

    fun parseChannelCount(count: String): Int {
        return try {
            val c = count.toInt()
            if (c <= 0) throw NumberFormatException()
            c
        } catch (e: NumberFormatException) {
            -1
        }
    }

    fun parseFrequency(freqString: String): Double {
        return try {
            val c = freqString.toDouble()
            if (c <= 0) throw NumberFormatException()
            c
        } catch (e: NumberFormatException) {
            -1.0
        }
    }

    fun parseDataspeed(speed: String): Double {
        return try {
            val c = speed.toDouble()
            if (c <= 0) throw NumberFormatException()
            c
        } catch (e: NumberFormatException) {
            -1.0
        }
    }

    fun parseFrameLength(length: String): Int {
        return try {
            val c = length.toInt()
            if (c <= 0) throw NumberFormatException()
            c
        } catch (e: NumberFormatException) {
            -1
        }
    }

    fun parseCodeWordLength(length: String): Int {
        return try {
            val c = length.toInt()
            if (c < MINIMAL_CODE_WORD_LENGTH) throw NumberFormatException()
            c
        } catch (e: NumberFormatException) {
            -1
        }
    }

    override fun onCleared() {
        disposable.dispose()
        super.onCleared()
    }
}
