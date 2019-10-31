package alektas.telecomapp.ui.datasource

import alektas.telecomapp.data.CodeGenerator
import alektas.telecomapp.domain.entities.ChannelData
import alektas.telecomapp.ui.utils.SimpleArrayAdapter
import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import alektas.telecomapp.utils.SystemUtils
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import kotlinx.android.synthetic.main.data_source_fragment.*
import java.lang.NumberFormatException

class DataSourceFragment : Fragment(), ChannelController {
    private lateinit var viewModel: DataSourceViewModel
    private var selectedCodeType: String? = null

    companion object {
        const val TAG = "DataSourceFragment"
        fun newInstance() = DataSourceFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(alektas.telecomapp.R.layout.data_source_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(DataSourceViewModel::class.java)
        setupCodeTypesDropdown()
        setInitValues(viewModel)

        val channelAdapter = ChannelAdapter(this)
        channel_list.adapter = channelAdapter
        channel_list.layoutManager = LinearLayoutManager(requireContext())

        ether_snr.setOnEditorActionListener { tv, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                changeNoisePower(tv.text.toString())
                SystemUtils.hideKeyboard(this)
                true
            } else false
        }

        frame_length.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                generate_channels_btn.performClick()
                true
            } else false
        }

        generate_channels_btn.setOnClickListener {
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

    private fun setInitValues(viewModel: DataSourceViewModel) {
        viewModel.initNoiseSnr.observe(viewLifecycleOwner, Observer {
            ether_snr.setText(it.toString())
        })

        viewModel.initCodeType.observe(viewLifecycleOwner, Observer {
            channel_code_type.setText(CodeGenerator.getCodeName(it))
        })

        viewModel.initChannelCount.observe(viewLifecycleOwner, Observer {
            channel_count.setText(it.toString())
        })

        viewModel.initFrameSize.observe(viewLifecycleOwner, Observer {
            frame_length.setText(it.toString())
        })
    }

    override fun removeChannel(channel: ChannelData) {
        viewModel.removeChannel(channel)
    }

    override fun showChannelDetails(channel: ChannelData) { }

    private fun setupCodeTypesDropdown() {
        val adapter = SimpleArrayAdapter(
            requireContext(),
            alektas.telecomapp.R.layout.support_simple_spinner_dropdown_item,
            CodeGenerator.codeNames.values.toList()
        )
        channel_code_type.setAdapter<ArrayAdapter<String>>(adapter)

        channel_code_type.setOnItemClickListener { parent, _, position, _ ->
            val type = parent.getItemAtPosition(position)
            if (type is String) {
                selectedCodeType = type
            }
        }

        val defaultType = CodeGenerator.getCodeName(CodeGenerator.WALSH)
        channel_code_type.setText(defaultType)
        selectedCodeType = defaultType

        channel_code_type_layout.setOnTouchListener { v, _ ->
            SystemUtils.hideKeyboard(this)
            (v as AutoCompleteTextView).showDropDown()
            false
        }
    }

    private fun generateChannels() {
        SystemUtils.hideKeyboard(this)

        val channelCount = try {
            val c = channel_count.text.toString().toInt()
            if (c < 0) throw NumberFormatException()
            else c
        } catch (e: NumberFormatException) {
            val msg = "Количество каналов должно быть положительным целым числом"
            Log.e(TAG, msg, e)
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
            return
        }
        val frameLength = try {
            val l = frame_length.text.toString().toInt()
            if (l <= 0) throw NumberFormatException()
            else l
        } catch (e: NumberFormatException) {
            val msg = "Длина фрейма должна быть положительным целым числом"
            Log.e(TAG, msg, e)
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
            return
        }
        val codeType = selectedCodeType?.let { CodeGenerator.getCodeTypeId(it) } ?: 0
        viewModel.generateChannels(channelCount, frameLength, codeType)
    }

    private fun changeNoisePower(text: String) {
        val snr = try {
            text.toDouble()
        } catch (e: NumberFormatException) {
            val msg = "Отношение сигнал/шум должно быть числом"
            Log.e(TAG, msg, e)
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
            0.0
        }
        viewModel.setNoise(snr)
    }

}
