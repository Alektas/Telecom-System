package alektas.telecomapp.ui.decoder

import alektas.telecomapp.App
import alektas.telecomapp.domain.entities.generators.ChannelCodesGenerator
import alektas.telecomapp.domain.Repository
import alektas.telecomapp.domain.entities.Channel
import alektas.telecomapp.domain.entities.SystemProcessor
import alektas.telecomapp.domain.entities.coders.DataCodesContract
import alektas.telecomapp.domain.entities.configs.DecoderConfig
import alektas.telecomapp.domain.processes.ProcessState
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
    val isDecodingAvailable = MutableLiveData<Boolean>(true)
    val inputSignalData = MutableLiveData<Array<DataPoint>>()
    val channels = MutableLiveData<List<Channel>>()
    val channelsCodesType = MutableLiveData<Int>()
    val channelsCodesLength = MutableLiveData<Int>()
    val channelCount = MutableLiveData<Int>()
    val threshold = MutableLiveData<Float>()
    val dataCodesType = MutableLiveData<Int>()

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
                }),

            storage.observeTransmittingState()
                .map {
                    it.state == ProcessState.AWAITING
                            || it.state == ProcessState.FINISHED
                            || it.state == ProcessState.ERROR
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableObserver<Boolean>() {
                    override fun onNext(b: Boolean) {
                        isDecodingAvailable.value = b
                    }

                    override fun onComplete() {
                        isDecodingAvailable.value = true
                    }

                    override fun onError(e: Throwable) {
                        isDecodingAvailable.value = true
                    }
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
        isAutoDetection: Boolean,
        countString: String,
        codeTypeString: String,
        codeLengthString: String,
        thresholdString: String,
        isDataDecoding: Boolean,
        dataCodesTypeString: String
    ): Boolean {
        return try {
            val config = buildConfig(
                isAutoDetection = isAutoDetection,
                channelsCountString = countString,
                channelsCodesTypeString = codeTypeString,
                channelsCodesLengthString = codeLengthString,
                thresholdString = thresholdString,
                isDataDecoding = isDataDecoding,
                dataCodesTypeString = dataCodesTypeString
            )
            processor.applyConfig(config)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun removeChannel(channel: Channel) {
        storage.removeDecoderChannel(channel)
    }

    private fun buildConfig(
        isAutoDetection: Boolean? = null,
        channelsCountString: String? = null,
        channelsCodesLengthString: String? = null,
        channelsCodesTypeString: String? = null,
        thresholdString: String? = null,
        isDataDecoding: Boolean? = null,
        dataCodesTypeString: String? = null
    ): DecoderConfig {
        val channelCount = channelsCountString?.let { parseChannelCount(it) }
        val channelsCodesType =
            channelsCodesTypeString?.let { ChannelCodesGenerator.getCodeTypeId(it) }
        val channelsCodesLength = channelsCodesLengthString?.let { parseChannelCount(it) }
        val threshold = thresholdString?.let { parseThreshold(it) }
        val dataCodesType = dataCodesTypeString?.let { DataCodesContract.getCodeTypeId(it) }

        if (channelsCodesType != null && channelsCodesType < 0
            || dataCodesType != null && dataCodesType < 0
        ) {
            throw NumberFormatException()
        }

        saveSettings(
            channelCount,
            channelsCodesType,
            channelsCodesLength,
            threshold,
            dataCodesType
        )

        return DecoderConfig(
            isAutoDetection = isAutoDetection,
            channelCount = channelCount,
            channelsCodeType = channelsCodesType,
            channelsCodeLength = channelsCodesLength,
            threshold = threshold,
            isDataCoding = isDataDecoding,
            dataCodesType = dataCodesType
        )
    }

    private fun saveSettings(
        channelCount: Int? = null,
        channelsCodeType: Int? = null,
        channelsCodeLength: Int? = null,
        threshold: Float? = null,
        dataCodesType: Int? = null
    ) {
        channelCount?.let { this.channelCount.value = it }
        channelsCodeType?.let { this.channelsCodesType.value = it }
        channelsCodeLength?.let { this.channelsCodesLength.value = it }
        threshold?.let { this.threshold.value = it }
        dataCodesType?.let { this.dataCodesType.value = it }
    }

    fun parseChannelCount(count: String): Int {
        val c = count.toInt()
        if (c <= 0) throw NumberFormatException()
        return c
    }

    fun parseThreshold(threshold: String): Float {
        val c = threshold.toFloat()
        if (c < 0) throw NumberFormatException()
        return c
    }

    private fun parseChannelCode(codeString: String): BooleanArray {
        return codeString.filter { it == '1' || it == '0' }.map { it == '1' }.toBooleanArray()
    }

    override fun onCleared() {
        disposable.dispose()
        super.onCleared()
    }

}
