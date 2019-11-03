package alektas.telecomapp.ui.decoder

import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import alektas.telecomapp.R
import alektas.telecomapp.data.CodeGenerator
import alektas.telecomapp.domain.entities.ChannelData
import alektas.telecomapp.ui.datasource.ChannelAdapter
import alektas.telecomapp.ui.datasource.ChannelController
import alektas.telecomapp.ui.utils.SimpleArrayAdapter
import alektas.telecomapp.utils.SystemUtils
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.jjoe64.graphview.series.LineGraphSeries
import kotlinx.android.synthetic.main.decoder_fragment.*
import kotlinx.android.synthetic.main.decoder_fragment.source_channel_code_type
import kotlinx.android.synthetic.main.decoder_fragment.source_channel_count
import kotlinx.android.synthetic.main.decoder_fragment.channel_list
import kotlinx.android.synthetic.main.decoder_fragment.generate_channels_btn

class DecoderFragment : Fragment(), ChannelController {
    private var selectedCodeType: String? = null

    companion object {
        const val TAG = "DecoderFragment"
        fun newInstance() = DecoderFragment()
    }

    private lateinit var viewModel: DecoderViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.decoder_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(DecoderViewModel::class.java)
        setupCodeTypesDropdown()
        setInitValues(viewModel)

        val channelAdapter = ChannelAdapter(this)
        channel_list.adapter = channelAdapter
        channel_list.layoutManager = LinearLayoutManager(requireContext())

        channel_code.setOnEditorActionListener { _, _, _ ->
            add_channel_btn.performClick()
            false
        }

        source_channel_count.setOnEditorActionListener { _, _, _ ->
            generate_channels_btn.performClick()
            false
        }

        add_channel_btn.setOnClickListener {
            SystemUtils.hideKeyboard(this)
            val codeString = channel_code.text.toString()
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
        source_channel_code_type.setAdapter<ArrayAdapter<String>>(adapter)

        source_channel_code_type.setOnItemClickListener { parent, _, position, _ ->
            val type = parent.getItemAtPosition(position)
            if (type is String) {
                selectedCodeType = type
            }
        }

        val defaultType = CodeGenerator.getCodeName(CodeGenerator.WALSH)
        source_channel_code_type.setText(defaultType)
        selectedCodeType = defaultType

        source_channel_code_type_layout.setOnTouchListener { v, _ ->
            SystemUtils.hideKeyboard(this)
            (v as AutoCompleteTextView).showDropDown()
            false
        }
    }

    private fun setInitValues(viewModel: DecoderViewModel) {
        viewModel.initCodeType.observe(viewLifecycleOwner, Observer {
            source_channel_code_type.setText(CodeGenerator.getCodeName(it))
        })

        viewModel.initChannelCount.observe(viewLifecycleOwner, Observer {
            source_channel_count.setText(it.toString())
        })
    }

    private fun decodeChannels() {
        viewModel.inputSignalData.value?.let {
            val channelCount = try {
                val c = source_channel_count.text.toString().toInt()
                if (c < 0) throw NumberFormatException()
                else c
            } catch (e: NumberFormatException) {
                val msg = "Количество каналов должно быть положительным целым числом"
                Log.e(TAG, msg, e)
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                return
            }
            val codeType = selectedCodeType?.let { CodeGenerator.getCodeTypeId(it) } ?: 0
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
