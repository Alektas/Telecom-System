package alektas.telecomapp.ui.chart

import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer

import kotlinx.android.synthetic.main.chart_fragment.*
import com.jjoe64.graphview.series.LineGraphSeries
import com.jjoe64.graphview.series.DataPoint


class ChartFragment : Fragment() {

    companion object {
        const val SIGNAL_MAGNITUDE = 1.5
        const val SIGNAL_DURATION = 0.0004
        const val SPECTRUM_HEIGHT = 800.0
        const val SPECTRUM_WIDTH = 500.0
        fun newInstance() = ChartFragment()
    }

    private lateinit var viewModel: ChartViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(alektas.telecomapp.R.layout.chart_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(ChartViewModel::class.java)

        data_chart.viewport.apply {
            isScalable = true
            isXAxisBoundsManual = true
            setMinX(0.0)
            setMaxX(SIGNAL_DURATION)
            isYAxisBoundsManual = true
            setMinY(-SIGNAL_MAGNITUDE)
            setMaxY(SIGNAL_MAGNITUDE)
        }

        input_signal_chart.viewport.apply {
            isScalable = true
            isXAxisBoundsManual = true
            setMinX(0.0)
            setMaxX(SIGNAL_DURATION)
            isYAxisBoundsManual = true
            setMinY(-SIGNAL_MAGNITUDE)
            setMaxY(SIGNAL_MAGNITUDE)
        }

        output_signal_chart.viewport.apply {
            isScalable = true
            isXAxisBoundsManual = true
            setMinX(0.0)
            setMaxX(SIGNAL_DURATION)
            isYAxisBoundsManual = true
            setMinY(-SIGNAL_MAGNITUDE)
            setMaxY(SIGNAL_MAGNITUDE)
        }

        spectrum_chart.viewport.apply {
            isScalable = true
            isXAxisBoundsManual = true
            setMinX(0.0)
            setMaxX(SPECTRUM_WIDTH)
            isYAxisBoundsManual = true
            setMinY(0.0)
            setMaxY(SPECTRUM_HEIGHT)
        }

        viewModel.inputSignalData.observe(viewLifecycleOwner, Observer {
            input_signal_chart.addSeries(LineGraphSeries<DataPoint>(it))
        })

        viewModel.outputSignalData.observe(viewLifecycleOwner, Observer {
            output_signal_chart.addSeries(LineGraphSeries<DataPoint>(it))
        })

        viewModel.spectrumData.observe(viewLifecycleOwner, Observer {
            spectrum_chart.addSeries(LineGraphSeries<DataPoint>(it))
        })

        viewModel.dataSignalData.observe(viewLifecycleOwner, Observer {
            data_chart.addSeries(LineGraphSeries<DataPoint>(it))
        })

        viewModel.constellationData.observe(viewLifecycleOwner, Observer {
            constellation_chart.setData(it)
        })
    }

}
