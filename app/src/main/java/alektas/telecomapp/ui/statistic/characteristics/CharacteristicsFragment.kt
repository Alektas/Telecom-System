package alektas.telecomapp.ui.statistic.characteristics

import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import alektas.telecomapp.R
import alektas.telecomapp.ui.utils.setupLabels
import alektas.telecomapp.utils.SystemUtils
import android.widget.Toast
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.Observer
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import kotlinx.android.synthetic.main.characteristics_fragment.*

class CharacteristicsFragment : Fragment() {
    private lateinit var viewModel: CharacteristicsViewModel
    private val berGraphPoints = LineGraphSeries<DataPoint>()
    private val capacityGraphPoints = LineGraphSeries<DataPoint>()

    companion object {
        fun newInstance() = CharacteristicsFragment()

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.characteristics_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<GraphView>(R.id.ber_graph).apply {
            gridLabelRenderer.padding = 45
            setupLabels(xIntMax = 3, yIntMax = 3)
            addSeries(berGraphPoints)
            viewport.isXAxisBoundsManual = true
        }
        view.findViewById<GraphView>(R.id.capacity_graph).apply {
            gridLabelRenderer.padding = 45
            setupLabels(xIntMax = 3, yIntMax = 3)
            addSeries(capacityGraphPoints)
            viewport.isXAxisBoundsManual = true
        }
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(CharacteristicsViewModel::class.java)
        setFieldsValidation()

        points_count.setOnEditorActionListener { _, _, _ ->
            SystemUtils.hideKeyboard(this)
            draw_graphs_btn.performClick()
            false
        }

        draw_graphs_btn.setOnClickListener {
            calculateCharacteristics()
        }

        viewModel.viewportData.observe(viewLifecycleOwner, Observer {
            ber_graph.viewport.apply {
                setMinX(it.first)
                setMaxX(it.second)
            }
            capacity_graph.viewport.apply {
                setMinX(it.first)
                setMaxX(it.second)
            }
        })

        viewModel.berData.observe(viewLifecycleOwner, Observer {
            berGraphPoints.resetData(it)
        })

        viewModel.capacityData.observe(viewLifecycleOwner, Observer {
            capacityGraphPoints.resetData(it)
        })

        viewModel.isChannelsInvalid.observe(viewLifecycleOwner, Observer {
            if (it) {
                draw_graphs_btn.isEnabled = false
                setup_channels_hint.visibility = View.VISIBLE
            } else {
                draw_graphs_btn.isEnabled = true
                setup_channels_hint.visibility = View.INVISIBLE
            }

        })
    }

    private fun calculateCharacteristics() {
        val from = from_snr.text.toString()
        val to = to_snr.text.toString()
        val count = points_count.text.toString()
        val isSuccess = viewModel.calculateCharacteristics(from, to, count)
        if (!isSuccess) {
            Toast.makeText(
                requireContext(),
                getString(R.string.enter_valid_data),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun setFieldsValidation() {
        from_snr.doOnTextChanged { text, _, _, _ ->
            if (viewModel.parseSnr(text.toString()) != CharacteristicsViewModel.INVALID_SNR) {
                from_snr_layout.error = null
            } else {
                from_snr_layout.error = getString(R.string.error_num)
            }
        }

        to_snr.doOnTextChanged { text, _, _, _ ->
            if (viewModel.parseSnr(text.toString()) != CharacteristicsViewModel.INVALID_SNR) {
                to_snr_layout.error = null
            } else {
                to_snr_layout.error = getString(R.string.error_num)
            }
        }

        points_count.doOnTextChanged { text, _, _, _ ->
            if (viewModel.parseSnr(text.toString()) > 0) {
                points_count_layout.error = null
            } else {
                points_count_layout.error = getString(R.string.error_positive_num)
            }
        }
    }

}
