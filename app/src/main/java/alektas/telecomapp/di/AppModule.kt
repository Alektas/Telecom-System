package alektas.telecomapp.di

import alektas.telecomapp.data.SystemStorage
import alektas.telecomapp.domain.Repository
import alektas.telecomapp.domain.entities.SystemProcessor
import alektas.telecomapp.utils.FileWorker
import android.app.Application
import android.content.Context
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class AppModule {

    @Provides
    @Singleton
    fun providesApplicationContext(app: Application): Context = app.applicationContext

    @Provides
    @Singleton
    fun providesSystemStorage(): Repository {
        return SystemStorage()
    }

    @Provides
    @Singleton
    fun providesUsbDriver(context: Context): FileWorker {
        return FileWorker(context)
    }

    @Provides
    @Singleton
    fun providesSystemProcessor(): SystemProcessor {
        return SystemProcessor()
    }
}