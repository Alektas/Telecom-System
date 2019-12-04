package alektas.telecomapp.domain.entities.signals

import java.util.*

open class BaseSignal() : Signal {
    var data: Map<Double, Double> = TreeMap()

    constructor(data: Map<Double, Double>) : this() {
        this.data = data
    }

    constructor(times: DoubleArray, values: DoubleArray) : this() {
        val d = TreeMap<Double, Double>()
        for (i in times.indices) {
            d[times[i]] = try {
                values[i]
            } catch (e: IndexOutOfBoundsException) {
                0.0
            }
        }
        this.data = d
    }

    override fun getPoints(): Map<Double, Double> {
        return data
    }

    override fun getPoints(from: Double, to: Double): Map<Double, Double> {
        return data.filterKeys { it >= from && it < to }
    }

    override fun getTimes(): DoubleArray {
        return data.keys.toDoubleArray()
    }

    override fun getTimes(from: Double, to: Double): DoubleArray {
        return data.filterKeys { it >= from && it < to }.keys.toDoubleArray()
    }

    override fun getValues(): DoubleArray {
        return data.values.toDoubleArray()
    }

    override fun getValues(from: Double, to: Double): DoubleArray {
        return data.filterKeys { it >= from && it < to }.values.toDoubleArray()
    }

    override fun getValueAt(time: Double): Double {
        return data[time] ?: 0.toDouble()
    }

    override fun isEmpty(): Boolean {
        return data.isEmpty()
    }

    override fun plus(other: Signal): Signal {
        other as BaseSignal
        val times = (getTimes() + other.getTimes()).sorted()
        val values = times.map { (this.data[it] ?: 0.0) + (other.data[it] ?: 0.0) }
        return BaseSignal(times.zip(values).toMap())
    }

    override fun minus(other: Signal): Signal {
        other as BaseSignal
        val times = (getTimes() + other.getTimes()).sorted()
        val values = times.map { (this.data[it] ?: 0.0) - (other.data[it] ?: 0.0) }
        return BaseSignal(times.zip(values).toMap())
    }

    override fun times(other: Signal): Signal {
        other as BaseSignal
        val times = (getTimes() + other.getTimes()).sorted()
        val values = times.map { (this.data[it] ?: 0.0) * (other.data[it] ?: 0.0) }
        return BaseSignal(times.zip(values).toMap())
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BaseSignal) return false

        if (data != other.data) return false

        return true
    }

    override fun hashCode(): Int {
        return data.hashCode()
    }

}