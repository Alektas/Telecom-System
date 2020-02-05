package alektas.telecomapp.ui.demodulator.output

import alektas.telecomapp.App
import alektas.telecomapp.domain.Repository
import alektas.telecomapp.domain.entities.signals.Signal
import alektas.telecomapp.utils.L
import alektas.telecomapp.utils.getNormalizedSpectrum
import alektas.telecomapp.utils.toDataPoints
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.jjoe64.graphview.series.DataPoint
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.DisposableObserver
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

class DemodulatorOutputViewModel : ViewModel() {
    @Inject
    lateinit var storage: Repository
    private val disposable = CompositeDisposable()
    val outputSignalData = MutableLiveData<Array<DataPoint>>()
    val spectrumData = MutableLiveData<Array<DataPoint>>()

    init {
        App.component.inject(this)

        disposable.add(
            storage.observeDemodulatedSignal()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeWith(object : DisposableObserver<Signal>() {
                override fun onNext(t: Signal) {
                    L.d(this, "Demodulation: draw output signal graphs")
                    extractData(t)
                }

                override fun onComplete() { }

                override fun onError(e: Throwable) { }
            })
        )
    }

    private fun extractData(signal: Signal) {
        disposable.add(Single.create<Array<DataPoint>> {
            val s = signal.toDataPoints()
            it.onSuccess(s)
        }
            .subscribeOn(Schedulers.computation())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { s: Array<DataPoint> -> outputSignalData.value = s })

        if (signal.isEmpty()) return

        disposable.add(Single.create<Array<DataPoint>> {
            val s = signal.getNormalizedSpectrum()
            it.onSuccess(s)
        }
            .subscribeOn(Schedulers.computation())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { s: Array<DataPoint> -> spectrumData.value = s })
    }

    override fun onCleared() {
        disposable.dispose()
        super.onCleared()
    }
}
