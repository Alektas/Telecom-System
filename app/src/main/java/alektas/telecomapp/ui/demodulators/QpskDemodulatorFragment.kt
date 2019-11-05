package alektas.telecomapp.ui.demodulators

import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import alektas.telecomapp.R

class QpskDemodulatorFragment : Fragment() {

    companion object {
        fun newInstance() = QpskDemodulatorFragment()
    }

    private lateinit var viewModel: QpskDemodulatorViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.qpsk_demodulator_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(QpskDemodulatorViewModel::class.java)
    }

}
