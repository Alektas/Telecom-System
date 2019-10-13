package alektas.telecomapp.domain.entities.generators

import alektas.telecomapp.domain.entities.signals.Signal

interface Generator {
    fun generate(type: Int): Signal
}