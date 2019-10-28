package alektas.telecomapp.ui.demodulators.processing

import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import alektas.telecomapp.R

class DemodulatorProcessFragment : Fragment() {

    companion object {
        fun newInstance() = DemodulatorProcessFragment()
    }

    private lateinit var viewModel: DemodulatorProcessViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.demodulator_process_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(DemodulatorProcessViewModel::class.java)
        // TODO: Use the ViewModel
    }

}
