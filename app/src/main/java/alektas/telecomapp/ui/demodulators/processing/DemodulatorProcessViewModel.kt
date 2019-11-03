package alektas.telecomapp.ui.demodulators.processing

import alektas.telecomapp.App
import alektas.telecomapp.domain.Repository
import alektas.telecomapp.domain.entities.QpskContract
import alektas.telecomapp.domain.entities.demodulators.DemodulatorConfig
import alektas.telecomapp.domain.entities.signals.BinarySignal
import alektas.telecomapp.domain.entities.signals.Signal
import alektas.telecomapp.utils.toDataPoints
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.jjoe64.graphview.series.DataPoint
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.DisposableObserver
import javax.inject.Inject

class DemodulatorProcessViewModel : ViewModel() {
    @Inject
    lateinit var storage: Repository
    private val disposable = CompositeDisposable()
    val outputSignalData = MutableLiveData<Array<DataPoint>>()
    val iSignalData = MutableLiveData<Array<DataPoint>>()
    val qSignalData = MutableLiveData<Array<DataPoint>>()
    val iBitsData = MutableLiveData<Array<DataPoint>>()
    val qBitsData = MutableLiveData<Array<DataPoint>>()
    val initFrameLength = MutableLiveData<Int>()
    val initCodeLength = MutableLiveData<Int>()
    val initDataSpeed = MutableLiveData<Double>()
    val initThreshold = MutableLiveData<Double>()

    init {
        App.component.inject(this)

        disposable.addAll(
            storage.observeDemodulatorConfig()
                .observeOn(AndroidSchedulers.mainThread())
                .take(1)
                .subscribeWith(object : DisposableObserver<DemodulatorConfig>() {
                    override fun onNext(t: DemodulatorConfig) {
                        initFrameLength.value = t.frameLength
                        initCodeLength.value = t.codeLength
                        initDataSpeed.value = 1.0e-3 / t.bitTime // преобразование в скорость (кБит/с)
                        initThreshold.value = t.bitThreshold
                    }

                    override fun onComplete() {}

                    override fun onError(e: Throwable) {}
                }),
            storage.observeDemodulatedSignal()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableObserver<BinarySignal>() {
                    override fun onNext(t: BinarySignal) {
                        outputSignalData.value = t.toDataPoints()

                        val iBits = t.bits.filterIndexed { i, _ -> i % 2 == 0 }.toBooleanArray()
                        val qBits = t.bits.filterIndexed { i, _ -> i % 2 != 0 }.toBooleanArray()

                        iBitsData.value =
                            BinarySignal(iBits, QpskContract.DEFAULT_SYMBOL_TIME).toDataPoints()
                        qBitsData.value =
                            BinarySignal(qBits, QpskContract.DEFAULT_SYMBOL_TIME).toDataPoints()
                    }

                    override fun onComplete() {}

                    override fun onError(e: Throwable) {}
                }),
            storage.observeFilteredChannelI()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableObserver<Signal>() {
                    override fun onNext(t: Signal) {
                        iSignalData.value = t.toDataPoints()
                    }

                    override fun onComplete() {}

                    override fun onError(e: Throwable) {}
                }),
            storage.observeFilteredChannelQ()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableObserver<Signal>() {
                    override fun onNext(t: Signal) {
                        qSignalData.value = t.toDataPoints()
                    }

                    override fun onComplete() {}

                    override fun onError(e: Throwable) {}
                })
        )
    }

    override fun onCleared() {
        disposable.dispose()
        super.onCleared()
    }

    fun setThreshold(threshold: Double) {
        storage.setDemodulatorThreshold(threshold)
    }

    fun setFrameLength(frameLength: Int) {
        storage.setDemodulatorFrameLength(frameLength)
    }

    fun setCodeLength(codeLength: Int) {
        storage.setDemodulatorCodeLength(codeLength)
    }

    /**
     * @param dataSpeed скорость передачи данных в кБ/с
     */
    fun setDataSpeed(dataSpeed: Double) {
        storage.setDemodulatorBitTime(1.0e-3 / dataSpeed)
    }

}
