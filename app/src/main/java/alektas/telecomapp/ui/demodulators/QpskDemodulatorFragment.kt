package alektas.telecomapp.ui.demodulators

import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import alektas.telecomapp.R
import androidx.lifecycle.Observer
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import kotlinx.android.synthetic.main.qpsk_demodulator_fragment.constellation_chart
import kotlinx.android.synthetic.main.qpsk_demodulator_fragment.input_signal_chart
import kotlinx.android.synthetic.main.qpsk_demodulator_fragment.output_signal_chart

class QpskDemodulatorFragment : Fragment() {

    companion object {
        const val TAG = "QpskDemodulatorFragment"
        const val SIGNAL_MAGNITUDE = 1.5
        const val SIGNAL_DURATION = 0.0004
        fun newInstance() = QpskDemodulatorFragment()
    }

    private lateinit var viewModel: QpskDemodulatorViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.qpsk_demodulator_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(QpskDemodulatorViewModel::class.java)

        input_signal_chart.viewport.apply {
            isScalable = true
            isXAxisBoundsManual = false
            isYAxisBoundsManual = false
            setMinX(0.0)
            setMaxX(SIGNAL_DURATION)
            setMinY(-SIGNAL_MAGNITUDE)
            setMaxY(SIGNAL_MAGNITUDE)
        }

        output_signal_chart.viewport.apply {
            isScalable = true
            isXAxisBoundsManual = false
            isYAxisBoundsManual = false
            setMinX(0.0)
            setMaxX(SIGNAL_DURATION)
            setMinY(-SIGNAL_MAGNITUDE)
            setMaxY(SIGNAL_MAGNITUDE)
        }

        viewModel.inputSignalData.observe(viewLifecycleOwner, Observer {
            input_signal_chart.addSeries(LineGraphSeries<DataPoint>(it))
        })

        viewModel.outputSignalData.observe(viewLifecycleOwner, Observer {
            output_signal_chart.addSeries(LineGraphSeries<DataPoint>(it))
        })

        viewModel.constellationData.observe(viewLifecycleOwner, Observer {
            constellation_chart.setData(it)
        })
    }

}
