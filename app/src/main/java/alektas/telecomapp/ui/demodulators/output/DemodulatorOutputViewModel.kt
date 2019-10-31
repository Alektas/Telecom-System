package alektas.telecomapp.ui.demodulators.output

import alektas.telecomapp.App
import alektas.telecomapp.domain.Repository
import alektas.telecomapp.domain.entities.signals.Signal
import alektas.telecomapp.utils.getNormalizedSpectrum
import alektas.telecomapp.utils.toFloat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.jjoe64.graphview.series.DataPoint
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.DisposableObserver
import org.apache.commons.math3.exception.MathIllegalArgumentException
import org.apache.commons.math3.transform.DftNormalization
import org.apache.commons.math3.transform.FastFourierTransformer
import org.apache.commons.math3.transform.TransformType
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

        disposable.addAll(storage.observeDemodulatedSignal()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeWith(object : DisposableObserver<Signal>() {
                override fun onNext(t: Signal) {
                    extractSignalData(t)
                }

                override fun onComplete() { }

                override fun onError(e: Throwable) { }
            }),
            storage.observeDemodulatedSignalConstellation()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableObserver<List<Pair<Double, Double>>>() {
                    override fun onNext(t: List<Pair<Double, Double>>) {
                        constellationData.value = t.toFloat()
                    }

                    override fun onComplete() { }

                    override fun onError(e: Throwable) { }
                }))
    }

    private fun extractSignalData(signal: Signal) {
        outputSignalData.value = signal.getPoints()
            .map { DataPoint(it.key, it.value) }.toTypedArray()

        if (signal.isEmpty()) return

        try {
            specturmData.value = signal.getNormalizedSpectrum()
        } catch (e: MathIllegalArgumentException) {
            e.printStackTrace()
        }
    }

    override fun onCleared() {
        disposable.dispose()
        super.onCleared()
    }
}
