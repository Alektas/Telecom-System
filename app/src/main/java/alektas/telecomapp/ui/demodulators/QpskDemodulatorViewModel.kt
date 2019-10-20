package alektas.telecomapp.ui.demodulators

import alektas.telecomapp.App
import alektas.telecomapp.domain.Repository
import alektas.telecomapp.domain.entities.demodulators.QpskDemodulator
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.jjoe64.graphview.series.DataPoint
import javax.inject.Inject

class QpskDemodulatorViewModel : ViewModel() {
    @Inject lateinit var system: Repository
    val inputSignalData = MutableLiveData<Array<DataPoint>>()
    val outputSignalData = MutableLiveData<Array<DataPoint>>()
    val constellationData = MutableLiveData<List<Pair<Float, Float>>>()

    init {
        App.component.inject(this)
        val config = system.getDemodulatorConfig()
        config.inputSignal?.let { signal ->
            inputSignalData.value = signal.getPoints()
                .map { DataPoint(it.key, it.value) }.toTypedArray()
            outputSignalData.value = QpskDemodulator().demodulate(signal).getPoints()
                .map { DataPoint(it.key, it.value) }.toTypedArray()
            constellationData.value = QpskDemodulator().getConstellation(signal)
                    .map { Pair(it.first.toFloat(), it.second.toFloat()) }
        }
    }
}
