package alektas.telecomapp.ui.decoder

import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import alektas.telecomapp.R
import alektas.telecomapp.domain.entities.generators.ChannelCodesGenerator
import alektas.telecomapp.domain.entities.contracts.CdmaContract
import alektas.telecomapp.domain.entities.Channel
import alektas.telecomapp.domain.entities.coders.DataCodesContract
import alektas.telecomapp.domain.entities.contracts.QpskContract
import alektas.telecomapp.ui.datasource.ChannelAdapter
import alektas.telecomapp.ui.datasource.ChannelController
import alektas.telecomapp.ui.utils.ChannelsAutoDetection
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
import androidx.recyclerview.widget.LinearLayoutManager
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import kotlinx.android.synthetic.main.decoder_fragment.*
import kotlinx.android.synthetic.main.decoder_fragment.channel_list
import kotlinx.android.synthetic.main.decoder_fragment.generate_channels_btn
import java.lang.NumberFormatException

class DecoderFragment : Fragment(), ChannelController {
    private lateinit var viewModel: DecoderViewModel
    private lateinit var prefs: SharedPreferences
    private val graphPoints = LineGraphSeries<DataPoint>()

    companion object {
        fun newInstance() = DecoderFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.decoder_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<GraphView>(R.id.decoder_input_chart).apply {
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
        viewModel = ViewModelProviders.of(this).get(DecoderViewModel::class.java)
        prefs = requireContext().getSharedPreferences(
            getString(R.string.settings_decoder_key),
            Context.MODE_PRIVATE
        )

        val channelCodes = listOf(ChannelCodesGenerator.codeNames[ChannelCodesGenerator.WALSH] ?: "")
        setupDropdown(decoder_channel_code_type, channelCodes)
        val dataCodes = DataCodesContract.codeNames.values.toList()
        setupDropdown(decoder_data_code_type, dataCodes)

        setFieldsValidation()
        setInitValues(prefs)
        setupControls()
        observeSettings(viewModel, prefs)

        val channelAdapter = ChannelAdapter(this)
        channel_list.adapter = channelAdapter
        channel_list.layoutManager = LinearLayoutManager(requireContext())

        viewModel.channels.observe(viewLifecycleOwner, Observer {
            channelAdapter.channels = it
        })

        viewModel.inputSignalData.observe(viewLifecycleOwner, Observer {
            graphPoints.resetData(it)
        })

        viewModel.isDecodingAvailable.observe(viewLifecycleOwner, Observer {
            channels_autodetection_checkbox.isEnabled = it
            generate_channels_btn.isEnabled = it
        })
    }

    override fun removeChannel(channel: Channel) {
        viewModel.removeChannel(channel)
    }

    override fun showChannelDetails(channel: Channel) {}

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
            }
        }
    }

    private fun setupControls() {
        decoder_channel_code.setOnEditorActionListener { _, _, _ ->
            add_channel_btn.performClick()
            false
        }

        add_channel_btn.setOnClickListener {
            SystemUtils.hideKeyboard(this)
            decodeCustomChannel()
        }

        generate_channels_btn.setOnClickListener {
            SystemUtils.hideKeyboard(this)
            setupDecoding()
        }

        channels_autodetection_checkbox.setOnCheckedChangeListener { _, isAuto ->
            setupViewByMode(ChannelsAutoDetection(isAuto))
            setupDecoding()
            prefs.edit().putBoolean(getString(R.string.decoder_channels_autodetection_key), isAuto).apply()
        }

        decoder_data_coding_checkbox.setOnCheckedChangeListener { _, isEnabled ->
            setupViewByMode(DataCoding(isEnabled))
            setupDecoding()
            prefs.edit().putBoolean(getString(R.string.decoder_data_decoding_enable_key), isEnabled).apply()
        }
    }

    private fun setInitValues(prefs: SharedPreferences) {
        val isAutoDetection = prefs.getBoolean(
            getString(R.string.decoder_channels_autodetection_key),
            CdmaContract.DEFAULT_IS_AUTO_DETECTION_ENABLED
        )
        channels_autodetection_checkbox.isChecked = isAutoDetection
        setupViewByMode(ChannelsAutoDetection(isAutoDetection))

        prefs.getInt(
            getString(R.string.decoder_channels_codetype_key),
            CdmaContract.DEFAULT_CHANNEL_CODE_TYPE
        ).let {
            decoder_channel_code_type.setText(ChannelCodesGenerator.getCodeName(it))
        }

        prefs.getInt(
            getString(R.string.decoder_code_length_key),
            CdmaContract.DEFAULT_CHANNEL_CODE_SIZE
        ).let {
            decoder_code_length.setText(it.toString())
        }

        prefs.getFloat(
            getString(R.string.decoder_threshold_key),
            QpskContract.DEFAULT_SIGNAL_THRESHOLD
        ).let {
            decoder_threshold.setText(it.toString())
        }

        prefs.getInt(
            getString(R.string.decoder_channels_count_key),
            CdmaContract.DEFAULT_CHANNEL_COUNT
        ).let {
            decoder_channel_count.setText(it.toString())
        }

        val isDataCodingEnabled = prefs.getBoolean(
            getString(R.string.decoder_data_decoding_enable_key),
            DataCodesContract.DEFAULT_IS_CODING_ENABLED
        )
        decoder_data_coding_checkbox.isChecked = isDataCodingEnabled
        setupViewByMode(DataCoding(isDataCodingEnabled))

        prefs.getInt(
            getString(R.string.decoder_data_coding_type_key),
            DataCodesContract.HAMMING
        ).let {
            decoder_data_code_type.setText(DataCodesContract.getCodeName(it))
        }
    }

    private fun observeSettings(viewModel: DecoderViewModel, prefs: SharedPreferences) {
        viewModel.channelsCodesType.observe(viewLifecycleOwner, Observer {
            prefs.edit().putInt(getString(R.string.decoder_channels_codetype_key), it).apply()
        })

        viewModel.channelsCodesLength.observe(viewLifecycleOwner, Observer {
            prefs.edit().putInt(getString(R.string.decoder_code_length_key), it).apply()
        })

        viewModel.threshold.observe(viewLifecycleOwner, Observer {
            prefs.edit().putFloat(getString(R.string.decoder_threshold_key), it).apply()
        })

        viewModel.channelCount.observe(viewLifecycleOwner, Observer {
            prefs.edit().putInt(getString(R.string.decoder_channels_count_key), it).apply()
        })

        viewModel.dataCodesType.observe(viewLifecycleOwner, Observer {
            prefs.edit().putInt(getString(R.string.decoder_data_coding_type_key), it).apply()
        })
    }

    private fun setFieldsValidation() {
        decoder_code_length.doOnTextChanged { text, _, _, _ ->
            decoder_code_length_layout.error = try {
                viewModel.parseChannelCount(text.toString())
                null
            } catch (e: NumberFormatException) {
                getString(R.string.error_positive_num)
            }
        }

        decoder_threshold.doOnTextChanged { text, _, _, _ ->
            decoder_threshold_layout.error = try {
                viewModel.parseThreshold(text.toString())
                null
            } catch (e: NumberFormatException) {
                getString(R.string.error_num)
            }
        }

        decoder_channel_count.doOnTextChanged { text, _, _, _ ->
            decoder_channel_count_layout.error = try {
                viewModel.parseChannelCount(text.toString())
                null
            } catch (e: NumberFormatException) {
                getString(R.string.error_positive_num)
            }
        }
    }

    private fun setupDecoding() {
        val isAutoDetection = channels_autodetection_checkbox.isChecked
        val channelCount = decoder_channel_count.text.toString()
        val channelsCodeLength = decoder_code_length.text.toString()
        val channelsCodeType = decoder_channel_code_type.text.toString()
        val threshold = decoder_threshold.text.toString()
        val isDataDecoding = decoder_data_coding_checkbox.isChecked
        val dataCodesType = decoder_data_code_type.text.toString()

        val isSuccess = viewModel.setupDecoding(
            isAutoDetection,
            channelCount,
            channelsCodeType,
            channelsCodeLength,
            threshold,
            isDataDecoding,
            dataCodesType
        )
        if (!isSuccess) {
            Toast.makeText(
                requireContext(),
                getString(R.string.enter_valid_data),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun decodeCustomChannel() {
        val code = decoder_channel_code.text.toString()
        val isSuccess = viewModel.addCustomChannel(code)
        if (!isSuccess) {
            Toast.makeText(
                requireContext(),
                "Введите код из нулей и единиц и укажите порог",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun setupViewByMode(mode: Mode) {
        when (mode) {
            is DataCoding -> {
                decoder_data_code_type_layout.isEnabled = mode.isEnabled
            }
            is ChannelsAutoDetection -> {
                if (mode.isEnabled) hideManualDetectionFields() else showManualDetectionFields()
            }
        }
    }

    private fun showManualDetectionFields() {
        View.VISIBLE.let {
            decoder_channel_code_layout.visibility = it
            add_channel_btn.visibility = it
            decoder_channel_count_layout.visibility = it
        }
    }

    private fun hideManualDetectionFields() {
        View.GONE.let {
            decoder_channel_code_layout.visibility = it
            add_channel_btn.visibility = it
            decoder_channel_count_layout.visibility = it
        }
    }

}