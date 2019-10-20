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
import kotlinx.android.synthetic.main.qpsk_demodulator_fragment.*

class QpskDemodulatorFragment : Fragment() {

    companion object {
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

        viewModel.inputSignalData.observe(viewLifecycleOwner, Observer {
            input_signal_chart.addSeries(LineGraphSeries<DataPoint>(it))
        })

        viewModel.iSignalData.observe(viewLifecycleOwner, Observer {
            i_signal_chart.addSeries(LineGraphSeries<DataPoint>(it))
        })

        viewModel.filteredISignalData.observe(viewLifecycleOwner, Observer {
            filtered_i_signal_chart.addSeries(LineGraphSeries<DataPoint>(it))
        })

        viewModel.qSignalData.observe(viewLifecycleOwner, Observer {
            q_signal_chart.addSeries(LineGraphSeries<DataPoint>(it))
        })

        viewModel.filteredQSignalData.observe(viewLifecycleOwner, Observer {
            filtered_q_signal_chart.addSeries(LineGraphSeries<DataPoint>(it))
        })

        viewModel.outputSignalData.observe(viewLifecycleOwner, Observer {
            output_signal_chart.addSeries(LineGraphSeries<DataPoint>(it))
        })

        viewModel.constellationData.observe(viewLifecycleOwner, Observer {
            constellation_chart.setData(it)
        })
    }

}
