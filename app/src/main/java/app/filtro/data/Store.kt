package app.filtro.data

import android.content.Context
import android.content.SharedPreferences
import android.telephony.TelephonyManager
import app.filtro.core.Classifier
import app.filtro.core.Config
import app.filtro.core.Countries
import app.filtro.core.LogEntry
import app.filtro.core.PrefixRule
import app.filtro.core.Presets
import app.filtro.core.normalizePrefix
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Locale
import java.util.concurrent.Executors

/**
 * Guarda la configuración (SharedPreferences) y el registro de llamadas (archivo JSON).
 * Todo queda en el teléfono: la app no tiene permiso de internet.
 */
object Store {
    private const val PREFS = "filtro"
    private const val KEY_CONFIG = "config"
    private const val KEY_TOTAL = "total_filtradas"
    private const val KEY_OVERLAY_Y = "overlay_y"
    private const val LOG_FILE = "registro.json"
    private const val MAX_LOG = 500

    private val lock = Any()
    private val io = Executors.newSingleThreadExecutor()
    private lateinit var app: Context

    @Volatile
    private var ready = false

    private val _config = MutableStateFlow(Config())
    val config: StateFlow<Config> = _config.asStateFlow()

    private val _log = MutableStateFlow<List<LogEntry>>(emptyList())
    val log: StateFlow<List<LogEntry>> = _log.asStateFlow()

    private val _totalFiltered = MutableStateFlow(0)
    val totalFiltered: StateFlow<Int> = _totalFiltered.asStateFlow()

    fun init(context: Context) {
        if (ready) return
        synchronized(lock) {
            if (ready) return
            app = context.applicationContext
            val stored = prefs().getString(KEY_CONFIG, null)?.let { runCatching { decode(it) }.getOrNull() }
            val cfg = stored ?: run {
                val code = detectCountryCode(app)
                Config(countryCode = code, prefixes = Presets.forCountry(code)).also { persist(it) }
            }
            _config.value = cfg
            _log.value = readLog()
            _totalFiltered.value = prefs().getInt(KEY_TOTAL, _log.value.count { it.wasFiltered })
            ready = true
        }
    }

    private fun prefs(): SharedPreferences = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // ---------- Configuración ----------

    fun update(transform: (Config) -> Config) {
        synchronized(lock) {
            val current = _config.value
            val next = transform(current)
            if (next == current) return
            _config.value = next
            persist(next)
        }
    }

    fun setCountry(code: String) = update { c ->
        if (code.isEmpty() || code == c.countryCode) c
        else c.copy(countryCode = code, prefixes = Presets.merge(c.prefixes, code))
    }

    fun addPrefix(input: String) = update { c ->
        val p = normalizePrefix(input)
        if (p.isEmpty() || c.prefixes.any { normalizePrefix(it.prefix) == p }) c
        else c.copy(prefixes = c.prefixes + PrefixRule(p, true))
    }

    fun removePrefix(prefix: String) = update { c ->
        c.copy(prefixes = c.prefixes.filterNot { it.prefix == prefix })
    }

    fun setPrefixEnabled(prefix: String, enabled: Boolean) = update { c ->
        c.copy(prefixes = c.prefixes.map { if (it.prefix == prefix) it.copy(enabled = enabled) else it })
    }

    /** Bloquear un número lo saca de "permitidos" y viceversa. */
    fun setBlocked(key: String, blocked: Boolean) = update { c ->
        if (key.isEmpty()) c
        else if (blocked) c.copy(blocked = (c.blocked - key) + key, allowed = c.allowed - key)
        else c.copy(blocked = c.blocked - key)
    }

    fun setAllowed(key: String, allowed: Boolean) = update { c ->
        if (key.isEmpty()) c
        else if (allowed) c.copy(allowed = (c.allowed - key) + key, blocked = c.blocked - key)
        else c.copy(allowed = c.allowed - key)
    }

    fun setLabel(key: String, name: String?) = update { c ->
        val clean = name?.trim().orEmpty()
        if (key.isEmpty()) c
        else if (clean.isEmpty()) c.copy(labels = c.labels - key)
        else c.copy(labels = c.labels + (key to clean))
    }

    /** Convierte lo que escribe el usuario en la clave canónica de un número. */
    fun keyFor(input: String): String = Classifier.analyze(input, _config.value.countryCode).key

    var overlayY: Int
        get() = prefs().getInt(KEY_OVERLAY_Y, -1)
        set(value) {
            prefs().edit().putInt(KEY_OVERLAY_Y, value).apply()
        }

    // ---------- Registro ----------

    fun addLog(entry: LogEntry) {
        synchronized(lock) {
            val next = (listOf(entry) + _log.value).take(MAX_LOG)
            _log.value = next
            if (entry.wasFiltered) {
                _totalFiltered.value += 1
                prefs().edit().putInt(KEY_TOTAL, _totalFiltered.value).apply()
            }
            writeLogAsync(next)
        }
    }

    fun removeLog(entry: LogEntry) {
        synchronized(lock) {
            val next = _log.value.filterNot { it == entry }
            _log.value = next
            writeLogAsync(next)
        }
    }

    fun clearLog() {
        synchronized(lock) {
            _log.value = emptyList()
            writeLogAsync(emptyList())
        }
    }

    // ---------- Persistencia ----------

    private fun persist(c: Config) {
        prefs().edit().putString(KEY_CONFIG, encode(c)).apply()
    }

    private fun encode(c: Config): String = JSONObject().apply {
        put("active", c.active)
        put("country", c.countryCode)
        put("prefixes", JSONArray().apply {
            c.prefixes.forEach { put(JSONObject().put("p", it.prefix).put("on", it.enabled)) }
        })
        put("blocked", JSONArray().apply { c.blocked.forEach { put(it) } })
        put("allowed", JSONArray().apply { c.allowed.forEach { put(it) } })
        put("labels", JSONObject().apply { c.labels.forEach { (k, v) -> put(k, v) } })
        put("blockIntl", c.blockInternational)
        put("silence", c.silenceInstead)
        put("overlay", c.showOverlay)
        put("notify", c.notifyBlocked)
    }.toString()

    private fun decode(s: String): Config {
        val o = JSONObject(s)
        fun strings(name: String): List<String> {
            val a = o.optJSONArray(name) ?: return emptyList()
            return (0 until a.length()).map { a.getString(it) }
        }
        val prefixes = o.optJSONArray("prefixes")?.let { a ->
            (0 until a.length()).map { i ->
                val p = a.getJSONObject(i)
                PrefixRule(p.getString("p"), p.optBoolean("on", true))
            }
        } ?: emptyList()
        val labels = HashMap<String, String>()
        o.optJSONObject("labels")?.let { l ->
            val keys = l.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                labels[k] = l.getString(k)
            }
        }
        return Config(
            active = o.optBoolean("active", true),
            countryCode = o.optString("country", ""),
            prefixes = prefixes,
            blocked = strings("blocked"),
            allowed = strings("allowed"),
            labels = labels,
            blockInternational = o.optBoolean("blockIntl", false),
            silenceInstead = o.optBoolean("silence", false),
            showOverlay = o.optBoolean("overlay", true),
            notifyBlocked = o.optBoolean("notify", true),
        )
    }

    private fun readLog(): List<LogEntry> = runCatching {
        val f = File(app.filesDir, LOG_FILE)
        if (!f.exists()) return emptyList()
        val a = JSONArray(f.readText())
        (0 until a.length()).map { i ->
            val o = a.getJSONObject(i)
            LogEntry(
                time = o.getLong("t"),
                number = o.optString("n"),
                key = o.optString("k"),
                outcome = o.optString("o", LogEntry.OUTCOME_ALLOWED),
                title = o.optString("ti"),
                reason = o.optString("r"),
            )
        }
    }.getOrDefault(emptyList())

    private fun writeLogAsync(entries: List<LogEntry>) {
        io.execute {
            runCatching {
                val a = JSONArray()
                entries.forEach {
                    a.put(
                        JSONObject()
                            .put("t", it.time).put("n", it.number).put("k", it.key)
                            .put("o", it.outcome).put("ti", it.title).put("r", it.reason)
                    )
                }
                val dir = app.filesDir
                val tmp = File(dir, "$LOG_FILE.tmp")
                tmp.writeText(a.toString())
                tmp.renameTo(File(dir, LOG_FILE))
            }
        }
    }

    fun detectCountryCode(context: Context): String {
        val tm = context.getSystemService(TelephonyManager::class.java)
        val candidates = listOf(
            runCatching { tm?.simCountryIso }.getOrNull(),
            runCatching { tm?.networkCountryIso }.getOrNull(),
            Locale.getDefault().country,
        )
        for (iso in candidates) {
            val code = Countries.codeForIso(iso)
            if (code != null) return code
        }
        return ""
    }
}
