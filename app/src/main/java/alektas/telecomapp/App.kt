package alektas.telecomapp

import alektas.telecomapp.di.AppComponent
import alektas.telecomapp.di.DaggerAppComponent
import android.app.Application

class App: Application() {
    companion object {
        val component: AppComponent = DaggerAppComponent.builder().build()
    }
}