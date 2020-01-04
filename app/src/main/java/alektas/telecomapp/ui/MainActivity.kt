package alektas.telecomapp.ui

import alektas.telecomapp.R
import alektas.telecomapp.domain.entities.Simulator
import alektas.telecomapp.ui.datasource.external.FileDataSourceViewModel
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
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import kotlinx.android.synthetic.main.main_activity.*

private const val OPEN_DOCUMENT_REQUEST_CODE = 1
private const val EXTERNAL_FILE_URI_KEY = "EXTERNAL_FILE_URI_KEY"

class MainActivity : AppCompatActivity() {
    private lateinit var viewModel: MainViewModel
    private lateinit var appBarConfig: AppBarConfiguration
    private var externalData: String? = null
    private var externalFileUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        val navController = findNavController(R.id.nav_host)
        appBarConfig = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfig)
        if (savedInstanceState == null) {
            findNavController(R.id.nav_host).navigate(R.id.main_menu)
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

    override fun onSupportNavigateUp(): Boolean {
        return findNavController(R.id.nav_host).navigateUp(appBarConfig) || super.onSupportNavigateUp()
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

        view.findNavController().navigate(
            when (view.id) {
                R.id.to_data_source_btn -> R.id.action_main_menu_to_dataSourceFragment
                R.id.to_simulation_data_source_btn -> R.id.action_dataSourceFragment_to_simulationDataSourceFragment
                R.id.to_ether_settings_btn -> R.id.action_simulationDataSourceFragment_to_etherSettingsFragment
                R.id.to_channels_settings_btn -> R.id.action_simulationDataSourceFragment_to_channelsSettingsFragment
                R.id.to_demodulation_btn -> R.id.action_main_menu_to_qpskDemodulatorFragment
                R.id.to_demodulator_input_btn -> R.id.action_qpskDemodulatorFragment_to_demodulatorInputFragment
                R.id.to_demodulator_generator_btn -> R.id.action_qpskDemodulatorFragment_to_demodulatorGeneratorFragment
                R.id.to_demodulator_filter_btn -> R.id.action_qpskDemodulatorFragment_to_firFilterFragment
                R.id.to_i_channel_btn -> R.id.action_firFilterFragment_to_IChannelFragment
                R.id.to_q_channel_btn -> R.id.action_firFilterFragment_to_QChannelFragment
                R.id.to_demodulator_process_btn -> R.id.action_qpskDemodulatorFragment_to_demodulatorProcessFragment
                R.id.to_demodulator_output_btn -> R.id.action_qpskDemodulatorFragment_to_demodulatorOutputFragment
                R.id.to_decoding_btn -> R.id.action_main_menu_to_decoderFragment
                R.id.to_statistics_btn -> R.id.action_main_menu_to_statisticFragment
                R.id.to_ber_calculation_btn -> R.id.action_statisticFragment_to_characteristicsFragment
                else -> R.id.main
            }
        )
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
        val vm = ViewModelProviders.of(this).get(FileDataSourceViewModel::class.java)
        vm.setDataString(data)
        findNavController(R.id.nav_host).navigate(R.id.action_dataSourceFragment_to_fileDataSourceFragment)
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
