package alektas.telecomapp.utils

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment

class SystemUtils {

    companion object {

        fun hideKeyboard(fragment: Fragment) {
            // Check if no view has focus:
            val view = fragment.view
            if (view != null) {
                val inputManager = fragment.requireContext()
                    .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                inputManager.hideSoftInputFromWindow(
                    view.windowToken,
                    InputMethodManager.HIDE_NOT_ALWAYS
                )
            }
        }

        fun hideKeyboard(view: View?) {
            // Check if no view has focus:
            if (view != null) {
                val inputManager = view.context
                    .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                inputManager.hideSoftInputFromWindow(
                    view.windowToken,
                    InputMethodManager.HIDE_NOT_ALWAYS
                )
            }
        }
    }
}