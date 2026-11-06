package app.filtro.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.filtro.core.Classifier
import app.filtro.core.Config
import app.filtro.core.LogEntry
import app.filtro.core.NumberInfo
import app.filtro.data.Store

/** Acciones sobre un número: copiar, buscar, nombrar, bloquear o permitir. */
@Composable
fun NumberActions(info: NumberInfo, config: Config, onAfter: () -> Unit = {}) {
    val context = LocalContext.current
    var naming by remember { mutableStateOf(false) }
    val name = config.labels[info.key]
    val isBlocked = info.key in config.blocked
    val isAllowed = info.key in config.allowed
    val tones = LocalTones.current

    ActionRow(Icons.Outlined.ContentCopy, "Copiar número") { copyNumber(context, info) }
    ActionRow(Icons.Outlined.Search, "Buscar en internet") { searchNumber(context, info) }
    ActionRow(Icons.Outlined.Edit, if (name == null) "Ponerle un nombre" else "Cambiar nombre") { naming = true }
    if (isBlocked) {
        ActionRow(Icons.Outlined.RemoveCircleOutline, "Desbloquear este número") {
            Store.setBlocked(info.key, false); onAfter()
        }
    } else {
        ActionRow(Icons.Outlined.Block, "Bloquear siempre este número", color = tones.warning) {
            Store.setBlocked(info.key, true); onAfter()
        }
    }
    if (isAllowed) {
        ActionRow(Icons.Outlined.RemoveCircleOutline, "Quitar de permitidos") {
            Store.setAllowed(info.key, false); onAfter()
        }
    } else {
        ActionRow(Icons.Outlined.CheckCircle, "Permitir siempre este número") {
            Store.setAllowed(info.key, true); onAfter()
        }
    }

    if (naming) {
        NameDialog(
            fixedNumber = info.display,
            initialName = name.orEmpty(),
            numberHint = { null },
            onDismiss = { naming = false },
            onRemove = if (name != null) ({ Store.setLabel(info.key, null); naming = false }) else null,
            onConfirm = { _, newName -> Store.setLabel(info.key, newName); naming = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntrySheet(entry: LogEntry, config: Config, onDismiss: () -> Unit) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val info = remember(entry, config.countryCode) {
        Classifier.analyze(entry.number.ifEmpty { entry.key }, config.countryCode)
    }
    val name = config.labels[info.key]
    val tones = LocalTones.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = state,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 12.dp)) {
            Column(Modifier.padding(horizontal = 24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusDot(tones.of(info.tone))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        (info.title).uppercase(ES),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(name ?: info.display, style = MaterialTheme.typography.headlineMedium)
                if (name != null) {
                    Text(info.display, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(6.dp))
                Text(info.detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                val what = buildString {
                    append(outcomeText(entry))
                    if (entry.reason.isNotEmpty() && entry.outcome != LogEntry.OUTCOME_ALLOWED) {
                        append(" por ")
                        append(entry.reason.replaceFirstChar { it.lowercase(ES) })
                    }
                    append(" · ")
                    append(dayLabel(entry.time))
                    append(", ")
                    append(timeLabel(entry.time))
                }
                Text(what, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
            }
            PanelDivider()
            NumberActions(info = info, config = config)
            ActionRow(Icons.Outlined.Delete, "Quitar del registro", color = MaterialTheme.colorScheme.onSurfaceVariant) {
                Store.removeLog(entry)
                onDismiss()
            }
        }
    }
}
