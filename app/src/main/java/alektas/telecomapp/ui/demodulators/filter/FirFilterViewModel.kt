package alektas.telecomapp.ui.demodulators.filter

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
import io.reactivex.disposables.Disposable
import io.reactivex.observers.DisposableObserver
import javax.inject.Inject

class FirFilterViewModel : ViewModel() {
    @Inject
    lateinit var system: Repository
    var config: FilterConfig
    var initConfigData = MutableLiveData<FilterConfig>()
    var impulseResponseData = MutableLiveData<Array<DataPoint>>()
    private val disposable: Disposable

    init {
        App.component.inject(this)
        config = system.getDemodulatorFilterConfig()
        initConfigData.value = config
        val filter = FirFilter(config)
        setImpulseResponse(filter)

        disposable = system.observeDemodulatorFilterConfig()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeWith(object : DisposableObserver<FilterConfig>() {
                override fun onComplete() {
                }

                override fun onNext(t: FilterConfig) {
                    config = t
                    val f = FirFilter(config)
                    setImpulseResponse(f)
                }

                override fun onError(e: Throwable) {
                }

            })
    }

    fun onOrderChanged(order: Int) {
        if (order != config.order) {
            config.order = order
            system.setDemodulatorFilterConfig(config)
        }
    }

    fun onCutoffFrequencyChanged(frequency: Double) {
        if (frequency != config.bandwidth) {
            config.bandwidth = frequency
            system.setDemodulatorFilterConfig(config)
        }
    }

    fun onWindowChanged(windowName: String) {
        val windowType = Window.getIdBy(windowName)

        if (windowType != config.windowType) {
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

}
