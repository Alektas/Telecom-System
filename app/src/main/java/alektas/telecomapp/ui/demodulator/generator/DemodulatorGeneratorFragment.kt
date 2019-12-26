package alektas.telecomapp.ui.demodulator.generator

import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import alektas.telecomapp.R
import alektas.telecomapp.domain.entities.contracts.QpskContract
import alektas.telecomapp.utils.SystemUtils
import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast
import androidx.lifecycle.Observer
import com.jjoe64.graphview.series.LineGraphSeries
import kotlinx.android.synthetic.main.demodulator_generator_fragment.*

class DemodulatorGeneratorFragment : Fragment() {
    private lateinit var prefs: SharedPreferences

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
        prefs = requireContext().getSharedPreferences(
            getString(R.string.settings_demodulator_key),
            Context.MODE_PRIVATE
        )
        setInitValues(prefs)
        observeSettings(viewModel, prefs)

        generator_frequency.setOnEditorActionListener { tv, _, _ ->
            SystemUtils.hideKeyboard(this)
            val success = viewModel.setGeneratorFrequency(tv.text.toString())
            if (!success) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.error_positive_num),
                    Toast.LENGTH_SHORT
                ).show()
            }
            false
        }

        viewModel.generatorSignalData.observe(viewLifecycleOwner, Observer {
            generator_signal_chart.removeAllSeries()
            generator_signal_chart.addSeries(LineGraphSeries(it))
        })
    }

    private fun setInitValues(prefs: SharedPreferences) {
        prefs.getFloat(
            getString(R.string.demodulator_generator_freq_key),
            (1.0e-6 * QpskContract.DEFAULT_CARRIER_FREQUENCY).toFloat()
        ).let {
            generator_frequency.setText(String.format("%.3f", it))
        }
    }

    private fun observeSettings(
        viewModel: DemodulatorGeneratorViewModel,
        prefs: SharedPreferences
    ) {
        viewModel.generatorFrequency.observe(viewLifecycleOwner, Observer {
            prefs.edit().putFloat(getString(R.string.demodulator_generator_freq_key), it).apply()
        })
    }

}
