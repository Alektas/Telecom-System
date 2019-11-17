package alektas.telecomapp.di

import alektas.telecomapp.R
import alektas.telecomapp.domain.entities.QpskContract
import alektas.telecomapp.domain.entities.demodulators.DemodulatorConfig
import alektas.telecomapp.domain.entities.filters.FilterConfig
import android.content.Context
import android.content.SharedPreferences
import dagger.Module
import dagger.Provides
import javax.inject.Named

@Module
class PreferencesModule {

    @Provides
    @Named("sourcePrefs")
    fun providesSourcePreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(
            context.getString(R.string.settings_source_key),
            Context.MODE_PRIVATE
        )
    }

    @Provides
    @Named("demodulatorPrefs")
    fun providesDemodulatorPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(
            context.getString(R.string.settings_demodulator_key),
            Context.MODE_PRIVATE
        )
    }

    @Provides
    fun providesDemodulatorConfig(
        context: Context,
        @Named("demodulatorPrefs") demodulatorPrefs: SharedPreferences,
        filterConfig: FilterConfig
    ): DemodulatorConfig {
        val genFreq = demodulatorPrefs.getFloat(
            context.getString(R.string.demodulator_generator_freq_key),
            QpskContract.DEFAULT_CARRIER_FREQUENCY.toFloat()
        ).let { it * 1.0e6 }
        return DemodulatorConfig(
            carrierFrequency = genFreq,
            filterConfig = filterConfig
        )
    }


    @Provides
    fun providesDemodulatorFilterConfig(
        context: Context,
        @Named("demodulatorPrefs") demodulatorPrefs: SharedPreferences
    ): FilterConfig {
        val order = demodulatorPrefs.getInt(
            context.getString(R.string.demodulator_filter_order_key),
            FilterConfig.DEFAULT_ORDER
        )

        val cutoffFreq = demodulatorPrefs.getFloat(
            context.getString(R.string.demodulator_filter_cutoff_freq_key),
            FilterConfig.DEFAULT_BANDWIDTH.toFloat()
        ).toDouble()

        val windowType = demodulatorPrefs.getInt(
            context.getString(R.string.demodulator_filter_window_type_key),
            FilterConfig.DEFAULT_WINDOW_TYPE
        )

        return FilterConfig(
            order = order,
            bandwidth = cutoffFreq,
            windowType = windowType
        )
    }
}