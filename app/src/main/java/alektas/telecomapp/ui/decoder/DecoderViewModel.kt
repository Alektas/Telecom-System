package alektas.telecomapp.ui.decoder

import alektas.telecomapp.App
import alektas.telecomapp.data.CodeGenerator
import alektas.telecomapp.domain.Repository
import alektas.telecomapp.domain.entities.Channel
import alektas.telecomapp.domain.entities.SystemProcessor
import alektas.telecomapp.domain.entities.configs.DecoderConfig
import alektas.telecomapp.utils.L
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
    val channels = MutableLiveData<List<Channel>>()
    val codeType = MutableLiveData<Int>()
    val channelCount = MutableLiveData<Int>()
    val threshold = MutableLiveData<Float>()
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

            storage.observeDecoderChannels()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableObserver<List<Channel>>() {
                    override fun onNext(t: List<Channel>) {
                        channels.value = t
                    }

                    override fun onComplete() {}

                    override fun onError(e: Throwable) {}
                })
        )
    }

    fun addCustomChannel(codeString: String): Boolean {
        if (codeString.isEmpty()) return false
        val code = parseChannelCode(codeString)
        if (code.isEmpty()) return false

        processor.addCustomDecoderChannel(code)
        return true
    }

    fun setupDecoding(
        countString: String,
        codeLengthString: String,
        codeTypeString: String,
        thresholdString: String
    ): Boolean {
        val channelCount = parseChannelCount(countString)
        val codeLength = parseChannelCount(codeLengthString)
        val codeType = CodeGenerator.getCodeTypeId(codeTypeString)
        val threshold = parseThreshold(thresholdString)

        if (channelCount <= 0 || codeLength <= 0 || codeType < 0 || threshold < 0) return false

        this.codeType.value = codeType
        this.channelCount.value = channelCount
        this.codeLength.value = codeLength
        this.threshold.value = threshold

        val config = DecoderConfig(
            isAutoDetection = false,
            channelCount = channelCount,
            codeLength = codeLength,
            codeType = codeType,
            threshold = threshold
        )

        processor.applyConfig(config)
        return true
    }

    fun setupAutoDecoding(
        codeLengthString: String,
        codeTypeString: String,
        thresholdString: String
    ): Boolean {
        val codeLength = parseChannelCount(codeLengthString)
        val codeType = CodeGenerator.getCodeTypeId(codeTypeString)
        val threshold = parseThreshold(thresholdString)

        if (codeLength <= 0 || codeType < 0 || threshold < 0) return false

        this.codeType.value = codeType
        this.codeLength.value = codeLength
        this.threshold.value = threshold

        val config = DecoderConfig(
            isAutoDetection = true,
            codeLength = codeLength,
            codeType = codeType,
            threshold = threshold
        )

        processor.applyConfig(config)
        return true
    }

    fun removeChannel(channel: Channel) {
        storage.removeDecoderChannel(channel)
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

    fun parseThreshold(threshold: String): Float {
        return try {
            val c = threshold.toFloat()
            if (c < 0) throw NumberFormatException()
            c
        } catch (e: NumberFormatException) {
            -1.0f
        }
    }

    override fun onCleared() {
        disposable.dispose()
        super.onCleared()
    }

}
