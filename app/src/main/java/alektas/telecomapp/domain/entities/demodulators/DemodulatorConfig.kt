package alektas.telecomapp.domain.entities.demodulators

import alektas.telecomapp.domain.entities.filters.FilterConfig
import alektas.telecomapp.domain.entities.signals.BaseSignal
import alektas.telecomapp.domain.entities.signals.Signal

class DemodulatorConfig(
    var inputSignal: Signal = BaseSignal(),
    var filterConfig: FilterConfig = FilterConfig()
)