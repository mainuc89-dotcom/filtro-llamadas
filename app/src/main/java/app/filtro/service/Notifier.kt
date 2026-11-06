package app.filtro.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import app.filtro.MainActivity
import app.filtro.R
import app.filtro.core.NumberInfo
import app.filtro.data.Store
import java.time.LocalDate
import java.time.ZoneId

object Notifier {
    private const val CHANNEL = "filtradas"
    private const val ID = 1001

    fun ensureChannel(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(CHANNEL, "Llamadas bloqueadas", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Aviso silencioso cada vez que Filtro bloquea o silencia una llamada"
            setShowBadge(false)
        }
        nm.createNotificationChannel(channel)
    }

    fun filtered(context: Context, info: NumberInfo, title: String, silenced: Boolean) {
        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        ensureChannel(context)

        val zone = ZoneId.systemDefault()
        val startOfDay = LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()
        val today = Store.log.value.count { it.wasFiltered && it.time >= startOfDay }

        val open = Intent(context, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_TAB, "registro")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pi = PendingIntent.getActivity(
            context, 1, open, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val n = Notification.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notif)
            .setColor(0xFF2E5E4E.toInt())
            .setContentTitle(if (silenced) "Llamada silenciada" else "Llamada bloqueada")
            .setContentText("${info.display} · $title")
            .setSubText(if (today > 1) "$today hoy" else null)
            .setCategory(Notification.CATEGORY_STATUS)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setShowWhen(true)
            .build()
        nm.notify(ID, n)
    }
}
