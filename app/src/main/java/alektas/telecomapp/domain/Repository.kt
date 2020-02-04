package alektas.telecomapp.domain

import alektas.telecomapp.domain.entities.Channel
import alektas.telecomapp.domain.entities.configs.ChannelsConfig
import alektas.telecomapp.domain.entities.configs.DecoderConfig
import alektas.telecomapp.domain.entities.configs.DemodulatorConfig
import alektas.telecomapp.domain.entities.filters.FilterConfig
import alektas.telecomapp.domain.entities.signals.DigitalSignal
import alektas.telecomapp.domain.entities.signals.Signal
import alektas.telecomapp.domain.entities.signals.noises.Noise
import alektas.telecomapp.domain.processes.ProcessState
import io.reactivex.Observable

interface Repository {
    fun getSimulatedChannelsConfiguration(): ChannelsConfig
    fun observeSimulationChannelsConfig(): Observable<ChannelsConfig>
    fun updateSimulationChannelsConfig(config: ChannelsConfig)
    fun getCurrentDemodulatorConfig(): DemodulatorConfig
    fun observeDemodulatorConfig(): Observable<DemodulatorConfig>
    fun getDemodulatorFilterConfig(): FilterConfig
    fun observeDemodulatorFilterConfig(): Observable<FilterConfig>
    fun setDemodulatorFilterConfig(config: FilterConfig)
    fun setDemodulatorFrequency(frequency: Double)
    fun updateDemodulatorConfig(
        delayCompensation: Float,
        frameLength: Int,
        bitTime: Double,
        codeLength: Int
    )
    fun getDecoderConfiguration(): DecoderConfig
    fun observeDecoderConfig(): Observable<DecoderConfig>
    fun updateDecoderConfig(config: DecoderConfig)

    fun startCountingStatistics()
    fun endCountingStatistics()
    fun setExpectedFrameCount(count: Int)
    fun startFileProcessingMode()

    fun setSimulatedChannels(channels: List<Channel>)
    fun removeSimulatedChannel(channel: Channel)
    fun getSimulatedChannels(): List<Channel>
    /**
     * @param withLast true - источник при подписке выдает последний список декодированных каналов
     */
    fun observeSimulatedChannels(withLast: Boolean = true): Observable<List<Channel>>

    fun setChannelsFrameSignal(signal: Signal)
    fun observeChannelsSignal(): Observable<Signal>

    fun setFileSignal(signal: Signal)
    fun observeFileSignal(): Observable<Signal>

    fun setNoise(signal: Noise)
    fun enableNoise(fromCache: Boolean)
    fun disableNoise()
    fun isNoiseEnabled(): Boolean
    fun observeNoise(): Observable<Noise>

    fun setInterference(signal: Noise)
    fun enableInterference(fromCache: Boolean)
    fun disableInterference()
    fun isInterferenceEnabled(): Boolean
    fun observeInterference(): Observable<Noise>

    fun setEther(ether: Signal)
    fun observeEther(): Observable<Signal>

    fun setDemodulatedSignal(signal: DigitalSignal)
    fun observeDemodulatedSignal(): Observable<DigitalSignal>

    fun setChannelI(sigI: Signal)
    fun observeChannelI(): Observable<Signal>
    fun setFilteredChannelI(filteredSigI: Signal)
    fun observeFilteredChannelI(): Observable<Signal>
    fun setChannelQ(sigQ: Signal)
    fun observeChannelQ(): Observable<Signal>
    fun setFilteredChannelQ(filteredSigQ: Signal)
    fun observeFilteredChannelQ(): Observable<Signal>

    fun addDecoderChannel(channel: Channel)
    fun removeDecoderChannel(channel: Channel)
    fun setDecoderChannels(channels: List<Channel>)
    fun getDecoderChannels(): List<Channel>
    /**
     * @param withLast true - источник при подписке выдает последний список декодированных каналов
     */
    fun observeDecoderChannels(withLast: Boolean = true): Observable<List<Channel>>

    fun setChannelsDataErrors(errors: Map<BooleanArray, List<Int>>)

    fun setTransmittingState(state: Int, progress: Int)
    fun setTransmittingSubProcess(state: ProcessState)
    fun removeTransmittingSubProcesses()
    fun resetTransmittingSubProcesses()
    fun observeTransmittingState(): Observable<ProcessState>

    fun observeTransmittingChannelsCount(): Observable<Int>
    fun observeTransmittedBitsCount(): Observable<Int>
    fun observeTransmittedDataBitsCount(): Observable<Int>
    fun observeReceivedBitsCount(): Observable<Int>
    fun observeReceivedDataBitsCount(): Observable<Int>
    fun observeReceivedErrorsCount(): Observable<Int>
    fun observeReceivedDataErrorsCount(): Observable<Int>
    fun observeBer(): Observable<Double>
    fun observeDataBer(): Observable<Double>

    fun setBerByNoise(berByNoise: Pair<Double, Double>)
    fun getBerByNoiseList(): List<Pair<Double, Double>>
    fun clearBerByNoiseList()
    fun observeBerByNoise(): Observable<Pair<Double, Double>>

    fun setTheoreticBerByNoise(berByNoise: Pair<Double, Double>)
    fun getTheoreticBerByNoiseList(): List<Pair<Double, Double>>
    fun clearTheoreticBerByNoiseList()
    fun observeTheoreticBerByNoise(): Observable<Pair<Double, Double>>

    fun setCapacityByNoise(capacityByNoise: Pair<Double, Double>)
    fun getCapacityByNoiseList(): List<Pair<Double, Double>>
    fun clearCapacityByNoiseList()
    fun observeCapacityByNoise(): Observable<Pair<Double, Double>>

    fun setDataSpeedByNoise(capacityByNoise: Pair<Double, Double>)
    fun getDataSpeedByNoiseList(): List<Pair<Double, Double>>
    fun clearDataSpeedByNoiseList()
    fun observeDataSpeedByNoise(): Observable<Pair<Double, Double>>
}