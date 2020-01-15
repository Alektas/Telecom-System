package alektas.telecomapp.domain.processes

import androidx.annotation.IntRange

class ProcessState(
    val key: String,
    val processName: String,
    var state: Int = AWAITING,
    @IntRange(from = 0, to = 100) var progress: Int = 0,
    subStates: List<ProcessState> = listOf()
) {
    private var subStates: MutableMap<String, ProcessState> = HashMap()

    companion object {
        const val AWAITING = 0
        const val STARTED = 1
        const val FINISHED = 2
        const val ERROR = 3
    }

    init {
        setSubStates(subStates)
    }

    fun withState(state: Int): ProcessState = apply { this.state = state }

    fun withProgress(progress: Int): ProcessState = apply { this.progress = progress }

    fun with(state: Int, progress: Int): ProcessState = apply {
        this.state = state
        this.progress = progress
    }

    fun withSubState(state: ProcessState): ProcessState = apply { setSubState(state) }

    /**
     * Обновление состояния подпроцесса, либо его добавление при отсутствии такового
     */
    fun setSubState(state: ProcessState) {
        subStates[state.key] = state
    }

    fun withSubStates(states: List<ProcessState>): ProcessState = apply { setSubStates(states) }

    fun setSubStates(states: List<ProcessState>) {
        subStates = states.associateBy { it.key }.toMutableMap()
    }

    fun withoutSubState(key: String): ProcessState = apply { removeSubState(key) }

    fun removeSubState(key: String) = subStates.remove(key)

    fun withoutSubStates(): ProcessState = apply { removeSubStates() }

    fun removeSubStates() = subStates.clear()

    fun getSubStates(): List<ProcessState> = subStates.values.toList()

    fun withResetedSubStates(): ProcessState = apply { resetSubStates() }

    /**
     * Сброс всех внутренних состояний до [AWAITING] с нулевым прогрессом.
     */
    fun resetSubStates() {
        subStates = subStates
            .mapValues {
                it.value.apply {
                    reset()
                    resetSubStates()
                }
            }
            .toMutableMap()
    }

    fun reseted(): ProcessState = apply { reset() }

    /**
     * Сброс состояния до [AWAITING] с нулевым прогрессом.
     */
    fun reset() {
        state = AWAITING
        progress = 0
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ProcessState) return false

        if (key != other.key) return false

        return true
    }

    override fun hashCode(): Int {
        return key.hashCode()
    }

    override fun toString(): String {
        return "[${key}] name=$processName, state=${when (state) {
            0 -> "AWAITING"
            1 -> "STARTED"
            2 -> "FINISHED"
            3 -> "ERROR"
            else -> "UNKNOWN"
        }}, progress=$progress, subprocesses=${subStates.size}"
    }

}