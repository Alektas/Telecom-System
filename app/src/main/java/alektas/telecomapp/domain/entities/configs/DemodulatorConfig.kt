package alektas.telecomapp.domain.entities.configs

import alektas.telecomapp.domain.entities.contracts.QpskContract
import alektas.telecomapp.domain.entities.filters.FilterConfig

class DemodulatorConfig(
    var carrierFrequency: Double = QpskContract.DEFAULT_CARRIER_FREQUENCY,
    var frameLength: Int = 0,
    var codeLength: Int = 0,
    var bitTime: Double = QpskContract.DEFAULT_DATA_BIT_TIME,
    var filterConfig: FilterConfig = FilterConfig()
)