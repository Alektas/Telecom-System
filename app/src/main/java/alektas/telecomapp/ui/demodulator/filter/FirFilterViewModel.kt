package alektas.telecomapp.ui.demodulator.filter

import alektas.telecomapp.App
import alektas.telecomapp.domain.Repository
import alektas.telecomapp.domain.entities.Window
import alektas.telecomapp.domain.entities.filters.Filter
import alektas.telecomapp.domain.entities.filters.FilterConfig
import alektas.telecomapp.domain.entities.filters.FirFilter
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.jjoe64.graphview.series.DataPoint
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.DisposableObserver
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

class FirFilterViewModel : ViewModel() {
    @Inject
    lateinit var system: Repository
    @Inject
    lateinit var config: FilterConfig
    var order = MutableLiveData<Int>()
    var cutoffFreq = MutableLiveData<Float>()
    var windowType = MutableLiveData<Int>()
    var impulseResponseData = MutableLiveData<Array<DataPoint>>()
    private val disposable = CompositeDisposable()

    init {
        App.component.inject(this)
        setImpulseResponse(FirFilter(config))

        disposable.addAll(
            system.observeDemodulatorFilterConfig()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableObserver<FilterConfig>() {
                    override fun onNext(t: FilterConfig) {
                        config = t
                        setImpulseResponse(FirFilter(t))
                    }

                    override fun onComplete() {}

                    override fun onError(e: Throwable) {}
                })
        )
    }

    fun onOrderChanged(order: Int) {
        if (order != config.order) {
            this.order.value = order
            config.order = order
            system.setDemodulatorFilterConfig(config)
        }
    }

    fun onCutoffFrequencyChanged(frequency: Double) {
        if (frequency != config.bandwidth) {
            cutoffFreq.value = frequency.toFloat()
            config.bandwidth = frequency
            system.setDemodulatorFilterConfig(config)
        }
    }

    fun onWindowChanged(windowName: String) {
        val windowType = Window.getIdBy(windowName)
        if (windowType != config.windowType) {
            this.windowType.value = windowType
            config.windowType = windowType
            system.setDemodulatorFilterConfig(config)
        }
    }

    private fun setImpulseResponse(filter: Filter) {
        impulseResponseData.value = filter.impulseResponse()
            .mapIndexed { i, v -> DataPoint(i.toDouble(), v) }
            .toTypedArray()
    }

    override fun onCleared() {
        disposable.dispose()
        super.onCleared()
    }

    fun parseFilterOrder(orderString: String): Int {
        return try {
            val c = orderString.toInt()
            if (c <= 0) throw NumberFormatException()
            c
        } catch (e: NumberFormatException) {
            -1
        }
    }

    fun parseCutoffFrequency(freqString: String): Double {
        return try {
            val c = freqString.toDouble()
            if (c <= 0) throw NumberFormatException()
            c
        } catch (e: NumberFormatException) {
            -1.0
        }
    }

}
