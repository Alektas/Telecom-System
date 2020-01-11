package alektas.telecomapp.ui.process

import alektas.telecomapp.R
import alektas.telecomapp.domain.processes.ProcessState
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_process.view.*

class ProcessesAdapter : RecyclerView.Adapter<ProcessesAdapter.ProcessViewHolder>() {
    var processes = listOf<ProcessState>()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProcessViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_process, parent, false)
        return ProcessViewHolder(view)
    }

    override fun getItemCount(): Int {
        return processes.size
    }

    override fun onBindViewHolder(holder: ProcessViewHolder, position: Int) {
        val process = processes[position]
        holder.bind(process)
    }

    inner class ProcessViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var stateIcon: ImageView = view.process_indicator
        var processName: TextView = view.process_name

        fun bind(process: ProcessState) {
            stateIcon.setImageResource(when(process.state) {
                ProcessState.STARTED -> R.drawable.ic_process_black_24dp
                ProcessState.FINISHED -> R.drawable.ic_check_black_24dp
                ProcessState.ERROR -> R.drawable.ic_error_outline_black_24dp
                else -> R.drawable.ic_launcher_foreground
            })
            processName.text = process.displayName
        }
    }
}