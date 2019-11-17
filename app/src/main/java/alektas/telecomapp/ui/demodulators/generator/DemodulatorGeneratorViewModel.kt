package alektas.telecomapp.ui.demodulators.generator

import alektas.telecomapp.App
import alektas.telecomapp.domain.Repository
import alektas.telecomapp.domain.entities.demodulators.DemodulatorConfig
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
     */
    fun setGeneratorFrequency(frequency: Double) {
        generatorFrequency.value = frequency.toFloat()
        storage.setDemodulatorFrequency(frequency * 1.0e6)
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

    override fun onCleared() {
        disposable.dispose()
        super.onCleared()
    }
}
