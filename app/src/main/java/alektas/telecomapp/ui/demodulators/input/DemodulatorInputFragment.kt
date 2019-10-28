package alektas.telecomapp.ui.demodulators.input

import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import alektas.telecomapp.R
import androidx.lifecycle.Observer
import com.jjoe64.graphview.series.LineGraphSeries
import kotlinx.android.synthetic.main.demodulator_input_fragment.*

class DemodulatorInputFragment : Fragment() {

    companion object {
        fun newInstance() = DemodulatorInputFragment()
    }

    private lateinit var viewModel: DemodulatorInputViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.demodulator_input_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(DemodulatorInputViewModel::class.java)

        viewModel.inputSignalData.observe(viewLifecycleOwner, Observer {
            input_signal_chart.addSeries(LineGraphSeries(it))
        })

        viewModel.specturmData.observe(viewLifecycleOwner, Observer {
            spectrum_chart.addSeries(LineGraphSeries(it))
        })
    }

}
