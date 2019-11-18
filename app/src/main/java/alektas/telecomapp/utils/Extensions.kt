package alektas.telecomapp.utils

import alektas.telecomapp.domain.entities.Window
import alektas.telecomapp.domain.entities.signals.Signal
import com.jjoe64.graphview.series.DataPoint
import io.reactivex.Observable
import org.apache.commons.math3.transform.DftNormalization
import org.apache.commons.math3.transform.FastFourierTransformer
import org.apache.commons.math3.transform.TransformType

fun Signal.toDataPoints(): Array<DataPoint> = this.getPoints()
        .map { DataPoint(it.key, it.value) }
        .toTypedArray()

fun List<Pair<Double, Double>>.toFloat(): List<Pair<Float, Float>> =
    this.map { Pair(it.first.toFloat(), it.second.toFloat()) }

fun Signal.getSpectrum(): Array<DataPoint> {
    val data = Window(Window.GAUSSE).applyTo(this).getValues()
    val oldSize = data.size
    val signalData = if (!oldSize.isPowerOfTwo()) {
        val newSize = oldSize.highestOneBit().shl(1)
        data.copyOf(newSize = newSize)
    } else {
        data
    }

    var spectrum = FastFourierTransformer(DftNormalization.STANDARD)
        .transform(
            signalData,
            TransformType.FORWARD
        )
    val actualSize = (spectrum.size / 2).coerceIn(0, 4000)
    spectrum = spectrum.take(actualSize).toTypedArray()
    return spectrum
        .mapIndexed { i, complex -> DataPoint(i.toDouble(), complex.abs()) }
        .toTypedArray()
}

fun Signal.getNormalizedSpectrum(): Array<DataPoint> {
    val data = Window(Window.GAUSSE).applyTo(this).getValues()
    val oldSize = data.size
    val signalData = if (!oldSize.isPowerOfTwo()) {
        val newSize = oldSize.highestOneBit().shl(1)
        data.copyOf(newSize = newSize)
    } else {
        data
    }

    var spectrum = FastFourierTransformer(DftNormalization.STANDARD)
        .transform(
            signalData,
            TransformType.FORWARD
        )
    val actualSize = (spectrum.size / 2).coerceIn(0, 4000)
    spectrum = spectrum.take(actualSize).toTypedArray()
    val maxSpectrumValue = spectrum.maxBy { it.abs() }?.abs() ?: 1.0
    return spectrum
        .mapIndexed { i, complex -> DataPoint(i.toDouble(), complex.abs() / maxSpectrumValue) }
        .toTypedArray()
}

fun Int.isPowerOfTwo(): Boolean {
    return this > 0 && this and this - 1 == 0
}

fun Int.highestOneBit(): Int {
    var i = this
    i = i or (i shr 1)
    i = i or (i shr 2)
    i = i or (i shr 4)
    i = i or (i shr 8)
    i = i or (i shr 16)
    return i - i.ushr(1)
}

infix fun ClosedRange<Double>.step(step: Double): Iterable<Double> {
    require(start.isFinite())
    require(endInclusive.isFinite())
    require(step > 0.0) { "Step must be positive, was: $step." }
    val sequence = generateSequence(start) { previous ->
        if (previous == Double.POSITIVE_INFINITY) return@generateSequence null
        val next = previous + step
        if (next > endInclusive) null else next
    }
    return sequence.asIterable()
}

fun <T> Observable<T>.doOnFirst(action: (T) -> Unit): Observable<T> =
    take(1).doOnNext(action).concatWith(skip(1))
