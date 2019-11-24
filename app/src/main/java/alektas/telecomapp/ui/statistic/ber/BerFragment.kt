package alektas.telecomapp.ui.statistic.ber

import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import alektas.telecomapp.R
import alektas.telecomapp.utils.SystemUtils
import android.widget.Toast
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.Observer
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import kotlinx.android.synthetic.main.ber_fragment.*
import kotlinx.android.synthetic.main.data_source_fragment.*

class BerFragment : Fragment() {
    private lateinit var viewModel: BerViewModel
    private val graphPoints = LineGraphSeries<DataPoint>()

    companion object {
        fun newInstance() = BerFragment()

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.ber_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<GraphView>(R.id.ber_graph).apply {
            addSeries(graphPoints)
            viewport.isXAxisBoundsManual = true
        }
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(BerViewModel::class.java)
        setFieldsValidation()

        ber_to_snr.setOnEditorActionListener { _, _, _ ->
            SystemUtils.hideKeyboard(this)
            ber_draw_graph_btn.performClick()
            false
        }

        ber_draw_graph_btn.setOnClickListener {
            calculateBer()
        }

        viewModel.viewportData.observe(viewLifecycleOwner, Observer {
            ber_graph.viewport.apply {
                setMinX(it.first)
                setMaxX(it.second)
            }
        })

        viewModel.berData.observe(viewLifecycleOwner, Observer {
            graphPoints.resetData(it)
        })
    }

    private fun calculateBer() {
        val from = ber_from_snr.text.toString()
        val to = ber_to_snr.text.toString()
        val isSuccess = viewModel.calculateBer(from, to)
        if (!isSuccess) {
            Toast.makeText(
                requireContext(),
                getString(R.string.error_ber_graph_invalid_data),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun setFieldsValidation() {
        ber_from_snr.doOnTextChanged { text, _, _, _ ->
            if (viewModel.parseSnr(text.toString()) != BerViewModel.INVALID_SNR) {
                ber_from_snr_layout.error = null
            } else {
                ber_from_snr_layout.error = getString(R.string.error_num)
            }
        }

        ber_to_snr.doOnTextChanged { text, _, _, _ ->
            if (viewModel.parseSnr(text.toString()) != BerViewModel.INVALID_SNR) {
                ber_to_snr_layout.error = null
            } else {
                ber_to_snr_layout.error = getString(R.string.error_num)
            }
        }
    }

}
