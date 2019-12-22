package alektas.telecomapp.ui.datasource.simulation

import alektas.telecomapp.App
import alektas.telecomapp.data.CodeGenerator
import alektas.telecomapp.domain.Repository
import alektas.telecomapp.domain.entities.SystemProcessor
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
    val codeType = MutableLiveData<Int>()
    val frameSize = MutableLiveData<Int>()
    val codeSize = MutableLiveData<Int>()

    init {
        App.component.inject(this)
    }

    fun createChannels(
        countString: String,
        carrierFrequencyString: String,
        dataSpeedString: String,
        codeLengthString: String,
        frameLengthString: String,
        codeTypeString: String
    ) {
        val channelCount = parseChannelCount(countString)
        val freq = parseFrequency(carrierFrequencyString)
        val dataSpeed = parseDataspeed(dataSpeedString)
        val codeLength = parseFrameLength(codeLengthString)
        val frameLength = parseFrameLength(frameLengthString)
        val codeType = CodeGenerator.getCodeTypeId(codeTypeString)

        if (channelCount <= 0 || freq <= 0 || dataSpeed <= 0 || codeLength <= 0 ||
            frameLength <= 0 || codeType < 0) return

        saveChannelsSettings(channelCount, freq, dataSpeed, codeLength, frameLength, codeType)

        processor.createChannels(channelCount, freq, dataSpeed, codeLength, frameLength, codeType)
    }

    private fun saveChannelsSettings(
        channelCount: Int,
        freq: Double,
        dataSpeed: Double,
        codeLength: Int,
        frameLength: Int,
        codeType: Int
    ) {
        this.channelCount.value = channelCount
        carrierFrequency.value = freq
        this.dataSpeed.value = dataSpeed
        frameSize.value = frameLength
        codeSize.value = codeLength
        this.codeType.value = codeType
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

    override fun onCleared() {
        disposable.dispose()
        super.onCleared()
    }
}
