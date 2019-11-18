package alektas.telecomapp.ui.demodulators.processing

import alektas.telecomapp.App
import alektas.telecomapp.domain.Repository
import alektas.telecomapp.domain.entities.signals.BinarySignal
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
    val iSignalData = MutableLiveData<Array<DataPoint>>()
    val qSignalData = MutableLiveData<Array<DataPoint>>()
    val iBitsData = MutableLiveData<Array<DataPoint>>()
    val qBitsData = MutableLiveData<Array<DataPoint>>()
    val frameLength = MutableLiveData<Int>()
    val codeLength = MutableLiveData<Int>()
    val dataSpeed = MutableLiveData<Float>()
    val threshold = MutableLiveData<Float>()

    init {
        App.component.inject(this)

        disposable.addAll(
            storage.observeDemodulatedSignal()
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.computation())
                .map {
                    val sigData = it.toDataPoints()

                    val iBits = it.bits.filterIndexed { i, _ -> i % 2 == 0 }.toBooleanArray()
                    val qBits = it.bits.filterIndexed { i, _ -> i % 2 != 0 }.toBooleanArray()

                    val iData = BinarySignal(iBits, it.bitTime * 2).toDataPoints()
                    val qData = BinarySignal(qBits, it.bitTime * 2).toDataPoints()

                    Triple(sigData, iData, qData)
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object :
                    DisposableObserver<Triple<Array<DataPoint>, Array<DataPoint>, Array<DataPoint>>>() {
                    override fun onNext(t: Triple<Array<DataPoint>, Array<DataPoint>, Array<DataPoint>>) {
                        outputSignalData.value = t.first
                        iBitsData.value = t.second
                        qBitsData.value = t.third
                    }

                    override fun onComplete() {}

                    override fun onError(e: Throwable) {}
                }),

            storage.observeFilteredChannelI()
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.computation())
                .map { it.toDataPoints() }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableObserver<Array<DataPoint>>() {
                    override fun onNext(t: Array<DataPoint>) {
                        iSignalData.value = t
                    }

                    override fun onComplete() {}

                    override fun onError(e: Throwable) {}
                }),

            storage.observeFilteredChannelQ()
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.computation())
                .map { it.toDataPoints() }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableObserver<Array<DataPoint>>() {
                    override fun onNext(t: Array<DataPoint>) {
                        qSignalData.value = t
                    }

                    override fun onComplete() {}

                    override fun onError(e: Throwable) {}
                })
        )
    }

    fun processData(
        frameLengthString: String,
        dataSpeedString: String,
        codeLengthString: String,
        thresholdString: String
    ) {
        val frameLength = parseFrameLength(frameLengthString)
        val dataSpeed = parseDataspeed(dataSpeedString)
        val bitTime = 1.0e-3 / dataSpeed // преобразование в скорость (кБит/с)
        val codeLength = parseCodeLength(codeLengthString)
        val threshold = parseThreshold(thresholdString)

        if (codeLength <= 0 || dataSpeed <= 0 || frameLength <= 0 || threshold < 0) return

        this.frameLength.value = frameLength
        this.codeLength.value = codeLength
        this.dataSpeed.value = dataSpeed.toFloat()
        this.threshold.value = threshold.toFloat()

        storage.updateDemodulatorConfig(frameLength, bitTime, codeLength, threshold)
    }

    fun parseThreshold(threshold: String): Double {
        return try {
            val c = threshold.toDouble()
            if (c < 0) throw NumberFormatException()
            c
        } catch (e: NumberFormatException) {
            -1.0
        }
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

    override fun onCleared() {
        disposable.dispose()
        super.onCleared()
    }

}
