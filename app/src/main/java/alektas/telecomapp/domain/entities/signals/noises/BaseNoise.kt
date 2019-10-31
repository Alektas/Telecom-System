package alektas.telecomapp.domain.entities.signals.noises

import alektas.telecomapp.domain.entities.signals.BaseSignal

/**
 * Класс-адаптер для шумовых сигналов
 *
 * @param snr отношение сигнал/шум (signal/noise rate) в дБ.
 */
open class BaseNoise(private val snr: Double = 0.0) : BaseSignal(), Noise {
    override fun snr(): Double = snr
}