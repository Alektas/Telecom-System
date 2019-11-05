package alektas.telecomapp.ui.demodulators.filter.qchannel

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
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

class QChannelViewModel : ViewModel() {
    @Inject
    lateinit var storage: Repository
    private val disposable = CompositeDisposable()
    val channelQData = MutableLiveData<Array<DataPoint>>()
    val filteredChannelQData = MutableLiveData<Array<DataPoint>>()

    init {
        App.component.inject(this)

        disposable.addAll(
            storage.observeChannelQ()
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.computation())
                .map { it.toDataPoints() }.observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableObserver<Array<DataPoint>>() {
                    override fun onNext(t: Array<DataPoint>) {
                        channelQData.value = t
                    }

                    override fun onComplete() {}

                    override fun onError(e: Throwable) {}
                }),

            storage.observeFilteredChannelQ()
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.computation())
                .map { it.toDataPoints() }.observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableObserver<Array<DataPoint>>() {
                    override fun onNext(t: Array<DataPoint>) {
                        filteredChannelQData.value = t
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
