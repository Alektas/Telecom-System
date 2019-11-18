package alektas.telecomapp.domain

import alektas.telecomapp.domain.entities.ChannelData
import alektas.telecomapp.domain.entities.demodulators.DemodulatorConfig
import alektas.telecomapp.domain.entities.filters.FilterConfig
import alektas.telecomapp.domain.entities.signals.BinarySignal
import alektas.telecomapp.domain.entities.signals.Signal
import alektas.telecomapp.domain.entities.signals.noises.Noise
import io.reactivex.Observable

interface Repository {
    fun observeDemodulatorConfig(): Observable<DemodulatorConfig>
    fun changeDemodulatorConfig(config: DemodulatorConfig)
    fun getDemodulatorFilterConfig(): FilterConfig
    fun observeDemodulatorFilterConfig(): Observable<FilterConfig>
    fun setDemodulatorFilterConfig(config: FilterConfig)
    fun setDemodulatorFrequency(frequency: Double)
    fun updateDemodulatorConfig(
        frameLength: Int,
        bitTime: Double,
        codeLength: Int,
        threshold: Double
    )

    fun setChannels(channels: List<ChannelData>)
    fun removeChannel(channel: ChannelData)

    fun observeChannels(): Observable<List<ChannelData>>
    fun setChannelsSignal(signal: Signal)
    fun observeChannelsSignal(): Observable<Signal>

    fun setNoise(signal: Noise)
    fun enableNoise()
    fun disableNoise()
    fun observeNoise(): Observable<Noise>
    fun observeEther(): Observable<Signal>

    fun setDemodulatedSignal(signal: BinarySignal)
    fun observeDemodulatedSignal(): Observable<BinarySignal>
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
    fun observeDecodedChannels(): Observable<List<ChannelData>>

    fun setChannelsErrors(errors: List<List<Int>>)
    fun observeChannelsErrors(): Observable<List<List<Int>>>

    fun observeBer(): Observable<Pair<Double, Double>>

}