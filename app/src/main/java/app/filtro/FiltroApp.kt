package app.filtro

import android.app.Application
import app.filtro.data.Store
import app.filtro.service.Notifier

class FiltroApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Store.init(this)
        Notifier.ensureChannel(this)
    }
}
