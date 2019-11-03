package alektas.telecomapp.ui.datasource

import alektas.telecomapp.R
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
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import kotlinx.android.synthetic.main.data_source_fragment.*
import java.lang.NumberFormatException

class DataSourceFragment : Fragment(), ChannelController {
    private lateinit var viewModel: DataSourceViewModel

    companion object {
        const val TAG = "DataSourceFragment"
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
        setupCodeTypesDropdown()
        setFieldsValidation()
        setInitValues(viewModel)

        val channelAdapter = ChannelAdapter(this)
        channel_list.adapter = channelAdapter
        channel_list.layoutManager = LinearLayoutManager(requireContext())

        ether_snr.setOnEditorActionListener { tv, _, _ ->
            SystemUtils.hideKeyboard(this)
            changeNoisePower(tv.text.toString())
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

    private fun setInitValues(viewModel: DataSourceViewModel) {
        viewModel.initNoiseSnr.observe(viewLifecycleOwner, Observer {
            ether_snr.setText(it.toString())
        })

        viewModel.initCodeType.observe(viewLifecycleOwner, Observer {
            source_channel_code_type.setText(CodeGenerator.getCodeName(it))
        })

        viewModel.initDataSpeed.observe(viewLifecycleOwner, Observer {
            source_data_speed.setText(it.toString())
        })

        viewModel.initChannelCount.observe(viewLifecycleOwner, Observer {
            source_channel_count.setText(it.toString())
        })

        viewModel.initFrameSize.observe(viewLifecycleOwner, Observer {
            source_frame_length.setText(it.toString())
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
            (v as AutoCompleteTextView).showDropDown()
            false
        }
    }

    private fun generateChannels() {
        val channelCount = source_channel_count.text.toString()
        val dataSpeed = source_data_speed.text.toString()
        val frameLength = source_frame_length.text.toString()
        val codeType = source_channel_code_type.text.toString()

        if (source_channel_count_layout.error != null ||
            source_data_speed_layout.error != null ||
            source_frame_length_layout.error != null ||
            channelCount.isEmpty() ||
            dataSpeed.isEmpty() ||
            frameLength.isEmpty() ||
            codeType.isEmpty()
        ) {
            Toast.makeText(requireContext(), "Введите корректные данные", Toast.LENGTH_SHORT).show()
            return
        }

        viewModel.generateChannels(channelCount, dataSpeed, frameLength, codeType)
    }

    private fun setFieldsValidation() {
        source_channel_count.doOnTextChanged { text, _, _, _ ->
            if (viewModel.parseChannelCount(text.toString()) > 0) {
                source_channel_count_layout.error = null
            } else {
                source_channel_count_layout.error = getString(R.string.error_positive_num)
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
