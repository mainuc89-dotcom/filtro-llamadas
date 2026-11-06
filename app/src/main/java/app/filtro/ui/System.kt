package app.filtro.ui

import android.Manifest
import android.app.role.RoleManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.filtro.core.NumberInfo
import kotlinx.coroutines.delay

/** Estado de los permisos del sistema; se vuelve a leer cada vez que la app pasa al frente. */
@Stable
class SystemStatus(private val context: Context) {
    var roleHeld by mutableStateOf(false)
        private set
    var overlay by mutableStateOf(false)
        private set
    var notifications by mutableStateOf(true)
        private set

    /** Cambia cada vez que se pide volver a revisar (tras un diálogo de permisos). */
    var pulse by mutableIntStateOf(0)
        private set

    fun recheck() {
        refresh()
        pulse++
    }

    fun refresh() {
        val rm = context.getSystemService(RoleManager::class.java)
        roleHeld = rm?.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) == true
        overlay = Settings.canDrawOverlays(context)
        notifications = Build.VERSION.SDK_INT < 33 ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }

    init {
        refresh()
    }
}

@Composable
fun rememberSystemStatus(): SystemStatus {
    val context = LocalContext.current
    val status = remember { SystemStatus(context.applicationContext) }
    val owner = LocalLifecycleOwner.current
    var resumes by remember { mutableIntStateOf(0) }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) resumes++
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    // Android puede tardar unos segundos en aplicar un rol o permiso recién concedido.
    LaunchedEffect(resumes, status.pulse) {
        status.refresh()
        delay(1200)
        status.refresh()
        delay(3000)
        status.refresh()
    }
    return status
}

class SystemActions(
    val requestRole: () -> Unit,
    val openDefaultApps: () -> Unit,
    val requestOverlay: () -> Unit,
    val requestNotifications: () -> Unit,
)

@Composable
fun rememberSystemActions(status: SystemStatus): SystemActions {
    val context = LocalContext.current
    val role = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { status.recheck() }
    val settings = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { status.recheck() }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { status.recheck() }

    return remember(status) {
        val openDefaultApps = {
            runCatching { settings.launch(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)) }
            Unit
        }
        SystemActions(
            requestRole = {
                val rm = context.getSystemService(RoleManager::class.java)
                if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
                    runCatching { role.launch(rm.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)) }
                        .onFailure { openDefaultApps() }
                } else {
                    openDefaultApps()
                }
            },
            openDefaultApps = openDefaultApps,
            requestOverlay = {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}"),
                )
                runCatching { settings.launch(intent) }
            },
            requestNotifications = {
                if (Build.VERSION.SDK_INT >= 33) {
                    permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    runCatching { settings.launch(intent) }
                }
            },
        )
    }
}

fun copyNumber(context: Context, info: NumberInfo) {
    val cm = context.getSystemService(ClipboardManager::class.java) ?: return
    cm.setPrimaryClip(ClipData.newPlainText("Número", info.display))
    if (Build.VERSION.SDK_INT < 33) Toast.makeText(context, "Número copiado", Toast.LENGTH_SHORT).show()
}

/** Abre el navegador con una búsqueda del número (la app en sí no usa internet). */
fun searchNumber(context: Context, info: NumberInfo) {
    val query = if (info.isDomestic && info.national != null) info.national else "+" + info.intlDigits
    val uri = Uri.parse("https://www.google.com/search?q=" + Uri.encode(query))
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }.onFailure {
        Toast.makeText(context, "No hay un navegador disponible", Toast.LENGTH_SHORT).show()
    }
}
