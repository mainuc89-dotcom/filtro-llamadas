package app.filtro

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import app.filtro.data.Store
import app.filtro.ui.AppRoot
import app.filtro.ui.FiltroTheme

class MainActivity : ComponentActivity() {

    /** Pestaña pedida desde afuera (notificación, pruebas). */
    private val tabRequest = mutableStateOf<String?>(null)

    /** Número para consultar (texto seleccionado en otra app → "Consultar en Filtro"). */
    private val lookupRequest = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Store.init(this)
        enableEdgeToEdge()
        if (savedInstanceState == null) handle(intent)
        setContent {
            FiltroTheme {
                AppRoot(tabRequest = tabRequest, lookupRequest = lookupRequest)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handle(intent)
    }

    private fun handle(intent: Intent?) {
        if (intent == null) return
        intent.getStringExtra(EXTRA_COUNTRY)?.let { code ->
            Store.setCountry(code.filter { it in '0'..'9' })
        }
        intent.getStringExtra(EXTRA_TAB)?.let { tabRequest.value = it }
        when (intent.action) {
            Intent.ACTION_PROCESS_TEXT -> {
                val text = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
                if (!text.isNullOrBlank()) {
                    lookupRequest.value = text
                    tabRequest.value = "inicio"
                }
            }
        }
    }

    companion object {
        const val EXTRA_TAB = "tab"
        const val EXTRA_COUNTRY = "pais"
    }
}
