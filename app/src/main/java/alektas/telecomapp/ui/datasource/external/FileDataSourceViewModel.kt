package alektas.telecomapp.ui.datasource.external

import alektas.telecomapp.App
import alektas.telecomapp.domain.Repository
import alektas.telecomapp.domain.entities.SystemProcessor
import alektas.telecomapp.utils.toDataPoints
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.jjoe64.graphview.series.DataPoint
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.DisposableObserver
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

class FileDataSourceViewModel : ViewModel() {
    @Inject
    lateinit var storage: Repository
    @Inject
    lateinit var processor: SystemProcessor
    private val disposable = CompositeDisposable()
    val ether = MutableLiveData<Array<DataPoint>>()
    val adcFrequency = MutableLiveData<Double>()
    val adcResolution = MutableLiveData<Int>()

    init {
        App.component.inject(this)

        disposable.addAll(
            storage.observeEther()
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.computation())
                .map { it.toDataPoints() }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableObserver<Array<DataPoint>>() {
                    override fun onNext(s: Array<DataPoint>) {
                        ether.value = s
                    }

                    override fun onComplete() {}

                    override fun onError(e: Throwable) {}
                })
        )
    }

    fun setAdcFrequency(adcFrequencyString: String) {
        val freq = parseFrequency(adcFrequencyString)
        if (freq <= 0) return
        adcFrequency.value = freq
        processor.setAdcFrequency(freq)
    }

    fun processData(
        dataString: String,
        adcResolutionString: String,
        adcFrequencyString: String
    ) {
        val freq = parseFrequency(adcFrequencyString)
        val res = parseResolution(adcResolutionString)

        if (dataString.isEmpty() || freq <= 0 || res <= 0) return

        adcFrequency.value = freq
        adcResolution.value = res
        processor.processData(dataString, res, freq)
    }

    fun parseFrequency(freqString: String): Double {
        return try {
            val c = freqString.toDouble()
            if (c <= 0) throw NumberFormatException()
            c
        } catch (e: NumberFormatException) {
            -1.0
        }
    }

    fun parseResolution(res: String): Int {
        return try {
            val c = res.toInt()
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
