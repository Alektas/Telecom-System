package alektas.telecomapp.ui.dialogs

import alektas.telecomapp.BuildConfig
import alektas.telecomapp.R
import android.app.Dialog
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

class AboutDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(requireContext()).apply {
            setView(R.layout.dialog_about)
            view?.findViewById<TextView>(R.id.app_version)?.text = BuildConfig.VERSION_NAME
            setTitle(getString(R.string.menu_about))
            setNegativeButton(getString(R.string.cancel)) { _, _ -> dismiss() }
        }
            .create()
    }
}