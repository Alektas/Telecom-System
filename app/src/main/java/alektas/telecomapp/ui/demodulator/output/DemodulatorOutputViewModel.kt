package alektas.telecomapp.ui.demodulator.output

import alektas.telecomapp.App
import alektas.telecomapp.domain.Repository
import alektas.telecomapp.domain.entities.signals.Signal
import alektas.telecomapp.utils.getNormalizedSpectrum
import alektas.telecomapp.utils.toDataPoints
import alektas.telecomapp.utils.toFloat
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
    val specturmData = MutableLiveData<Array<DataPoint>>()
    val constellationData = MutableLiveData<List<Pair<Float, Float>>>()

    init {
        App.component.inject(this)

        disposable.addAll(
            storage.observeDemodulatedSignal()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeWith(object : DisposableObserver<Signal>() {
                override fun onNext(t: Signal) {
                    extractSignalData(t)
                }

                override fun onComplete() { }

                override fun onError(e: Throwable) { }
            }),

            storage.observeDemodulatedSignalConstellation()
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.computation())
                .map { it.toFloat() }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableObserver<List<Pair<Float, Float>>>() {
                    override fun onNext(t: List<Pair<Float, Float>>) {
                        constellationData.value = t
                    }

                    override fun onComplete() { }

                    override fun onError(e: Throwable) { }
                }))
    }

    private fun extractSignalData(signal: Signal) {
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
            .subscribe { s: Array<DataPoint> -> specturmData.value = s })
    }

    override fun onCleared() {
        disposable.dispose()
        super.onCleared()
    }
}
