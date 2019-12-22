package alektas.telecomapp.ui.datasource.simulation

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

class EtherSettingsViewModel : ViewModel() {
    @Inject
    lateinit var storage: Repository
    @Inject
    lateinit var processor: SystemProcessor
    private val disposable = CompositeDisposable()
    val ether = MutableLiveData<Array<DataPoint>>()
    val noiseRate = MutableLiveData<Double>()
    val interferenceRate = MutableLiveData<Double>()
    val interferenceSparseness = MutableLiveData<Double>()

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

    companion object {
        const val INVALID_SNR = -1000.0
    }

    fun enableNoise() {
        processor.enableNoise()
    }

    fun disableNoise() {
        processor.disableNoise()
    }

    fun setNoise(snrString: String) {
        val snr = parseSnr(snrString)

        if (snr == INVALID_SNR) return

        noiseRate.value = snr
        processor.setNoise(snr)
    }

    fun enableInterference() {
        processor.enableInterference()
    }

    fun disableInterference() {
        processor.disableInterference()
    }

    fun changeInterferenceSparseness(sparsenessString: String) {
        val sparseness = parseSparseness(sparsenessString)
        if ( sparseness < 0) return

        interferenceSparseness.value = sparseness
        processor.setInterferenceSparseness(sparseness)
    }

    fun changeInterferenceRate(rateString: String) {
        val rate = parseSnr(rateString)
        if (rate == INVALID_SNR) return

        interferenceRate.value = rate
        processor.setInterferenceRate(rate)
    }

    fun setInterference(rateString: String, sparsenessString: String) {
        val rate = parseSnr(rateString)
        val sparseness = parseSparseness(sparsenessString)

        if (rate == INVALID_SNR || sparseness < 0) return

        interferenceRate.value = rate
        interferenceSparseness.value = sparseness
        processor.setInterference(sparseness, rate)
    }

    fun parseSnr(snr: String): Double {
        return try {
            snr.toDouble()
        } catch (e: NumberFormatException) {
            INVALID_SNR
        }
    }

    fun parseSparseness(sparsenessString: String): Double {
        return try {
            val c = sparsenessString.toDouble()
            if (c < 0) throw NumberFormatException()
            c
        } catch (e: NumberFormatException) {
            -1.0
        }
    }
}
