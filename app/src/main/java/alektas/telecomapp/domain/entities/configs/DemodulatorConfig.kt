package alektas.telecomapp.domain.entities.configs

import alektas.telecomapp.domain.entities.contracts.QpskContract
import alektas.telecomapp.domain.entities.filters.FilterConfig

/**
 * @param delayCompensation компенсация временной задержки КИХ (FIR) фильтров,
 * в долях от времени бита. Т.е. 1.0f -> компенсирует задержку длительностью одного бита.
 */
class DemodulatorConfig(
    var delayCompensation: Float = QpskContract.DEFAULT_FILTERS_DELAY_COMPENSATION,
    var carrierFrequency: Double = QpskContract.DEFAULT_CARRIER_FREQUENCY,
    var frameLength: Int = 0,
    var codeLength: Int = 0,
    var bitTime: Double = QpskContract.DEFAULT_DATA_BIT_TIME,
    var filterConfig: FilterConfig = FilterConfig()
)