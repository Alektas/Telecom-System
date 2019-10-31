package alektas.telecomapp.ui.decoder

import alektas.telecomapp.App
import alektas.telecomapp.domain.Repository
import alektas.telecomapp.domain.entities.ChannelData
import alektas.telecomapp.domain.entities.SystemProcessor
import alektas.telecomapp.domain.entities.signals.BinarySignal
import alektas.telecomapp.utils.doOnFirst
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.jjoe64.graphview.series.DataPoint
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.DisposableObserver
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

class DecoderViewModel : ViewModel() {
    @Inject
    lateinit var storage: Repository
    @Inject
    lateinit var processor: SystemProcessor
    private val disposable = CompositeDisposable()
    val inputSignalData = MutableLiveData<Array<DataPoint>>()
    val channels = MutableLiveData<List<ChannelData>>()
    val initCodeType = MutableLiveData<Int>()
    val initChannelCount = MutableLiveData<Int>()

    init {
        App.component.inject(this)

        disposable.addAll(storage.observeDemodulatedSignal()
            .subscribeOn(Schedulers.computation())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeWith(object : DisposableObserver<BinarySignal>() {
                override fun onNext(t: BinarySignal) {
                    inputSignalData.value =
                        t.getPoints().map { DataPoint(it.key, it.value) }.toTypedArray()
                }

                override fun onComplete() {}

                override fun onError(e: Throwable) {}
            }),
            storage.observeDecodedChannels()
                .observeOn(AndroidSchedulers.mainThread())
                .doOnFirst {
                    initChannelCount.value = it.size
                    initCodeType.value = it.first().codeType
                }
                .subscribeWith(object : DisposableObserver<List<ChannelData>>() {
                    override fun onNext(t: List<ChannelData>) {
                        channels.value = t
                    }

                    override fun onComplete() {}

                    override fun onError(e: Throwable) {}
                })
        )
    }

    fun decodeChannels(count: Int, codesType: Int) {
        processor.decodeChannels(count, codesType)
    }

    fun decodeCustomChannel(codeString: String) {
        val code = codeString.filter { it == '1' || it == '0' }.map { it == '1' }.toBooleanArray()
        processor.decodeChannel(code)
    }

    fun removeChannel(channel: ChannelData) {
        storage.removeDecodedChannel(channel)
    }

}
