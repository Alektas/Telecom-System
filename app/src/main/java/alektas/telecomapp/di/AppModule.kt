package alektas.telecomapp.di

import alektas.telecomapp.data.TelecomSystem
import alektas.telecomapp.domain.Repository
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class AppModule {
    @Provides
    @Singleton
    fun providesSystem(): Repository {
        return TelecomSystem()
    }
}