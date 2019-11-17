package alektas.telecomapp

import alektas.telecomapp.di.AppComponent
import alektas.telecomapp.di.DaggerAppComponent
import android.app.Application

class App: Application() {
    companion object {
        lateinit var component: AppComponent
    }

    override fun onCreate() {
        super.onCreate()
        component = DaggerAppComponent
            .builder()
            .application(this)
            .build()
    }
}