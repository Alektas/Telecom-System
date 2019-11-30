package alektas.telecomapp.ui.statistic

import alektas.telecomapp.App
import alektas.telecomapp.domain.Repository
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.DisposableObserver
import io.reactivex.schedulers.Schedulers
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
            storage.observeTransmittedBitsCount()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableObserver<Int>() {
                    override fun onNext(t: Int) {
                        bitTransmittedData.value = t
                    }

                    override fun onComplete() {}

                    override fun onError(e: Throwable) {}
                }),

            storage.observeReceivedBitsCount()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableObserver<Int>() {
                    override fun onNext(t: Int) {
                        bitReceivedData.value = t
                    }

                    override fun onComplete() {}

                    override fun onError(e: Throwable) {}
                }),

            storage.observeReceivedErrorsCount()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableObserver<Int>() {
                    override fun onNext(t: Int) {
                        errorBitCountData.value = t
                    }

                    override fun onComplete() {}

                    override fun onError(e: Throwable) {}
                }),

            storage.observeBer()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableObserver<Double>() {
                    override fun onNext(t: Double) {
                        berData.value = t
                    }

                    override fun onComplete() {}

                    override fun onError(e: Throwable) {}
                }),

            storage.observeTransmittingChannelsCount()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableObserver<Int>() {
                    override fun onNext(t: Int) {
                        channelCountData.value = t
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
