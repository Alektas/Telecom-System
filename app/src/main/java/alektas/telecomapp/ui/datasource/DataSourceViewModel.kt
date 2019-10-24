package alektas.telecomapp.ui.datasource

import alektas.telecomapp.App
import alektas.telecomapp.data.CodeGenerator
import alektas.telecomapp.domain.Repository
import alektas.telecomapp.domain.entities.ChannelData
import alektas.telecomapp.domain.entities.SystemProcessor
import alektas.telecomapp.domain.entities.signals.Signal
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

    init {
        App.component.inject(this)

        disposable.addAll(storage.observeChannels()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeWith(object: DisposableObserver<List<ChannelData>>() {
                override fun onNext(t: List<ChannelData>) {
                    channels.value = t
                }

                override fun onComplete() { }

                override fun onError(e: Throwable) { }
            }),
            storage.observeEther()
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object: DisposableObserver<Signal>() {
                    override fun onNext(s: Signal) {
                        ether.value = s.getPoints().map { DataPoint(it.key, it.value) }.toTypedArray()
                    }

                    override fun onComplete() {
                        println("Complete ether stream")
                    }

                    override fun onError(e: Throwable) { }
                })
        )
    }

    fun generateChannels(
        count: Int,
        frameLength: Int,
        codeType: Int
    ) {
        processor.generateChannels(count, frameLength, codeType)
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
