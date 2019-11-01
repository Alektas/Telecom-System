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
import alektas.telecomapp.domain.entities.signals.noises.Noise
import alektas.telecomapp.domain.entities.signals.noises.WhiteNoise
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.BiFunction
import io.reactivex.observers.DisposableObserver
import javax.inject.Inject
import kotlin.math.max

class SystemProcessor {
    @Inject
    lateinit var storage: Repository
    private var codedGroupData: BooleanArray? = null
    private var noiseSnr: Double? = null

    init {
        App.component.inject(this)

        storage.observeChannels()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : DisposableObserver<List<ChannelData>>() {
                override fun onNext(s: List<ChannelData>) {
                    generateChannelsSignal(s)
                }

                override fun onComplete() {}

                override fun onError(e: Throwable) {}
            })

        storage.observeDemodulatorConfig()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : DisposableObserver<DemodulatorConfig>() {
                override fun onNext(c: DemodulatorConfig) {
                    demodulate(c)
                }

                override fun onComplete() {}

                override fun onError(e: Throwable) {}
            })

        storage.observeDemodulatedSignal()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : DisposableObserver<BinarySignal>() {
                override fun onNext(t: BinarySignal) {
                    codedGroupData = t.bits
                }

                override fun onComplete() {}

                override fun onError(e: Throwable) {}
            })

        storage.observeNoise()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : DisposableObserver<Noise>() {
                override fun onNext(t: Noise) {
                    noiseSnr = t.snr()
                }

                override fun onComplete() {}

                override fun onError(e: Throwable) {}
            })

        Observable.combineLatest(
            storage.observeChannels(),
            storage.observeDecodedChannels(),
            BiFunction<List<ChannelData>, List<ChannelData>, List<List<Int>>> { origin, decoded ->
                diffChannels(origin, decoded)
            })
            .apply {
                subscribe { errors ->
                    storage.setChannelsErrors(errors)
                }
            }
    }

    fun generateChannels(
        count: Int,
        frameLength: Int,
        codesType: Int = CodeGenerator.WALSH
    ) {
        val channels = mutableListOf<ChannelData>()
        val codeGen = CodeGenerator()
        val codes = when (codesType) {
            CodeGenerator.WALSH -> codeGen.generateWalshMatrix(count)
            else -> codeGen.generateRandomCodes(count, count)
        }

        for (i in 0 until count) {
            val frameData = UserDataProvider.generateData(frameLength)
            val channel = ChannelData("${i + 1}", frameData, codes[i], codesType)
            channels.add(channel)
        }

        val commTime = frameLength * codes[0].size * CdmaContract.CODE_BIT_TIME
        Simulator.setSimulationTime(commTime)
        noiseSnr?.let { setNoise(it) }

        CdmaContract.DATA_FRAME_LENGTH = frameLength
        CdmaContract.CODE_LENGTH = codes[0].size
        CdmaContract.SPREAD_RATIO = codes[0].size
        CdmaContract.SPREAD_DATA_LENGTH = frameLength * codes[0].size

        storage.setChannels(channels)
    }

    fun generateChannelsSignal(channels: List<ChannelData>) {
        if (channels.isEmpty()) {
            storage.setChannelsSignal(BaseSignal())
            return
        }

        val groupData = channels.map { CdmaCoder().encode(it.code, it.data) }
            .reduce { acc, data ->
                data.mapIndexed { i, bit -> acc[i].xor(bit) }.toBooleanArray()
            }

        val carrier = SignalGenerator().cos(frequency = QpskContract.CARRIER_FREQUENCY)
        val signal = QpskModulator().modulate(carrier, groupData)

        storage.setChannelsSignal(signal)
    }

    fun removeChannel(channel: ChannelData) {
        storage.removeChannel(channel)
    }

    fun setNoise(snr: Double) {
        storage.setNoise(WhiteNoise(snr, QpskContract.SIGNAL_MAGNITUDE))
    }

    fun demodulate(config: DemodulatorConfig) {
        val demodulator = QpskDemodulator(config)
        val demodSignal = demodulator.demodulate(config.inputSignal)
        storage.setDemodulatedSignal(demodSignal)
        storage.setChannelI(demodulator.sigI)
        storage.setFilteredChannelI(demodulator.filteredSigI)
        storage.setChannelQ(demodulator.sigQ)
        storage.setFilteredChannelQ(demodulator.filteredSigQ)
        storage.setDemodulatedSignalConstellation(demodulator.constellation)
    }

    fun decodeChannel(code: BooleanArray) {
        codedGroupData?.let {
            val data = CdmaCoder().decode(code, it)
            val channel = ChannelData(data = data, code = code)
            storage.addDecodedChannel(channel)
        }
    }

    fun decodeChannels(count: Int, codesType: Int) {
        codedGroupData?.let {
            val channels = mutableListOf<ChannelData>()
            val codeGen = CodeGenerator()
            val codes = when (codesType) {
                CodeGenerator.WALSH -> codeGen.generateWalshMatrix(count)
                else -> codeGen.generateRandomCodes(count, CdmaContract.CODE_LENGTH)
            }

            for (i in 0 until count) {
                val frameData = CdmaCoder().decode(codes[i], it)
                val channel = ChannelData(data = frameData, code = codes[i])
                channels.add(channel)
            }

            storage.setDecodedChannels(channels)
        }
    }

    /**
     * Поиск несовпадающих битов двух массивах каналов.
     * Если размер одного из массивов больше другого, то все данные "лишних" каналов
     * считются несовпадающими.
     *
     * @return список списков индексов несовпадающих битов каналов
     */
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

}