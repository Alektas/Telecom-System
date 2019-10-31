package alektas.telecomapp.utils

import alektas.telecomapp.domain.entities.Window
import alektas.telecomapp.domain.entities.signals.Signal
import com.jjoe64.graphview.series.DataPoint
import io.reactivex.Observable
import org.apache.commons.math3.transform.DftNormalization
import org.apache.commons.math3.transform.FastFourierTransformer
import org.apache.commons.math3.transform.TransformType

class Extensions

fun Signal.toDataPoints(): Array<DataPoint> = this.getPoints()
        .map { DataPoint(it.key, it.value) }
        .toTypedArray()

fun List<Pair<Double, Double>>.toFloat(): List<Pair<Float, Float>> =
    this.map { Pair(it.first.toFloat(), it.second.toFloat()) }

fun Signal.getSpectrum(): Array<DataPoint> {
    val signalData = Window(Window.GAUSSE).applyTo(this).getValues()
    var spectrum = FastFourierTransformer(DftNormalization.STANDARD)
        .transform(
            signalData,
            TransformType.FORWARD
        )
    val actualSize = spectrum.size / 2
    spectrum = spectrum.take(actualSize).toTypedArray()
    return spectrum
        .mapIndexed { i, complex -> DataPoint(i.toDouble(), complex.abs()) }
        .toTypedArray()
}

fun Signal.getNormalizedSpectrum(): Array<DataPoint> {
    val signalData = Window(Window.GAUSSE).applyTo(this).getValues()
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

fun <T> Observable<T>.doOnFirst(action: (T) -> Unit): Observable<T> =
    take(1).doOnNext(action).concatWith(skip(1))
