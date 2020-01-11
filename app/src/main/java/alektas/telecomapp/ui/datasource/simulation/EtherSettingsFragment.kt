package alektas.telecomapp.ui.datasource.simulation

import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import alektas.telecomapp.R
import alektas.telecomapp.domain.entities.contracts.QpskContract
import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.Observer
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import kotlinx.android.synthetic.main.ether_settings_fragment.*

class EtherSettingsFragment : Fragment() {

    companion object {
        fun newInstance() = EtherSettingsFragment()
    }

    private lateinit var viewModel: EtherSettingsViewModel
    private lateinit var prefs: SharedPreferences
    private val graphPoints = LineGraphSeries<DataPoint>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.ether_settings_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<GraphView>(R.id.ether_chart).apply {
            addSeries(graphPoints)
            viewport.apply {
                isScrollable = true
                isXAxisBoundsManual = true
                setMaxX(10 * QpskContract.DEFAULT_DATA_BIT_TIME)
            }
        }
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(EtherSettingsViewModel::class.java)
        prefs = requireContext().getSharedPreferences(
            getString(R.string.settings_source_key),
            Context.MODE_PRIVATE
        )

        setupFieldsValidation(viewModel)
        setInitValues(prefs)
        observeSettings(viewModel, prefs)
        setupControls()

        viewModel.ether.observe(viewLifecycleOwner, Observer {
            graphPoints.resetData(it)
        })
    }

    private fun setInitValues(prefs: SharedPreferences) {
        val isNoiseEnabled = prefs.getBoolean(getString(R.string.source_noise_enable_key), false)
        noise_checkbox.isChecked = isNoiseEnabled
        noise_rate_layout.isEnabled = isNoiseEnabled

        prefs.getFloat(
            getString(R.string.source_noise_snr_key),
            QpskContract.DEFAULT_SIGNAL_NOISE_RATE.toFloat()
        ).let {
            noise_rate.setText(it.toString())
        }

        val isInterferenceEnabled = prefs.getBoolean(getString(R.string.source_interference_enable_key), false)
        interference_checkbox.isChecked = isInterferenceEnabled
        interference_rate_layout.isEnabled = isInterferenceEnabled
        interference_sparseness_layout.isEnabled = isInterferenceEnabled

        prefs.getFloat(
            getString(R.string.source_interference_snr_key),
            QpskContract.DEFAULT_SIGNAL_NOISE_RATE.toFloat()
        ).let {
            interference_rate.setText(it.toString())
        }

        prefs.getFloat(
            getString(R.string.source_interference_sparseness_key),
            QpskContract.DEFAULT_INTERFERENCE_SPARSENESS.toFloat()
        ).let {
            interference_sparseness.setText(it.toString())
        }
    }

    private fun observeSettings(viewModel: EtherSettingsViewModel, prefs: SharedPreferences) {
        viewModel.noiseRate.observe(viewLifecycleOwner, Observer {
            prefs.edit().putFloat(getString(R.string.source_noise_snr_key), it.toFloat()).apply()
        })

        viewModel.interferenceRate.observe(viewLifecycleOwner, Observer {
            prefs.edit().putFloat(getString(R.string.source_interference_snr_key), it.toFloat()).apply()
        })

        viewModel.interferenceSparseness.observe(viewLifecycleOwner, Observer {
            prefs.edit().putFloat(getString(R.string.source_interference_sparseness_key), it.toFloat()).apply()
        })
    }

    private fun setupControls() {
        noise_checkbox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                viewModel.enableNoise()
                noise_rate_layout.isEnabled = true
                prefs.edit().putBoolean(getString(R.string.source_noise_enable_key), true).apply()
            } else {
                viewModel.disableNoise()
                noise_rate_layout.isEnabled = false
                prefs.edit().putBoolean(getString(R.string.source_noise_enable_key), false).apply()
            }
        }

        interference_checkbox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                viewModel.enableInterference()
                interference_rate_layout.isEnabled = true
                interference_sparseness_layout.isEnabled = true
                prefs.edit().putBoolean(getString(R.string.source_interference_enable_key), true).apply()
            } else {
                viewModel.disableInterference()
                interference_rate_layout.isEnabled = false
                interference_sparseness_layout.isEnabled = false
                prefs.edit().putBoolean(getString(R.string.source_interference_enable_key), false).apply()
            }
        }

        noise_rate.setOnEditorActionListener { tv, _, _ ->
            changeNoiseRate(tv.text.toString())
            false
        }

        interference_rate.setOnEditorActionListener { tv, _, _ ->
            changeInterferenceRate(tv.text.toString())
            false
        }

        interference_sparseness.setOnEditorActionListener { tv, _, _ ->
            changeInterferenceSparseness(tv.text.toString())
            false
        }
    }

    private fun setupFieldsValidation(viewModel: EtherSettingsViewModel) {
        noise_rate.doOnTextChanged { text, _, _, _ ->
            if (viewModel.parseSnr(text.toString()) != EtherSettingsViewModel.INVALID_SNR) {
                noise_rate_layout.error = null
            } else {
                noise_rate_layout.error = getString(R.string.error_num)
            }
        }

        interference_rate.doOnTextChanged { text, _, _, _ ->
            if (viewModel.parseSnr(text.toString()) != EtherSettingsViewModel.INVALID_SNR) {
                interference_rate_layout.error = null
            } else {
                interference_rate_layout.error = getString(R.string.error_num)
            }
        }

        interference_sparseness.doOnTextChanged { text, _, _, _ ->
            if (viewModel.parseSparseness(text.toString()) >= 0) {
                interference_sparseness_layout.error = null
            } else {
                interference_sparseness_layout.error = getString(R.string.error_positive_num)
            }
        }
    }

    private fun changeNoiseRate(snr: String) {
        if (snr.isEmpty() || noise_rate_layout.error != null) {
            Toast.makeText(requireContext(), getString(R.string.error_num), Toast.LENGTH_SHORT).show()
            return
        }

        viewModel.setNoise(snr)
    }

    private fun changeInterferenceSparseness(sparseness: String) {
        if (sparseness.isEmpty() || interference_sparseness_layout.error != null) {
            Toast.makeText(requireContext(), getString(R.string.error_positive_num), Toast.LENGTH_SHORT).show()
            return
        }

        viewModel.changeInterferenceSparseness(sparseness)
    }

    private fun changeInterferenceRate(rate: String) {
        if (rate.isEmpty() || interference_rate_layout.error != null) {
            Toast.makeText(requireContext(), getString(R.string.error_num), Toast.LENGTH_SHORT).show()
            return
        }

        viewModel.changeInterferenceRate(rate)
    }

}
