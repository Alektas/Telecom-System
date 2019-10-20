package alektas.telecomapp.di

import alektas.telecomapp.ui.chart.ChartViewModel
import alektas.telecomapp.ui.demodulators.QpskDemodulatorViewModel
import alektas.telecomapp.ui.filters.FirFilterViewModel
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [AppModule::class])
interface AppComponent {
    fun inject(vm: ChartViewModel)
    fun inject(vm: QpskDemodulatorViewModel)
    fun inject(vm: FirFilterViewModel)
}