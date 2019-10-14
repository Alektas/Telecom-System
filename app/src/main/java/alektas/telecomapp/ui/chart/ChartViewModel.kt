package alektas.telecomapp.ui.chart

import alektas.telecomapp.data.CodesProvider
import alektas.telecomapp.data.UserDataProvider
import alektas.telecomapp.domain.entities.CdmaContract
import alektas.telecomapp.domain.entities.QpskContract
import alektas.telecomapp.domain.entities.Window
import alektas.telecomapp.domain.entities.coders.CdmaCoder
import alektas.telecomapp.domain.entities.demodulators.QpskDemodulator
import alektas.telecomapp.domain.entities.generators.SignalGenerator
import alektas.telecomapp.domain.entities.modulators.QpskModulator
import alektas.telecomapp.domain.entities.signals.DigitalSignal
import alektas.telecomapp.domain.entities.signals.Signal
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.jjoe64.graphview.series.DataPoint
import org.apache.commons.math3.transform.DftNormalization
import org.apache.commons.math3.transform.FastFourierTransformer
import org.apache.commons.math3.transform.TransformType

class ChartViewModel : ViewModel() {
    val inputSignalData = MutableLiveData<Array<DataPoint>>()
    val outputSignalData = MutableLiveData<Array<DataPoint>>()
    val spectrumData = MutableLiveData<Array<DataPoint>>()
    val dataSignalData = MutableLiveData<Array<DataPoint>>()
    val constellationData = MutableLiveData<List<Pair<Float, Float>>>()

    init {
        val gen = SignalGenerator()
        val carrier = gen.cos(frequency = QpskContract.CARRIER_FREQUENCY)

        val data = CdmaCoder().encode(
            CodesProvider.generateCode(CdmaContract.CODE_LENGTH),
            UserDataProvider.generateData(CdmaContract.DATA_FRAME_LENGTH)
        )

        val dataSignal = DigitalSignal(data, CdmaContract.CODE_BIT_TIME)
        val inSignal = QpskModulator().modulate(carrier, data)
        val outSignal = QpskDemodulator().demodulate(inSignal)
        val outConstellation = QpskDemodulator().getConstellation(inSignal)

        show(dataSignal, inSignal, outSignal, outConstellation)
    }

    private fun show(data: Signal, input: Signal, output: Signal, outConstellation: List<Pair<Double, Double>>) {
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

        val spectrum = FastFourierTransformer(DftNormalization.STANDARD)
            .transform(Window(Window.GAUSSE).applyTo(output).getValues(), TransformType.FORWARD)
        spectrumData.value = spectrum
            .mapIndexed { i, complex -> DataPoint(i.toDouble(), complex.abs()) }
            .toTypedArray()

        constellationData.value = outConstellation.map { Pair(it.first.toFloat(), it.second.toFloat()) }
    }
}
