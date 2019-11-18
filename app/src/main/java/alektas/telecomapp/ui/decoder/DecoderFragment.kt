package alektas.telecomapp.ui.decoder

import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import alektas.telecomapp.R
import alektas.telecomapp.data.CodeGenerator
import alektas.telecomapp.domain.entities.CdmaContract
import alektas.telecomapp.domain.entities.ChannelData
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
import com.jjoe64.graphview.series.LineGraphSeries
import kotlinx.android.synthetic.main.decoder_fragment.*
import kotlinx.android.synthetic.main.decoder_fragment.channel_list
import kotlinx.android.synthetic.main.decoder_fragment.generate_channels_btn

class DecoderFragment : Fragment(), ChannelController {
    private lateinit var viewModel: DecoderViewModel
    private lateinit var prefs: SharedPreferences

    companion object {
        fun newInstance() = DecoderFragment()

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.decoder_fragment, container, false)
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
            val codeString = decoder_channel_code.text.toString()
            if (codeString.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    "Введите код из нулей и единиц",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            viewModel.decodeCustomChannel(codeString)
        }

        generate_channels_btn.setOnClickListener {
            SystemUtils.hideKeyboard(this)
            decodeChannels()
        }

        viewModel.channels.observe(viewLifecycleOwner, Observer {
            channelAdapter.channels = it
        })


        viewModel.inputSignalData.observe(viewLifecycleOwner, Observer {
            input_chart.removeAllSeries()
            input_chart.addSeries(LineGraphSeries(it))
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

        viewModel.channelCount.observe(viewLifecycleOwner, Observer {
            prefs.edit().putInt(getString(R.string.decoder_channels_count_key), it).apply()
        })
    }

    private fun setFieldsValidation() {
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
        val codeType = decoder_channel_code_type.text.toString()

        if (decoder_channel_count_layout.error != null ||
            channelCount.isEmpty() ||
            codeType.isEmpty()
        ) {
            Toast.makeText(requireContext(), "Введите корректные данные", Toast.LENGTH_SHORT).show()
            return
        }

        viewModel.inputSignalData.value?.let {
            viewModel.decodeChannels(channelCount, codeType)
            return
        }

        Toast.makeText(
            requireContext(),
            "Сначала необходимо настроить источник данных",
            Toast.LENGTH_SHORT
        ).show()
    }

}
