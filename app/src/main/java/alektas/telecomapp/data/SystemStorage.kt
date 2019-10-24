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
    private var demodulatorConfig: DemodulatorConfig = DemodulatorConfig()
    private var channelList = mutableListOf<ChannelData>()
    private val channelsSource: BehaviorSubject<List<ChannelData>> = BehaviorSubject.create()
    private val channelsSignalSource: BehaviorSubject<Signal> = BehaviorSubject.create()
    private val noiseSource: BehaviorSubject<Signal> = BehaviorSubject.create()
    private val demodulatedSignalSource: BehaviorSubject<BinarySignal> = BehaviorSubject.create()
    private val etherSource = Observable.combineLatest(
        channelsSignalSource.startWith(BaseSignal()),
        noiseSource.startWith(BaseSignal()),
        BiFunction<Signal, Signal, Signal> { signal, noise ->
            signal + noise
        })
        .debounce(500L, TimeUnit.MILLISECONDS)
        .switchMap { signal -> Observable.create<Signal> {
            demodulatorConfig.inputSignal = signal
            demodulatorConfigSource.onNext(demodulatorConfig)
            it.onNext(signal) } }
    private val demodulatorConfigSource: BehaviorSubject<DemodulatorConfig> =
        BehaviorSubject.create()
    private var filterConfig: FilterConfig = FilterConfig()
    private val filterConfigSource: BehaviorSubject<FilterConfig> = BehaviorSubject.create()

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
        demodulatorConfig.filterConfig = filterConfig
        demodulatorConfigSource.onNext(demodulatorConfig)
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
}