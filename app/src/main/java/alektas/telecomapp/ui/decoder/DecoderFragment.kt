package alektas.telecomapp.ui.decoder

import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import alektas.telecomapp.R
import alektas.telecomapp.data.CodeGenerator
import alektas.telecomapp.domain.entities.contracts.CdmaContract
import alektas.telecomapp.domain.entities.Channel
import alektas.telecomapp.domain.entities.contracts.QpskContract
import alektas.telecomapp.ui.datasource.ChannelAdapter
import alektas.telecomapp.ui.datasource.ChannelController
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

class DecoderFragment : Fragment(), ChannelController {
    private lateinit var viewModel: DecoderViewModel
    private lateinit var prefs: SharedPreferences
    private val graphPoints = LineGraphSeries<DataPoint>()
    private var isAutoDetection = true

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
        setupCodeTypesDropdown()
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

    private fun setupCodeTypesDropdown() {
        val adapter = SimpleArrayAdapter(
            requireContext(),
            R.layout.support_simple_spinner_dropdown_item,
            listOf(CodeGenerator.codeNames[CodeGenerator.WALSH] ?: "")
        )
        decoder_channel_code_type.setAdapter<ArrayAdapter<String>>(adapter)

        val defaultType = CodeGenerator.getCodeName(CodeGenerator.WALSH)
        decoder_channel_code_type.setText(defaultType)

        decoder_channel_code_type_layout.setOnTouchListener { v, _ ->
            SystemUtils.hideKeyboard(this)
            v.findViewById<AutoCompleteTextView>(R.id.decoder_channel_code_type).showDropDown()
            false
        }
    }

    private fun setupControls() {
        decoder_channel_code.setOnEditorActionListener { _, _, _ ->
            add_channel_btn.performClick()
            false
        }

        decoder_threshold.setOnEditorActionListener { _, _, _ ->
            generate_channels_btn.performClick()
            false
        }

        add_channel_btn.setOnClickListener {
            SystemUtils.hideKeyboard(this)
            decodeCustomChannel()
        }

        generate_channels_btn.setOnClickListener {
            SystemUtils.hideKeyboard(this)
            decodeChannels(isAutoDetection)
        }

        channels_autodetection_checkbox.setOnCheckedChangeListener { _, isAuto ->
            setupViewByMode(isAuto)
            decodeChannels(isAuto)
            prefs.edit().putBoolean(getString(R.string.decoder_channels_autodetection_key), isAuto).apply()
        }
    }

    private fun setInitValues(prefs: SharedPreferences) {
        val isAutoDetection = prefs.getBoolean(getString(R.string.decoder_channels_autodetection_key), false)
        channels_autodetection_checkbox.isChecked = isAutoDetection
        setupViewByMode(isAutoDetection)

        prefs.getInt(
            getString(R.string.decoder_channels_codetype_key),
            CdmaContract.DEFAULT_CODE_TYPE
        ).let {
            decoder_channel_code_type.setText(CodeGenerator.getCodeName(it))
        }

        prefs.getInt(
            getString(R.string.decoder_code_length_key),
            CdmaContract.DEFAULT_CODE_SIZE
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
    }

    private fun observeSettings(viewModel: DecoderViewModel, prefs: SharedPreferences) {
        viewModel.codeType.observe(viewLifecycleOwner, Observer {
            prefs.edit().putInt(getString(R.string.decoder_channels_codetype_key), it).apply()
        })

        viewModel.codeLength.observe(viewLifecycleOwner, Observer {
            prefs.edit().putInt(getString(R.string.decoder_code_length_key), it).apply()
        })

        viewModel.threshold.observe(viewLifecycleOwner, Observer {
            prefs.edit().putFloat(getString(R.string.decoder_threshold_key), it).apply()
        })

        viewModel.channelCount.observe(viewLifecycleOwner, Observer {
            prefs.edit().putInt(getString(R.string.decoder_channels_count_key), it).apply()
        })
    }

    private fun setFieldsValidation() {
        decoder_code_length.doOnTextChanged { text, _, _, _ ->
            if (viewModel.parseChannelCount(text.toString()) > 0) {
                decoder_code_length_layout.error = null
            } else {
                decoder_code_length_layout.error = getString(R.string.error_positive_num)
            }
        }

        decoder_threshold.doOnTextChanged { text, _, _, _ ->
            if (viewModel.parseThreshold(text.toString()) >= 0) {
                decoder_threshold_layout.error = null
            } else {
                decoder_threshold_layout.error = getString(R.string.error_num)
            }
        }

        decoder_channel_count.doOnTextChanged { text, _, _, _ ->
            if (viewModel.parseChannelCount(text.toString()) > 0) {
                decoder_channel_count_layout.error = null
            } else {
                decoder_channel_count_layout.error = getString(R.string.error_positive_num)
            }
        }
    }

    private fun decodeChannels(isAuto: Boolean = false) {
        val channelCount = decoder_channel_count.text.toString()
        val codeLength = decoder_code_length.text.toString()
        val codeType = decoder_channel_code_type.text.toString()
        val threshold = decoder_threshold.text.toString()

        val isSuccess = if (isAuto) {
            viewModel.setupAutoDecoding(codeLength, codeType, threshold)
        } else {
            viewModel.setupDecoding(channelCount, codeLength, codeType, threshold)
        }
        if (!isSuccess) {
            Toast.makeText(requireContext(), getString(R.string.enter_valid_data), Toast.LENGTH_SHORT).show()
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

    private fun setupViewByMode(auto: Boolean) {
        isAutoDetection = auto
        if (auto) {
            View.GONE.let {
                decoder_channel_code_layout.visibility = it
                add_channel_btn.visibility = it
                decoder_channel_count_layout.visibility = it
            }
        } else {
            View.VISIBLE.let {
                decoder_channel_code_layout.visibility = it
                add_channel_btn.visibility = it
                decoder_channel_count_layout.visibility = it
            }
        }
    }

}
