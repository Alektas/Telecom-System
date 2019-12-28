package alektas.telecomapp.ui.statistic.characteristics

import alektas.telecomapp.App
import alektas.telecomapp.domain.Repository
import alektas.telecomapp.domain.entities.Channel
import alektas.telecomapp.domain.entities.SystemProcessor
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.jjoe64.graphview.series.DataPoint
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.DisposableObserver
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

class CharacteristicsViewModel : ViewModel() {
    @Inject
    lateinit var storage: Repository
    @Inject
    lateinit var processor: SystemProcessor
    private val disposable = CompositeDisposable()
    val viewportData = MutableLiveData<Pair<Double, Double>>()
    val berData = MutableLiveData<Array<DataPoint>>()
    val capacityData = MutableLiveData<Array<DataPoint>>()
    val berList = mutableListOf<DataPoint>()
    val capacityList = mutableListOf<DataPoint>()
    var channels = listOf<Channel>()

    companion object {
        const val INVALID_SNR = -1000.0
    }

    init {
        App.component.inject(this)

        disposable.addAll(
            storage.observeBerByNoise()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableObserver<Pair<Double, Double>>() {
                    override fun onNext(t: Pair<Double, Double>) {
                        berList.apply {
                            add(DataPoint(t.first, t.second))
                            sortBy { it.x }
                        }
                        berData.value = berList.toTypedArray()
                    }

                    override fun onComplete() {}

                    override fun onError(e: Throwable) {}
                }),

            storage.observeCapacityByNoise()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableObserver<Pair<Double, Double>>() {
                    override fun onNext(t: Pair<Double, Double>) {
                        capacityList.apply {
                            add(DataPoint(t.first, t.second))
                            sortBy { it.x }
                        }
                        capacityData.value = capacityList.toTypedArray()
                    }

                    override fun onComplete() {}

                    override fun onError(e: Throwable) {}
                }),

            storage.observeDecodedChannels()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableObserver<List<Channel>>() {
                    override fun onNext(t: List<Channel>) {
                        channels = t
                    }

                    override fun onComplete() {}

                    override fun onError(e: Throwable) {}
                })
        )
    }

    fun calculateCharacteristics(from: String, to: String, count: String): Boolean {
        val fromSnr = parseSnr(from)
        val toSnr = parseSnr(to)
        val pointsCount = parseCount(count)

        if (fromSnr != INVALID_SNR && toSnr != INVALID_SNR && fromSnr < toSnr &&
            pointsCount > 0 && channels.isNotEmpty()) {
            viewportData.value = Pair(fromSnr, toSnr)
            berList.clear()
            capacityList.clear()
            processor.calculateCharacteristics(fromSnr, toSnr, pointsCount)
            return true
        }

        return false
    }

    fun parseSnr(snr: String): Double {
        return try {
            snr.toDouble()
        } catch (e: NumberFormatException) {
            INVALID_SNR
        }
    }

    fun parseCount(count: String): Int {
        return try {
            val c = count.toInt()
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
