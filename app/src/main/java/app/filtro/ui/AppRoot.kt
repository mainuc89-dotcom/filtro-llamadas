package app.filtro.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.filtro.core.LogEntry
import app.filtro.data.Store

enum class Tab(val title: String, val icon: ImageVector, val id: String) {
    Inicio("Inicio", Icons.Outlined.Shield, "inicio"),
    Registro("Registro", Icons.Outlined.History, "registro"),
    Reglas("Reglas", Icons.Outlined.Tune, "reglas"),
}

@Composable
fun AppRoot(tabRequest: MutableState<String?>, lookupRequest: MutableState<String?>) {
    var tab by rememberSaveable { mutableStateOf(Tab.Inicio) }
    val requested = tabRequest.value
    LaunchedEffect(requested) {
        if (requested != null) {
            Tab.entries.firstOrNull { it.id == requested }?.let { tab = it }
            tabRequest.value = null
        }
    }

    val config by Store.config.collectAsStateWithLifecycle()
    val log by Store.log.collectAsStateWithLifecycle()
    val total by Store.totalFiltered.collectAsStateWithLifecycle()
    val status = rememberSystemStatus()
    val actions = rememberSystemActions(status)
    var openEntry by remember { mutableStateOf<LogEntry?>(null) }
    LaunchedEffect(log.size) { status.refresh() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 0.dp,
            ) {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { tab = t },
                        icon = { Icon(t.icon, contentDescription = null) },
                        label = { Text(t.title) },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.onSurface,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            Crossfade(targetState = tab, animationSpec = tween(220), label = "tab") { current ->
                when (current) {
                    Tab.Inicio -> HomeScreen(
                        config = config,
                        log = log,
                        totalFiltered = total,
                        status = status,
                        actions = actions,
                        lookupRequest = lookupRequest,
                        onOpenLog = { tab = Tab.Registro },
                        onOpenEntry = { openEntry = it },
                    )
                    Tab.Registro -> LogScreen(config = config, log = log, onOpenEntry = { openEntry = it })
                    Tab.Reglas -> RulesScreen(config = config, status = status, actions = actions)
                }
            }
        }
    }

    openEntry?.let { entry ->
        EntrySheet(entry = entry, config = config, onDismiss = { openEntry = null })
    }
}
