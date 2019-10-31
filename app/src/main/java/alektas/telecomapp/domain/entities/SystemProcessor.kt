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
import alektas.telecomapp.domain.entities.signals.noises.WhiteNoise
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.observers.DisposableObserver
import javax.inject.Inject

class SystemProcessor {
    @Inject
    lateinit var storage: Repository
    private var codedGroupData: BooleanArray? = null

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

}