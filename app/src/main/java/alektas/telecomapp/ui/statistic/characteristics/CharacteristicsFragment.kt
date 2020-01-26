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
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.widget.Toast
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.Observer
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import kotlinx.android.synthetic.main.characteristics_fragment.*

private const val DEFAULT_START_SNR: Float = 0f
private const val DEFAULT_FINISH_SNR: Float = 10f
private const val DEFAULT_POINTS_COUNT: Int = 10

class CharacteristicsFragment : Fragment() {
    private lateinit var viewModel: CharacteristicsViewModel
    private val berGraphPoints = LineGraphSeries<DataPoint>()
        .apply {
            title = "Практическая"
        }
    private val theoreticBerGraphPoints = LineGraphSeries<DataPoint>()
        .apply {
            title = "Теоретическая"
            color = Color.rgb(255, 100, 100)
        }
    private val capacityGraphPoints = LineGraphSeries<DataPoint>()
        .apply {
            title = "Проп. спос."
            color = Color.rgb(255, 100, 100)
        }
    private val dataSpeedGraphPoints = LineGraphSeries<DataPoint>()
        .apply {
            title = "Скорость пер."
        }

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
            legendRenderer.apply {
                isVisible = true
                backgroundColor = Color.argb(50, 0, 0, 0)
            }
            gridLabelRenderer.padding = 45
            setupLabels(xIntMax = 3, yIntMax = 3)
            addSeries(berGraphPoints)
            addSeries(theoreticBerGraphPoints)
            viewport.isXAxisBoundsManual = true
        }
        view.findViewById<GraphView>(R.id.capacity_graph).apply {
            legendRenderer.apply {
                isVisible = true
                backgroundColor = Color.argb(50, 0, 0, 0)
            }
            gridLabelRenderer.padding = 45
            setupLabels(xIntMax = 3, yIntMax = 3)
            addSeries(capacityGraphPoints)
            addSeries(dataSpeedGraphPoints)
            viewport.isXAxisBoundsManual = true
        }
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val prefs = requireContext().getSharedPreferences(
            getString(R.string.settings_characteristics_key),
            Context.MODE_PRIVATE
        )
        viewModel = ViewModelProviders.of(this).get(CharacteristicsViewModel::class.java)
        setInitValues(prefs)
        observeSettings(viewModel, prefs)
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

        viewModel.theoreticBerData.observe(viewLifecycleOwner, Observer {
            theoreticBerGraphPoints.resetData(it)
        })

        viewModel.capacityData.observe(viewLifecycleOwner, Observer {
            capacityGraphPoints.resetData(it)
        })

        viewModel.dataSpeedData.observe(viewLifecycleOwner, Observer {
            dataSpeedGraphPoints.resetData(it)
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

    private fun setInitValues(prefs: SharedPreferences) {
        prefs.getFloat(
            getString(R.string.characteristics_start_snr_key),
            DEFAULT_START_SNR
        ).let {
            from_snr.setText(it.toString())
        }

        prefs.getFloat(
            getString(R.string.characteristics_finish_snr_key),
            DEFAULT_FINISH_SNR
        ).let {
            to_snr.setText(it.toString())
        }

        prefs.getInt(
            getString(R.string.characteristics_points_count_key),
            DEFAULT_POINTS_COUNT
        ).let {
            points_count.setText(it.toString())
        }
    }

    private fun observeSettings(viewModel: CharacteristicsViewModel, prefs: SharedPreferences) {
        viewModel.fromSnr.observe(viewLifecycleOwner, Observer {
            prefs.edit().putFloat(getString(R.string.characteristics_start_snr_key), it).apply()
        })

        viewModel.toSnr.observe(viewLifecycleOwner, Observer {
            prefs.edit().putFloat(getString(R.string.characteristics_finish_snr_key), it).apply()
        })

        viewModel.pointsCount.observe(viewLifecycleOwner, Observer {
            prefs.edit().putInt(getString(R.string.characteristics_points_count_key), it).apply()
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
