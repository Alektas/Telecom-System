package alektas.telecomapp.domain.entities.generators

import alektas.telecomapp.domain.entities.signals.BinarySignal
import alektas.telecomapp.domain.entities.signals.HarmonicSignal

class SignalGenerator {

    fun sin(
        magnitude: Double = 1.0,
        frequency: Double = 1000.0
    ): HarmonicSignal = HarmonicSignal(magnitude, frequency, 0.0)

    fun cos(
        magnitude: Double = 1.0,
        frequency: Double = 1000.0
    ): HarmonicSignal = HarmonicSignal(magnitude, frequency, Math.PI / 2)

    fun binary(
        data: Array<Boolean>,
        bitTime: Double,
        magnitude: Double = 1.0,
        bipolar: Boolean = true
    ): BinarySignal = BinarySignal(data, bitTime, magnitude, bipolar)

    fun pulse(
        pulseTime: Double,
        magnitude: Double = 1.0,
        negative: Boolean = false
    ): BinarySignal = BinarySignal(arrayOf(true), pulseTime, magnitude, negative)
}