package alektas.telecomapp.utils

import alektas.telecomapp.domain.entities.signals.Signal
import com.jjoe64.graphview.series.DataPoint

class Extensions

fun Signal.toDataPoints(): Array<DataPoint> {
    return this.getPoints()
        .map { DataPoint(it.key, it.value) }
        .toTypedArray()
}
