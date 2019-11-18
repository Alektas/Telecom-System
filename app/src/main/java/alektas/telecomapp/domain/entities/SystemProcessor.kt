package alektas.telecomapp.domain.entities

import alektas.telecomapp.App
import alektas.telecomapp.data.CodeGenerator
import alektas.telecomapp.data.UserDataProvider
import alektas.telecomapp.domain.Repository
import alektas.telecomapp.domain.entities.coders.CdmaCoder
import alektas.telecomapp.domain.entities.demodulators.DemodulatorConfig
import alektas.telecomapp.domain.entities.demodulators.QpskDemodulator
import alektas.telecomapp.domain.entities.generators.SignalGenerator
import alektas.telecomapp.domain.entities.modulators.QpskModulator
import alektas.telecomapp.domain.entities.signals.BaseSignal
import alektas.telecomapp.domain.entities.signals.BinarySignal
import alektas.telecomapp.domain.entities.signals.Signal
import alektas.telecomapp.domain.entities.signals.noises.Noise
import alektas.telecomapp.domain.entities.signals.noises.WhiteNoise
import android.annotation.SuppressLint
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.functions.BiFunction
import io.reactivex.rxkotlin.toObservable
import io.reactivex.rxkotlin.zipWith
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject
import kotlin.math.max

private const val BER_POINTS_COUNT = 20

class SystemProcessor {
    @Inject
    lateinit var storage: Repository
    private var codedGroupData: BooleanArray? = null
    private var transmittedChannels: List<ChannelData>? = null
    private var decodedChannels: List<ChannelData>? = null
    private var noiseSnr: Double? = null
    private lateinit var berDisposable: Disposable

    init {
        App.component.inject(this)

        storage.observeChannels()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                transmittedChannels = it
                generateChannelsSignal(it)
            }

        storage.observeDemodulatorConfig()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { demodulate(it) }

        storage.observeDemodulatedSignal()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { s ->
                codedGroupData = s.bits
                decodedChannels?.let { updateDecodedChannels(it, s.bits) }
            }

        storage.observeDecodedChannels()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                decodedChannels = it
            }

        storage.observeNoise()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { noiseSnr = it.snr() }

        Observable.combineLatest(
            storage.observeChannels(),
            storage.observeDecodedChannels(),
            BiFunction<List<ChannelData>, List<ChannelData>, List<List<Int>>> { origin, decoded ->
                diffChannels(origin, decoded)
            })
            .subscribeOn(Schedulers.computation())
            .subscribe { storage.setChannelsErrors(it) }
    }

    fun setAdcFrequency(frequency: Double) {
        val freq = frequency * 1.0e6 // МГц -> Гц
        Simulator.samplingRate = freq
        noiseSnr?.let { n -> setNoise(n) }
        val filterConfig = storage.getDemodulatorFilterConfig().apply { samplingRate = freq }
        storage.setDemodulatorFilterConfig(filterConfig)
        transmittedChannels?.let { generateChannelsSignal(it) }
    }

    @SuppressLint("CheckResult")
    fun generateChannels(
        count: Int,
        carrierFrequency: Double, // МГц
        dataSpeed: Double, // кБит/с
        frameLength: Int,
        codesType: Int = CodeGenerator.WALSH
    ) {
        Single.create<List<ChannelData>> {
            val channels = mutableListOf<ChannelData>()
            val codeGen = CodeGenerator()
            val codes = when (codesType) {
                CodeGenerator.WALSH -> codeGen.generateWalshMatrix(count)
                else -> codeGen.generateRandomCodes(count, count)
            }

            val bitTime = 1.0e-3 / dataSpeed // скорость в период бита (в секундах)

            for (i in 0 until count) {
                val frameData = UserDataProvider.generateData(frameLength)
                val channel = ChannelData(
                    "${i + 1}",
                    carrierFrequency * 1.0e6, // МГц -> Гц
                    frameData,
                    bitTime,
                    codes[i],
                    codesType
                )
                channels.add(channel)
            }

            val dataTime = frameLength * codes[0].size * bitTime
            Simulator.simulationTime = dataTime
            noiseSnr?.let { n -> setNoise(n) }

            it.onSuccess(channels)
        }
            .subscribeOn(Schedulers.computation())
            .subscribe { channels: List<ChannelData> -> storage.setChannels(channels) }
    }

    @SuppressLint("CheckResult")
    fun generateChannelsSignal(channels: List<ChannelData>) {
        if (channels.isEmpty()) {
            storage.setChannelsSignal(BaseSignal())
            return
        }

        Single.create<Signal> {
            val groupData = dataAggregation(channels)

            val carrier = SignalGenerator().cos(frequency = channels[0].carrierFrequency)
            val signal = QpskModulator(channels[0].bitTime).modulate(carrier, groupData)

            it.onSuccess(signal)
        }
            .subscribeOn(Schedulers.computation())
            .subscribe { signal: Signal -> storage.setChannelsSignal(signal) }
    }

    private fun dataAggregation(channels: List<ChannelData>): BooleanArray {
        return channels.map { CdmaCoder().encode(it.code, it.data) }
            .reduce { acc, data ->
                data.mapIndexed { i, bit -> acc[i].xor(bit) }.toBooleanArray()
            }
    }

    fun removeChannel(channel: ChannelData) {
        storage.removeChannel(channel)
    }

    @SuppressLint("CheckResult")
    fun setNoise(snr: Double, singleThread: Boolean = false) {
        Single.create<Noise> {
            val noise = WhiteNoise(snr, QpskContract.DEFAULT_SIGNAL_MAGNITUDE)
            it.onSuccess(noise)
        }
            .subscribeOn(if (singleThread) Schedulers.single() else Schedulers.computation())
            .subscribe { noise: Noise -> storage.setNoise(noise) }
    }

    fun disableNoise() {
        storage.disableNoise()
    }


    fun enableNoise() {
        storage.enableNoise()
    }

    @SuppressLint("CheckResult")
    fun demodulate(config: DemodulatorConfig) {
        val demodulator = QpskDemodulator(config)

        Single.create<BinarySignal> {
            val demodSignal = demodulator.demodulate(config.inputSignal)
            it.onSuccess(demodSignal)
        }
            .subscribeOn(Schedulers.computation())
            .subscribe { signal: BinarySignal ->
                storage.setDemodulatedSignal(signal)
                storage.setChannelI(demodulator.sigI)
                storage.setFilteredChannelI(demodulator.filteredSigI)
                storage.setChannelQ(demodulator.sigQ)
                storage.setFilteredChannelQ(demodulator.filteredSigQ)
                storage.setDemodulatedSignalConstellation(demodulator.constellation)
            }
    }

    fun updateDecodedChannels(channels: List<ChannelData>, groupData: BooleanArray) {
        val newChannels = mutableListOf<ChannelData>()

        for (c in channels) {
            val frameData = CdmaCoder().decode(c.code, groupData)
            c.data = frameData
            newChannels.add(c)
        }

        storage.setDecodedChannels(channels)
    }

    fun addDecodedChannel(code: BooleanArray) {
        codedGroupData?.let {
            val data = CdmaCoder().decode(code, it)
            val channel = ChannelData(data = data, code = code)
            storage.addDecodedChannel(channel)
        }
    }

    @SuppressLint("CheckResult")
    fun setDecodedChannels(count: Int, codesType: Int) {
        Single.create<List<ChannelData>> { emitter ->
            codedGroupData?.let {
                val channels = mutableListOf<ChannelData>()
                val codeGen = CodeGenerator()
                val codes = when (codesType) {
                    CodeGenerator.WALSH -> codeGen.generateWalshMatrix(count)
                    else -> codeGen.generateRandomCodes(count, count)
                }

                for (i in 0 until count) {
                    val frameData = CdmaCoder().decode(codes[i], it)
                    val channel = ChannelData(data = frameData, code = codes[i])
                    channels.add(channel)
                }

                emitter.onSuccess(channels)
            }
        }
            .subscribeOn(Schedulers.computation())
            .subscribe { channels: List<ChannelData> -> storage.setDecodedChannels(channels) }
    }

    /**
     * Поиск несовпадающих битов двух массивах каналов.
     * Если размер одного из массивов больше другого, то все данные "лишних" каналов
     * считются несовпадающими.
     *
     * @return список списков индексов несовпадающих битов каналов
     */
    @SuppressLint("CheckResult")
    private fun diffChannels(
        first: List<ChannelData>,
        second: List<ChannelData>
    ): List<List<Int>> {
        val channelsErrorBits = mutableListOf<List<Int>>()

        if (first.isEmpty()) {
            second.forEach { channelsErrorBits.add(it.data.indices.toList()) }
            return channelsErrorBits
        }
        if (second.isEmpty()) {
            first.forEach { channelsErrorBits.add(it.data.indices.toList()) }
            return channelsErrorBits
        }

        val maxSize = max(first.size, second.size)
        for (i in 0 until maxSize) {
            try {
                channelsErrorBits.add(diffIndices(first[i].data, second[i].data))
            } catch (e: IndexOutOfBoundsException) {
                val erBits = if (first.size > second.size) {
                    first[i].data.indices.toList()
                } else {
                    second[i].data.indices.toList()
                }
                channelsErrorBits.add(erBits)
            }
        }

        return channelsErrorBits
    }

    /**
     * Поиск несовпадающих битов в двух массивах.
     * Если размер одного из массивов больше другого, то "лишние" биты считются несовпадающими.
     *
     * @return список индексов несовпадающих битов
     */
    private fun diffIndices(first: BooleanArray, second: BooleanArray): List<Int> {
        if (first.isEmpty()) return second.indices.toList()
        if (second.isEmpty()) return first.indices.toList()

        val indices = mutableListOf<Int>()

        val maxSize = max(first.size, second.size)
        for (i in 0 until maxSize) {
            try {
                if (first[i] != second[i]) indices.add(i)
            } catch (e: IndexOutOfBoundsException) {
                indices.add(i)
            }
        }

        return indices
    }

    fun calculateBer(fromSnr: Double, toSnr: Double) {
        val step = (toSnr - fromSnr) / BER_POINTS_COUNT
        val snrs = DoubleArray(BER_POINTS_COUNT) { fromSnr + it * step }
        berDisposable = snrs.toObservable()
            .skip(1)
            .zipWith(storage.observeBer()) { snr, _ -> snr }
            .doOnSubscribe { setNoise(fromSnr, true) }
            .subscribe { setNoise(it, true) }
    }

}