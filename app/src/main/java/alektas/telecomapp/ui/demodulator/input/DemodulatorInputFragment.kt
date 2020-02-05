package alektas.telecomapp.ui.demodulator.input

import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import alektas.telecomapp.R
import androidx.lifecycle.Observer
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries

class DemodulatorInputFragment : Fragment() {

    companion object {
        fun newInstance() = DemodulatorInputFragment()
    }

    private lateinit var viewModel: DemodulatorInputViewModel
    private val signalGraph = LineGraphSeries<DataPoint>()
    private val spectrumGraph = LineGraphSeries<DataPoint>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.demodulator_input_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<GraphView>(R.id.input_signal_chart).apply { addSeries(signalGraph) }
        view.findViewById<GraphView>(R.id.spectrum_chart).apply { addSeries(spectrumGraph) }
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(DemodulatorInputViewModel::class.java)

        viewModel.inputSignalData.observe(viewLifecycleOwner, Observer {
            signalGraph.resetData(it)
        })

        viewModel.spectrumData.observe(viewLifecycleOwner, Observer {
            spectrumGraph.resetData(it)
        })
    }

}
