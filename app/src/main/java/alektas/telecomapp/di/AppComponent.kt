package alektas.telecomapp.di

import alektas.telecomapp.data.SystemStorage
import alektas.telecomapp.domain.entities.SystemProcessor
import alektas.telecomapp.ui.datasource.DataSourceViewModel
import alektas.telecomapp.ui.decoder.DecoderViewModel
import alektas.telecomapp.ui.demodulators.QpskDemodulatorViewModel
import alektas.telecomapp.ui.demodulators.processing.DemodulatorProcessViewModel
import alektas.telecomapp.ui.demodulators.filter.FirFilterViewModel
import alektas.telecomapp.ui.demodulators.filter.ichannel.IChannelViewModel
import alektas.telecomapp.ui.demodulators.filter.qchannel.QChannelViewModel
import alektas.telecomapp.ui.demodulators.generator.DemodulatorGeneratorViewModel
import alektas.telecomapp.ui.demodulators.input.DemodulatorInputViewModel
import alektas.telecomapp.ui.demodulators.output.DemodulatorOutputViewModel
import alektas.telecomapp.ui.statistic.StatisticViewModel
import android.app.Application
import dagger.BindsInstance
import alektas.telecomapp.ui.statistic.ber.BerViewModel
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [AppModule::class, PreferencesModule::class])
interface AppComponent {
    fun inject(proc: SystemProcessor)
    fun inject(vm: DataSourceViewModel)
    fun inject(vm: QpskDemodulatorViewModel)
    fun inject(vm: DemodulatorInputViewModel)
    fun inject(vm: DemodulatorGeneratorViewModel)
    fun inject(vm: FirFilterViewModel)
    fun inject(vm: DemodulatorProcessViewModel)
    fun inject(vm: DemodulatorOutputViewModel)
    fun inject(vm: DecoderViewModel)
    fun inject(vm: IChannelViewModel)
    fun inject(vm: QChannelViewModel)
    fun inject(vm: StatisticViewModel)
    fun inject(vm: BerViewModel)
    fun inject(storage: SystemStorage)

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun application(app: Application): Builder
        fun build(): AppComponent
    }
}