package alektas.telecomapp.ui.demodulators.filter.ichannel

import alektas.telecomapp.App
import alektas.telecomapp.domain.Repository
import alektas.telecomapp.domain.entities.signals.Signal
import alektas.telecomapp.utils.toDataPoints
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.jjoe64.graphview.series.DataPoint
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.DisposableObserver
import javax.inject.Inject

class IChannelViewModel : ViewModel() {
    @Inject
    lateinit var storage: Repository
    private val disposable = CompositeDisposable()
    val channelIData = MutableLiveData<Array<DataPoint>>()
    val filteredChannelIData = MutableLiveData<Array<DataPoint>>()

    init {
        App.component.inject(this)

        disposable.addAll(
            storage.observeChannelI()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableObserver<Signal>() {
                    override fun onNext(t: Signal) {
                        channelIData.value = t.toDataPoints()
                    }

                    override fun onComplete() {}

                    override fun onError(e: Throwable) {}
                }),
            storage.observeFilteredChannelI()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableObserver<Signal>() {
                    override fun onNext(t: Signal) {
                        filteredChannelIData.value = t.toDataPoints()
                    }

                    override fun onComplete() {}

                    override fun onError(e: Throwable) {}
                })
        )
    }

    override fun onCleared() {
        disposable.dispose()
        super.onCleared()
    }
}
