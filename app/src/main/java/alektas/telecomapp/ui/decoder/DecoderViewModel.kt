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
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
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
            storage.observeDecodedChannels()
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
        codeTypeString: String
    ) {
        val channelCount = parseChannelCount(countString)
        val codeType = CodeGenerator.getCodeTypeId(codeTypeString)

        if (channelCount <= 0 || codeType < 0) return

        this.codeType.value = codeType
        this.channelCount.value = channelCount

        processor.setDecodedChannels(channelCount, codeType)
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
