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
import alektas.telecomapp.domain.entities.ChannelData
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
            getString(R.string.settings_source_key),
            Context.MODE_PRIVATE
        )
        setupCodeTypesDropdown()
        setFieldsValidation()
        setInitValues(prefs)
        observeSettings(viewModel, prefs)

        val channelAdapter = ChannelAdapter(this)
        channel_list.adapter = channelAdapter
        channel_list.layoutManager = LinearLayoutManager(requireContext())

        decoder_channel_code.setOnEditorActionListener { _, _, _ ->
            add_channel_btn.performClick()
            false
        }

        decoder_channel_count.setOnEditorActionListener { _, _, _ ->
            generate_channels_btn.performClick()
            false
        }

        add_channel_btn.setOnClickListener {
            SystemUtils.hideKeyboard(this)
            decodeCustomChannel()
        }

        generate_channels_btn.setOnClickListener {
            SystemUtils.hideKeyboard(this)
            decodeChannels()
        }

        viewModel.channels.observe(viewLifecycleOwner, Observer {
            channelAdapter.channels = it
        })


        viewModel.inputSignalData.observe(viewLifecycleOwner, Observer {
            graphPoints.resetData(it)
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
        decoder_channel_code_type.setAdapter<ArrayAdapter<String>>(adapter)

        val defaultType = CodeGenerator.getCodeName(CodeGenerator.WALSH)
        decoder_channel_code_type.setText(defaultType)

        decoder_channel_code_type_layout.setOnTouchListener { v, _ ->
            SystemUtils.hideKeyboard(this)
            (v as AutoCompleteTextView).showDropDown()
            false
        }
    }

    private fun setInitValues(prefs: SharedPreferences) {
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
            QpskContract.DEFAULT_SIGNAL_THRESHOLD.toFloat()
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

    private fun decodeChannels() {
        val channelCount = decoder_channel_count.text.toString()
        val codeLength = decoder_code_length.text.toString()
        val codeType = decoder_channel_code_type.text.toString()
        val threshold = decoder_threshold.text.toString()

        if (decoder_channel_count_layout.error != null ||
            decoder_code_length_layout.error != null ||
            decoder_threshold_layout.error != null ||
            channelCount.isEmpty() ||
            codeLength.isEmpty() ||
            codeType.isEmpty() ||
            threshold.isEmpty()
        ) {
            Toast.makeText(requireContext(), "Введите корректные данные", Toast.LENGTH_SHORT).show()
            return
        }

        viewModel.inputSignalData.value?.let {
            viewModel.decodeChannels(channelCount, codeLength, codeType, threshold)
            return
        }

        Toast.makeText(
            requireContext(),
            "Сначала необходимо настроить источник данных",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun decodeCustomChannel() {
        val codeString = decoder_channel_code.text.toString()
        if (codeString.isEmpty()) {
            Toast.makeText(
                requireContext(),
                "Введите код из нулей и единиц",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        val thresholdString = decoder_threshold.text.toString()
        val threshold = viewModel.parseThreshold(thresholdString)
        if (threshold < 0) {
            Toast.makeText(
                requireContext(),
                "Введите неотрицательное пороговое значение",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        viewModel.decodeCustomChannel(codeString, threshold)
    }

}
