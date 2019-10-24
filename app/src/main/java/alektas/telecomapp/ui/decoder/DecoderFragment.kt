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
import alektas.telecomapp.utils.SystemUtils
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.jjoe64.graphview.series.LineGraphSeries
import kotlinx.android.synthetic.main.decoder_fragment.*
import kotlinx.android.synthetic.main.decoder_fragment.channel_code_type
import kotlinx.android.synthetic.main.decoder_fragment.channel_count
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
        val channelAdapter = ChannelAdapter(this)
        channel_list.adapter = channelAdapter
        channel_list.layoutManager = LinearLayoutManager(requireContext())

        channel_code.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                add_channel_btn.performClick()
                true
            } else false
        }

        add_channel_btn.setOnClickListener {
            val codeString = channel_code.text.toString()
            viewModel.decodeCustomChannel(codeString)
        }

        generate_channels_btn.setOnClickListener {
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
        val adapter = ArrayAdapter(
            requireContext(),
            R.layout.support_simple_spinner_dropdown_item,
            CodeGenerator.codeNames.values.toTypedArray()
        )
        channel_code_type.setAdapter<ArrayAdapter<String>>(adapter)

        channel_code_type.setOnItemClickListener { parent, _, position, _ ->
            val type = parent.getItemAtPosition(position)
            if (type is String) {
                selectedCodeType = type
            }
        }

        channel_code_type.setOnTouchListener { v, _ ->
            SystemUtils.hideKeyboard(this)
            (v as AutoCompleteTextView).showDropDown()
            false
        }
    }

    private fun decodeChannels() {
        SystemUtils.hideKeyboard(this)

        viewModel.inputSignalData.value?.let {
            val channelCount = try {
                val c = channel_count.text.toString().toInt()
                if (c < 0) throw NumberFormatException()
                else c
            } catch (e: NumberFormatException) {
                val msg = "Количество каналов должно быть положительным целым числом"
                Log.e(TAG, msg, e)
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                0
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
