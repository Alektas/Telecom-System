package alektas.telecomapp.ui.process

import alektas.telecomapp.R
import alektas.telecomapp.domain.processes.ProcessState
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_process_complex.view.*

/**
 * Адаптер для отображения списка выполняемых процессов.
 * Любой процесс может содержать подпроцессы, поэтому получается иерархия процессов из уровней.
 *
 * @param maxLevel максимальное количество отображаемых уровней подпроцессов.
 * @param curLevel текущий уровень подпроцессов
 */
class ProcessesAdapter(private val maxLevel: Int, private val curLevel: Int = 1) :
    RecyclerView.Adapter<ProcessesAdapter.ProcessViewHolder>() {
    var processes = listOf<ProcessState>()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProcessViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(
                if (curLevel < maxLevel) R.layout.item_process_complex else R.layout.item_process_simple,
                parent,
                false
            )
        return ProcessViewHolder(view)
    }

    override fun getItemId(position: Int): Long {
        return processes[position].hashCode().toLong()
    }

    override fun getItemCount(): Int {
        return processes.size
    }

    override fun onBindViewHolder(holder: ProcessViewHolder, position: Int) {
        val process = processes[position]
        holder.bind(process)
    }

    inner class ProcessViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val stateIcon: ImageView = view.process_indicator
        private val processName: TextView = view.process_name
        private var processesAdapter: ProcessesAdapter? = null

        init {
            if (curLevel < maxLevel) {
                processesAdapter = ProcessesAdapter(maxLevel, curLevel + 1)
                view.sub_processes_list.apply {
                    layoutManager = LinearLayoutManager(view.context)
                    adapter = processesAdapter
                }
            }
        }

        fun bind(process: ProcessState) {
            stateIcon.setImageResource(
                when (process.state) {
                    ProcessState.STARTED -> R.drawable.ic_process_black_24dp
                    ProcessState.FINISHED -> R.drawable.ic_check_black_24dp
                    ProcessState.ERROR -> R.drawable.ic_error_outline_black_24dp
                    else -> R.drawable.ic_awaiting_black_24dp
                }
            )
            processName.text = process.processName
            processesAdapter?.processes = process.getSubStates()
        }
    }
}