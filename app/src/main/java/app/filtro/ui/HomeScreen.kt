package app.filtro.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.filtro.core.Classifier
import app.filtro.core.Config
import app.filtro.core.LogEntry
import app.filtro.core.Rules
import app.filtro.core.normalizePrefix
import app.filtro.data.Store
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun HomeScreen(
    config: Config,
    log: List<LogEntry>,
    totalFiltered: Int,
    status: SystemStatus,
    actions: SystemActions,
    lookupRequest: MutableState<String?>,
    onOpenLog: () -> Unit,
    onOpenEntry: (LogEntry) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val pending = lookupRequest.value
    LaunchedEffect(pending) {
        if (pending != null) {
            query = pending
            lookupRequest.value = null
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().imePadding(),
        contentPadding = ScreenPadding,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { ScreenHeader("Filtro", "Bloqueo e identificación de llamadas") }

        item { StatusCard(config, status, actions) }

        if (status.roleHeld && config.showOverlay && !status.overlay) {
            item {
                SetupCard(
                    icon = Icons.Outlined.Layers,
                    title = "Ver quién llama",
                    text = "Permite que Filtro muestre el tipo de número sobre la pantalla de llamada.",
                    action = "Permitir",
                    onClick = actions.requestOverlay,
                )
            }
        }
        if (status.roleHeld && config.notifyBlocked && !status.notifications) {
            item {
                SetupCard(
                    icon = Icons.Outlined.Notifications,
                    title = "Avisos de bloqueo",
                    text = "Un aviso silencioso cada vez que Filtro bloquea una llamada.",
                    action = "Permitir",
                    onClick = actions.requestNotifications,
                )
            }
        }

        item { Stats(log, totalFiltered) }

        item { SectionTitle("Consultar un número") }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Escribe o pega un número") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) { Icon(Icons.Outlined.Close, contentDescription = "Borrar") }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Search),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            AnimatedVisibility(
                visible = Classifier.digitsOf(query).length >= 3,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                LookupResult(query, config)
            }
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionTitle("Recientes", Modifier.weight(1f))
                if (log.isNotEmpty()) TextButton(onClick = onOpenLog) { Text("Ver todo") }
            }
        }
        if (log.isEmpty()) {
            item {
                Text(
                    "Aquí verás las llamadas que Filtro bloquee o identifique.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
        } else {
            item {
                Panel {
                    log.take(4).forEachIndexed { i, e ->
                        if (i > 0) PanelDivider()
                        LogRow(e, Classifier.analyze(e.number.ifEmpty { e.key }, config.countryCode).display) { onOpenEntry(e) }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusCard(config: Config, status: SystemStatus, actions: SystemActions) {
    val protecting = status.roleHeld && config.active
    val scheme = MaterialTheme.colorScheme
    val active = config.prefixes.filter { it.enabled }.map { normalizePrefix(it.prefix) }
    val summary = when {
        !status.roleHeld -> "Falta un paso: permite que Filtro revise las llamadas antes de que suenen. Tus contactos siempre suenan."
        !config.active -> "En pausa. Las llamadas suenan normalmente."
        active.isEmpty() && !config.blockInternational -> "Sin reglas de bloqueo. Filtro solo identifica las llamadas."
        else -> buildString {
            append(if (config.silenceInstead) "Silenciando " else "Bloqueando ")
            append(active.joinToString(" · "))
            if (config.blockInternational) {
                if (active.isNotEmpty()) append(" · ")
                append("internacionales")
            }
        }
    }

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = if (protecting) scheme.primaryContainer else scheme.surfaceContainer,
        border = if (protecting) null else BorderStroke(1.dp, scheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(start = 22.dp, end = 18.dp, top = 20.dp, bottom = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusDot(if (protecting) scheme.primary else if (status.roleHeld) scheme.outline else scheme.error)
                Spacer(Modifier.width(12.dp))
                Text(
                    when {
                        !status.roleHeld -> "Filtro está inactivo"
                        config.active -> "Protección activa"
                        else -> "Filtro en pausa"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    color = if (protecting) scheme.onPrimaryContainer else scheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                if (status.roleHeld) {
                    Switch(checked = config.active, onCheckedChange = { on -> Store.update { it.copy(active = on) } })
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                summary,
                style = MaterialTheme.typography.bodyMedium,
                color = if (protecting) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
            )
            if (!status.roleHeld) {
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = actions.requestRole) { Text("Activar filtro") }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = actions.openDefaultApps) { Text("Abrir ajustes") }
                }
            }
        }
    }
}

@Composable
private fun SetupCard(icon: ImageVector, title: String, text: String, action: String, onClick: () -> Unit) {
    Panel {
        Row(
            Modifier.padding(start = 20.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = onClick) { Text(action) }
        }
    }
}

@Composable
private fun Stats(log: List<LogEntry>, total: Int) {
    val counts = remember(log) {
        val zone = ZoneId.systemDefault()
        val startOfDay = LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()
        val weekAgo = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
        val filtered = log.filter { it.wasFiltered }
        Pair(filtered.count { it.time >= startOfDay }, filtered.count { it.time >= weekAgo })
    }
    Column {
        SectionTitle("Llamadas filtradas")
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("Hoy", counts.first, Modifier.weight(1f))
            StatTile("7 días", counts.second, Modifier.weight(1f))
            StatTile("Total", total, Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatTile(label: String, value: Int, modifier: Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(value.toString(), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun LookupResult(query: String, config: Config) {
    val info = remember(query, config.countryCode) { Classifier.analyze(query, config.countryCode) }
    val verdict = Rules.decide(info, config.copy(active = true))
    val name = config.labels[info.key]
    val tones = LocalTones.current
    val accent = tones.of(info.tone)

    Panel {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusDot(accent)
                Spacer(Modifier.width(10.dp))
                Text(
                    (name ?: info.title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            if (name != null) {
                Text(
                    info.title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 20.dp),
                )
            }
            Spacer(Modifier.height(10.dp))
            Digits(info.display)
            Spacer(Modifier.height(4.dp))
            Text(info.detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (verdict.block) Icons.Outlined.Block else Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = if (verdict.block) tones.warning else tones.neutral,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (verdict.block) {
                        (if (config.silenceInstead) "Se silenciaría · " else "Se bloquearía · ") + verdict.reason
                    } else {
                        "Sonaría normalmente"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (verdict.block) tones.warning else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        PanelDivider()
        NumberActions(info = info, config = config)
    }
}
