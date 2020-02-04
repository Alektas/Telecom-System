package alektas.telecomapp.data

import alektas.telecomapp.App
import alektas.telecomapp.domain.Repository
import alektas.telecomapp.domain.entities.Channel
import alektas.telecomapp.domain.entities.configs.ChannelsConfig
import alektas.telecomapp.domain.entities.configs.DecoderConfig
import alektas.telecomapp.domain.entities.configs.DemodulatorConfig
import alektas.telecomapp.domain.entities.converters.ValueConverter
import alektas.telecomapp.domain.entities.filters.FilterConfig
import alektas.telecomapp.domain.entities.signals.BaseSignal
import alektas.telecomapp.domain.entities.signals.DigitalSignal
import alektas.telecomapp.domain.entities.signals.Signal
import alektas.telecomapp.domain.entities.signals.noises.BaseNoise
import alektas.telecomapp.domain.entities.signals.noises.Noise
import alektas.telecomapp.domain.processes.ProcessState
import alektas.telecomapp.domain.processes.TRANSMITTING_PROCESS_KEY
import alektas.telecomapp.domain.processes.TRANSMITTING_PROCESS_NAME
import alektas.telecomapp.utils.FileWorker
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Function3
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject
import javax.inject.Named

private const val INTERNAL_ETHER_DATA_FILE_NAME = "ether_data.txt"

class SystemStorage : Repository {
    @Inject
    lateinit var fileWorker: FileWorker
    @Inject
    lateinit var demodulatorConfig: DemodulatorConfig
    @Inject
    lateinit var filterConfig: FilterConfig
    @Inject
    lateinit var decoderConfig: DecoderConfig
    @Inject
    lateinit var simulatedChannelsConfig: ChannelsConfig
    /**
     * Шум источника.
     * Если шум отключен с помощью метода {@see #disableNoise}, то этот объект сохраняется,
     * а потребителям взамен выдается пустой сигнал. При включении шума методом {@see #enableNoise}
     * потребителям снова выдается данный объект (если он не был нулевым)
     */
    private var noise: Noise? = null
    @JvmField
    @field:[Inject Named("sourceSnrEnabled")]
    var isNoiseEnabled: Boolean = false
    /**
     * Помехи источника.
     * Если помехи отключены с помощью метода [@see #disableInterference()], то этот объект сохраняется,
     * а потребителям взамен выдается пустой сигнал. При включении шума методом [@see #enableInterference()]
     * потребителям снова выдается данный объект (если он не был нулевым)
     */
    private var interference: Noise? = null
    @JvmField
    @field:[Inject Named("sourceInterferenceEnabled")]
    var isInterferenceEnabled: Boolean = false
    private var isStatisticsCounting: Boolean = false
    /**
     * Сохряняется ли эфир в файл в виде битов с разрядностью {@code adcBitDepth}.
     */
    private var isSavedToFile = false
    private var adcBitDepth = 8
    private var simulatedChannelList = mutableListOf<Channel>()
    private var decoderChannelList = mutableListOf<Channel>()
    private val disposable = CompositeDisposable()
    private val simulatedChannelsSource = BehaviorSubject.create<List<Channel>>()
    private val channelsFrameSignalSource = BehaviorSubject.create<Signal>()
    private val fileSignalSource = BehaviorSubject.create<Signal>()
    private val channelISource = BehaviorSubject.create<Signal>()
    private val channelQSource = BehaviorSubject.create<Signal>()
    private val filteredChannelISource = BehaviorSubject.create<Signal>()
    private val filteredChannelQSource = BehaviorSubject.create<Signal>()
    private val noiseSource = BehaviorSubject.create<Noise>()
    private val interferenceSource = BehaviorSubject.create<Noise>()
    private val demodulatedSignalSource =
        BehaviorSubject.create<DigitalSignal>()
    private val decoderChannelsSource =
        BehaviorSubject.create<List<Channel>>()
    private val decoderChannelsLiveSource =
        PublishSubject.create<List<Channel>>()
    private val channelsErrorsSource =
        BehaviorSubject.create<Map<BooleanArray, List<Int>>>()
    private val etherSource: Observable<Signal>
    private val filterConfigSource = BehaviorSubject.create<FilterConfig>()
    private val demodulatorConfigSource =
        BehaviorSubject.create<DemodulatorConfig>()
    private val decoderConfigSource =
        BehaviorSubject.create<DecoderConfig>()
    private val simulatedChannelsConfigSource = BehaviorSubject.create<ChannelsConfig>()
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
    private val berByNoiseList = mutableListOf<Pair<Double, Double>>()
    private val berByNoiseSource = PublishSubject.create<Pair<Double, Double>>()
    private val theoreticBerByNoiseList = mutableListOf<Pair<Double, Double>>()
    private val theoreticBerByNoiseSource = PublishSubject.create<Pair<Double, Double>>()
    private val capacityByNoiseList = mutableListOf<Pair<Double, Double>>()
    private val capacityByNoiseSource = PublishSubject.create<Pair<Double, Double>>()
    private val dataSpeedByNoiseList = mutableListOf<Pair<Double, Double>>()
    private val dataSpeedByNoiseSource = PublishSubject.create<Pair<Double, Double>>()
    private val transmittingStateSource = BehaviorSubject.create<ProcessState>()
    private val transmittingState =
        ProcessState(TRANSMITTING_PROCESS_KEY, TRANSMITTING_PROCESS_NAME)

    init {
        App.component.inject(this)
        demodulatorConfigSource.onNext(demodulatorConfig)

        disposable.addAll(
            simulatedChannelsSource
                .subscribe {
                    if (isStatisticsCounting) {
                        transmittedBitsCount += it.sumBy { c -> c.frameData.size }
                        simulatedChannelsCountSource.onNext(it.size)
                    }
                },

            decoderChannelsSource
                .subscribe {
                    if (isStatisticsCounting) {
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
                        transmittingStateSource.onNext(transmittingState.apply {
                            if (receivedFramesCount == expectedFramesCount) endCountingStatistics()
                            this.progress = progress
                        })
                    }
                },

            channelsFrameSignalSource
                .observeOn(Schedulers.io())
                .doOnSubscribe {
                    if (isSavedToFile) {
                        fileWorker.cleanFile(INTERNAL_ETHER_DATA_FILE_NAME)
                    }
                }
                .subscribe {
                    if (isStatisticsCounting && isSavedToFile) {
                        val bitString =
                            ValueConverter(adcBitDepth).convertToBitString(it.getValues())
                        fileWorker.appendToFile(INTERNAL_ETHER_DATA_FILE_NAME, bitString)
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
            interferenceSource.startWith(BaseNoise()),
            Function3<Signal, Signal, Signal, Signal> { signal, noise, interf ->
                signal + noise + interf
            })
            .mergeWith(fileSignalSource)
    }

    override fun getSimulatedChannelsConfiguration(): ChannelsConfig {
        return simulatedChannelsConfig
    }

    override fun observeSimulationChannelsConfig(): Observable<ChannelsConfig> {
        return simulatedChannelsConfigSource
    }

    override fun updateSimulationChannelsConfig(config: ChannelsConfig) {
        simulatedChannelsConfig.update(config)
        simulatedChannelsConfigSource.onNext(simulatedChannelsConfig)
    }

    override fun getCurrentDemodulatorConfig(): DemodulatorConfig {
        return demodulatorConfig
    }

    override fun observeDemodulatorConfig(): Observable<DemodulatorConfig> {
        return demodulatorConfigSource
    }

    override fun updateDemodulatorConfig(
        delayCompensation: Float,
        frameLength: Int,
        bitTime: Double,
        codeLength: Int
    ) {
        demodulatorConfig.delayCompensation = delayCompensation
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

    override fun updateDecoderConfig(config: DecoderConfig) {
        decoderConfig.update(config)
        decoderConfigSource.onNext(decoderConfig)
    }

    override fun getDecoderConfiguration(): DecoderConfig {
        return decoderConfig
    }

    override fun observeDecoderConfig(): Observable<DecoderConfig> {
        return decoderConfigSource
    }

    override fun startCountingStatistics() {
        clearStatistics()
        isStatisticsCounting = true
        setTransmittingState(ProcessState.STARTED, 0)
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
        isStatisticsCounting = false
        removeTransmittingSubProcesses()
        setTransmittingState(ProcessState.FINISHED, 100)
    }

    override fun setExpectedFrameCount(count: Int) {
        expectedFramesCount = count
    }

    override fun setChannels(channels: List<Channel>) {
        simulatedChannelList = channels.toMutableList()
        simulatedChannelsSource.onNext(simulatedChannelList)
    }

    override fun removeChannel(channel: Channel) {
        if (simulatedChannelList.remove(channel)) simulatedChannelsSource.onNext(
            simulatedChannelList
        )
    }

    override fun getSimulatedChannels(): List<Channel> {
        return simulatedChannelList
    }

    override fun observeSimulatedChannels(): Observable<List<Channel>> {
        return simulatedChannelsSource
    }

    override fun setFileSignal(signal: Signal) {
        fileSignalSource.onNext(signal)
    }

    override fun observeFileSignal(): Observable<Signal> {
        return fileSignalSource
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

    override fun setInterference(signal: Noise) {
        interference = signal
        if (isInterferenceEnabled) interferenceSource.onNext(signal)
    }

    /**
     * Включить помехи.
     * @param fromCache Если <code>true</code>, то сохраненный в кэше экземпляр помех добавляется в эфир.
     */
    override fun enableInterference(fromCache: Boolean) {
        isInterferenceEnabled = true
        if (fromCache) interference?.let { interferenceSource.onNext(it) }
    }

    override fun disableInterference() {
        isInterferenceEnabled = false
        interferenceSource.onNext(BaseNoise())
    }

    override fun isInterferenceEnabled(): Boolean {
        return isInterferenceEnabled
    }

    override fun observeInterference(): Observable<Noise> {
        return interferenceSource
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

    override fun addDecoderChannel(channel: Channel) {
        decoderChannelList.add(channel)
        decoderChannelsSource.onNext(decoderChannelList)
        decoderChannelsLiveSource.onNext(decoderChannelList)
    }

    override fun removeDecoderChannel(channel: Channel) {
        if (decoderChannelList.remove(channel)) {
            decoderChannelsSource.onNext(decoderChannelList)
            decoderChannelsLiveSource.onNext(decoderChannelList)
        }
    }

    override fun setDecoderChannels(channels: List<Channel>) {
        decoderChannelList = channels.toMutableList()
        decoderChannelsSource.onNext(decoderChannelList)
        decoderChannelsLiveSource.onNext(decoderChannelList)
    }

    override fun getDecoderChannels(): List<Channel> {
        return decoderChannelList
    }

    /**
     * @param withLast true - источник при подписке выдает последний список декодированных каналов
     */
    override fun observeDecoderChannels(withLast: Boolean): Observable<List<Channel>> {
        return if (withLast) decoderChannelsSource else decoderChannelsLiveSource
    }

    override fun setSimulatedChannelsErrors(errors: Map<BooleanArray, List<Int>>) {
        channelsErrorsSource.onNext(errors)
    }

    override fun observeSimulatedChannelsErrors(): Observable<Map<BooleanArray, List<Int>>> {
        return channelsErrorsSource
    }

    override fun setTransmittingState(state: Int, progress: Int) {
        transmittingState.apply {
            this.state = state
            this.progress = progress
        }
        return transmittingStateSource.onNext(transmittingState)
    }

    override fun setTransmittingSubProcess(state: ProcessState) {
        transmittingState.setSubState(state)
        transmittingStateSource.onNext(transmittingState)
    }

    override fun removeTransmittingSubProcesses() {
        transmittingState.removeSubStates()
        transmittingStateSource.onNext(transmittingState)
    }

    override fun resetTransmittingSubProcesses() {
        transmittingState.resetSubStates()
        transmittingStateSource.onNext(transmittingState)
    }

    override fun observeTransmittingState(): Observable<ProcessState> {
        return transmittingStateSource
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

    override fun setBerByNoise(berByNoise: Pair<Double, Double>) {
        berByNoiseList.add(berByNoise)
        berByNoiseSource.onNext(berByNoise)
    }

    override fun getBerByNoiseList(): List<Pair<Double, Double>> {
        return berByNoiseList
    }

    override fun clearBerByNoiseList() {
        berByNoiseList.clear()
    }

    override fun observeBerByNoise(): Observable<Pair<Double, Double>> {
        return berByNoiseSource
    }

    override fun setTheoreticBerByNoise(berByNoise: Pair<Double, Double>) {
        theoreticBerByNoiseList.add(berByNoise)
        theoreticBerByNoiseSource.onNext(berByNoise)
    }

    override fun getTheoreticBerByNoiseList(): List<Pair<Double, Double>> {
        return theoreticBerByNoiseList
    }

    override fun clearTheoreticBerByNoiseList() {
        theoreticBerByNoiseList.clear()
    }

    override fun observeTheoreticBerByNoise(): Observable<Pair<Double, Double>> {
        return theoreticBerByNoiseSource
    }

    override fun setCapacityByNoise(capacityByNoise: Pair<Double, Double>) {
        capacityByNoiseList.add(capacityByNoise)
        capacityByNoiseSource.onNext(capacityByNoise)
    }

    override fun getCapacityByNoiseList(): List<Pair<Double, Double>> {
        return capacityByNoiseList
    }

    override fun clearCapacityByNoiseList() {
        capacityByNoiseList.clear()
    }

    override fun observeCapacityByNoise(): Observable<Pair<Double, Double>> {
        return capacityByNoiseSource
    }

    override fun setDataSpeedByNoise(capacityByNoise: Pair<Double, Double>) {
        dataSpeedByNoiseList.add(capacityByNoise)
        dataSpeedByNoiseSource.onNext(capacityByNoise)
    }

    override fun getDataSpeedByNoiseList(): List<Pair<Double, Double>> {
        return dataSpeedByNoiseList
    }

    override fun clearDataSpeedByNoiseList() {
        dataSpeedByNoiseList.clear()
    }

    override fun observeDataSpeedByNoise(): Observable<Pair<Double, Double>> {
        return dataSpeedByNoiseSource
    }
}