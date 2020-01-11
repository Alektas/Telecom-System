package alektas.telecomapp.ui.demodulator.filter.qchannel

import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import alektas.telecomapp.R
import androidx.lifecycle.Observer
import com.jjoe64.graphview.series.LineGraphSeries
import kotlinx.android.synthetic.main.qchannel_fragment.*

class QChannelFragment : Fragment() {

    companion object {
        fun newInstance() = QChannelFragment()
    }

    private lateinit var viewModel: QChannelViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.qchannel_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(QChannelViewModel::class.java)

        viewModel.channelQData.observe(viewLifecycleOwner, Observer {
            q_signal_chart.addSeries(LineGraphSeries(it))
        })

        viewModel.filteredChannelQData.observe(viewLifecycleOwner, Observer {
            filtered_q_signal_chart.addSeries(LineGraphSeries(it))
        })
    }

}
