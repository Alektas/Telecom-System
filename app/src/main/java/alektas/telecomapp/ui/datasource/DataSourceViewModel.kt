package alektas.telecomapp.ui.datasource

import alektas.telecomapp.App
import alektas.telecomapp.data.CodeGenerator
import alektas.telecomapp.domain.Repository
import alektas.telecomapp.domain.entities.ChannelData
import alektas.telecomapp.domain.entities.SystemProcessor
import alektas.telecomapp.utils.toDataPoints
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.jjoe64.graphview.series.DataPoint
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.DisposableObserver
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

class DataSourceViewModel : ViewModel() {
    @Inject
    lateinit var storage: Repository
    @Inject
    lateinit var processor: SystemProcessor
    private val disposable = CompositeDisposable()
    val channels = MutableLiveData<List<ChannelData>>()
    val ether = MutableLiveData<Array<DataPoint>>()
    val adcFrequency = MutableLiveData<Double>()
    val noiseSnr = MutableLiveData<Double>()
    val carrierFrequency = MutableLiveData<Double>()
    val dataSpeed = MutableLiveData<Double>()
    val channelCount = MutableLiveData<Int>()
    val codeType = MutableLiveData<Int>()
    val frameSize = MutableLiveData<Int>()
    val codeSize = MutableLiveData<Int>()

    init {
        App.component.inject(this)

        disposable.addAll(
            storage.observeChannels()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableObserver<List<ChannelData>>() {
                    override fun onNext(it: List<ChannelData>) {
                        channels.value = it
                    }

                    override fun onComplete() {}

                    override fun onError(e: Throwable) {}
                }),

            storage.observeEther()
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.computation())
                .map { it.toDataPoints() }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableObserver<Array<DataPoint>>() {
                    override fun onNext(s: Array<DataPoint>) {
                        ether.value = s
                    }

                    override fun onComplete() {}

                    override fun onError(e: Throwable) {}
                })
        )
    }

    companion object {
        const val INVALID_SNR = -1000.0
    }

    fun enableNoise() {
        processor.enableNoise()
    }

    fun disableNoise() {
        processor.disableNoise()
    }

    fun setNoise(snrString: String) {
        val snr = parseSnr(snrString)

        if (snr == INVALID_SNR) return

        noiseSnr.value = snr
        processor.setNoise(snr)
    }

    fun setAdcFrequency(freqString: String) {
        val freq = parseFrequency(freqString)

        if (freq <= 0) return

        adcFrequency.value = freq
        processor.setAdcFrequency(freq)
    }

    fun removeChannel(channel: ChannelData) {
        processor.removeChannel(channel)
    }

    fun generateChannels(
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

        if (channelCount <= 0 || freq <= 0 || dataSpeed <= 0 || codeLength <= 0 || frameLength <= 0 || codeType < 0) return

        saveChannelsSettings(channelCount, freq, dataSpeed, codeLength, frameLength, codeType)

        processor.generateChannels(channelCount, freq, dataSpeed, codeLength, frameLength, codeType)
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

    fun parseSnr(snr: String): Double {
        return try {
            snr.toDouble()
        } catch (e: NumberFormatException) {
            INVALID_SNR
        }
    }

    override fun onCleared() {
        disposable.dispose()
        super.onCleared()
    }

}
