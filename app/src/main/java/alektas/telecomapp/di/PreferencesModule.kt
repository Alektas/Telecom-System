package alektas.telecomapp.di

import alektas.telecomapp.R
import alektas.telecomapp.domain.entities.coders.DataCodesContract
import alektas.telecomapp.domain.entities.configs.ChannelsConfig
import alektas.telecomapp.domain.entities.configs.DecoderConfig
import alektas.telecomapp.domain.entities.contracts.CdmaContract
import alektas.telecomapp.domain.entities.contracts.QpskContract
import alektas.telecomapp.domain.entities.configs.DemodulatorConfig
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
    @Named("decoderPrefs")
    fun providesDecoderPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(
            context.getString(R.string.settings_decoder_key),
            Context.MODE_PRIVATE
        )
    }

    @Provides
    @Named("sourceSnr")
    fun providesSourceSnr(context: Context, @Named("sourcePrefs") prefs: SharedPreferences): Double {
        return prefs.getFloat(
            context.getString(R.string.source_noise_snr_key),
            QpskContract.DEFAULT_SIGNAL_NOISE_RATE.toFloat()
        ).toDouble()
    }

    @Provides
    @Named("interferenceRate")
    fun providesInterferenceRate(context: Context, @Named("sourcePrefs") prefs: SharedPreferences): Double {
        return prefs.getFloat(
            context.getString(R.string.source_interference_snr_key),
            QpskContract.DEFAULT_SIGNAL_NOISE_RATE.toFloat()
        ).toDouble()
    }

    @Provides
    @Named("interferenceSparseness")
    fun providesInterferenceSparseness(context: Context, @Named("sourcePrefs") prefs: SharedPreferences): Double {
        return prefs.getFloat(
            context.getString(R.string.source_interference_sparseness_key),
            QpskContract.DEFAULT_INTERFERENCE_SPARSENESS.toFloat()
        ).toDouble()
    }

    @Provides
    @Named("sourceSnrEnabled")
    fun providesSourceSnrEnabled(context: Context, @Named("sourcePrefs") prefs: SharedPreferences): Boolean {
        return prefs.getBoolean(context.getString(R.string.source_noise_enable_key), false)
    }

    @Provides
    @Named("sourceInterferenceEnabled")
    fun providesInterferenceEnabled(context: Context, @Named("sourcePrefs") prefs: SharedPreferences): Boolean {
        return prefs.getBoolean(context.getString(R.string.source_interference_enable_key), false)
    }

    @Provides
    fun providesChannelsConfig(
        context: Context,
        @Named("sourcePrefs") prefs: SharedPreferences
    ): ChannelsConfig {
        val channelsCodeType = prefs.getInt(
            context.getString(R.string.source_channels_codetype_key),
            CdmaContract.DEFAULT_CHANNEL_CODE_TYPE
        )

        val carrierFrequency = prefs.getFloat(
            context.getString(R.string.source_channels_freq_key),
            (1.0e-6 * QpskContract.DEFAULT_CARRIER_FREQUENCY).toFloat()
        ).toDouble()

        val dataSpeed = prefs.getFloat(
            context.getString(R.string.source_channels_dataspeed_key),
            (1.0e-3 / QpskContract.DEFAULT_DATA_BIT_TIME).toFloat()
        ).toDouble()

        val channelCount = prefs.getInt(
            context.getString(R.string.source_channels_count_key),
            CdmaContract.DEFAULT_CHANNEL_COUNT
        )

        val channelsCodeLength = prefs.getInt(
            context.getString(R.string.source_channels_codesize_key),
            CdmaContract.DEFAULT_CHANNEL_CODE_SIZE
        )

        val frameLength = prefs.getInt(
            context.getString(R.string.source_channels_framesize_key),
            CdmaContract.DEFAULT_FRAME_SIZE
        )

        val isDataCodingEnabled = prefs.getBoolean(
            context.getString(R.string.source_data_coding_enable_key),
            DataCodesContract.DEFAULT_IS_CODING_ENABLED
        )

        val dataCodeType = prefs.getInt(
            context.getString(R.string.source_data_coding_type_key),
            DataCodesContract.HAMMING
        )

        return ChannelsConfig(
            channelCount,
            carrierFrequency,
            dataSpeed,
            channelsCodeType,
            channelsCodeLength,
            frameLength,
            isDataCodingEnabled,
            dataCodeType
        )
    }

    @Provides
    fun providesDemodulatorConfig(
        context: Context,
        @Named("demodulatorPrefs") prefs: SharedPreferences,
        filterConfig: FilterConfig
    ): DemodulatorConfig {
        val delayCompensation = prefs.getFloat(
            context.getString(R.string.demodulator_process_delay_compensation_key),
            QpskContract.DEFAULT_FILTERS_DELAY_COMPENSATION
        )

        val genFreq = prefs.getFloat(
            context.getString(R.string.demodulator_generator_freq_key),
            (QpskContract.DEFAULT_CARRIER_FREQUENCY.toFloat() * 1.0e-6).toFloat() // Гц -> МГц
        ).let { it * 1.0e6 } // МГц -> Гц

        val dataspeed = prefs.getFloat(
            context.getString(R.string.demodulator_process_dataspeed_key),
            (1.0e-3 / QpskContract.DEFAULT_DATA_BIT_TIME).toFloat()
        ).let { it * 1.0e3 }
        val bitTime = 1 / dataspeed

        val frameLength = prefs.getInt(
            context.getString(R.string.demodulator_process_frame_length_key),
            CdmaContract.DEFAULT_FRAME_SIZE
        )

        val codeLength = prefs.getInt(
            context.getString(R.string.demodulator_process_code_length_key),
            CdmaContract.DEFAULT_CHANNEL_CODE_SIZE
        )

        return DemodulatorConfig(
            delayCompensation = delayCompensation,
            carrierFrequency = genFreq,
            frameLength = frameLength,
            codeLength = codeLength,
            bitTime = bitTime,
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

    @Provides
    fun providesDecoderConfig(
        context: Context,
        @Named("decoderPrefs") prefs: SharedPreferences
    ): DecoderConfig {
        val isAuto = prefs.getBoolean(
            context.getString(R.string.decoder_channels_autodetection_key),
            CdmaContract.DEFAULT_IS_AUTO_DETECTION_ENABLED
        )

        val channelsCodeType = prefs.getInt(
            context.getString(R.string.decoder_channels_codetype_key),
            CdmaContract.DEFAULT_CHANNEL_CODE_TYPE
        )

        val threshold = prefs.getFloat(
            context.getString(R.string.decoder_threshold_key),
            QpskContract.DEFAULT_SIGNAL_THRESHOLD
        )

        val channelCount = prefs.getInt(
            context.getString(R.string.decoder_channels_count_key),
            CdmaContract.DEFAULT_CHANNEL_COUNT
        )

        val codeLength = prefs.getInt(
            context.getString(R.string.decoder_code_length_key),
            CdmaContract.DEFAULT_CHANNEL_CODE_SIZE
        )

        val isDataCoding = prefs.getBoolean(
            context.getString(R.string.decoder_data_decoding_enable_key),
            DataCodesContract.DEFAULT_IS_CODING_ENABLED
        )

        val dataCodeType = prefs.getInt(
            context.getString(R.string.decoder_data_coding_type_key),
            DataCodesContract.HAMMING
        )

        return DecoderConfig(
            isAuto,
            channelCount,
            channelsCodeType,
            codeLength,
            threshold,
            isDataCoding,
            dataCodeType
        )
    }
}