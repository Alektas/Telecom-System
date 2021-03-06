package alektas.telecomapp.ui.demodulator.processing

import alektas.telecomapp.App
import alektas.telecomapp.domain.Repository
import alektas.telecomapp.utils.toDataPoints
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.jjoe64.graphview.series.DataPoint
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.DisposableObserver
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

class DemodulatorProcessViewModel : ViewModel() {
    @Inject
    lateinit var storage: Repository
    private val disposable = CompositeDisposable()
    val outputSignalData = MutableLiveData<Array<DataPoint>>()
    val frameLength = MutableLiveData<Int>()
    val codeLength = MutableLiveData<Int>()
    val dataSpeed = MutableLiveData<Float>()
    val delayCompensation = MutableLiveData<Float>()

    init {
        App.component.inject(this)

        disposable.addAll(
            storage.observeDemodulatedSignal()
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.computation())
                .map { it.toDataPoints() }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableObserver<Array<DataPoint>>() {
                    override fun onNext(t: Array<DataPoint>) {
                        outputSignalData.value = t
                    }

                    override fun onComplete() {}

                    override fun onError(e: Throwable) {}
                })
        )
    }

    fun processData(
        delayCompensationString: String,
        frameLengthString: String,
        dataSpeedString: String,
        codeLengthString: String
    ): Boolean {
        val delayCompensation = parseDelayCompensation(delayCompensationString)
        val frameLength = parseFrameLength(frameLengthString)
        val dataSpeed = parseDataspeed(dataSpeedString)
        val bitTime = 1.0e-3 / dataSpeed // преобразование в скорость (кБит/с)
        val codeLength = parseCodeLength(codeLengthString)

        if (codeLength <= 0 || dataSpeed <= 0 || frameLength <= 0 || delayCompensation < 0) return false

        this.frameLength.value = frameLength
        this.codeLength.value = codeLength
        this.dataSpeed.value = dataSpeed.toFloat()
        this.delayCompensation.value = delayCompensation

        storage.updateDemodulatorConfig(delayCompensation, frameLength, bitTime, codeLength)
        return true
    }

    fun parseDataspeed(speed: String): Double {
        return try {
            val c = speed.toDouble()
            if (c <= 0) throw NumberFormatException()
            c
        } catch (e: NumberFormatException) {
            -1.0
        }
    }

    fun parseFrameLength(length: String): Int {
        return try {
            val c = length.toInt()
            if (c <= 0) throw NumberFormatException()
            c
        } catch (e: NumberFormatException) {
            -1
        }
    }

    fun parseCodeLength(length: String): Int {
        return try {
            val c = length.toInt()
            if (c <= 0) throw NumberFormatException()
            c
        } catch (e: NumberFormatException) {
            -1
        }
    }

    fun parseDelayCompensation(compensation: String): Float {
        return try {
            val c = compensation.toFloat()
            if (c > 1f) throw NumberFormatException()
            c
        } catch (e: NumberFormatException) {
            -1f
        }
    }

    override fun onCleared() {
        disposable.dispose()
        super.onCleared()
    }

}
