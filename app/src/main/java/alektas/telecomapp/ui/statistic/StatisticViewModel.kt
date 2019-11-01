package alektas.telecomapp.ui.statistic

import alektas.telecomapp.App
import alektas.telecomapp.domain.Repository
import alektas.telecomapp.domain.entities.ChannelData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.DisposableObserver
import javax.inject.Inject

class StatisticViewModel : ViewModel() {
    @Inject
    lateinit var storage: Repository
    private val disposable = CompositeDisposable()
    val channelCountData = MutableLiveData<Int>()
    val bitTransmittedData = MutableLiveData<Int>()
    val bitReceivedData = MutableLiveData<Int>()
    val errorBitCountData = MutableLiveData<Int>()
    val berData = MutableLiveData<Double>()

    init {
        App.component.inject(this)

        disposable.addAll(
            storage.observeChannels()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableObserver<List<ChannelData>>() {
                    override fun onNext(t: List<ChannelData>) {
                        val bitCount = t.sumBy { it.data.size }
                        channelCountData.value = t.size
                        bitTransmittedData.value = bitCount
                    }

                    override fun onComplete() {}

                    override fun onError(e: Throwable) {}
                }),
            storage.observeDecodedChannels()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableObserver<List<ChannelData>>() {
                    override fun onNext(t: List<ChannelData>) {
                        val bitCount = t.sumBy { it.data.size }
                        bitReceivedData.value = bitCount
                    }

                    override fun onComplete() {}

                    override fun onError(e: Throwable) {}
                }),
            storage.observeChannelsErrors()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableObserver<List<List<Int>>>() {
                    override fun onNext(t: List<List<Int>>) {
                        val errorBitCount = t.sumBy { it.size }
                        errorBitCountData.value = errorBitCount
                        val bitCount = bitTransmittedData.value ?: 0
                        if (bitCount == 0) berData.value
                        if (bitCount != 0) berData.value = (errorBitCount / bitCount.toDouble()) * 100
                    }

                    override fun onComplete() {}

                    override fun onError(e: Throwable) {}
                })
        )
    }

}
