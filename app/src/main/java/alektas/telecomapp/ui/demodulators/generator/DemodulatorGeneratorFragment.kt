package alektas.telecomapp.ui.demodulators.generator

import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import alektas.telecomapp.R
import alektas.telecomapp.utils.SystemUtils
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.Observer
import com.jjoe64.graphview.series.LineGraphSeries
import kotlinx.android.synthetic.main.data_source_fragment.*
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
        setInitValues(viewModel)

        generator_frequency.setOnEditorActionListener { tv, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                SystemUtils.hideKeyboard(this)
                try {
                    val freq = tv.text.toString().toDouble()
                    if (freq <= 0) throw NumberFormatException()
                    viewModel.setGeneratorFrequency(freq)
                    true
                } catch (e: NumberFormatException) {
                    val msg = "Частота генератора должна быть положительным целым числом"
                    Log.e(TAG, msg, e)
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                    false
                }
            } else false
        }

        viewModel.generatorSignalData.observe(viewLifecycleOwner, Observer {
            generator_signal_chart.removeAllSeries()
            generator_signal_chart.addSeries(LineGraphSeries(it))
        })
    }

    private fun setInitValues(viewModel: DemodulatorGeneratorViewModel) {
        viewModel.initFrequency.observe(viewLifecycleOwner, Observer {
            generator_frequency.setText(it.toString())
        })
    }

}
