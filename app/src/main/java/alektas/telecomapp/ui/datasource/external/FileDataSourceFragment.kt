package alektas.telecomapp.ui.datasource.external

import alektas.telecomapp.R
import alektas.telecomapp.domain.entities.contracts.QpskContract
import alektas.telecomapp.domain.entities.Simulator
import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import alektas.telecomapp.utils.SystemUtils
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.widget.Toast
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.Observer
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import kotlinx.android.synthetic.main.file_data_source_fragment.*

private const val DEFAULT_ADC_RESOLUTION = 8

class FileDataSourceFragment : Fragment() {
    private lateinit var viewModel: FileDataSourceViewModel
    private lateinit var prefs: SharedPreferences
    private val graphPoints = LineGraphSeries<DataPoint>()

    companion object {
        fun newInstance() = FileDataSourceFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.file_data_source_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<GraphView>(R.id.ether_chart).apply {
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
        viewModel = ViewModelProviders.of(requireActivity()).get(FileDataSourceViewModel::class.java)
        prefs = requireContext().getSharedPreferences(
            getString(R.string.settings_source_key),
            MODE_PRIVATE
        )
        setFieldsValidation()
        setInitValues(prefs)
        observeSettings(viewModel, prefs)

        adc_frequency.setOnEditorActionListener { tv, _, _ ->
            viewModel.setAdcFrequency(tv.text.toString())
            false
        }

        receive_channels_btn.setOnClickListener {
            SystemUtils.hideKeyboard(this)
            receiveChannels()
        }

        viewModel.ether.observe(viewLifecycleOwner, Observer {
            graphPoints.resetData(it)
        })
    }

    private fun setInitValues(prefs: SharedPreferences) {
        val defaultAdcFreq = (1.0e-6 * Simulator.DEFAULT_SAMPLING_RATE).toFloat()
        prefs.getFloat(
            getString(R.string.usb_source_adc_freq_key),
            defaultAdcFreq
        ).let {
            adc_frequency.setText(String.format("%.3f", it))
        }

        prefs.getInt(
            getString(R.string.usb_source_adc_resolution_key),
            DEFAULT_ADC_RESOLUTION
        ).let {
            adc_resolution.setText(it.toString())
        }
    }

    private fun observeSettings(viewModel: FileDataSourceViewModel, prefs: SharedPreferences) {
        viewModel.adcFrequency.observe(viewLifecycleOwner, Observer {
            prefs.edit().putFloat(getString(R.string.usb_source_adc_freq_key), it.toFloat()).apply()
        })

        viewModel.adcResolution.observe(viewLifecycleOwner, Observer {
            prefs.edit().putInt(getString(R.string.usb_source_adc_resolution_key), it).apply()
        })
    }

    private fun setFieldsValidation() {
        adc_frequency.doOnTextChanged { text, _, _, _ ->
            if (viewModel.parseFrequency(text.toString()) > 0) {
                adc_frequency_layout.error = null
            } else {
                adc_frequency_layout.error = getString(R.string.error_positive_num_decimal)
            }
        }

        adc_resolution.doOnTextChanged { text, _, _, _ ->
            if (viewModel.parseResolution(text.toString()) > 0) {
                adc_resolution_layout.error = null
            } else {
                adc_resolution_layout.error = getString(R.string.error_positive_num)
            }
        }
    }

    private fun receiveChannels() {
        val adcFrequency = adc_frequency.text.toString()
        val adcResolution = adc_resolution.text.toString()

        if (adc_frequency_layout.error != null ||
            adc_resolution_layout.error != null
        ) {
            Toast.makeText(
                requireContext(),
                getString(R.string.enter_valid_data),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val success = viewModel.processData(adcResolution, adcFrequency)
        if (!success) {
            Toast.makeText(
                requireContext(),
                "Ошибка чтения файла. Убедитесь, что файл содержит корректные данные.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

}
