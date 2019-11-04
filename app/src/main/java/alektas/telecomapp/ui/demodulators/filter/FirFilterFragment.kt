package alektas.telecomapp.ui.demodulators.filter

import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import alektas.telecomapp.R
import alektas.telecomapp.domain.entities.Window
import alektas.telecomapp.ui.utils.SimpleArrayAdapter
import alektas.telecomapp.utils.SystemUtils
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
        setupWindowsDropdown(viewModel)
        setFieldsValidation()
        setInitValues(viewModel)

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
            (v as AutoCompleteTextView).showDropDown()
            false
        }
    }

    private fun setInitValues(viewModel: FirFilterViewModel) {
        viewModel.initConfigData.observe(viewLifecycleOwner, Observer { data ->
            filter_order_input.setText(data.order.toString())
            filter_cutoff_freq_input.setText(data.bandwidth.toString())
            val winName: String = Window.getName(data.windowType)
            filter_window_input.setText(winName, false)
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
