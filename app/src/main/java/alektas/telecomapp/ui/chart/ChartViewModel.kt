package alektas.telecomapp.ui.chart

import alektas.telecomapp.App
import alektas.telecomapp.BuildConfig
import alektas.telecomapp.data.CodeGenerator
import alektas.telecomapp.data.UserDataProvider
import alektas.telecomapp.domain.Repository
import alektas.telecomapp.domain.entities.*
import alektas.telecomapp.domain.entities.coders.CdmaCoder
import alektas.telecomapp.domain.entities.demodulators.DemodulatorConfig
import alektas.telecomapp.domain.entities.demodulators.QpskDemodulator
import alektas.telecomapp.domain.entities.filters.FirFilter
import alektas.telecomapp.domain.entities.generators.SignalGenerator
import alektas.telecomapp.domain.entities.modulators.QpskModulator
import alektas.telecomapp.domain.entities.signals.*
import alektas.telecomapp.domain.entities.signals.noises.WhiteNoise
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.jjoe64.graphview.series.DataPoint
import org.apache.commons.math3.transform.DftNormalization
import org.apache.commons.math3.transform.FastFourierTransformer
import org.apache.commons.math3.transform.TransformType
import java.util.*
import javax.inject.Inject

class ChartViewModel : ViewModel() {
    @Inject
    lateinit var system: Repository
    val inputSignalData = MutableLiveData<Array<DataPoint>>()
    val outputSignalData = MutableLiveData<Array<DataPoint>>()
    val spectrumData = MutableLiveData<Array<DataPoint>>()
    val dataSignalData = MutableLiveData<Array<DataPoint>>()
    val constellationData = MutableLiveData<List<Pair<Float, Float>>>()

    init {
        App.component.inject(this)
//
//        val gen = SignalGenerator()
//        val carrier = gen.cos(frequency = QpskContract.CARRIER_FREQUENCY)
//
//        val codeGenerator = CodeGenerator(CodeGenerator.WALSH, CdmaContract.MAX_CHANNEL_COUNT)
//
//        val code1 = CodeGenerator().generateCode(CodeGenerator.WALSH, CdmaContract.CODE_LENGTH)
//        val userData1 = UserDataProvider.generateData(CdmaContract.DATA_FRAME_LENGTH)
//
//        val code2 = CodeGenerator().generateCode(CodeGenerator.WALSH, CdmaContract.CODE_LENGTH)
//        val userData2 = UserDataProvider.generateData(CdmaContract.DATA_FRAME_LENGTH)
//
//        val code3 = CodeGenerator().generateCode(CodeGenerator.WALSH, CdmaContract.CODE_LENGTH)
//        val userData3 = UserDataProvider.generateData(CdmaContract.DATA_FRAME_LENGTH)
//
//        val code4 = CodeGenerator().generateCode(CodeGenerator.WALSH, CdmaContract.CODE_LENGTH)
//        val userData4 = UserDataProvider.generateData(CdmaContract.DATA_FRAME_LENGTH)
//
//        val coder = CdmaCoder()
//        val data1 = coder.encode(code1, userData1)
//        val data2 = coder.encode(code2, userData2)
//
//        val modulatedGroupSignal = QpskModulator().modulate(carrier, data1)
//
//        val channel = RadioChannel.Builder()
//            .addSignal(modulatedGroupSignal)
//            .addWhiteNoise(0.0)
//            .build()
//
//        val demodConfig = DemodulatorConfig().apply { inputSignal = channel.ether }
//        storage.setDemodulatorConfig(demodConfig)
//
//        val receivedGroupSignal = QpskDemodulator(DemodulatorConfig()).demodulate(channel.ether) as BinarySignal
//        val receivedGroupConstellation = QpskDemodulator(DemodulatorConfig()).getConstellation(channel.ether)
//
//        if (BuildConfig.DEBUG) {
//            println("Transmit: encoded data1: ${data1.joinToString { if (it) "1" else "0" }}")
//            println("Transmit: encoded data2: ${data2.joinToString { if (it) "1" else "0" }}")
//            println("Received:   meshed data: ${receivedGroupSignal.bits.joinToString { if (it) "1" else "0" }}")
//            println("Transmit: User1 data: ${userData1.joinToString { if (it) "1" else "0" }}")
//            println("Transmit: User2 data: ${userData2.joinToString { if (it) "1" else "0" }}")
//        }
//
//        val sin = gen.sin(frequency = QpskContract.CARRIER_FREQUENCY)
//        val filteredSignalData = TreeMap<Double, Double>()
//
//        val constSignal = WhiteNoise() + sin
//        val filteredData = FirFilter(
//            256,
//            1.0e6,
//            1.0e1,
//            Simulator.SAMPLING_RATE
//        ).filter((constSignal as BaseSignal).data.values.toDoubleArray())
//        sin.data.keys.forEachIndexed { i, time -> filteredSignalData[time] = filteredData[i] }
//
//        val filter = FirFilter(256, 1.0e3, 1.0e1, Simulator.SAMPLING_RATE)
//        val impulseResp =
//            DigitalSignal(filter.impulseResponse(), Simulator.SAMPLE_TIME)
//
//        show(BaseSignal(), constSignal, impulseResp, receivedGroupConstellation)
    }

    private fun show(
        data: Signal,
        input: Signal,
        output: Signal,
        outConstellation: List<Pair<Double, Double>>
    ) {
        val dataValues = data.getPoints()
            .map { DataPoint(it.key, it.value) }
            .toTypedArray()
        dataSignalData.value = dataValues

        val inValues = input.getPoints()
            .map { DataPoint(it.key, it.value) }
            .toTypedArray()
        inputSignalData.value = inValues

        val outValues = output.getPoints()
            .map { DataPoint(it.key, it.value) }
            .toTypedArray()
        outputSignalData.value = outValues

        constellationData.value =
            outConstellation.map { Pair(it.first.toFloat(), it.second.toFloat()) }

        var spectrum = FastFourierTransformer(DftNormalization.STANDARD)
            .transform(
                Window(Window.GAUSSE).applyTo(output).getValues(),
                TransformType.FORWARD
            )
        val actualSize = spectrum.size / 2
        spectrum = spectrum.take(actualSize).toTypedArray()
        val maxSpectrumValue = spectrum.maxBy { it.abs() }?.abs() ?: 1.0
        spectrumData.value = spectrum
            .mapIndexed { i, complex -> DataPoint(i.toDouble(), complex.abs() / maxSpectrumValue) }
            .toTypedArray()
    }
}
