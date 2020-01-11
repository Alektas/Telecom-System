package alektas.telecomapp.ui.datasource.simulation

import alektas.telecomapp.R
import alektas.telecomapp.domain.entities.contracts.CdmaContract
import alektas.telecomapp.domain.entities.Channel
import alektas.telecomapp.domain.entities.contracts.QpskContract
import alektas.telecomapp.domain.entities.Simulator
import alektas.telecomapp.ui.datasource.ChannelAdapter
import alektas.telecomapp.ui.datasource.ChannelController
import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import alektas.telecomapp.utils.SystemUtils
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.widget.Toast
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import kotlinx.android.synthetic.main.simulation_data_source_fragment.*

class SimulationDataSourceFragment : Fragment(),
    ChannelController {
    private lateinit var viewModel: SimulationDataSourceViewModel
    private lateinit var prefs: SharedPreferences
    private val graphPoints = LineGraphSeries<DataPoint>()

    companion object {
        fun newInstance() = SimulationDataSourceFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.simulation_data_source_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<GraphView>(R.id.ether_chart).apply {
            addSeries(graphPoints)
            viewport.apply {
                isScrollable = true
                isXAxisBoundsManual = true
                setMaxX(10 * QpskContract.DEFAULT_DATA_BIT_TIME)
            }
        }
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(SimulationDataSourceViewModel::class.java)
        prefs = requireContext().getSharedPreferences(
            getString(R.string.settings_source_key),
            MODE_PRIVATE
        )

        setupFieldsValidation()
        setInitValues(prefs)
        observeSettings(viewModel, prefs)
        setupControls()

        val channelAdapter = ChannelAdapter(this)
        channel_list.adapter = channelAdapter
        channel_list.layoutManager = LinearLayoutManager(requireContext())

        viewModel.channels.observe(viewLifecycleOwner, Observer {
            channelAdapter.channels = it
            if (it.isNotEmpty()) {
                setup_channels_hint.visibility = View.INVISIBLE
            } else {
                setup_channels_hint.visibility = View.VISIBLE
            }
        })

        viewModel.ether.observe(viewLifecycleOwner, Observer {
            graphPoints.resetData(it)
        })

        viewModel.isTransmitAvailable.observe(viewLifecycleOwner, Observer {
            transmit_frames_btn.isEnabled = it
        })
    }

    private fun setInitValues(prefs: SharedPreferences) {
        val defaultAdcFreq = (1.0e-6 * Simulator.DEFAULT_SAMPLING_RATE).toFloat()
        prefs.getFloat(
            getString(R.string.source_adc_freq_key),
            defaultAdcFreq
        ).let {
            adc_frequency.setText(String.format("%.3f", it))
        }

        prefs.getInt(
            getString(R.string.source_channels_frame_count_key),
            CdmaContract.DEFAULT_FRAME_COUNT
        ).let {
            source_frame_count.setText(it.toString())
        }
    }

    private fun observeSettings(viewModel: SimulationDataSourceViewModel, prefs: SharedPreferences) {
        viewModel.adcFrequency.observe(viewLifecycleOwner, Observer {
            prefs.edit().putFloat(getString(R.string.source_adc_freq_key), it.toFloat()).apply()
        })

        viewModel.frameCount.observe(viewLifecycleOwner, Observer {
            prefs.edit().putInt(getString(R.string.source_channels_frame_count_key), it).apply()
        })
    }

    override fun removeChannel(channel: Channel) {
        viewModel.removeChannel(channel)
    }

    override fun showChannelDetails(channel: Channel) {}

    private fun setupControls() {
        adc_frequency.setOnEditorActionListener { tv, _, _ ->
            changeAdcFrequency(tv.text.toString())
            false
        }

        source_frame_count.setOnEditorActionListener { _, _, _ ->
            transmit_frames_btn.performClick()
            false
        }

        transmit_frames_btn.setOnClickListener {
            SystemUtils.hideKeyboard(this)
            transmitFrames()
        }
    }

    private fun setupFieldsValidation() {
        adc_frequency.doOnTextChanged { text, _, _, _ ->
            if (viewModel.parseFrequency(text.toString()) > 0) {
                adc_frequency_layout.error = null
            } else {
                adc_frequency_layout.error = getString(R.string.error_positive_num_decimal)
            }
        }

        source_frame_count.doOnTextChanged { text, _, _, _ ->
            if (viewModel.parseChannelCount(text.toString()) > 0) {
                source_frame_count_layout.error = null
            } else {
                source_frame_count_layout.error = getString(R.string.error_positive_num)
            }
        }
    }

    private fun transmitFrames() {
        val channelCount = channel_list.adapter?.itemCount ?: 0
        if (channelCount == 0) {
            Toast.makeText(requireContext(), "Для передачи настройте каналы", Toast.LENGTH_SHORT).show()
            return
        }

        val frameCount = source_frame_count.text.toString()
        if (source_frame_count_layout.error != null || frameCount.isEmpty()) {
            Toast.makeText(requireContext(), "Введите корректные данные", Toast.LENGTH_SHORT).show()
            return
        }

        viewModel.transmitFrames(frameCount)
    }

    private fun changeAdcFrequency(freq: String) {
        if (freq.isEmpty() || adc_frequency_layout.error != null) {
            Toast.makeText(requireContext(), getString(R.string.error_positive_num_decimal), Toast.LENGTH_SHORT).show()
            return
        }

        viewModel.setAdcFrequency(freq)
    }

}
