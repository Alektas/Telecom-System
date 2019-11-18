package alektas.telecomapp.ui.demodulators.processing

import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import alektas.telecomapp.R
import alektas.telecomapp.domain.entities.CdmaContract
import alektas.telecomapp.domain.entities.QpskContract
import alektas.telecomapp.utils.SystemUtils
import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.lifecycle.Observer
import com.jjoe64.graphview.series.LineGraphSeries
import kotlinx.android.synthetic.main.demodulator_process_channel_fragment.*
import kotlinx.android.synthetic.main.demodulator_process_fragment.*

class DemodulatorProcessFragment : Fragment() {
    private lateinit var pagerAdapter: ProcessPagerAdapter
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        pagerAdapter = ProcessPagerAdapter(
            childFragmentManager,
            FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT
        )
        demodulation_process_pager.adapter = pagerAdapter
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

        process_threshold.setOnEditorActionListener { _, _, _ ->
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
        val frameLength = process_frame_length.text.toString()
        val dataSpeed = process_data_speed.text.toString()
        val codeLength = process_code_length.text.toString()
        val threshold = process_threshold.text.toString()

        if (process_code_length_layout.error != null ||
            process_data_speed_layout.error != null ||
            process_frame_length_layout.error != null ||
            process_threshold_layout.error != null ||
            codeLength.isEmpty() ||
            dataSpeed.isEmpty() ||
            frameLength.isEmpty() ||
            threshold.isEmpty()
        ) {
            Toast.makeText(requireContext(), "Введите корректные данные", Toast.LENGTH_SHORT).show()
            return
        }

        viewModel.processData(frameLength, dataSpeed, codeLength, threshold)
    }

    private fun setFieldsValidation() {
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

        process_threshold.doOnTextChanged { text, _, _, _ ->
            if (viewModel.parseThreshold(text.toString()) >= 0) {
                process_threshold_layout.error = null
            } else {
                process_threshold_layout.error = getString(R.string.error_num)
            }
        }
    }

    private fun setInitValues(prefs: SharedPreferences) {
        prefs.getFloat(
            getString(R.string.demodulator_process_dataspeed_key),
            (1.0e-3 / QpskContract.DEFAULT_DATA_BIT_TIME).toFloat()
        ).let {
            process_data_speed.setText(it.toString())
        }

        prefs.getFloat(
            getString(R.string.demodulator_process_threshold_key),
            QpskContract.DEFAULT_SIGNAL_THRESHOLD.toFloat()
        ).let {
            process_threshold.setText(it.toString())
        }

        prefs.getInt(
            getString(R.string.demodulator_process_frame_length_key),
            CdmaContract.DEFAULT_FRAME_SIZE
        ).let {
            process_frame_length.setText(it.toString())
        }

        prefs.getInt(
            getString(R.string.demodulator_process_code_length_key),
            CdmaContract.DEFAULT_CODE_SIZE
        ).let {
            process_code_length.setText(it.toString())
        }
    }

    private fun observeSettings(viewModel: DemodulatorProcessViewModel, prefs: SharedPreferences) {
        viewModel.frameLength.observe(viewLifecycleOwner, Observer {
            prefs.edit().putInt(getString(R.string.demodulator_process_frame_length_key), it).apply()
        })

        viewModel.codeLength.observe(viewLifecycleOwner, Observer {
            prefs.edit().putInt(getString(R.string.demodulator_process_code_length_key), it).apply()
        })

        viewModel.dataSpeed.observe(viewLifecycleOwner, Observer {
            prefs.edit().putFloat(getString(R.string.demodulator_process_dataspeed_key), it).apply()
        })

        viewModel.threshold.observe(viewLifecycleOwner, Observer {
            prefs.edit().putFloat(getString(R.string.demodulator_process_threshold_key), it).apply()
        })
    }

}

class ProcessPagerAdapter(fm: FragmentManager, behavior: Int) :
    FragmentPagerAdapter(fm, behavior) {
    override fun getItem(position: Int): Fragment {
        return ChannelFragment().apply {
            arguments = bundleOf(
                ARG_CHANNEL to when (position) {
                    0 -> CHANNEL_I
                    else -> CHANNEL_Q
                }
            )
        }
    }

    override fun getCount(): Int = 2

    override fun getPageTitle(position: Int): CharSequence? {
        return when (position) {
            0 -> "I-канал"
            else -> "Q-канал"
        }
    }

}

private const val ARG_CHANNEL = "channel"
private const val CHANNEL_I = 0
private const val CHANNEL_Q = 1

class ChannelFragment : Fragment() {
    private lateinit var viewModel: DemodulatorProcessViewModel
    private var type: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.demodulator_process_channel_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        arguments?.takeIf { it.containsKey(ARG_CHANNEL) }?.apply {
            type = getInt(ARG_CHANNEL)
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(DemodulatorProcessViewModel::class.java)

        if (type == CHANNEL_I) {
            viewModel.iSignalData.observe(viewLifecycleOwner, Observer {
                signal_chart.removeAllSeries()
                signal_chart.addSeries(LineGraphSeries(it))
            })

            viewModel.iBitsData.observe(viewLifecycleOwner, Observer {
                data_chart.removeAllSeries()
                data_chart.addSeries(LineGraphSeries(it))
            })
        } else {
            viewModel.qSignalData.observe(viewLifecycleOwner, Observer {
                signal_chart.removeAllSeries()
                signal_chart.addSeries(LineGraphSeries(it))
            })

            viewModel.qBitsData.observe(viewLifecycleOwner, Observer {
                data_chart.removeAllSeries()
                data_chart.addSeries(LineGraphSeries(it))
            })
        }
    }
}
