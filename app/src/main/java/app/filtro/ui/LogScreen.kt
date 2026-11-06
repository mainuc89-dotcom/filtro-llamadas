package app.filtro.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.filtro.core.Classifier
import app.filtro.core.Config
import app.filtro.core.LogEntry
import app.filtro.data.Store

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogScreen(config: Config, log: List<LogEntry>, onOpenEntry: (LogEntry) -> Unit) {
    var filter by rememberSaveable { mutableIntStateOf(0) }
    var confirmClear by remember { mutableStateOf(false) }

    val shown = when (filter) {
        1 -> log.filter { it.wasFiltered }
        2 -> log.filter { !it.wasFiltered }
        else -> log
    }
    val groups = remember(shown) { shown.groupBy { dayLabel(it.time) }.toList() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = ScreenPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ScreenHeader(
                title = "Registro",
                subtitle = "Llamadas que Filtro revisó",
                action = {
                    if (log.isNotEmpty()) {
                        IconButton(onClick = { confirmClear = true }) {
                            Icon(Icons.Outlined.DeleteSweep, contentDescription = "Borrar registro")
                        }
                    }
                },
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Todas", "Bloqueadas", "Identificadas").forEachIndexed { i, label ->
                    FilterChip(
                        selected = filter == i,
                        onClick = { filter = i },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    )
                }
            }
        }

        if (shown.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Outlined.History,
                    title = if (log.isEmpty()) "Todo tranquilo" else "Nada por aquí",
                    text = if (log.isEmpty()) {
                        "Cuando llegue una llamada de un número que no está en tus contactos, aparecerá aquí."
                    } else {
                        "No hay llamadas con este filtro."
                    },
                )
            }
        } else {
            groups.forEach { (day, entries) ->
                item { SectionTitle(day) }
                item {
                    Panel {
                        entries.forEachIndexed { i, e ->
                            if (i > 0) PanelDivider()
                            LogRow(e, Classifier.analyze(e.number.ifEmpty { e.key }, config.countryCode).display) {
                                onOpenEntry(e)
                            }
                        }
                    }
                }
            }
            item {
                Text(
                    "Se guardan las últimas 500 llamadas, solo en este teléfono.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                )
            }
        }
    }

    if (confirmClear) {
        ConfirmDialog(
            title = "¿Borrar el registro?",
            text = "Se eliminará el historial de llamadas de Filtro. Tus reglas y nombres no se tocan.",
            confirmText = "Borrar",
            onDismiss = { confirmClear = false },
            onConfirm = {
                Store.clearLog()
                confirmClear = false
            },
        )
    }
}
