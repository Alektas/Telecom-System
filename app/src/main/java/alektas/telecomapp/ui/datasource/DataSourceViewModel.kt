package alektas.telecomapp.ui.datasource

import alektas.telecomapp.App
import alektas.telecomapp.data.CodeGenerator
import alektas.telecomapp.domain.Repository
import alektas.telecomapp.domain.entities.ChannelData
import alektas.telecomapp.domain.entities.Simulator
import alektas.telecomapp.domain.entities.SystemProcessor
import alektas.telecomapp.domain.entities.signals.Signal
import alektas.telecomapp.domain.entities.signals.noises.Noise
import alektas.telecomapp.utils.doOnFirst
import alektas.telecomapp.utils.toDataPoints
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.jjoe64.graphview.series.DataPoint
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.DisposableObserver
import io.reactivex.observers.DisposableSingleObserver
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
    val initAdcFrequency = MutableLiveData<Double>()
    val initNoiseSnr = MutableLiveData<Double>()
    val initCarrierFrequency = MutableLiveData<Double>()
    val initDataSpeed = MutableLiveData<Double>()
    val initChannelCount = MutableLiveData<Int>()
    val initCodeType = MutableLiveData<Int>()
    val initFrameSize = MutableLiveData<Int>()

    init {
        App.component.inject(this)

        disposable.addAll(
            storage.observeChannels()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnFirst {
                    initChannelCount.value = it.size
                    initCodeType.value = it.first().codeType
                    initCarrierFrequency.value =
                        1.0e-6 * it.first().carrierFrequency // Гц -> МГц
                    initDataSpeed.value =
                        1.0e-3 / it.first().bitTime // преобразование в скорость (кБит/с)
                    initFrameSize.value = it.first().data.size
                }
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
                }),

            storage.observeNoise()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .take(1)
                .subscribeWith(object : DisposableObserver<Noise>() {
                    override fun onNext(s: Noise) {
                        initNoiseSnr.value = s.snr()
                    }

                    override fun onComplete() {}

                    override fun onError(e: Throwable) {}
                }),

            Single.just(Simulator.samplingRate)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object: DisposableSingleObserver<Double>() {
                    override fun onSuccess(t: Double) {
                        initAdcFrequency.value = t * 1.0e-6
                    }

                    override fun onError(e: Throwable) { }
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

        processor.generateChannels(channelCount, freq, dataSpeed, frameLength, codeType)
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
        processor.setAdcFrequency(frequency)
    }

    fun setNoise(snr: Double) {
        processor.setNoise(snr)
    }

    override fun onCleared() {
        disposable.dispose()
        super.onCleared()
    }
}
