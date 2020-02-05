package alektas.telecomapp.ui.demodulator.output

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

class DemodulatorOutputFragment : Fragment() {

    companion object {
        fun newInstance() = DemodulatorOutputFragment()
    }

    private lateinit var viewModel: DemodulatorOutputViewModel
    private val signalGraph = LineGraphSeries<DataPoint>()
    private val spectrumGraph = LineGraphSeries<DataPoint>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.demodulator_output_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<GraphView>(R.id.output_signal_chart).apply { addSeries(signalGraph) }
        view.findViewById<GraphView>(R.id.spectrum_chart).apply { addSeries(spectrumGraph) }
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(DemodulatorOutputViewModel::class.java)

        viewModel.outputSignalData.observe(viewLifecycleOwner, Observer {
            signalGraph.resetData(it)
        })

        viewModel.spectrumData.observe(viewLifecycleOwner, Observer {
            spectrumGraph.resetData(it)
        })
    }

}
