package alektas.telecomapp.ui

import alektas.telecomapp.R
import alektas.telecomapp.domain.entities.Simulator
import alektas.telecomapp.ui.datasource.DataSourceFragment
import alektas.telecomapp.ui.decoder.DecoderFragment
import alektas.telecomapp.ui.demodulator.QpskDemodulatorFragment
import alektas.telecomapp.ui.demodulator.filter.FirFilterFragment
import alektas.telecomapp.ui.demodulator.filter.ichannel.IChannelFragment
import alektas.telecomapp.ui.demodulator.filter.qchannel.QChannelFragment
import alektas.telecomapp.ui.demodulator.generator.DemodulatorGeneratorFragment
import alektas.telecomapp.ui.demodulator.input.DemodulatorInputFragment
import alektas.telecomapp.ui.demodulator.output.DemodulatorOutputFragment
import alektas.telecomapp.ui.demodulator.processing.DemodulatorProcessFragment
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import alektas.telecomapp.ui.main.MainFragment
import alektas.telecomapp.ui.statistic.StatisticFragment
import alektas.telecomapp.ui.statistic.ber.BerFragment
import android.content.Context
import android.content.SharedPreferences
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import kotlinx.android.synthetic.main.main_activity.*

class MainActivity : AppCompatActivity() {
    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, MainFragment.newInstance())
                .commitNow()
        }

        loadSettings()

        viewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)

        viewModel.processProgress.observe(this, Observer {
            progress_bar.progress = it
            progress_bar.visibility = if (it in 0..99) View.VISIBLE else View.INVISIBLE
        })
    }

    fun onNavigateBtnClick(view: View) {
        supportFragmentManager.beginTransaction()
            .replace(
                R.id.container,
                when (view.id) {
                    R.id.to_data_source_btn -> DataSourceFragment.newInstance()
                    R.id.to_demodulation_btn -> QpskDemodulatorFragment.newInstance()
                    R.id.to_demodulator_input_btn -> DemodulatorInputFragment.newInstance()
                    R.id.to_demodulator_generator_btn -> DemodulatorGeneratorFragment.newInstance()
                    R.id.to_demodulator_filter_btn -> FirFilterFragment.newInstance()
                    R.id.to_i_channel_btn -> IChannelFragment.newInstance()
                    R.id.to_q_channel_btn -> QChannelFragment.newInstance()
                    R.id.to_demodulator_process_btn -> DemodulatorProcessFragment.newInstance()
                    R.id.to_demodulator_output_btn -> DemodulatorOutputFragment.newInstance()
                    R.id.to_decoding_btn -> DecoderFragment.newInstance()
                    R.id.to_statistics_btn -> StatisticFragment.newInstance()
                    R.id.to_ber_calculation_btn -> BerFragment.newInstance()
                    else -> MainFragment.newInstance()
                }
            )
            .addToBackStack(null)
            .commit()
    }

    private fun loadSettings() {
        val sourcePrefs = getSharedPreferences(
            getString(R.string.settings_source_key),
            Context.MODE_PRIVATE
        )
        loadSourceSettings(sourcePrefs)
    }

    private fun loadSourceSettings(prefs: SharedPreferences) {
        val samplingRate = prefs.getFloat(
            getString(R.string.source_adc_freq_key),
            Simulator.DEFAULT_SAMPLING_RATE.toFloat()
        ).let { it * 1.0e6 } // МГц -> Гц
        Simulator.samplingRate = samplingRate
    }

}
