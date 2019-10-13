package alektas.telecomapp.ui.chart

import alektas.telecomapp.data.CodesProvider
import alektas.telecomapp.data.UserDataProvider
import alektas.telecomapp.domain.entities.CdmaContract
import alektas.telecomapp.domain.entities.coders.CdmaCoder
import alektas.telecomapp.domain.entities.generators.SignalGenerator
import alektas.telecomapp.domain.entities.modulators.QpskModulator
import alektas.telecomapp.domain.entities.signals.Signal
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.jjoe64.graphview.series.DataPoint
import org.apache.commons.math3.transform.DftNormalization
import org.apache.commons.math3.transform.FastFourierTransformer
import org.apache.commons.math3.transform.TransformType

class ChartViewModel : ViewModel() {
    val signalData = MutableLiveData<Array<DataPoint>>()
    val spectrumData = MutableLiveData<Array<DataPoint>>()

    init {
        val gen = SignalGenerator()

        val sin1 = gen.sin(frequency = 1.0e5, magnitude = 1.0)

        val cos1 = gen.cos(frequency = 1.0e4, magnitude = 0.8)

        val sin2 = gen.sin(frequency = 1.0e3, magnitude = 0.1)

        val digitSignal = gen.digit(arrayOf(true, false, false, true, true, false, true), 0.001)

        val data = CdmaCoder().encode(
            CodesProvider.generateCode(CdmaContract.CODE_LENGTH),
            UserDataProvider.generateData(CdmaContract.DATA_FRAME_LENGTH)
        )

        val signal = QpskModulator().modulate(sin1, data)
        show(signal)
    }

    private fun show(signal: Signal) {
        val values = signal.getPoints()
            .map { DataPoint(it.key, it.value) }
            .toTypedArray()
        signalData.value = values

        val spectrum = FastFourierTransformer(DftNormalization.STANDARD)
            .transform(signal.getValues(), TransformType.FORWARD)
        spectrumData.value = spectrum
            .mapIndexed { i, complex -> DataPoint(i.toDouble(), complex.abs()) }
            .toTypedArray()
    }
}
