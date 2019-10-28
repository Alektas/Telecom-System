package alektas.telecomapp.ui.demodulators

import alektas.telecomapp.App
import alektas.telecomapp.domain.Repository
import alektas.telecomapp.domain.entities.demodulators.DemodulatorConfig
import alektas.telecomapp.domain.entities.demodulators.QpskDemodulator
import alektas.telecomapp.domain.entities.signals.BinarySignal
import alektas.telecomapp.domain.entities.signals.Signal
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.jjoe64.graphview.series.DataPoint
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.observers.DisposableObserver
import javax.inject.Inject

class QpskDemodulatorViewModel : ViewModel() {
    @Inject lateinit var storage: Repository
    private val disposable = CompositeDisposable()
    val inputSignalData = MutableLiveData<Array<DataPoint>>()
    val iSignalData = MutableLiveData<Array<DataPoint>>()
    val filteredISignalData = MutableLiveData<Array<DataPoint>>()
    val qSignalData = MutableLiveData<Array<DataPoint>>()
    val filteredQSignalData = MutableLiveData<Array<DataPoint>>()
    val outputSignalData = MutableLiveData<Array<DataPoint>>()
    val constellationData = MutableLiveData<List<Pair<Float, Float>>>()

    init {
        App.component.inject(this)
//        settingCharts(storage.getDemodulatorConfig())

        disposable.addAll(storage.observeDemodulatorConfig()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeWith(object : DisposableObserver<DemodulatorConfig>() {
                override fun onNext(t: DemodulatorConfig) {
                    inputSignalData.value = t.inputSignal.getPoints()
                        .map { DataPoint(it.key, it.value) }.toTypedArray()
                }

                override fun onComplete() { }

                override fun onError(e: Throwable) { }
            }),
            storage.observeDemodulatedSignal()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableObserver<Signal>() {
                    override fun onNext(t: Signal) {
                        outputSignalData.value = t.getPoints()
                            .map { DataPoint(it.key, it.value) }.toTypedArray()
                    }

                    override fun onComplete() { }

                    override fun onError(e: Throwable) { }
                })
        )
    }

    private fun settingCharts(config: DemodulatorConfig) {
        config.inputSignal.let { signal ->
            inputSignalData.value = signal.getPoints()
                .map { DataPoint(it.key, it.value) }.toTypedArray()

            val demodulator = QpskDemodulator(config)
            val demodulatedSignal = demodulator.demodulate(signal)
            outputSignalData.value = demodulatedSignal.getPoints()
                .map { DataPoint(it.key, it.value) }.toTypedArray()

            storage.setDemodulatedSignal(demodulatedSignal as BinarySignal)

            constellationData.value = demodulator.getConstellation(signal)
                .map { Pair(it.first.toFloat(), it.second.toFloat()) }

            iSignalData.value = demodulator.sigI.getPoints()
                .map { DataPoint(it.key, it.value) }.toTypedArray()

            filteredISignalData.value = demodulator.filteredSigI.getPoints()
                .map { DataPoint(it.key, it.value) }.toTypedArray()

            qSignalData.value = demodulator.sigQ.getPoints()
                .map { DataPoint(it.key, it.value) }.toTypedArray()

            filteredQSignalData.value = demodulator.filteredSigQ.getPoints()
                .map { DataPoint(it.key, it.value) }.toTypedArray()
        }
    }

    override fun onCleared() {
        disposable.dispose()
        super.onCleared()
    }
}
