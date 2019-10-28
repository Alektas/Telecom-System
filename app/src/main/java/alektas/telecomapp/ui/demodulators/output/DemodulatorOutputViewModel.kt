package alektas.telecomapp.ui.demodulators.output

import alektas.telecomapp.App
import alektas.telecomapp.domain.Repository
import alektas.telecomapp.domain.entities.Window
import alektas.telecomapp.domain.entities.signals.Signal
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
                        extractConstellationData(t)
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
            val signalData = Window(Window.GAUSSE).applyTo(signal).getValues()
            var spectrum = FastFourierTransformer(DftNormalization.STANDARD)
                .transform(
                    signalData,
                    TransformType.FORWARD
                )
            val actualSize = spectrum.size / 2
            spectrum = spectrum.take(actualSize).toTypedArray()
            val maxSpectrumValue = spectrum.maxBy { it.abs() }?.abs() ?: 1.0
            specturmData.value = spectrum
                .mapIndexed { i, complex -> DataPoint(i.toDouble(), complex.abs() / maxSpectrumValue) }
                .toTypedArray()
        } catch (e: MathIllegalArgumentException) {
            e.printStackTrace()
        }
    }

    private fun extractConstellationData(points: List<Pair<Double, Double>>) {
        constellationData.value =
            points.map { Pair(it.first.toFloat(), it.second.toFloat()) }
    }

    override fun onCleared() {
        disposable.dispose()
        super.onCleared()
    }
}
