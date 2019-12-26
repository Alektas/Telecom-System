package alektas.telecomapp.ui.demodulator.generator

import alektas.telecomapp.App
import alektas.telecomapp.domain.Repository
import alektas.telecomapp.domain.entities.configs.DemodulatorConfig
import alektas.telecomapp.domain.entities.generators.SignalGenerator
import alektas.telecomapp.utils.toDataPoints
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.jjoe64.graphview.series.DataPoint
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.DisposableObserver
import io.reactivex.schedulers.Schedulers
import java.lang.NumberFormatException
import javax.inject.Inject

class DemodulatorGeneratorViewModel : ViewModel() {
    @Inject
    lateinit var storage: Repository
    private val disposable = CompositeDisposable()
    val generatorSignalData = MutableLiveData<Array<DataPoint>>()
    val generatorFrequency = MutableLiveData<Float>()

    init {
        App.component.inject(this)

        disposable.addAll(
            storage.observeDemodulatorConfig()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableObserver<DemodulatorConfig>() {
                    override fun onNext(t: DemodulatorConfig) {
                        showGeneratorSignal(t.carrierFrequency)
                    }

                    override fun onComplete() {}

                    override fun onError(e: Throwable) {}
                })
        )
    }

    /**
     * Частота генератор задается в МГц, поэтому введенное число умножается на 10^6
     *
     * @return true если парсинг и установка частоты прошли успешно, false в противном случае
     */
    fun setGeneratorFrequency(freqString: String): Boolean {
        val f = parseFrequency(freqString)
        if (f <= 0) return false

        generatorFrequency.value = f.toFloat()
        storage.setDemodulatorFrequency(f * 1.0e6)
        return true
    }

    private fun showGeneratorSignal(frequency: Double) {
        disposable.add(Single.create<Array<DataPoint>> {
            val s = SignalGenerator().cos(frequency = frequency).toDataPoints()
            it.onSuccess(s)
        }
            .subscribeOn(Schedulers.computation())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { s: Array<DataPoint> -> generatorSignalData.value = s })
    }

    fun parseFrequency(freqString: String): Double {
        return try {
            freqString.toDouble()
        } catch (e: NumberFormatException) {
            -1.0
        }
    }

    override fun onCleared() {
        disposable.dispose()
        super.onCleared()
    }
}
