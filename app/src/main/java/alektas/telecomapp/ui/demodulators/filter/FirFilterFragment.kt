package alektas.telecomapp.ui.demodulators.filter

import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import alektas.telecomapp.R
import alektas.telecomapp.domain.entities.Window
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

        filter_order_input.doOnTextChanged { text, _, _, _ ->
            try {
                val order = text.toString().toInt()
                viewModel.onOrderChanged(order)
            } catch (e: NumberFormatException) {
                Toast.makeText(
                    requireContext(),
                    "Введите целое число больше 0",
                    Toast.LENGTH_SHORT
                ).show()
                e.printStackTrace()
            }
        }

        filter_cutoff_freq_input.doOnTextChanged { text, _, _, _ ->
            try {
                val freq = text.toString().toDouble()
                viewModel.onCutoffFrequencyChanged(freq)
            } catch (e: NumberFormatException) {
                Toast.makeText(
                    requireContext(),
                    "Введите число больше 0.0",
                    Toast.LENGTH_SHORT
                ).show()
                e.printStackTrace()
            }
        }

        viewModel.initConfigData.observe(viewLifecycleOwner, Observer { data ->
            filter_order_input.setText(data.order.toString())
            filter_cutoff_freq_input.setText(data.bandwidth.toString())
            val winName: String = Window.windowNames[data.windowType] ?: "${data.windowType}"
            filter_window_input.setText(winName, false)
        })

        viewModel.impulseResponseData.observe(viewLifecycleOwner, Observer { data ->
            filter_impulse_response_chart.removeAllSeries()
            filter_impulse_response_chart.addSeries(LineGraphSeries<DataPoint>(data))
        })
    }

    private fun setupWindowsDropdown(viewModel: FirFilterViewModel) {
        val adapter = ArrayAdapter(
            requireContext(),
            R.layout.support_simple_spinner_dropdown_item,
            Window.windowNames.values.toTypedArray()
        )
        filter_window_input.setAdapter<ArrayAdapter<String>>(adapter)

        filter_window_input.setOnItemClickListener { parent, _, position, _ ->
            val windowName = parent.getItemAtPosition(position)
            if (windowName is String) viewModel.onWindowChanged(windowName)
        }

        filter_window_input.setOnTouchListener { v, _ ->
            SystemUtils.hideKeyboard(this)
            (v as AutoCompleteTextView).showDropDown()
            false
        }
    }

}
