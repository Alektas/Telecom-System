package alektas.telecomapp.ui.demodulator.filter.ichannel

import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import alektas.telecomapp.R
import androidx.lifecycle.Observer
import com.jjoe64.graphview.series.LineGraphSeries
import kotlinx.android.synthetic.main.ichannel_fragment.*

class IChannelFragment : Fragment() {

    companion object {
        fun newInstance() = IChannelFragment()
    }

    private lateinit var viewModel: IChannelViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.ichannel_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(IChannelViewModel::class.java)

        viewModel.channelIData.observe(viewLifecycleOwner, Observer {
            i_signal_chart.addSeries(LineGraphSeries(it))
        })

        viewModel.filteredChannelIData.observe(viewLifecycleOwner, Observer {
            filtered_i_signal_chart.addSeries(LineGraphSeries(it))
        })
    }

}
