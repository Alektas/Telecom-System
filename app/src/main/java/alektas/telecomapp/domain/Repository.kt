package alektas.telecomapp.domain

import alektas.telecomapp.domain.entities.ChannelData
import alektas.telecomapp.domain.entities.configs.DemodulatorConfig
import alektas.telecomapp.domain.entities.filters.FilterConfig
import alektas.telecomapp.domain.entities.signals.DigitalSignal
import alektas.telecomapp.domain.entities.signals.Signal
import alektas.telecomapp.domain.entities.signals.noises.Noise
import io.reactivex.Observable

interface Repository {
    fun getCurrentDemodulatorConfig(): DemodulatorConfig
    fun observeDemodulatorConfig(): Observable<DemodulatorConfig>
    fun getDemodulatorFilterConfig(): FilterConfig
    fun observeDemodulatorFilterConfig(): Observable<FilterConfig>
    fun setDemodulatorFilterConfig(config: FilterConfig)
    fun setDemodulatorFrequency(frequency: Double)
    fun updateDemodulatorConfig(
        frameLength: Int,
        bitTime: Double,
        codeLength: Int
    )

    fun startCountingStatistics()
    fun endCountingStatistics()
    fun setExpectedFrameCount(count: Int)

    fun setChannelsData(channels: List<ChannelData>)
    fun removeChannel(channel: ChannelData)
    fun observeSimulatedChannels(): Observable<List<ChannelData>>

    fun setChannelsFrameSignal(signal: Signal)
    fun observeChannelsSignal(): Observable<Signal>

    fun setNoise(signal: Noise)
    fun enableNoise(fromCache: Boolean)
    fun disableNoise()
    fun isNoiseEnabled(): Boolean
    fun observeNoise(): Observable<Noise>

    fun setInterference(signal: Noise)
    fun enableInterference(fromCache: Boolean)
    fun disableInterference()
    fun observeInterference(): Observable<Noise>

    fun setEther(ether: Signal)
    fun observeEther(): Observable<Signal>

    fun setDemodulatedSignal(signal: DigitalSignal)
    fun observeDemodulatedSignal(): Observable<DigitalSignal>
    fun setDemodulatedSignalConstellation(points: List<Pair<Double, Double>>)
    fun observeDemodulatedSignalConstellation(): Observable<List<Pair<Double, Double>>>

    fun setChannelI(sigI: Signal)
    fun observeChannelI(): Observable<Signal>
    fun setFilteredChannelI(filteredSigI: Signal)
    fun observeFilteredChannelI(): Observable<Signal>
    fun setChannelQ(sigQ: Signal)
    fun observeChannelQ(): Observable<Signal>
    fun setFilteredChannelQ(filteredSigQ: Signal)
    fun observeFilteredChannelQ(): Observable<Signal>

    fun addDecodedChannel(channel: ChannelData)
    fun removeDecodedChannel(channel: ChannelData)
    fun setDecodedChannels(channels: List<ChannelData>)
    fun observeDecodedChannels(withLast: Boolean = true): Observable<List<ChannelData>>

    fun setSimulatedChannelsErrors(errors: Map<BooleanArray, List<Int>>)
    fun observeSimulatedChannelsErrors(): Observable<Map<BooleanArray, List<Int>>>

    fun observeTransmitProcess(): Observable<Int>
    fun observeTransmittingChannelsCount(): Observable<Int>
    fun observeTransmittedBitsCount(): Observable<Int>
    fun observeReceivedBitsCount(): Observable<Int>
    fun observeReceivedErrorsCount(): Observable<Int>
    fun observeBer(): Observable<Double>
    fun observeBerProcess(): Observable<Int>

    fun observeBerByNoise(): Observable<Pair<Double, Double>>
}