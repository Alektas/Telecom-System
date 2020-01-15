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

    /**
     * Обновление состояния подпроцесса, либо его добавление при отсутствии такового
     */
    fun setSubState(state: ProcessState) {
        subStates[state.key] = state
    }

    fun removeSubState(key: String) {
        subStates.remove(key)
    }

    fun setSubStates(states: List<ProcessState>) {
        subStates = states.associateBy { it.key }.toMutableMap()
    }

    fun removeSubStates() {
        subStates.clear()
    }

    fun getSubStates(): List<ProcessState> = subStates.values.toList()

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


}