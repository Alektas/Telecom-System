package alektas.telecomapp.domain.processes

import androidx.annotation.IntRange

class ProcessState(
    val key: String,
    val displayName: String,
    var state: Int = AWAITING,
    @IntRange(from = 0, to = 100) var progress: Int = 0,
    subProcesses: List<ProcessState> = listOf()
) {
    companion object {
        const val AWAITING = 0
        const val STARTED = 1
        const val FINISHED = 2
        const val ERROR = 3
    }

    init {
        setSubProcesses(subProcesses)
    }

    private var subProcesses: MutableMap<String, ProcessState> = HashMap()

    fun setSubProcess(process: ProcessState) {
        subProcesses[process.key] = process
    }

    fun setSubProcesses(processes: List<ProcessState>) {
        subProcesses = processes.associateBy { it.key }.toMutableMap()
    }

    fun getSubProcesses(): List<ProcessState> = subProcesses.values.toList()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ProcessState) return false

        if (key != other.key) return false

        return true
    }

    override fun hashCode(): Int {
        return key.hashCode()
    }


}