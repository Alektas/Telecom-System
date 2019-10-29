package alektas.telecomapp.ui.utils

import android.content.Context
import android.widget.ArrayAdapter
import android.widget.Filter


class SimpleArrayAdapter<T> (
    context: Context, textViewResourceId: Int,
    var items: List<T>
) : ArrayAdapter<T>(context, textViewResourceId, items) {
    private val filter = DummyFilter()

    override fun getFilter(): Filter {
        return filter
    }

    private open inner class DummyFilter : Filter() {

        override fun performFiltering(arg0: CharSequence): FilterResults {
            val result = FilterResults()
            result.values = items
            result.count = items.size
            return result
        }

        override fun publishResults(arg0: CharSequence, arg1: FilterResults) {
            notifyDataSetChanged()
        }
    }
}