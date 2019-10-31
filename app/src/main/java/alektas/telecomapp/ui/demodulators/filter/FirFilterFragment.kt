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
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
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
        setInitValues(viewModel)

        filter_order_input.setOnEditorActionListener { tv, _, _ ->
            try {
                val order = tv.text.toString().toInt()
                if (order <= 0) throw NumberFormatException()
                viewModel.onOrderChanged(order)
            } catch (e: NumberFormatException) {
                Toast.makeText(
                    requireContext(),
                    "Введите целое положительное число",
                    Toast.LENGTH_SHORT
                ).show()
                e.printStackTrace()
            }
            false
        }

        filter_cutoff_freq_input.setOnEditorActionListener { tv, _, _ ->
            SystemUtils.hideKeyboard(this)
            try {
                val freq = tv.text.toString().toDouble()
                if (freq <= 0) throw NumberFormatException()
                viewModel.onCutoffFrequencyChanged(freq)
            } catch (e: NumberFormatException) {
                Toast.makeText(
                    requireContext(),
                    "Введите положительное число",
                    Toast.LENGTH_SHORT
                ).show()
                e.printStackTrace()
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

}
