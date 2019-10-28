package alektas.telecomapp.ui.demodulators.generator

import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import alektas.telecomapp.R
import android.util.Log
import android.widget.Toast
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.Observer
import com.jjoe64.graphview.series.LineGraphSeries
import kotlinx.android.synthetic.main.demodulator_generator_fragment.*
import java.lang.NumberFormatException

private const val TAG = "GeneratorFragment"

class DemodulatorGeneratorFragment : Fragment() {

    companion object {
        fun newInstance() = DemodulatorGeneratorFragment()
    }

    private lateinit var viewModel: DemodulatorGeneratorViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.demodulator_generator_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(DemodulatorGeneratorViewModel::class.java)

        generator_frequency.doOnTextChanged { text, _, _, _ ->
            try {
                val freq = text.toString().toDouble()
                if (freq <= 0) throw NumberFormatException()
                viewModel.setGeneratorFrequency(freq)
            } catch (e: NumberFormatException) {
                val msg = "Частота генератора должна быть положительным целым числом"
                Log.e(TAG, msg, e)
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.generatorSignalData.observe(viewLifecycleOwner, Observer {
            generator_signal_chart.removeAllSeries()
            generator_signal_chart.addSeries(LineGraphSeries(it))
        })
    }

}
