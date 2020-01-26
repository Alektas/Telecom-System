package alektas.telecomapp.ui.statistic.characteristics

import alektas.telecomapp.App
import alektas.telecomapp.domain.Repository
import alektas.telecomapp.domain.entities.Channel
import alektas.telecomapp.domain.entities.SystemProcessor
import alektas.telecomapp.domain.entities.configs.DecoderConfig
import alektas.telecomapp.utils.toSortedPoints
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.jjoe64.graphview.series.DataPoint
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Function3
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
    val theoreticBerData = MutableLiveData<Array<DataPoint>>()
    val dataSpeedData = MutableLiveData<Array<DataPoint>>()
    val capacityData = MutableLiveData<Array<DataPoint>>()
    val isChannelsInvalid = MutableLiveData<Boolean>()
    val pointsCount = MutableLiveData<Int>()
    val fromSnr = MutableLiveData<Float>()
    val toSnr = MutableLiveData<Float>()
    var theoreticBerList = mutableListOf<DataPoint>()
    var berList = mutableListOf<DataPoint>()
    var capacityList = mutableListOf<DataPoint>()
    var dataSpeedList = mutableListOf<DataPoint>()

    companion object {
        const val INVALID_SNR = -1000.0
    }

    init {
        App.component.inject(this)

        viewportData.value = processor.getCharacteristicsProcessRange()

        berList = storage.getBerByNoiseList().toSortedPoints()
        berData.value = berList.toTypedArray()
        theoreticBerList = storage.getTheoreticBerByNoiseList().toSortedPoints()
        theoreticBerData.value = theoreticBerList.toTypedArray()
        capacityList = storage.getCapacityByNoiseList().toSortedPoints()
        capacityData.value = capacityList.toTypedArray()
        dataSpeedList = storage.getDataSpeedByNoiseList().toSortedPoints()
        dataSpeedData.value = dataSpeedList.toTypedArray()

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

            storage.observeTheoreticBerByNoise()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableObserver<Pair<Double, Double>>() {
                    override fun onNext(t: Pair<Double, Double>) {
                        theoreticBerList.apply {
                            add(DataPoint(t.first, t.second))
                            sortBy { it.x }
                        }
                        theoreticBerData.value = theoreticBerList.toTypedArray()
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

            storage.observeDataSpeedByNoise()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableObserver<Pair<Double, Double>>() {
                    override fun onNext(t: Pair<Double, Double>) {
                        dataSpeedList.apply {
                            add(DataPoint(t.first, t.second))
                            sortBy { it.x }
                        }
                        dataSpeedData.value = dataSpeedList.toTypedArray()
                    }

                    override fun onComplete() {}

                    override fun onError(e: Throwable) {}
                }),

            Observable.combineLatest(
                storage.observeSimulatedChannels().startWith(listOf<Channel>()),
                storage.observeDecoderChannels().startWith(listOf<Channel>()),
                storage.observeDecoderConfig(),
                Function3 { sim: List<Channel>, dec: List<Channel>, dc: DecoderConfig ->
                    sim.isEmpty() || (!dc.isAutoDetection && dec.isEmpty())
                }
            )
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableObserver<Boolean>() {
                    override fun onNext(t: Boolean) {
                        isChannelsInvalid.value = t
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
            pointsCount > 0 && isChannelsInvalid.value != true) {
            viewportData.value = Pair(fromSnr, toSnr)
            berList.clear()
            capacityList.clear()
            theoreticBerList.clear()
            capacityList.clear()
            dataSpeedList.clear()
            processor.calculateCharacteristics(fromSnr, toSnr, pointsCount)

            this.pointsCount.value = pointsCount
            this.fromSnr.value = fromSnr.toFloat()
            this.toSnr.value = toSnr.toFloat()
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
