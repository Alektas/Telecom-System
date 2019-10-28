package alektas.telecomapp.ui.demodulators.input

import alektas.telecomapp.App
import alektas.telecomapp.domain.Repository
import alektas.telecomapp.domain.entities.Window
import alektas.telecomapp.domain.entities.signals.Signal
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.jjoe64.graphview.series.DataPoint
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.observers.DisposableObserver
import org.apache.commons.math3.exception.MathIllegalArgumentException
import org.apache.commons.math3.transform.DftNormalization
import org.apache.commons.math3.transform.FastFourierTransformer
import org.apache.commons.math3.transform.TransformType
import javax.inject.Inject

class DemodulatorInputViewModel : ViewModel() {
    @Inject
    lateinit var storage: Repository
    private val disposable: Disposable
    val inputSignalData = MutableLiveData<Array<DataPoint>>()
    val specturmData = MutableLiveData<Array<DataPoint>>()

    init {
        App.component.inject(this)

        disposable = storage.observeEther()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeWith(object : DisposableObserver<Signal>() {
                override fun onNext(t: Signal) {
                    extractData(t)
                }

                override fun onComplete() { }

                override fun onError(e: Throwable) { }
            })
    }

    private fun extractData(signal: Signal) {
        inputSignalData.value = signal.getPoints()
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

    override fun onCleared() {
        disposable.dispose()
        super.onCleared()
    }
}
