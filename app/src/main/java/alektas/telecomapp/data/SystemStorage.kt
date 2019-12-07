package alektas.telecomapp.data

import alektas.telecomapp.App
import alektas.telecomapp.domain.Repository
import alektas.telecomapp.domain.entities.ChannelData
import alektas.telecomapp.domain.entities.configs.DemodulatorConfig
import alektas.telecomapp.domain.entities.converters.ValueConverter
import alektas.telecomapp.domain.entities.filters.FilterConfig
import alektas.telecomapp.domain.entities.signals.BaseSignal
import alektas.telecomapp.domain.entities.signals.DigitalSignal
import alektas.telecomapp.domain.entities.signals.Signal
import alektas.telecomapp.domain.entities.signals.noises.BaseNoise
import alektas.telecomapp.domain.entities.signals.noises.Noise
import alektas.telecomapp.utils.FileWorker
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject
import javax.inject.Named

private const val USB_ETHER_DATA_FILE_NAME = "ether_data.txt"

class SystemStorage : Repository {
    @Inject
    lateinit var fileWorker: FileWorker
    @Inject
    lateinit var demodulatorConfig: DemodulatorConfig
    @Inject
    lateinit var filterConfig: FilterConfig
    /**
     * Шум источника.
     * Если шум отключен с помощью метода {@see #disableNoise}, то этот объект сохраняется,
     * а потребителям выдается пустой сигнал. При включении шума методом {@see #enableNoise}
     * потребителям снова выдается данный объект (если он не был нулевым)
     */
    private var noise: Noise? = null
    @JvmField
    @field:[Inject Named("sourceSnrEnabled")]
    var isNoiseEnabled: Boolean = false
    private var isStatisticsCounted: Boolean = false
    /**
     * Сохряняется ли эфир в файл в виде битов с разрядностью {@code adcBitDepth}.
     */
    private var isSavedToFile = false
    private var adcBitDepth = 8
    private var channelList = mutableListOf<ChannelData>()
    private var decodedChannelList = mutableListOf<ChannelData>()
    private val disposable = CompositeDisposable()
    private val simulatedChannelsSource = BehaviorSubject.create<List<ChannelData>>()
    private val channelsFrameSignalSource = BehaviorSubject.create<Signal>()
    private val channelISource = BehaviorSubject.create<Signal>()
    private val channelQSource = BehaviorSubject.create<Signal>()
    private val filteredChannelISource = BehaviorSubject.create<Signal>()
    private val filteredChannelQSource = BehaviorSubject.create<Signal>()
    private val noiseSource = BehaviorSubject.create<Noise>()
    private val demodulatedSignalSource =
        BehaviorSubject.create<DigitalSignal>()
    private val demodulatedSignalConstellationSource =
        BehaviorSubject.create<List<Pair<Double, Double>>>()
    private val decodedChannelsSource =
        BehaviorSubject.create<List<ChannelData>>()
    private val decodedChannelsLiveSource =
        PublishSubject.create<List<ChannelData>>()
    private val channelsErrorsSource =
        BehaviorSubject.create<Map<BooleanArray, List<Int>>>()
    private val etherSource: Observable<Signal>
    private val filterConfigSource = BehaviorSubject.create<FilterConfig>()
    private val demodulatorConfigSource =
        BehaviorSubject.create<DemodulatorConfig>()
    private val simulatedChannelsCountSource = BehaviorSubject.create<Int>()
    private var transmittedBitsCount = 0
        set(value) {
            field = value
            transmittedBitsCountSource.onNext(value)
        }
    private val transmittedBitsCountSource = BehaviorSubject.create<Int>()
    private var expectedFramesCount = 0
        set(value) {
            field = value
            expectedFramesCountSource.onNext(value)
        }
    private val expectedFramesCountSource = BehaviorSubject.create<Int>()
    private var receivedFramesCount = 0
        set(value) {
            field = value
            receivedFramesCountSource.onNext(value)
        }
    private val receivedFramesCountSource = BehaviorSubject.create<Int>()
    private var receivedBitsCount = 0
        set(value) {
            field = value
            receivedBitsCountSource.onNext(value)
        }
    private val receivedBitsCountSource = BehaviorSubject.create<Int>()
    private var receivedErrorsCount = 0
        set(value) {
            field = value
            receivedErrorsCountSource.onNext(value)
        }
    private val receivedErrorsCountSource = BehaviorSubject.create<Int>()
    private var ber = 0.0
        set(value) {
            field = value
            berSource.onNext(value)
        }
    private val berSource = BehaviorSubject.create<Double>()
    private val berByNoiseSource: Observable<Pair<Double, Double>>
    private val berProcess = BehaviorSubject.create<Int>()
    private val transmitProcess = BehaviorSubject.create<Int>()

    init {
        App.component.inject(this)

        disposable.addAll(
            simulatedChannelsSource
                .subscribe {
                    if (isStatisticsCounted) {
                        transmittedBitsCount += it.sumBy { c -> c.frameData.size }
                        simulatedChannelsCountSource.onNext(it.size)
                    }
                },

            decodedChannelsSource
                .subscribe {
                    if (isStatisticsCounted) {
                        if (receivedFramesCount >= expectedFramesCount) {
                            endCountingStatistics()
                            return@subscribe
                        }

                        receivedBitsCount += it.sumBy { c -> c.frameData.size }
                        receivedErrorsCount += it.sumBy { c -> c.errors?.size ?: 0 }
                        ber = receivedErrorsCount / receivedBitsCount.toDouble() * 100

                        receivedFramesCount++
                        val progress =
                            (receivedFramesCount / expectedFramesCount.toDouble() * 100).toInt()
                        transmitProcess.onNext(progress)
                    }
                },

            channelsFrameSignalSource
                .observeOn(Schedulers.io())
                .doOnSubscribe {
                    if (isSavedToFile) {
                        fileWorker.cleanFile(USB_ETHER_DATA_FILE_NAME)
                    }
                }
                .subscribe {
                    if (isStatisticsCounted && isSavedToFile) {
                        val bitString =
                            ValueConverter(adcBitDepth).convertToBitString(it.getValues())
                        fileWorker.appendToFile(USB_ETHER_DATA_FILE_NAME, bitString)
                    }
                },

            filterConfigSource
                .subscribe { fConf ->
                    demodulatorConfig.filterConfig = fConf
                    demodulatorConfigSource.onNext(demodulatorConfig)
                }
        )

        etherSource = Observable.combineLatest(
            channelsFrameSignalSource.startWith(BaseSignal()),
            noiseSource.startWith(BaseNoise()),
            BiFunction<Signal, Signal, Signal> { signal, noise -> signal + noise })
            .apply {
                subscribe { ether ->
                    demodulatorConfig.inputSignal = ether
                    demodulatorConfigSource.onNext(demodulatorConfig)
                }
            }

        berByNoiseSource = Observable.zip(
            decodedChannelsSource, noiseSource,
            BiFunction { channels: List<ChannelData>, noise: Noise ->
                val errorsCount = channels.sumBy { it.errors?.size ?: 0 }
                val bitsReceived = channels.sumBy { it.frameData.size }.toDouble()
                Pair(noise.snr(), errorsCount / bitsReceived * 100)
            })
    }

    override fun receiveDataFromUsb(): String {
        return fileWorker.readFile(USB_ETHER_DATA_FILE_NAME)
    }

    override fun getCurrentDemodulatorConfig(): DemodulatorConfig {
        return demodulatorConfig
    }

    override fun observeDemodulatorConfig(): Observable<DemodulatorConfig> {
        return demodulatorConfigSource
    }

    override fun updateDemodulatorConfig(
        frameLength: Int,
        bitTime: Double,
        codeLength: Int
    ) {
        demodulatorConfig.frameLength = frameLength
        demodulatorConfig.bitTime = bitTime
        demodulatorConfig.codeLength = codeLength
        demodulatorConfigSource.onNext(demodulatorConfig)
    }

    override fun setDemodulatorFrequency(frequency: Double) {
        demodulatorConfig.carrierFrequency = frequency
        demodulatorConfigSource.onNext(demodulatorConfig)
    }

    override fun getDemodulatorFilterConfig(): FilterConfig {
        return filterConfig
    }

    override fun observeDemodulatorFilterConfig(): Observable<FilterConfig> {
        return filterConfigSource
    }

    override fun setDemodulatorFilterConfig(config: FilterConfig) {
        filterConfig = config
        filterConfigSource.onNext(filterConfig)
    }

    override fun startCountingStatistics() {
        clearStatistics()
        isStatisticsCounted = true
    }

    private fun clearStatistics() {
        expectedFramesCount = 0
        transmittedBitsCount = 0
        receivedFramesCount = 0
        receivedBitsCount = 0
        receivedErrorsCount = 0
        ber = 0.0
    }

    override fun endCountingStatistics() {
        isStatisticsCounted = false
    }

    override fun setExpectedFrameCount(count: Int) {
        expectedFramesCount = count
    }

    override fun setChannelsData(channels: List<ChannelData>) {
        channelList = channels.toMutableList()
        simulatedChannelsSource.onNext(channelList)
    }

    override fun removeChannel(channel: ChannelData) {
        if (channelList.remove(channel)) simulatedChannelsSource.onNext(channelList)
    }

    override fun observeSimulatedChannels(): Observable<List<ChannelData>> {
        return simulatedChannelsSource
    }

    override fun setNoise(signal: Noise) {
        noise = signal
        if (isNoiseEnabled) noiseSource.onNext(signal)
    }

    /**
     * Включить шум.
     * @param fromCache Если <code>true</code>, то сохраненный в кэше экземпляр шума добавляется в эфир.
     */
    override fun enableNoise(fromCache: Boolean) {
        isNoiseEnabled = true
        if (fromCache) noise?.let { noiseSource.onNext(it) }
    }

    override fun disableNoise() {
        isNoiseEnabled = false
        noiseSource.onNext(BaseNoise())
    }

    override fun isNoiseEnabled(): Boolean {
        return isNoiseEnabled
    }

    override fun observeNoise(): Observable<Noise> {
        return noiseSource
    }

    override fun setChannelsFrameSignal(signal: Signal) {
        channelsFrameSignalSource.onNext(signal)
    }

    override fun observeChannelsSignal(): Observable<Signal> {
        return channelsFrameSignalSource
    }

    override fun setEther(ether: Signal) {
        channelsFrameSignalSource.onNext(ether)
    }

    override fun observeEther(): Observable<Signal> {
        return etherSource
    }

    override fun setDemodulatedSignal(signal: DigitalSignal) {
        demodulatedSignalSource.onNext(signal)
    }

    override fun observeDemodulatedSignal(): Observable<DigitalSignal> {
        return demodulatedSignalSource
    }

    override fun setDemodulatedSignalConstellation(points: List<Pair<Double, Double>>) {
        demodulatedSignalConstellationSource.onNext(points)
    }

    override fun observeDemodulatedSignalConstellation(): Observable<List<Pair<Double, Double>>> {
        return demodulatedSignalConstellationSource
    }

    override fun setChannelI(sigI: Signal) {
        channelISource.onNext(sigI)
    }

    override fun setFilteredChannelI(filteredSigI: Signal) {
        filteredChannelISource.onNext(filteredSigI)
    }

    override fun setChannelQ(sigQ: Signal) {
        channelQSource.onNext(sigQ)
    }

    override fun setFilteredChannelQ(filteredSigQ: Signal) {
        filteredChannelQSource.onNext(filteredSigQ)
    }

    override fun observeChannelI(): Observable<Signal> {
        return channelISource
    }

    override fun observeFilteredChannelI(): Observable<Signal> {
        return filteredChannelISource
    }

    override fun observeChannelQ(): Observable<Signal> {
        return channelQSource
    }

    override fun observeFilteredChannelQ(): Observable<Signal> {
        return filteredChannelQSource
    }


    override fun addDecodedChannel(channel: ChannelData) {
        decodedChannelList.add(channel)
        decodedChannelsSource.onNext(decodedChannelList)
        decodedChannelsLiveSource.onNext(decodedChannelList)
    }

    override fun removeDecodedChannel(channel: ChannelData) {
        if (decodedChannelList.remove(channel)) {
            decodedChannelsSource.onNext(decodedChannelList)
            decodedChannelsLiveSource.onNext(decodedChannelList)
        }
    }

    override fun setDecodedChannels(channels: List<ChannelData>) {
        decodedChannelList = channels.toMutableList()
        decodedChannelsSource.onNext(decodedChannelList)
        decodedChannelsLiveSource.onNext(decodedChannelList)
    }

    /**
     * @param withLast true - источник при подписке выдает последний список декодированных каналов
     */
    override fun observeDecodedChannels(withLast: Boolean): Observable<List<ChannelData>> {
        return if (withLast) decodedChannelsSource else decodedChannelsLiveSource
    }

    override fun setSimulatedChannelsErrors(errors: Map<BooleanArray, List<Int>>) {
        channelsErrorsSource.onNext(errors)
    }

    override fun observeSimulatedChannelsErrors(): Observable<Map<BooleanArray, List<Int>>> {
        return channelsErrorsSource
    }

    override fun observeTransmitProcess(): Observable<Int> {
        return transmitProcess
    }

    override fun observeTransmittingChannelsCount(): Observable<Int> {
        return simulatedChannelsCountSource
    }

    override fun observeTransmittedBitsCount(): Observable<Int> {
        return transmittedBitsCountSource
    }

    override fun observeReceivedBitsCount(): Observable<Int> {
        return receivedBitsCountSource
    }

    override fun observeReceivedErrorsCount(): Observable<Int> {
        return receivedErrorsCountSource
    }

    override fun observeBer(): Observable<Double> {
        return berSource
    }

    override fun observeBerProcess(): Observable<Int> {
        return berProcess
    }

    override fun observeBerByNoise(): Observable<Pair<Double, Double>> {
        return berByNoiseSource
    }
}