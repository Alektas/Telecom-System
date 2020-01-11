package alektas.telecomapp.domain.entities.signals.noises

import alektas.telecomapp.domain.entities.signals.Signal

interface Noise : Signal {
    fun rate(): Double
}