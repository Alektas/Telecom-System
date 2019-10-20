package alektas.telecomapp.data

import alektas.telecomapp.domain.Repository
import alektas.telecomapp.domain.entities.demodulators.DemodulatorConfig
import alektas.telecomapp.domain.entities.filters.FilterConfig
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject

class TelecomSystem: Repository {
    private var demodulatorConfig: DemodulatorConfig = DemodulatorConfig()
    private var filterConfig: FilterConfig =
        FilterConfig()
    private val filterConfigSource: BehaviorSubject<FilterConfig> = BehaviorSubject.create()

    override fun getDemodulatorConfig(): DemodulatorConfig {
        return demodulatorConfig
    }

    override fun setDemodulatorConfig(config: DemodulatorConfig) {
        demodulatorConfig = config
    }

    override fun getDemodulatorFilterConfig(): FilterConfig {
        return filterConfig
    }

    override fun observeDemodulatorFilterConfig(): Observable<FilterConfig> {
        return filterConfigSource
    }

    override fun setDemodulatorFilterConfig(config: FilterConfig) {
        filterConfig = config
        filterConfigSource.onNext(config)
    }
}