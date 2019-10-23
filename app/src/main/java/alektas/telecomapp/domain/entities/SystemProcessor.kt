package alektas.telecomapp.domain.entities

import alektas.telecomapp.App
import alektas.telecomapp.data.CodeGenerator
import alektas.telecomapp.data.UserDataProvider
import alektas.telecomapp.domain.Repository
import alektas.telecomapp.domain.entities.coders.CdmaCoder
import alektas.telecomapp.domain.entities.generators.SignalGenerator
import alektas.telecomapp.domain.entities.modulators.QpskModulator
import alektas.telecomapp.domain.entities.signals.BaseSignal
import alektas.telecomapp.domain.entities.signals.noises.WhiteNoise
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.observers.DisposableObserver
import javax.inject.Inject

class SystemProcessor {
    @Inject
    lateinit var storage: Repository

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
    }

    fun generateChannels(
        count: Int = CdmaContract.MAX_CHANNEL_COUNT,
        frameLength: Int = CdmaContract.DATA_FRAME_LENGTH,
        codesType: Int = CodeGenerator.WALSH
    ) {
        val channels = mutableListOf<ChannelData>()
        val codeGen = CodeGenerator()
        val codes = when (codesType) {
            CodeGenerator.WALSH -> codeGen.generateWalshMatrix(count)
            else -> codeGen.generateRandomCodes(count, CdmaContract.CODE_LENGTH)
        }

        for (i in 0 until count) {
            val frameData = UserDataProvider.generateData(frameLength)
            val channel = ChannelData(i, "${i + 1}", frameData, codes[i])
            channels.add(channel)
        }

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

}