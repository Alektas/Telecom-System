package alektas.telecomapp.ui.demodulators.generator

import alektas.telecomapp.App
import alektas.telecomapp.domain.Repository
import alektas.telecomapp.domain.entities.demodulators.DemodulatorConfig
import alektas.telecomapp.domain.entities.generators.SignalGenerator
import alektas.telecomapp.utils.doOnFirst
import alektas.telecomapp.utils.toDataPoints
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.jjoe64.graphview.series.DataPoint
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.DisposableObserver
import javax.inject.Inject

class DemodulatorGeneratorViewModel : ViewModel() {
    @Inject
    lateinit var storage: Repository
    private val disposable = CompositeDisposable()
    val generatorSignalData = MutableLiveData<Array<DataPoint>>()
    val initFrequency = MutableLiveData<Float>()

    init {
        App.component.inject(this)

        disposable.addAll(
            storage.observeDemodulatorConfig()
                .observeOn(AndroidSchedulers.mainThread())
                .doOnFirst {
                    // Частоты хранятся в Гц, а отображаются МГц, поэтому домножаем на 10^-6.
                    // Переводим в Float, потому что в Double число некрасивое ;)
                    val freq = (it.carrierFrequency * 1.0e-6).toFloat()
                    initFrequency.value = freq
                }
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
        storage.setDemodulatorFrequency(frequency * 1.0e6)
    }

    private fun showGeneratorSignal(frequency: Double) {
        val signal = SignalGenerator().cos(frequency = frequency)
        generatorSignalData.value = signal.toDataPoints()
    }

    override fun onCleared() {
        disposable.dispose()
        super.onCleared()
    }
}
