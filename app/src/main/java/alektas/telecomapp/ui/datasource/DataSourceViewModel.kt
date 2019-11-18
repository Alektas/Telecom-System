package alektas.telecomapp.ui.datasource

import alektas.telecomapp.App
import alektas.telecomapp.data.CodeGenerator
import alektas.telecomapp.domain.Repository
import alektas.telecomapp.domain.entities.ChannelData
import alektas.telecomapp.domain.entities.SystemProcessor
import alektas.telecomapp.domain.entities.signals.Signal
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
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableObserver<Signal>() {
                    override fun onNext(s: Signal) {
                        ether.value = s.toDataPoints()
                    }

                    override fun onComplete() {}

                    override fun onError(e: Throwable) {}
                })
        )
    }

    fun generateChannels(
        countString: String,
        carrierFrequencyString: String,
        dataSpeedString: String,
        frameLengthString: String,
        codeTypeString: String
    ) {
        val channelCount = parseChannelCount(countString)
        val freq = parseFrequency(carrierFrequencyString)
        val dataSpeed = parseDataspeed(dataSpeedString)
        val frameLength = parseFrameLength(frameLengthString)
        val codeType = CodeGenerator.getCodeTypeId(codeTypeString)

        if (channelCount <= 0 || freq <= 0 || dataSpeed <= 0 || frameLength <= 0 || codeType < 0) return

        saveChannelsSettings(channelCount, freq, dataSpeed, frameLength, codeType)

        processor.generateChannels(channelCount, freq, dataSpeed, frameLength, codeType)
    }

    private fun saveChannelsSettings(
        channelCount: Int,
        freq: Double,
        dataSpeed: Double,
        frameLength: Int,
        codeType: Int
    ) {
        this.channelCount.value = channelCount
        carrierFrequency.value = freq
        this.dataSpeed.value = dataSpeed
        frameSize.value = frameLength
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

    fun removeChannel(channel: ChannelData) {
        processor.removeChannel(channel)
    }

    fun setAdcFrequency(frequency: Double) {
        adcFrequency.value = frequency
        processor.setAdcFrequency(frequency)
    }

    fun setNoise(snr: Double) {
        noiseSnr.value = snr
        processor.setNoise(snr)
    }

    override fun onCleared() {
        disposable.dispose()
        super.onCleared()
    }

    fun disableNoise() {
        processor.disableNoise()
    }

    fun enableNoise() {
        processor.enableNoise()
    }
}
