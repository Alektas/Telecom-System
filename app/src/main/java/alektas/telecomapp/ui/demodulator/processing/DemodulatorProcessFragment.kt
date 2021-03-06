package alektas.telecomapp.ui.demodulator.processing

import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import alektas.telecomapp.R
import alektas.telecomapp.domain.entities.contracts.CdmaContract
import alektas.telecomapp.domain.entities.contracts.QpskContract
import alektas.telecomapp.utils.SystemUtils
import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.Observer
import com.jjoe64.graphview.series.LineGraphSeries
import kotlinx.android.synthetic.main.demodulator_process_fragment.*

class DemodulatorProcessFragment : Fragment() {
    private lateinit var prefs: SharedPreferences

    companion object {
        private const val TAG = "DemodulatorProcess"
        fun newInstance() = DemodulatorProcessFragment()
    }

    private lateinit var viewModel: DemodulatorProcessViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.demodulator_process_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(DemodulatorProcessViewModel::class.java)
        prefs = requireContext().getSharedPreferences(
            getString(R.string.settings_demodulator_key),
            Context.MODE_PRIVATE
        )
        setFieldsValidation()
        setInitValues(prefs)
        observeSettings(viewModel, prefs)

        process_data_speed.setOnEditorActionListener { _, _, _ ->
            process_btn.performClick()
        }

        process_btn.setOnClickListener {
            SystemUtils.hideKeyboard(this)
            processData()
        }

        viewModel.outputSignalData.observe(viewLifecycleOwner, Observer {
            sum_data_chart.removeAllSeries()
            sum_data_chart.addSeries(LineGraphSeries(it))
        })
    }

    private fun processData() {
        val delayCompensation = process_delay_compensation.text.toString()
        val frameLength = process_frame_length.text.toString()
        val dataSpeed = process_data_speed.text.toString()
        val codeLength = process_code_length.text.toString()

        val isSuccess = viewModel.processData(delayCompensation, frameLength, dataSpeed, codeLength)
        if (!isSuccess) {
            Toast.makeText(
                requireContext(),
                getString(R.string.enter_valid_data),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun setFieldsValidation() {
        process_delay_compensation.doOnTextChanged { text, _, _, _ ->
            if (viewModel.parseDelayCompensation(text.toString()) >= 0) {
                process_delay_compensation_layout.error = null
            } else {
                process_delay_compensation_layout.error =
                    getString(R.string.error_from_zero_to_one)
            }
        }

        process_frame_length.doOnTextChanged { text, _, _, _ ->
            if (viewModel.parseFrameLength(text.toString()) > 0) {
                process_frame_length_layout.error = null
            } else {
                process_frame_length_layout.error = getString(R.string.error_positive_num)
            }
        }

        process_code_length.doOnTextChanged { text, _, _, _ ->
            if (viewModel.parseCodeLength(text.toString()) > 0) {
                process_code_length_layout.error = null
            } else {
                process_code_length_layout.error = getString(R.string.error_positive_num)
            }
        }

        process_data_speed.doOnTextChanged { text, _, _, _ ->
            if (viewModel.parseDataspeed(text.toString()) > 0) {
                process_data_speed_layout.error = null
            } else {
                process_data_speed_layout.error = getString(R.string.error_positive_num_decimal)
            }
        }
    }

    private fun setInitValues(prefs: SharedPreferences) {
        prefs.getFloat(
            getString(R.string.demodulator_process_delay_compensation_key),
            QpskContract.DEFAULT_FILTERS_DELAY_COMPENSATION
        ).let {
            process_delay_compensation.setText(it.toString())
        }

        prefs.getFloat(
            getString(R.string.demodulator_process_dataspeed_key),
            (1.0e-3 / QpskContract.DEFAULT_DATA_BIT_TIME).toFloat()
        ).let {
            process_data_speed.setText(it.toString())
        }

        prefs.getInt(
            getString(R.string.demodulator_process_frame_length_key),
            CdmaContract.DEFAULT_FRAME_SIZE
        ).let {
            process_frame_length.setText(it.toString())
        }

        prefs.getInt(
            getString(R.string.demodulator_process_code_length_key),
            CdmaContract.DEFAULT_CHANNEL_CODE_SIZE
        ).let {
            process_code_length.setText(it.toString())
        }
    }

    private fun observeSettings(viewModel: DemodulatorProcessViewModel, prefs: SharedPreferences) {
        viewModel.frameLength.observe(viewLifecycleOwner, Observer {
            prefs.edit().putInt(getString(R.string.demodulator_process_frame_length_key), it)
                .apply()
        })

        viewModel.codeLength.observe(viewLifecycleOwner, Observer {
            prefs.edit().putInt(getString(R.string.demodulator_process_code_length_key), it).apply()
        })

        viewModel.dataSpeed.observe(viewLifecycleOwner, Observer {
            prefs.edit().putFloat(getString(R.string.demodulator_process_dataspeed_key), it).apply()
        })

        viewModel.delayCompensation.observe(viewLifecycleOwner, Observer {
            prefs.edit()
                .putFloat(getString(R.string.demodulator_process_delay_compensation_key), it)
                .apply()
        })
    }

}