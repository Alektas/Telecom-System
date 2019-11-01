package alektas.telecomapp

import alektas.telecomapp.ui.datasource.DataSourceFragment
import alektas.telecomapp.ui.decoder.DecoderFragment
import alektas.telecomapp.ui.demodulators.QpskDemodulatorFragment
import alektas.telecomapp.ui.demodulators.filter.FirFilterFragment
import alektas.telecomapp.ui.demodulators.filter.ichannel.IChannelFragment
import alektas.telecomapp.ui.demodulators.filter.qchannel.QChannelFragment
import alektas.telecomapp.ui.demodulators.generator.DemodulatorGeneratorFragment
import alektas.telecomapp.ui.demodulators.input.DemodulatorInputFragment
import alektas.telecomapp.ui.demodulators.output.DemodulatorOutputFragment
import alektas.telecomapp.ui.demodulators.processing.DemodulatorProcessFragment
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import alektas.telecomapp.ui.main.MainFragment
import alektas.telecomapp.ui.statistic.StatisticFragment
import android.view.View

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, MainFragment.newInstance())
                .commitNow()
        }
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
                    else -> MainFragment.newInstance()
                }
            )
            .addToBackStack(null)
            .commit()
    }

}
