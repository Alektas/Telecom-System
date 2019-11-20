package alektas.telecomapp.ui.decoder

import alektas.telecomapp.App
import alektas.telecomapp.data.CodeGenerator
import alektas.telecomapp.domain.Repository
import alektas.telecomapp.domain.entities.ChannelData
import alektas.telecomapp.domain.entities.SystemProcessor
import alektas.telecomapp.utils.toDataPoints
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.jjoe64.graphview.series.DataPoint
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import io.reactivex.observers.DisposableObserver
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

class DecoderViewModel : ViewModel() {
    @Inject
    lateinit var storage: Repository
    @Inject
    lateinit var processor: SystemProcessor
    private val disposable = CompositeDisposable()
    val inputSignalData = MutableLiveData<Array<DataPoint>>()
    val channels = MutableLiveData<List<ChannelData>>()
    val codeType = MutableLiveData<Int>()
    val channelCount = MutableLiveData<Int>()
    val codeLength = MutableLiveData<Int>()

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
                        inputSignalData.value = t
                    }

                    override fun onComplete() {}

                    override fun onError(e: Throwable) {}
                }),

            Observable.combineLatest(
                storage.observeDecodedChannels(),
                storage.observeChannelsErrors().startWith(mapOf()),
                BiFunction<List<ChannelData>, Map<BooleanArray, List<Int>>, List<ChannelData>> { channels, errorsMap ->
                    errorsMap.forEach { (code, errors) ->
                        channels.find { it.code.contentEquals(code) }?.let { it.errors = errors }
                    }
                    channels
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableObserver<List<ChannelData>>() {
                    override fun onNext(t: List<ChannelData>) {
                        channels.value = t
                    }

                    override fun onComplete() {}

                    override fun onError(e: Throwable) {}
                })
        )
    }

    fun decodeCustomChannel(codeString: String) {
        val code = parseChannelCode(codeString)
        processor.addDecodedChannel(code)
    }

    fun decodeChannels(
        countString: String,
        codeLengthString: String,
        codeTypeString: String
    ) {
        val channelCount = parseChannelCount(countString)
        val codeLength = parseChannelCount(codeLengthString)
        val codeType = CodeGenerator.getCodeTypeId(codeTypeString)

        if (channelCount <= 0 || codeLength <= 0 || codeType < 0) return

        this.codeType.value = codeType
        this.channelCount.value = channelCount
        this.codeLength.value = codeLength

        processor.setDecodedChannels(channelCount, codeLength, codeType)
    }

    fun parseChannelCount(count: String): Int {
        return try {
            val c = count.toInt()
            if (c <= 0) throw NumberFormatException()
            c
        } catch (e: NumberFormatException) {
            -1
        }
    }

    fun parseChannelCode(codeString: String): BooleanArray {
        return codeString.filter { it == '1' || it == '0' }.map { it == '1' }.toBooleanArray()
    }

    fun removeChannel(channel: ChannelData) {
        storage.removeDecodedChannel(channel)
    }

}
