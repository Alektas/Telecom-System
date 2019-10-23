package alektas.telecomapp.di

import alektas.telecomapp.data.SystemStorage
import alektas.telecomapp.domain.Repository
import alektas.telecomapp.domain.entities.SystemProcessor
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class AppModule {
    @Provides
    @Singleton
    fun providesSystemStorage(): Repository {
        return SystemStorage()
    }

    @Provides
    @Singleton
    fun providesSystemProcessor(): SystemProcessor {
        return SystemProcessor()
    }
}