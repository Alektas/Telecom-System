package alektas.telecomapp.di

import alektas.telecomapp.data.SystemStorage
import alektas.telecomapp.domain.entities.SystemProcessor
import alektas.telecomapp.domain.entities.configs.ChannelsConfig
import alektas.telecomapp.ui.MainViewModel
import alektas.telecomapp.ui.datasource.external.FileDataSourceViewModel
import alektas.telecomapp.ui.datasource.simulation.ChannelsSettingsViewModel
import alektas.telecomapp.ui.datasource.simulation.EtherSettingsViewModel
import alektas.telecomapp.ui.datasource.simulation.SimulationDataSourceViewModel
import alektas.telecomapp.ui.decoder.DecoderViewModel
import alektas.telecomapp.ui.demodulator.QpskDemodulatorViewModel
import alektas.telecomapp.ui.demodulator.processing.DemodulatorProcessViewModel
import alektas.telecomapp.ui.demodulator.filter.FirFilterViewModel
import alektas.telecomapp.ui.demodulator.filter.ichannel.IChannelViewModel
import alektas.telecomapp.ui.demodulator.filter.qchannel.QChannelViewModel
import alektas.telecomapp.ui.demodulator.generator.DemodulatorGeneratorViewModel
import alektas.telecomapp.ui.demodulator.input.DemodulatorInputViewModel
import alektas.telecomapp.ui.demodulator.output.DemodulatorOutputViewModel
import alektas.telecomapp.ui.statistic.StatisticViewModel
import android.app.Application
import dagger.BindsInstance
import alektas.telecomapp.ui.statistic.ber.BerViewModel
import android.content.Context
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [AppModule::class, PreferencesModule::class])
interface AppComponent {
    fun inject(proc: SystemProcessor)
    fun inject(vm: SimulationDataSourceViewModel)
    fun inject(vm: FileDataSourceViewModel)
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
    fun inject(vm: MainViewModel)
    fun inject(vm: EtherSettingsViewModel)
    fun inject(vm: ChannelsSettingsViewModel)
    fun inject(storage: SystemStorage)

    fun context(): Context
    fun channelsConfig(): ChannelsConfig

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun application(app: Application): Builder
        fun build(): AppComponent
    }
}