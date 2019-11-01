package alektas.telecomapp.ui.statistic

import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import alektas.telecomapp.R
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.statistic_fragment.*

class StatisticFragment : Fragment() {

    companion object {
        fun newInstance() = StatisticFragment()
    }

    private lateinit var viewModel: StatisticViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.statistic_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(StatisticViewModel::class.java)

        viewModel.channelCountData.observe(viewLifecycleOwner, Observer {
            statistic_channel_count.text = it.toString()
        })

        viewModel.bitTransmittedData.observe(viewLifecycleOwner, Observer {
            statistic_bits_transmitted.text = it.toString()
        })

        viewModel.bitReceivedData.observe(viewLifecycleOwner, Observer {
            statistic_bits_received.text = it.toString()
        })

        viewModel.errorBitCountData.observe(viewLifecycleOwner, Observer {
            statistic_error_bits_count.text = it.toString()
        })

        viewModel.berData.observe(viewLifecycleOwner, Observer {
            statistic_ber.text = it.toString()
        })
    }

}
