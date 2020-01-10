package alektas.telecomapp.ui.datasource.simulation

import alektas.telecomapp.App
import alektas.telecomapp.domain.Repository
import alektas.telecomapp.domain.entities.Channel
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

class SimulationDataSourceViewModel : ViewModel() {
    @Inject
    lateinit var storage: Repository
    @Inject
    lateinit var processor: SystemProcessor
    val channels = MutableLiveData<List<Channel>>()
    val ether = MutableLiveData<Array<DataPoint>>()
    val adcFrequency = MutableLiveData<Double>()
    val frameCount = MutableLiveData<Int>()
    val isTransmitAvailable = MutableLiveData<Boolean>(true)
    private val disposable = CompositeDisposable()
    private var isTransmitCompleted = true
        set(value) {
            field = value
            isTransmitAvailable.value = isChannelsExist && value
        }
    private var isChannelsExist = false
        set(value) {
            field = value
            isTransmitAvailable.value = value && isTransmitCompleted
        }

    init {
        App.component.inject(this)

        disposable.addAll(
            storage.observeSimulatedChannels()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableObserver<List<Channel>>() {
                    override fun onNext(it: List<Channel>) {
                        channels.value = it
                        isChannelsExist = it.isNotEmpty()
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
                }),

            storage.observeTransmitProgress()
                .map { (it < 0 || it >= 100) }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableObserver<Boolean>() {
                    override fun onNext(b: Boolean) {
                        isTransmitCompleted = b
                    }

                    override fun onComplete() {
                        isTransmitCompleted = true
                    }

                    override fun onError(e: Throwable) {
                        isTransmitCompleted = true
                    }
                })
        )
    }

    fun setAdcFrequency(freqString: String) {
        val freq = parseFrequency(freqString)

        if (freq <= 0) return

        adcFrequency.value = freq
        processor.setAdcFrequency(freq)
    }

    fun removeChannel(channel: Channel) {
        processor.removeChannel(channel)
    }

    fun transmitFrames(frameCountString: String) {
        val frameCount = parseChannelCount(frameCountString)
        if (frameCount <= 0) return

        saveChannelsSettings(frameCount)

        channels.value?.let {
            isTransmitCompleted = false
            processor.transmitFrames(it, frameCount)
        }
    }

    private fun saveChannelsSettings(frameCount: Int) {
        this.frameCount.value = frameCount
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

    override fun onCleared() {
        disposable.dispose()
        super.onCleared()
    }

}
