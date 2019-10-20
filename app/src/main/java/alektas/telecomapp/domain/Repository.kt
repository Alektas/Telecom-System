package alektas.telecomapp.domain

import alektas.telecomapp.domain.entities.demodulators.DemodulatorConfig
import alektas.telecomapp.domain.entities.filters.FilterConfig
import io.reactivex.Observable

interface Repository {
    fun getDemodulatorConfig(): DemodulatorConfig
    fun observeDemodulatorConfig(): Observable<DemodulatorConfig>
    fun setDemodulatorConfig(config: DemodulatorConfig)
    fun getDemodulatorFilterConfig(): FilterConfig
    fun observeDemodulatorFilterConfig(): Observable<FilterConfig>
    fun setDemodulatorFilterConfig(config: FilterConfig)
}