package alektas.telecomapp.ui.datasource

import alektas.telecomapp.App
import alektas.telecomapp.domain.Repository
import alektas.telecomapp.domain.entities.ChannelData
import alektas.telecomapp.domain.entities.SystemProcessor
import alektas.telecomapp.domain.entities.signals.Signal
import alektas.telecomapp.domain.entities.signals.noises.Noise
import alektas.telecomapp.utils.doOnFirst
import alektas.telecomapp.utils.toDataPoints
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.jjoe64.graphview.series.DataPoint
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.DisposableObserver
import javax.inject.Inject

class DataSourceViewModel : ViewModel() {
    @Inject
    lateinit var storage: Repository
    @Inject
    lateinit var processor: SystemProcessor
    private val disposable = CompositeDisposable()
    val channels = MutableLiveData<List<ChannelData>>()
    val ether = MutableLiveData<Array<DataPoint>>()
    val initNoiseSnr = MutableLiveData<Double>()
    val initDataSpeed = MutableLiveData<Double>()
    val initChannelCount = MutableLiveData<Int>()
    val initCodeType = MutableLiveData<Int>()
    val initFrameSize = MutableLiveData<Int>()

    init {
        App.component.inject(this)

        disposable.addAll(storage.observeChannels()
            .observeOn(AndroidSchedulers.mainThread())
            .doOnFirst {
                initChannelCount.value = it.size
                initCodeType.value = it.first().codeType
                initDataSpeed.value = 1.0e-3 / it.first().bitTime // преобразование в скорость (кБит/с)
                initFrameSize.value = it.first().data.size
            }
            .subscribeWith(object: DisposableObserver<List<ChannelData>>() {
                override fun onNext(t: List<ChannelData>) {
                    channels.value = t
                }

                override fun onComplete() { }

                override fun onError(e: Throwable) { }
            }),
            storage.observeEther()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object: DisposableObserver<Signal>() {
                    override fun onNext(s: Signal) {
                        ether.value = s.toDataPoints()
                    }

                    override fun onComplete() {
                        println("Complete ether stream")
                    }

                    override fun onError(e: Throwable) { }
                }),
            storage.observeNoise()
                .observeOn(AndroidSchedulers.mainThread())
                .take(1)
                .subscribeWith(object: DisposableObserver<Noise>() {
                    override fun onNext(s: Noise) {
                        initNoiseSnr.value = s.snr()
                    }

                    override fun onComplete() { }

                    override fun onError(e: Throwable) { }
                })
        )
    }

    fun generateChannels(
        count: Int,
        dataSpeed: Double,
        frameLength: Int,
        codeType: Int
    ) {
        processor.generateChannels(count, dataSpeed, frameLength, codeType)
    }

    fun removeChannel(channel: ChannelData) {
        processor.removeChannel(channel)
    }

    fun setNoise(snr: Double) {
        processor.setNoise(snr)
    }

    override fun onCleared() {
        disposable.dispose()
        super.onCleared()
    }
}
