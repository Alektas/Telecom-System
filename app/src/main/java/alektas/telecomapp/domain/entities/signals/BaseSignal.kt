package alektas.telecomapp.domain.entities.signals

import java.util.*

open class BaseSignal() : Signal {
    var data = TreeMap<Double, Double>()

    constructor(data: TreeMap<Double, Double>) : this() {
        this.data = data
    }

    override fun getPoints(): Map<Double, Double> {
        return data
    }

    override fun getPoints(from: Double, to: Double): Map<Double, Double> {
        return data.filterKeys { it >= from || it <= to }
    }

    override fun getValues(): DoubleArray {
        return data.values.toDoubleArray()
    }

    override fun getValueAt(time: Double): Double {
        return data[time] ?: 0.toDouble()
    }

    override fun plus(other: Signal): Signal {
        val d = TreeMap<Double, Double>()
        other.getPoints().mapValuesTo(d) { it.value + (data[it.key] ?: 0.0) }
        return BaseSignal(d)
    }

    override fun minus(other: Signal): Signal {
        val d = TreeMap<Double, Double>()
        other.getPoints().mapValuesTo(d) { it.value - (data[it.key] ?: 0.0) }
        return BaseSignal(d)
    }

    override fun times(other: Signal): Signal {
        val d = TreeMap<Double, Double>()
        other.getPoints().mapValuesTo(d) { it.value * (data[it.key] ?: 1.0) }
        return BaseSignal(d)
    }

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is Signal) return false
        return other.getPoints() == this.data
    }

    override fun hashCode(): Int {
        return data.hashCode()
    }

}