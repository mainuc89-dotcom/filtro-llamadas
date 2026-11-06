package app.filtro.service

import android.telecom.Call
import android.telecom.CallScreeningService
import android.telecom.CallScreeningService.CallResponse
import android.util.Log
import app.filtro.core.Classifier
import app.filtro.core.LogEntry
import app.filtro.core.NumberInfo
import app.filtro.core.Rules
import app.filtro.data.Store

/**
 * Android llama a este servicio antes de que suene cada llamada entrante
 * (solo números que no están en tus contactos). Hay que responder en menos de 5 segundos.
 */
class ScreeningService : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        if (callDetails.callDirection != Call.Details.DIRECTION_INCOMING) {
            respondToCall(callDetails, CallResponse.Builder().build())
            return
        }

        val decision = runCatching { decide(callDetails) }.getOrNull()
        if (decision == null) {
            // Ante cualquier error, la llamada suena normalmente.
            respondToCall(callDetails, CallResponse.Builder().build())
            return
        }
        respondToCall(callDetails, decision.response)

        runCatching { afterResponse(decision) }
            .onFailure { Log.w(TAG, "No se pudo registrar la llamada", it) }
    }

    private class Decision(
        val response: CallResponse,
        val raw: String,
        val info: NumberInfo,
        val name: String?,
        val outcome: String,
        val reason: String,
    )

    private fun decide(details: Call.Details): Decision {
        Store.init(this)
        val cfg = Store.config.value
        val raw = details.handle?.schemeSpecificPart.orEmpty()
        val info = Classifier.analyze(raw, cfg.countryCode)
        val verdict = Rules.decide(info, cfg)
        val silence = verdict.block && cfg.silenceInstead

        val builder = CallResponse.Builder()
        val outcome = when {
            !verdict.block -> LogEntry.OUTCOME_ALLOWED
            silence -> {
                builder.setSilenceCall(true)
                LogEntry.OUTCOME_SILENCED
            }
            else -> {
                builder.setDisallowCall(true)
                    .setRejectCall(true)
                    .setSkipCallLog(false)
                    .setSkipNotification(true)
                LogEntry.OUTCOME_BLOCKED
            }
        }
        Log.i(TAG, "Llamada ${info.display}: $outcome ${verdict.reason}")
        return Decision(builder.build(), raw, info, cfg.labels[info.key], outcome, verdict.reason)
    }

    private fun afterResponse(d: Decision) {
        val cfg = Store.config.value
        val title = d.name ?: d.info.title
        Store.addLog(
            LogEntry(
                time = System.currentTimeMillis(),
                number = d.raw,
                key = d.info.key,
                outcome = d.outcome,
                title = title,
                reason = d.reason,
            )
        )
        val filtered = d.outcome != LogEntry.OUTCOME_ALLOWED
        if (filtered && cfg.notifyBlocked) {
            Notifier.filtered(this, d.info, title, d.outcome == LogEntry.OUTCOME_SILENCED)
        }
        // Las bloqueadas no suenan, así que no hace falta mostrar nada en pantalla.
        if (cfg.showOverlay && d.outcome != LogEntry.OUTCOME_BLOCKED) {
            CallerOverlay.show(this, d.info, d.name, d.outcome == LogEntry.OUTCOME_SILENCED)
        }
    }

    companion object {
        const val TAG = "Filtro"
    }
}
