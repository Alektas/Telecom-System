package alektas.telecomapp.ui.datasource.simulation

import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import alektas.telecomapp.R
import alektas.telecomapp.domain.entities.coders.DataCodesContract
import alektas.telecomapp.domain.entities.generators.ChannelCodesGenerator
import alektas.telecomapp.domain.entities.contracts.CdmaContract
import alektas.telecomapp.domain.entities.contracts.QpskContract
import alektas.telecomapp.ui.utils.DataCoding
import alektas.telecomapp.ui.utils.Mode
import alektas.telecomapp.ui.utils.SimpleArrayAdapter
import alektas.telecomapp.utils.SystemUtils
import android.content.Context
import android.content.SharedPreferences
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.channels_settings_fragment.*
import java.lang.NumberFormatException

class ChannelsSettingsFragment : Fragment() {

    companion object {
        fun newInstance() = ChannelsSettingsFragment()
    }

    private lateinit var viewModel: ChannelsSettingsViewModel
    private lateinit var prefs: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.channels_settings_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(ChannelsSettingsViewModel::class.java)
        prefs = requireContext().getSharedPreferences(
            getString(R.string.settings_source_key),
            Context.MODE_PRIVATE
        )

        val channelCodes = ChannelCodesGenerator.codeNames.values.toList()
        setupDropdown(source_channel_code_type, channelCodes)
        val dataCodes = DataCodesContract.codeNames.values.toList()
        setupDropdown(source_data_code_type, dataCodes)

        setInitValues(prefs)
        // Нужно вызывать после setInitValues, иначе метка изменений настроек
        // будет всегда появлятся при открытии страницы
        setupFieldsValidation()
        observeSettings(viewModel, prefs)
        setupControls()
    }

    private fun setInitValues(prefs: SharedPreferences) {
        prefs.getInt(
            getString(R.string.source_channels_codetype_key),
            CdmaContract.DEFAULT_CHANNEL_CODE_TYPE
        ).let {
            source_channel_code_type.setText(ChannelCodesGenerator.getCodeName(it))
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
            getString(R.string.source_channels_codesize_key),
            CdmaContract.DEFAULT_CHANNEL_CODE_SIZE
        ).let {
            source_code_length.setText(it.toString())
        }

        prefs.getInt(
            getString(R.string.source_channels_framesize_key),
            CdmaContract.DEFAULT_FRAME_SIZE
        ).let {
            source_frame_length.setText(it.toString())
        }

        val isDataCodingEnabled = prefs.getBoolean(
            getString(R.string.source_data_coding_enable_key),
            DataCodesContract.DEFAULT_IS_CODING_ENABLED
        )
        source_data_coding_checkbox.isChecked = isDataCodingEnabled
        setupViewByMode(DataCoding(isDataCodingEnabled))

        prefs.getInt(
            getString(R.string.source_data_coding_type_key),
            DataCodesContract.HAMMING
        ).let {
            source_data_code_type.setText(DataCodesContract.getCodeName(it))
        }
    }

    private fun observeSettings(viewModel: ChannelsSettingsViewModel, prefs: SharedPreferences) {
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

        viewModel.channelCodeType.observe(viewLifecycleOwner, Observer {
            prefs.edit().putInt(getString(R.string.source_channels_codetype_key), it).apply()
        })

        viewModel.channelCodeLength.observe(viewLifecycleOwner, Observer {
            prefs.edit().putInt(getString(R.string.source_channels_codesize_key), it).apply()
        })

        viewModel.frameSize.observe(viewLifecycleOwner, Observer {
            prefs.edit().putInt(getString(R.string.source_channels_framesize_key), it).apply()
        })

        viewModel.dataCodeType.observe(viewLifecycleOwner, Observer {
            prefs.edit().putInt(getString(R.string.source_data_coding_type_key), it).apply()
        })

        viewModel.isSettingsChanged.observe(viewLifecycleOwner, Observer {
            settings_changed_label.visibility = if (it) View.VISIBLE else View.INVISIBLE
        })
    }

    private fun setupDropdown(dropdown: AutoCompleteTextView, items: List<String>) {
        val adapter = SimpleArrayAdapter(
            requireContext(),
            R.layout.support_simple_spinner_dropdown_item,
            items
        )

        dropdown.apply {
            setAdapter<ArrayAdapter<String>>(adapter)
            setOnItemClickListener { _, _, _, _ ->
                SystemUtils.hideKeyboard(this)
                viewModel.setSettingsChanged()
            }
        }
    }

    private fun setupFieldsValidation() {
        source_channel_count.doOnTextChanged { text, _, _, _ ->
            viewModel.setSettingsChanged()
            source_channel_count_layout.error = try {
                viewModel.parseChannelCount(text.toString())
                null
            } catch (e: NumberFormatException) {
                getString(R.string.error_positive_num)
            }
        }

        source_carrier_frequency.doOnTextChanged { text, _, _, _ ->
            viewModel.setSettingsChanged()
            source_carrier_frequency_layout.error = try {
                viewModel.parseFrequency(text.toString())
                null
            } catch (e: NumberFormatException) {
                getString(R.string.error_positive_num_decimal)
            }
        }

        source_data_speed.doOnTextChanged { text, _, _, _ ->
            viewModel.setSettingsChanged()
            source_data_speed_layout.error = try {
                viewModel.parseDataspeed(text.toString())
                null
            } catch (e: NumberFormatException) {
                getString(R.string.error_positive_num_decimal)
            }
        }

        source_code_length.doOnTextChanged { text, _, _, _ ->
            viewModel.setSettingsChanged()
            source_code_length_layout.error = try {
                viewModel.parseFrameLength(text.toString())
                null
            } catch (e: NumberFormatException) {
                getString(R.string.error_positive_num)
            }
        }

        source_frame_length.doOnTextChanged { text, _, _, _ ->
            viewModel.setSettingsChanged()
            source_frame_length_layout.error = try {
                viewModel.parseFrameLength(text.toString())
                null
            } catch (e: NumberFormatException) {
                getString(R.string.error_positive_num)
            }
        }
    }

    private fun setupControls() {
        source_data_coding_checkbox.setOnCheckedChangeListener { _, isEnabled ->
            setupViewByMode(DataCoding(isEnabled))
            prefs.edit()
                .putBoolean(getString(R.string.source_data_coding_enable_key), isEnabled)
                .apply()
            createChannels()
        }

        generate_channels_btn.setOnClickListener {
            SystemUtils.hideKeyboard(this)
            createChannels()
        }
    }

    private fun createChannels() {
        val channelCount = source_channel_count.text.toString()
        val freq = source_carrier_frequency.text.toString()
        val dataSpeed = source_data_speed.text.toString()
        val channelCodeLength = source_code_length.text.toString()
        val channelCodeType = source_channel_code_type.text.toString()
        val frameLength = source_frame_length.text.toString()
        val isDataDecoding = source_data_coding_checkbox.isChecked
        val dataCodesType = source_data_code_type.text.toString()

        val isSuccess = viewModel.setupChannelsConfig(
            channelCount,
            freq,
            dataSpeed,
            channelCodeType,
            channelCodeLength,
            frameLength,
            isDataDecoding,
            dataCodesType
        )

        if (!isSuccess) {
            Toast.makeText(requireContext(), "Введите корректные данные", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupViewByMode(mode: Mode) {
        source_data_code_type_layout.isEnabled = mode is DataCoding && mode.isEnabled
    }

}
