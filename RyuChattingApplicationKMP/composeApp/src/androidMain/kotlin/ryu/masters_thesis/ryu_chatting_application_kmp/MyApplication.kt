package ryu.masters_thesis.ryu_chatting_application_kmp

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import ryu.masters_thesis.core.di.domain.corePlatformModule
import ryu.masters_thesis.feature.di.domain.featurePlatformModule
import ryu.masters_thesis.presentation.di.presentationModule
import org.koin.core.context.GlobalContext
import ryu.masters_thesis.feature.bluetoothFinderProtocol.domain.FinderResponder
import ryu.masters_thesis.data.di.domain.dataPlatformModule
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@MyApplication)
            modules(
                corePlatformModule(),
                dataPlatformModule(),
                featurePlatformModule(),
                presentationModule(),
            )
        }
        // Eager start BFP responder → removed
    }
}