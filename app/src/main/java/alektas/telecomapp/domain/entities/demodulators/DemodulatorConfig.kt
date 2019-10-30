package alektas.telecomapp.domain.entities.demodulators

import alektas.telecomapp.domain.entities.QpskContract
import alektas.telecomapp.domain.entities.filters.FilterConfig
import alektas.telecomapp.domain.entities.signals.BaseSignal
import alektas.telecomapp.domain.entities.signals.Signal

class DemodulatorConfig(
    var inputSignal: Signal = BaseSignal(),
    var carrierFrequency: Double = QpskContract.CARRIER_FREQUENCY,
    var frameLength: Int = 0,
    var codeLength: Int = 0,
    var bitTime: Double = QpskContract.DATA_BIT_TIME,
    var bitThreshold: Double = QpskContract.SIGNAL_THRESHOLD,
    var filterConfig: FilterConfig = FilterConfig()
)