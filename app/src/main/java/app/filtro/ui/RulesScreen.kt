package app.filtro.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.filtro.core.Classifier
import app.filtro.core.Config
import app.filtro.core.Countries
import app.filtro.core.Rules
import app.filtro.core.normalizePrefix
import app.filtro.data.Store

private sealed interface RulesDialog {
    data object AddPrefix : RulesDialog
    data class PrefixInfo(val prefix: String) : RulesDialog
    data object AddBlocked : RulesDialog
    data object AddAllowed : RulesDialog
    data object AddLabel : RulesDialog
    data class EditLabel(val key: String) : RulesDialog
    data object Country : RulesDialog
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RulesScreen(config: Config, status: SystemStatus, actions: SystemActions) {
    var dialog by remember { mutableStateOf<RulesDialog?>(null) }
    val home = config.countryCode
    fun display(key: String) = Classifier.analyze(key, home).display
    fun hintFor(text: String): String? {
        if (Classifier.digitsOf(text).length < 3) return null
        val info = Classifier.analyze(text, home)
        return "${info.title} · ${info.display}"
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = ScreenPadding,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { ScreenHeader("Reglas", "Decide qué llamadas no deben sonar") }

        item { SectionTitle("Bloquear por prefijo") }
        item {
            Panel {
                config.prefixes.forEachIndexed { i, rule ->
                    if (i > 0) PanelDivider()
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { dialog = RulesDialog.PrefixInfo(rule.prefix) }
                            .padding(start = 20.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Digits(normalizePrefix(rule.prefix), Modifier.widthIn(min = 56.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(
                            Rules.describePrefix(rule.prefix, home),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        Switch(checked = rule.enabled, onCheckedChange = { Store.setPrefixEnabled(rule.prefix, it) })
                    }
                }
                if (config.prefixes.isNotEmpty()) PanelDivider()
                AddRow("Agregar prefijo") { dialog = RulesDialog.AddPrefix }
            }
        }

        item { SectionTitle("Números bloqueados") }
        item {
            Panel {
                config.blocked.forEach { key ->
                    KeyRow(display(key), config.labels[key]) { Store.setBlocked(key, false) }
                    PanelDivider()
                }
                AddRow("Agregar número") { dialog = RulesDialog.AddBlocked }
            }
        }

        item { SectionTitle("Siempre permitir") }
        item {
            Panel {
                config.allowed.forEach { key ->
                    KeyRow(display(key), config.labels[key]) { Store.setAllowed(key, false) }
                    PanelDivider()
                }
                AddRow("Agregar número") { dialog = RulesDialog.AddAllowed }
            }
        }

        item { SectionTitle("Nombres guardados") }
        item {
            Panel {
                config.labels.entries.sortedBy { it.value.lowercase(ES) }.forEach { (key, name) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { dialog = RulesDialog.EditLabel(key) }
                            .padding(start = 20.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(name, style = MaterialTheme.typography.bodyLarge)
                            Text(display(key), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { Store.setLabel(key, null) }) {
                            Icon(Icons.Outlined.Close, contentDescription = "Quitar", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    PanelDivider()
                }
                AddRow("Agregar nombre") { dialog = RulesDialog.AddLabel }
            }
        }

        item { SectionTitle("Opciones") }
        item {
            Panel {
                SwitchRow(
                    title = "Bloquear llamadas internacionales",
                    subtitle = "Cualquier número de otro país",
                    checked = config.blockInternational,
                    onChange = { on -> Store.update { it.copy(blockInternational = on) } },
                )
                PanelDivider()
                Column(Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
                    Text("Al bloquear", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        if (config.silenceInstead) {
                            "La llamada entra sin sonar y queda como perdida"
                        } else {
                            "La llamada se corta sin sonar ni dejar aviso"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.padding(top = 10.dp))
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = !config.silenceInstead,
                            onClick = { Store.update { it.copy(silenceInstead = false) } },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        ) { Text("Rechazar") }
                        SegmentedButton(
                            selected = config.silenceInstead,
                            onClick = { Store.update { it.copy(silenceInstead = true) } },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        ) { Text("Silenciar") }
                    }
                }
                PanelDivider()
                SwitchRow(
                    title = "Identificador en pantalla",
                    subtitle = if (status.overlay) {
                        "Muestra el tipo de número sobre la llamada entrante"
                    } else {
                        "Necesita el permiso \"Mostrar sobre otras apps\""
                    },
                    checked = config.showOverlay && status.overlay,
                    onChange = { on ->
                        Store.update { it.copy(showOverlay = on) }
                        if (on && !status.overlay) actions.requestOverlay()
                    },
                )
                PanelDivider()
                SwitchRow(
                    title = "Avisar llamadas bloqueadas",
                    subtitle = "Notificación silenciosa, sin sonido",
                    checked = config.notifyBlocked && status.notifications,
                    onChange = { on ->
                        Store.update { it.copy(notifyBlocked = on) }
                        if (on && !status.notifications) actions.requestNotifications()
                    },
                )
                PanelDivider()
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { dialog = RulesDialog.Country }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("País de tu línea", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Para reconocer los números nacionales",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        if (home.isEmpty()) "Sin definir" else "${Countries.name(home) ?: "País"} (+$home)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        item {
            Text(
                "Tus contactos siempre suenan: Filtro no lee tu agenda. Funciona sin internet y nada sale de tu teléfono.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 10.dp),
            )
        }
    }

    when (val d = dialog) {
        null -> Unit
        RulesDialog.AddPrefix -> InputDialog(
            title = "Agregar prefijo",
            label = "Prefijo (ej. 600 o +234)",
            confirmText = "Agregar",
            hint = { t ->
                val p = normalizePrefix(t)
                if (p.isEmpty()) "Se bloquearán los números que empiecen así. Usa + para países." else Rules.describePrefix(p, home)
            },
            isValid = { normalizePrefix(it).let { p -> p.isNotEmpty() && p.length <= 16 } },
            onDismiss = { dialog = null },
            onConfirm = { Store.addPrefix(it); dialog = null },
        )
        is RulesDialog.PrefixInfo -> ConfirmDialog(
            title = "Prefijo ${normalizePrefix(d.prefix)}",
            text = Rules.describePrefix(d.prefix, home) + ". Puedes apagarlo con el interruptor o quitarlo de la lista.",
            confirmText = "Quitar",
            onDismiss = { dialog = null },
            onConfirm = { Store.removePrefix(d.prefix); dialog = null },
        )
        RulesDialog.AddBlocked -> InputDialog(
            title = "Bloquear un número",
            label = "Número",
            confirmText = "Bloquear",
            hint = ::hintFor,
            isValid = { Classifier.digitsOf(it).length >= 3 },
            onDismiss = { dialog = null },
            onConfirm = { Store.setBlocked(Store.keyFor(it), true); dialog = null },
        )
        RulesDialog.AddAllowed -> InputDialog(
            title = "Permitir siempre",
            label = "Número",
            confirmText = "Permitir",
            hint = { t -> hintFor(t) ?: "Suena aunque coincida con un prefijo bloqueado." },
            isValid = { Classifier.digitsOf(it).length >= 3 },
            onDismiss = { dialog = null },
            onConfirm = { Store.setAllowed(Store.keyFor(it), true); dialog = null },
        )
        RulesDialog.AddLabel -> NameDialog(
            fixedNumber = null,
            initialName = "",
            numberHint = ::hintFor,
            onDismiss = { dialog = null },
            onRemove = null,
            onConfirm = { number, name -> Store.setLabel(Store.keyFor(number), name); dialog = null },
        )
        is RulesDialog.EditLabel -> NameDialog(
            fixedNumber = display(d.key),
            initialName = config.labels[d.key].orEmpty(),
            numberHint = { null },
            onDismiss = { dialog = null },
            onRemove = { Store.setLabel(d.key, null); dialog = null },
            onConfirm = { _, name -> Store.setLabel(d.key, name); dialog = null },
        )
        RulesDialog.Country -> InputDialog(
            title = "País de tu línea",
            label = "Código de país (ej. 56)",
            initial = home,
            keyboardType = KeyboardType.Number,
            confirmText = "Guardar",
            hint = { t ->
                val c = Classifier.digitsOf(t)
                Countries.name(c)?.let { "$it (+$c)" } ?: "Escribe el código sin el +"
            },
            isValid = { Countries.name(Classifier.digitsOf(it)) != null },
            onDismiss = { dialog = null },
            onConfirm = { Store.setCountry(Classifier.digitsOf(it)); dialog = null },
        )
    }
}

@Composable
private fun KeyRow(display: String, name: String?, onRemove: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(name ?: display, style = MaterialTheme.typography.bodyLarge)
            if (name != null) {
                Text(display, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        IconButton(onClick = onRemove) {
            Icon(Icons.Outlined.Close, contentDescription = "Quitar", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
