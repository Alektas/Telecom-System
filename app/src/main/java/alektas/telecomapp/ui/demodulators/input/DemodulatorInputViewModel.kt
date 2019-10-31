package alektas.telecomapp.ui.demodulators.input

import alektas.telecomapp.App
import alektas.telecomapp.domain.Repository
import alektas.telecomapp.domain.entities.signals.Signal
import alektas.telecomapp.utils.getNormalizedSpectrum
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.jjoe64.graphview.series.DataPoint
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.observers.DisposableObserver
import org.apache.commons.math3.exception.MathIllegalArgumentException
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
