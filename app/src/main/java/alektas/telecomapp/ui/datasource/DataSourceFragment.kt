package alektas.telecomapp.ui.datasource

import alektas.telecomapp.R
import alektas.telecomapp.data.CodeGenerator
import alektas.telecomapp.domain.entities.CdmaContract
import alektas.telecomapp.domain.entities.ChannelData
import alektas.telecomapp.domain.entities.QpskContract
import alektas.telecomapp.domain.entities.Simulator
import alektas.telecomapp.ui.utils.SimpleArrayAdapter
import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import alektas.telecomapp.utils.SystemUtils
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import kotlinx.android.synthetic.main.data_source_fragment.*
import java.lang.NumberFormatException

class DataSourceFragment : Fragment(), ChannelController {
    private lateinit var viewModel: DataSourceViewModel
    private lateinit var prefs: SharedPreferences

    companion object {
        fun newInstance() = DataSourceFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.data_source_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(DataSourceViewModel::class.java)
        prefs = requireContext().getSharedPreferences(
            getString(R.string.settings_source_key),
            MODE_PRIVATE
        )
        setupCodeTypesDropdown()
        setFieldsValidation()
        setInitValues(prefs)
        observeSettings(viewModel, prefs)

        val channelAdapter = ChannelAdapter(this)
        channel_list.adapter = channelAdapter
        channel_list.layoutManager = LinearLayoutManager(requireContext())

        ether_noise_checkbox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                viewModel.enableNoise()
                ether_snr_layout.isEnabled = true
                prefs.edit().putBoolean(getString(R.string.source_noise_enable_key), true).apply()
            } else {
                viewModel.disableNoise()
                ether_snr_layout.isEnabled = false
                prefs.edit().putBoolean(getString(R.string.source_noise_enable_key), false).apply()
            }
        }

        ether_snr.setOnEditorActionListener { tv, _, _ ->
            SystemUtils.hideKeyboard(this)
            changeNoisePower(tv.text.toString())
            false
        }

        adc_frequency.setOnEditorActionListener { tv, _, _ ->
            SystemUtils.hideKeyboard(this)
            changeAdcFrequency(tv.text.toString())
            false
        }

        source_frame_length.setOnEditorActionListener { _, _, _ ->
            generate_channels_btn.performClick()
            false
        }

        generate_channels_btn.setOnClickListener {
            SystemUtils.hideKeyboard(this)
            generateChannels()
        }

        viewModel.channels.observe(viewLifecycleOwner, Observer {
            channelAdapter.channels = it
        })

        viewModel.ether.observe(viewLifecycleOwner, Observer {
            ether_chart.removeAllSeries()
            ether_chart.addSeries(LineGraphSeries<DataPoint>(it))
        })
    }

    private fun setInitValues(prefs: SharedPreferences) {
        val isNoiseEnabled = prefs.getBoolean(getString(R.string.source_noise_enable_key), true)
        ether_noise_checkbox.isChecked = isNoiseEnabled
        ether_snr_layout.isEnabled = isNoiseEnabled

        prefs.getFloat(
            getString(R.string.source_noise_snr_key),
            QpskContract.DEFAULT_SIGNAL_NOISE_RATE.toFloat()
        ).let {
            ether_snr.setText(it.toString())
        }

        val defaultAdcFreq = (1.0e-6 / Simulator.DEFAULT_SAMPLING_RATE).toFloat()
        prefs.getFloat(
            getString(R.string.source_adc_freq_key),
            defaultAdcFreq
        ).let {
            adc_frequency.setText(String.format("%.3f", it))
        }

        prefs.getInt(
            getString(R.string.source_channels_codetype_key),
            CdmaContract.DEFAULT_CODE_TYPE
        ).let {
            source_channel_code_type.setText(CodeGenerator.getCodeName(it))
        }

        val defaultCarFreq = (1.0e-6 * QpskContract.DEFAULT_CARRIER_FREQUENCY).toFloat()
        prefs.getFloat(
            getString(R.string.source_channels_freq_key),
            defaultCarFreq
        ).let {
            source_carrier_frequency.setText(String.format("%.3f", it))
        }

        val defaultDataspeed = (1.0e-3 / QpskContract.DEFAULT_DATA_BIT_TIME).toFloat()
        prefs.getFloat(getString(R.string.source_channels_dataspeed_key), defaultDataspeed).let {
            source_data_speed.setText(it.toString())
        }

        prefs.getInt(
            getString(R.string.source_channels_count_key),
            CdmaContract.DEFAULT_CHANNEL_COUNT
        ).let {
            source_channel_count.setText(it.toString())
        }

        prefs.getInt(
            getString(R.string.source_channels_framesize_key),
            CdmaContract.DEFAULT_FRAME_SIZE
        ).let {
            source_frame_length.setText(it.toString())
        }
    }

    private fun observeSettings(viewModel: DataSourceViewModel, prefs: SharedPreferences) {
        viewModel.noiseSnr.observe(viewLifecycleOwner, Observer {
            prefs.edit().putFloat(getString(R.string.source_noise_snr_key), it.toFloat()).apply()
        })

        viewModel.adcFrequency.observe(viewLifecycleOwner, Observer {
            prefs.edit().putFloat(getString(R.string.source_adc_freq_key), it.toFloat()).apply()
        })

        viewModel.codeType.observe(viewLifecycleOwner, Observer {
            prefs.edit().putInt(getString(R.string.source_channels_codetype_key), it).apply()
        })

        viewModel.carrierFrequency.observe(viewLifecycleOwner, Observer {
            prefs.edit().putFloat(getString(R.string.source_channels_freq_key), it.toFloat())
                .apply()
        })

        viewModel.dataSpeed.observe(viewLifecycleOwner, Observer {
            prefs.edit().putFloat(getString(R.string.source_channels_dataspeed_key), it.toFloat())
                .apply()
        })

        viewModel.channelCount.observe(viewLifecycleOwner, Observer {
            prefs.edit().putInt(getString(R.string.source_channels_count_key), it).apply()
        })

        viewModel.frameSize.observe(viewLifecycleOwner, Observer {
            prefs.edit().putInt(getString(R.string.source_channels_framesize_key), it).apply()
        })
    }

    override fun removeChannel(channel: ChannelData) {
        viewModel.removeChannel(channel)
    }

    override fun showChannelDetails(channel: ChannelData) {}

    private fun setupCodeTypesDropdown() {
        val adapter = SimpleArrayAdapter(
            requireContext(),
            R.layout.support_simple_spinner_dropdown_item,
            CodeGenerator.codeNames.values.toList()
        )
        source_channel_code_type.setAdapter<ArrayAdapter<String>>(adapter)

        val defaultType = CodeGenerator.getCodeName(CodeGenerator.WALSH)
        source_channel_code_type.setText(defaultType)

        source_channel_code_type_layout.setOnTouchListener { v, _ ->
            SystemUtils.hideKeyboard(this)
            val dropDown = v.findViewById<AutoCompleteTextView>(R.id.source_channel_code_type)
            dropDown.showDropDown()
            false
        }
    }

    private fun generateChannels() {
        val channelCount = source_channel_count.text.toString()
        val freq = source_carrier_frequency.text.toString()
        val dataSpeed = source_data_speed.text.toString()
        val frameLength = source_frame_length.text.toString()
        val codeType = source_channel_code_type.text.toString()

        if (source_channel_count_layout.error != null ||
            source_carrier_frequency_layout.error != null ||
            source_data_speed_layout.error != null ||
            source_frame_length_layout.error != null ||
            channelCount.isEmpty() ||
            freq.isEmpty() ||
            dataSpeed.isEmpty() ||
            frameLength.isEmpty() ||
            codeType.isEmpty()
        ) {
            Toast.makeText(requireContext(), "Введите корректные данные", Toast.LENGTH_SHORT).show()
            return
        }

        viewModel.generateChannels(channelCount, freq, dataSpeed, frameLength, codeType)
    }

    private fun setFieldsValidation() {
        adc_frequency.doOnTextChanged { text, _, _, _ ->
            if (viewModel.parseFrequency(text.toString()) > 0) {
                adc_frequency_layout.error = null
            } else {
                adc_frequency_layout.error = getString(R.string.error_positive_num_decimal)
            }
        }

        source_channel_count.doOnTextChanged { text, _, _, _ ->
            if (viewModel.parseChannelCount(text.toString()) > 0) {
                source_channel_count_layout.error = null
            } else {
                source_channel_count_layout.error = getString(R.string.error_positive_num)
            }
        }

        source_carrier_frequency.doOnTextChanged { text, _, _, _ ->
            if (viewModel.parseFrequency(text.toString()) > 0) {
                source_carrier_frequency_layout.error = null
            } else {
                source_carrier_frequency_layout.error =
                    getString(R.string.error_positive_num_decimal)
            }
        }

        source_data_speed.doOnTextChanged { text, _, _, _ ->
            if (viewModel.parseDataspeed(text.toString()) > 0) {
                source_data_speed_layout.error = null
            } else {
                source_data_speed_layout.error = getString(R.string.error_positive_num_decimal)
            }
        }

        source_frame_length.doOnTextChanged { text, _, _, _ ->
            if (viewModel.parseFrameLength(text.toString()) > 0) {
                source_frame_length_layout.error = null
            } else {
                source_frame_length_layout.error = getString(R.string.error_positive_num)
            }
        }
    }

    private fun changeNoisePower(text: String) {
        try {
            val snr = text.toDouble()
            viewModel.setNoise(snr)
        } catch (e: NumberFormatException) {
            val msg = getString(R.string.error_num)
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        }
    }

    private fun changeAdcFrequency(freqString: String) {
        val freq = viewModel.parseFrequency(freqString)
        if (freq > 0) {
            viewModel.setAdcFrequency(freq)
        } else {
            val msg = getString(R.string.error_positive_num_decimal)
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        }
    }

}
