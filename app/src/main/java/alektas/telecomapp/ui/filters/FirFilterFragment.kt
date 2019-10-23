package alektas.telecomapp.ui.filters

import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import alektas.telecomapp.R
import android.widget.ArrayAdapter
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
            val windows = viewModel.windowsData.value
            if (windows != null) {
                val winName: String = windows[data.windowType] ?: "${data.windowType}"
                filter_window_input.setText(winName, false)
            }
        })

        viewModel.impulseResponseData.observe(viewLifecycleOwner, Observer { data ->
            filter_impulse_response_chart.removeAllSeries()
            filter_impulse_response_chart.addSeries(LineGraphSeries<DataPoint>(data))
        })
    }

    private fun setupWindowsDropdown(viewModel: FirFilterViewModel) {
        val windows = mutableListOf<Pair<Int, String>>()

        viewModel.windowsData.observe(viewLifecycleOwner, Observer { w ->
            windows.clear()
            windows.addAll(w.map { Pair(it.key, it.value) })
            val adapter = ArrayAdapter(
                requireContext(),
                R.layout.support_simple_spinner_dropdown_item,
                w.values.toTypedArray()
            )
            filter_window_input.setAdapter<ArrayAdapter<String>>(adapter)
        })

        filter_window_input.setOnItemClickListener { _, _, position, _ ->
            viewModel.onWindowChanged(windows[position].first)
        }
    }

}
