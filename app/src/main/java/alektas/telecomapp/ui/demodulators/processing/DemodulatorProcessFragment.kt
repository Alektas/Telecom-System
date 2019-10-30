package alektas.telecomapp.ui.demodulators.processing

import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import alektas.telecomapp.R
import android.util.Log
import android.widget.Toast
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

        process_frame_length.doOnTextChanged { text, _, _, _ ->
            try {
                val frameLength = text.toString().toInt()
                if (frameLength <= 0) throw NumberFormatException()
                viewModel.setFrameLength(frameLength)
            } catch (e: NumberFormatException) {
                val msg = "Длина фрейма должна быть положительным целым числом"
                Log.e(TAG, msg, e)
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
            }
        }

        process_code_length.doOnTextChanged { text, _, _, _ ->
            try {
                val codeLength = text.toString().toInt()
                if (codeLength <= 0) throw NumberFormatException()
                viewModel.setCodeLength(codeLength)
            } catch (e: NumberFormatException) {
                val msg = "Длина кода должна быть положительным целым числом"
                Log.e(TAG, msg, e)
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
            }
        }

        process_bit_time.doOnTextChanged { text, _, _, _ ->
            try {
                val bitTime = text.toString().toDouble()
                if (bitTime <= 0) throw NumberFormatException()
                viewModel.setBitTime(bitTime)
            } catch (e: NumberFormatException) {
                val msg = "Время бита должно быть положительным числом"
                Log.e(TAG, msg, e)
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
            }
        }

        process_threshold.doOnTextChanged { text, _, _, _ ->
            try {
                val threshold = text.toString().toDouble()
                if (threshold < 0) throw NumberFormatException()
                viewModel.setThreshold(threshold)
            } catch (e: NumberFormatException) {
                val msg = "Пороговый уровень сигнала должен быть неотрицательным числом"
                Log.e(TAG, msg, e)
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.outputSignalData.observe(viewLifecycleOwner, Observer {
            sum_data_chart.removeAllSeries()
            sum_data_chart.addSeries(LineGraphSeries(it))
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
