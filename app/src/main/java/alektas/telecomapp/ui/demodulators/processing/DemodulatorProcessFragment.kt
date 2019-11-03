package alektas.telecomapp.ui.demodulators.processing

import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import alektas.telecomapp.R
import alektas.telecomapp.utils.SystemUtils
import androidx.core.os.bundleOf
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.lifecycle.Observer
import com.jjoe64.graphview.series.LineGraphSeries
import kotlinx.android.synthetic.main.demodulator_process_channel_fragment.*
import kotlinx.android.synthetic.main.demodulator_process_fragment.*
import java.lang.NumberFormatException

class DemodulatorProcessFragment : Fragment() {
    private lateinit var pagerAdapter: ProcessPagerAdapter

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
        setInitValues(viewModel)
        setFieldsValidation()

        process_btn.setOnClickListener {
            processData()
        }

        viewModel.outputSignalData.observe(viewLifecycleOwner, Observer {
            sum_data_chart.removeAllSeries()
            sum_data_chart.addSeries(LineGraphSeries(it))
        })
    }

    private fun processData() {
        SystemUtils.hideKeyboard(this)

        val frameLength = process_frame_length.text.toString()
        val dataSpeed = process_data_speed.text.toString()
        val codeLength = process_code_length.text.toString()
        val threshold = process_threshold.text.toString()

        viewModel.processData(frameLength, dataSpeed, codeLength, threshold)
    }

    private fun setFieldsValidation() {
        process_frame_length.doOnTextChanged { text, _, _, _ ->
            try {
                val frameLength = text.toString().toInt()
                if (frameLength <= 0) throw NumberFormatException()
                process_frame_length_layout.error = null
            } catch (e: NumberFormatException) {
                process_frame_length_layout.error = getString(R.string.error_positive_num)
            }
        }

        process_code_length.doOnTextChanged { text, _, _, _ ->
            try {
                val codeLength = text.toString().toInt()
                if (codeLength <= 0) throw NumberFormatException()
                process_code_length_layout.error = null
            } catch (e: NumberFormatException) {
                process_code_length_layout.error = getString(R.string.error_positive_num)
            }
        }

        process_data_speed.doOnTextChanged { text, _, _, _ ->
            try {
                val dataSpeed = text.toString().toDouble()
                if (dataSpeed <= 0) throw NumberFormatException()
                process_data_speed_layout.error = null
            } catch (e: NumberFormatException) {
                process_data_speed_layout.error = getString(R.string.error_positive_num_decimal)
            }
        }

        process_threshold.doOnTextChanged { text, _, _, _ ->
            try {
                val threshold = text.toString().toDouble()
                if (threshold < 0) throw NumberFormatException()
                process_threshold_layout.error = null
            } catch (e: NumberFormatException) {
                process_threshold_layout.error = getString(R.string.error_num)
            }
        }
    }

    private fun setInitValues(viewModel: DemodulatorProcessViewModel) {
        viewModel.initFrameLength.observe(viewLifecycleOwner, Observer {
            process_frame_length.setText(it.toString())
        })

        viewModel.initCodeLength.observe(viewLifecycleOwner, Observer {
            process_code_length.setText(it.toString())
        })

        viewModel.initDataSpeed.observe(viewLifecycleOwner, Observer {
            process_data_speed.setText(it.toBigDecimal().toPlainString())
        })

        viewModel.initThreshold.observe(viewLifecycleOwner, Observer {
            process_threshold.setText(it.toString())
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
