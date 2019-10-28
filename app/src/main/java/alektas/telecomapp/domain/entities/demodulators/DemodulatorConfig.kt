package alektas.telecomapp.domain.entities.demodulators

import alektas.telecomapp.domain.entities.CdmaContract
import alektas.telecomapp.domain.entities.QpskContract
import alektas.telecomapp.domain.entities.filters.FilterConfig
import alektas.telecomapp.domain.entities.signals.BaseSignal
import alektas.telecomapp.domain.entities.signals.Signal

class DemodulatorConfig(
    var inputSignal: Signal = BaseSignal(),
    var carrierFrequency: Double = QpskContract.CARRIER_FREQUENCY,
    var dataLength: Int = CdmaContract.SPREAD_DATA_LENGTH,
    var filterConfig: FilterConfig = FilterConfig()
)