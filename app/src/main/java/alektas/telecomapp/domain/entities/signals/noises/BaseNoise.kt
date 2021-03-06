package alektas.telecomapp.domain.entities.signals.noises

import alektas.telecomapp.domain.entities.signals.BaseSignal

/**
 * Класс-адаптер для шумовых сигналов
 *
 * @param rate отношение энергии бита к спектральной мощности шума (Eb/N0 rate) в дБ.
 */
open class BaseNoise(private val rate: Double = 0.0) : BaseSignal(), Noise {
    override fun rate(): Double = rate
}