package alektas.telecomapp.data

import alektas.telecomapp.App
import alektas.telecomapp.domain.Repository
import alektas.telecomapp.domain.entities.ChannelData
import alektas.telecomapp.domain.entities.configs.DemodulatorConfig
import alektas.telecomapp.domain.entities.filters.FilterConfig
import alektas.telecomapp.domain.entities.signals.BaseSignal
import alektas.telecomapp.domain.entities.signals.DigitalSignal
import alektas.telecomapp.domain.entities.signals.Signal
import alektas.telecomapp.domain.entities.signals.noises.BaseNoise
import alektas.telecomapp.domain.entities.signals.noises.Noise
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import io.reactivex.subjects.BehaviorSubject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named

class SystemStorage : Repository {
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
    @field:[Inject Named("sourceSnrEnabled")] var isNoiseEnabled: Boolean = false
    private var channelList = mutableListOf<ChannelData>()
    private var decodedChannelList = mutableListOf<ChannelData>()
    private val channelsSource = BehaviorSubject.create<List<ChannelData>>()
    private val channelsSignalSource = BehaviorSubject.create<Signal>()
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
    private val channelsErrorsSource =
        BehaviorSubject.create<Map<BooleanArray, List<Int>>>()
    private val etherSource: Observable<Signal>
    private val filterConfigSource: BehaviorSubject<FilterConfig>
    private val demodulatorConfigSource =
        BehaviorSubject.create<DemodulatorConfig>()
    private val berByNoiseSource: Observable<Pair<Double, Double>>

    init {
        App.component.inject(this)

        etherSource = Observable.combineLatest(
            channelsSignalSource.startWith(BaseSignal()),
            noiseSource.startWith(BaseNoise()),
            BiFunction<Signal, Signal, Signal> { signal, noise -> signal + noise })
            .apply {
                subscribe { ether ->
                    demodulatorConfig.inputSignal = ether
                    demodulatorConfigSource.onNext(demodulatorConfig)
                }
            }

        filterConfigSource = BehaviorSubject.create<FilterConfig>().apply {
            subscribe { fConf ->
                demodulatorConfig.filterConfig = fConf
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

    override fun setChannels(channels: List<ChannelData>) {
        channelList = channels.toMutableList()
        channelsSource.onNext(channelList)
    }

    override fun removeChannel(channel: ChannelData) {
        if (channelList.remove(channel)) channelsSource.onNext(channelList)
    }

    override fun observeChannels(): Observable<List<ChannelData>> {
        return channelsSource
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

    override fun setChannelsSignal(signal: Signal) {
        channelsSignalSource.onNext(signal)
    }

    override fun observeChannelsSignal(): Observable<Signal> {
        return channelsSignalSource
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
    }

    override fun removeDecodedChannel(channel: ChannelData) {
        if (decodedChannelList.remove(channel)) decodedChannelsSource.onNext(decodedChannelList)
    }

    override fun setDecodedChannels(channels: List<ChannelData>) {
        decodedChannelList = channels.toMutableList()
        decodedChannelsSource.onNext(decodedChannelList)
    }

    override fun observeDecodedChannels(): Observable<List<ChannelData>> {
        return decodedChannelsSource
    }

    override fun setSimulatedChannelsErrors(errors: Map<BooleanArray, List<Int>>) {
        channelsErrorsSource.onNext(errors)
    }

    override fun observeSimulatedChannelsErrors(): Observable<Map<BooleanArray, List<Int>>> {
        return channelsErrorsSource
    }

    override fun observeBerByNoise(): Observable<Pair<Double, Double>> {
        return berByNoiseSource
    }
}