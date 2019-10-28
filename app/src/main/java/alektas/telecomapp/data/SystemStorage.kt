package alektas.telecomapp.data

import alektas.telecomapp.domain.Repository
import alektas.telecomapp.domain.entities.ChannelData
import alektas.telecomapp.domain.entities.demodulators.DemodulatorConfig
import alektas.telecomapp.domain.entities.filters.FilterConfig
import alektas.telecomapp.domain.entities.signals.BaseSignal
import alektas.telecomapp.domain.entities.signals.BinarySignal
import alektas.telecomapp.domain.entities.signals.Signal
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import io.reactivex.subjects.BehaviorSubject
import java.util.concurrent.TimeUnit

class SystemStorage : Repository {
    private var channelList = mutableListOf<ChannelData>()
    private var decodedChannelList = mutableListOf<ChannelData>()
    private val channelsSource = BehaviorSubject.create<List<ChannelData>>()
    private val channelsSignalSource = BehaviorSubject.create<Signal>()
    private val noiseSource = BehaviorSubject.create<Signal>()
    private val demodulatedSignalSource = BehaviorSubject.create<BinarySignal>()
    private val demodulatedSignalConstellationSource =
        BehaviorSubject.create<List<Pair<Double, Double>>>()
    private val decodedChannelsSource = BehaviorSubject.create<List<ChannelData>>()
    private val etherSource = Observable.combineLatest(
        channelsSignalSource.startWith(BaseSignal()),
        noiseSource.startWith(BaseSignal()),
        BiFunction<Signal, Signal, Signal> { signal, noise -> signal + noise })
        .debounce(500L, TimeUnit.MILLISECONDS)
        .switchMap { signal -> Observable.create<Signal> { it.onNext(signal) } }
        .apply {
            subscribe { ether ->
                demodulatorConfig.inputSignal = ether
                demodulatorConfigSource.onNext(demodulatorConfig)
            }
        }
    private var filterConfig: FilterConfig = FilterConfig()
    private val filterConfigSource = BehaviorSubject.create<FilterConfig>().apply {
        subscribe { fConf ->
            demodulatorConfig.filterConfig = fConf
            demodulatorConfigSource.onNext(demodulatorConfig)
        }
    }
    private var demodulatorConfig: DemodulatorConfig = DemodulatorConfig()
    private val demodulatorConfigSource = BehaviorSubject.create<DemodulatorConfig>()

    override fun getDemodulatorConfig(): DemodulatorConfig {
        return demodulatorConfig
    }

    override fun observeDemodulatorConfig(): Observable<DemodulatorConfig> {
        return demodulatorConfigSource
    }

    override fun setDemodulatorConfig(config: DemodulatorConfig) {
        demodulatorConfig = config
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

    override fun setDemodulatorFrequency(frequency: Double) {
        demodulatorConfig.carrierFrequency = frequency
        setDemodulatorConfig(demodulatorConfig)
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

    override fun setNoise(signal: Signal) {
        noiseSource.onNext(signal)
    }

    override fun observeNoise(): Observable<Signal> {
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

    override fun setDemodulatedSignal(signal: BinarySignal) {
        demodulatedSignalSource.onNext(signal)
    }

    override fun observeDemodulatedSignal(): Observable<BinarySignal> {
        return demodulatedSignalSource
    }

    override fun setDemodulatedSignalConstellation(points: List<Pair<Double, Double>>) {
        demodulatedSignalConstellationSource.onNext(points)
    }

    override fun observeDemodulatedSignalConstellation(): Observable<List<Pair<Double, Double>>> {
        return demodulatedSignalConstellationSource
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
}