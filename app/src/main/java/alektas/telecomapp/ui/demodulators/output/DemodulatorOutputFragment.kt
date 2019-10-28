package alektas.telecomapp.ui.demodulators.output

import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import alektas.telecomapp.R
import androidx.lifecycle.Observer
import com.jjoe64.graphview.series.LineGraphSeries
import kotlinx.android.synthetic.main.demodulator_output_fragment.*

class DemodulatorOutputFragment : Fragment() {

    companion object {
        fun newInstance() = DemodulatorOutputFragment()
    }

    private lateinit var viewModel: DemodulatorOutputViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.demodulator_output_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(DemodulatorOutputViewModel::class.java)

        viewModel.outputSignalData.observe(viewLifecycleOwner, Observer {
            output_signal_chart.addSeries(LineGraphSeries(it))
        })

        viewModel.specturmData.observe(viewLifecycleOwner, Observer {
            spectrum_chart.addSeries(LineGraphSeries(it))
        })

        viewModel.constellationData.observe(viewLifecycleOwner, Observer {
            constellation_chart.setData(it)
        })
    }

}
