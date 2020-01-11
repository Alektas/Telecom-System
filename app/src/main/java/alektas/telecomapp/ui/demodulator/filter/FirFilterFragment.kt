package alektas.telecomapp.ui.demodulator.filter

import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import alektas.telecomapp.R
import alektas.telecomapp.domain.entities.Window
import alektas.telecomapp.domain.entities.filters.FilterConfig
import alektas.telecomapp.ui.utils.SimpleArrayAdapter
import alektas.telecomapp.utils.SystemUtils
import android.content.Context
import android.content.SharedPreferences
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.Observer
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import kotlinx.android.synthetic.main.fir_filter_fragment.*

class FirFilterFragment : Fragment() {
    private lateinit var viewModel: FirFilterViewModel
    private lateinit var prefs: SharedPreferences

    companion object {
        fun newInstance() = FirFilterFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fir_filter_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(FirFilterViewModel::class.java)
        prefs = requireContext().getSharedPreferences(
            getString(R.string.settings_demodulator_key),
            Context.MODE_PRIVATE
        )
        setupWindowsDropdown(viewModel)
        setFieldsValidation()
        setInitValues(prefs)
        observeSettings(viewModel, prefs)

        filter_order_input.setOnEditorActionListener { tv, _, _ ->
            SystemUtils.hideKeyboard(this)
            val order = viewModel.parseFilterOrder(tv.text.toString())

            if (order > 0) {
                viewModel.onOrderChanged(order)
            } else {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.error_positive_num),
                    Toast.LENGTH_SHORT
                ).show()
            }

            false
        }

        filter_cutoff_freq_input.setOnEditorActionListener { tv, _, _ ->
            SystemUtils.hideKeyboard(this)
            val freq = viewModel.parseCutoffFrequency(tv.text.toString())

            if (freq > 0) {
                viewModel.onCutoffFrequencyChanged(freq)
            } else {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.error_positive_num_decimal),
                    Toast.LENGTH_SHORT
                ).show()
            }

            false
        }

        viewModel.impulseResponseData.observe(viewLifecycleOwner, Observer { data ->
            filter_impulse_response_chart.removeAllSeries()
            filter_impulse_response_chart.addSeries(LineGraphSeries<DataPoint>(data))
        })
    }

    private fun setupWindowsDropdown(viewModel: FirFilterViewModel) {
        val adapter = SimpleArrayAdapter(
            requireContext(),
            R.layout.support_simple_spinner_dropdown_item,
            Window.windowNames.values.toList()
        )
        filter_window_input.setAdapter<ArrayAdapter<String>>(adapter)

        filter_window_input.setOnItemClickListener { parent, _, position, _ ->
            val windowName = parent.getItemAtPosition(position)
            if (windowName is String) viewModel.onWindowChanged(windowName)
        }

        filter_window_input_layout.setOnTouchListener { v, _ ->
            SystemUtils.hideKeyboard(this)
            v.findViewById<AutoCompleteTextView>(R.id.filter_window_input).showDropDown() // TODO: проверить в других местах (ClassCastException)
            false
        }
    }

    private fun setInitValues(prefs: SharedPreferences) {
        prefs.getFloat(
            getString(R.string.demodulator_filter_cutoff_freq_key),
            FilterConfig.DEFAULT_BANDWIDTH.toFloat()
        ).let {
            filter_cutoff_freq_input.setText(String.format("%.3f", it))
        }

        prefs.getInt(
            getString(R.string.demodulator_filter_order_key),
            FilterConfig.DEFAULT_ORDER
        ).let {
            filter_order_input.setText(it.toString())
        }

        prefs.getInt(
            getString(R.string.demodulator_filter_window_type_key),
            FilterConfig.DEFAULT_WINDOW_TYPE
        ).let {
            val winName: String = Window.getName(it)
            filter_window_input.setText(winName, false)
        }
    }

    private fun observeSettings(viewModel: FirFilterViewModel, prefs: SharedPreferences) {
        viewModel.order.observe(viewLifecycleOwner, Observer {
            prefs.edit().putInt(getString(R.string.demodulator_filter_order_key), it).apply()
        })

        viewModel.cutoffFreq.observe(viewLifecycleOwner, Observer {
            prefs.edit().putFloat(getString(R.string.demodulator_filter_cutoff_freq_key), it).apply()
        })

        viewModel.windowType.observe(viewLifecycleOwner, Observer {
            prefs.edit().putInt(getString(R.string.demodulator_filter_window_type_key), it).apply()
        })
    }

    private fun setFieldsValidation() {
        filter_order_input.doOnTextChanged { text, _, _, _ ->
            if (viewModel.parseFilterOrder(text.toString()) > 0) {
                filter_order_input_layout.error = null
            } else {
                filter_order_input_layout.error = getString(R.string.error_positive_num)
            }
        }

        filter_cutoff_freq_input.doOnTextChanged { text, _, _, _ ->
            if (viewModel.parseCutoffFrequency(text.toString()) > 0) {
                filter_cutoff_freq_input_layout.error = null
            } else {
                filter_cutoff_freq_input_layout.error = getString(R.string.error_positive_num_decimal)
            }
        }
    }

}
