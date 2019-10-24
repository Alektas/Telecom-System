package alektas.telecomapp

import alektas.telecomapp.ui.chart.ChartFragment
import alektas.telecomapp.ui.datasource.DataSourceFragment
import alektas.telecomapp.ui.decoder.DecoderFragment
import alektas.telecomapp.ui.demodulators.QpskDemodulatorFragment
import alektas.telecomapp.ui.filters.FirFilterFragment
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import alektas.telecomapp.ui.main.MainFragment
import android.view.View

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, MainFragment.newInstance(), ChartFragment.TAG)
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
                    R.id.to_qpsk_demod_filter_btn -> FirFilterFragment.newInstance()
                    R.id.to_decoding_btn -> DecoderFragment.newInstance()
                    else -> MainFragment.newInstance()
                }
            )
            .addToBackStack(null)
            .commit()
    }

}
