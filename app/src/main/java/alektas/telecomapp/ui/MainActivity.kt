package alektas.telecomapp.ui

import alektas.telecomapp.R
import alektas.telecomapp.domain.entities.Simulator
import alektas.telecomapp.ui.datasource.DataSourceFragment
import alektas.telecomapp.ui.datasource.external.FileDataSourceFragment
import alektas.telecomapp.ui.datasource.simulation.ChannelsSettingsFragment
import alektas.telecomapp.ui.datasource.simulation.EtherSettingsFragment
import alektas.telecomapp.ui.datasource.simulation.SimulationDataSourceFragment
import alektas.telecomapp.ui.decoder.DecoderFragment
import alektas.telecomapp.ui.demodulator.QpskDemodulatorFragment
import alektas.telecomapp.ui.demodulator.filter.FirFilterFragment
import alektas.telecomapp.ui.demodulator.filter.ichannel.IChannelFragment
import alektas.telecomapp.ui.demodulator.filter.qchannel.QChannelFragment
import alektas.telecomapp.ui.demodulator.generator.DemodulatorGeneratorFragment
import alektas.telecomapp.ui.demodulator.input.DemodulatorInputFragment
import alektas.telecomapp.ui.demodulator.output.DemodulatorOutputFragment
import alektas.telecomapp.ui.demodulator.processing.DemodulatorProcessFragment
import alektas.telecomapp.ui.main.MainFragment
import alektas.telecomapp.ui.statistic.StatisticFragment
import alektas.telecomapp.ui.statistic.ber.BerFragment
import alektas.telecomapp.utils.FileWorker
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import kotlinx.android.synthetic.main.main_activity.*

private const val OPEN_DOCUMENT_REQUEST_CODE = 1
private const val EXTERNAL_FILE_URI_KEY = "EXTERNAL_FILE_URI_KEY"

class MainActivity : AppCompatActivity() {
    private lateinit var viewModel: MainViewModel
    private var externalData: String? = null
    private var externalFileUri: Uri? = null

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

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelable(EXTERNAL_FILE_URI_KEY, externalFileUri)
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        externalFileUri = savedInstanceState.getParcelable(EXTERNAL_FILE_URI_KEY)
        externalData = externalFileUri?.let { FileWorker(this).readFile(it) }
    }

    fun onNavigateBtnClick(view: View) {
        if (view.id == R.id.to_file_explorer_btn) {
            selectFileForProcessing()
            return
        }

        if (view.id == R.id.to_file_data_source_btn) {
            if (externalData.isNullOrBlank()) {
                selectFileForProcessing()
            } else {
                processData(externalData!!)
            }
            return
        }

        supportFragmentManager.beginTransaction()
            .replace(
                R.id.container,
                when (view.id) {
                    R.id.to_data_source_btn -> DataSourceFragment.newInstance()
                    R.id.to_simulation_data_source_btn -> SimulationDataSourceFragment.newInstance()
                    R.id.to_ether_settings_btn -> EtherSettingsFragment.newInstance()
                    R.id.to_channels_settings_btn -> ChannelsSettingsFragment.newInstance()
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

    private fun selectFileForProcessing() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "text/plain"
            addCategory(Intent.CATEGORY_OPENABLE)
//        putExtra("android.content.extra.SHOW_ADVANCED", true)
//        putExtra("android.content.extra.FANCY", true)
//        putExtra("android.content.extra.SHOW_FILESIZE", true)
        }
        startActivityForResult(intent, OPEN_DOCUMENT_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == OPEN_DOCUMENT_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                data?.data?.let {
                    externalFileUri = it
                    val s = FileWorker(this).readFile(it)
                    externalData = s
                    processData(s)
                }
            } else {
                Toast.makeText(this, "Для обработки выберите подходящий файл", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun processData(data: String) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, FileDataSourceFragment.newInstance(data))
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
            (Simulator.DEFAULT_SAMPLING_RATE * 1.0e-6).toFloat()
        ).let { it * 1.0e6 } // МГц -> Гц
        Simulator.samplingRate = samplingRate
    }

}
