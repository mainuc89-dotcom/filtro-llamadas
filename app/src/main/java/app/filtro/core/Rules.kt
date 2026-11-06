package app.filtro.core

data class PrefixRule(val prefix: String, val enabled: Boolean = true)

/** Configuración del usuario. Las listas guardan claves canónicas ("+56912345678"). */
data class Config(
    val active: Boolean = true,
    /** Código de país de la línea, p. ej. "56". Vacío si no se pudo detectar. */
    val countryCode: String = "",
    val prefixes: List<PrefixRule> = emptyList(),
    val blocked: List<String> = emptyList(),
    val allowed: List<String> = emptyList(),
    val labels: Map<String, String> = emptyMap(),
    val blockInternational: Boolean = false,
    val silenceInstead: Boolean = false,
    val showOverlay: Boolean = true,
    val notifyBlocked: Boolean = true,
)

data class Verdict(val block: Boolean, val reason: String)

data class LogEntry(
    val time: Long,
    val number: String,
    val key: String,
    /** "blocked", "silenced" o "allowed". */
    val outcome: String,
    val title: String,
    val reason: String,
) {
    val wasFiltered: Boolean get() = outcome != OUTCOME_ALLOWED

    companion object {
        const val OUTCOME_BLOCKED = "blocked"
        const val OUTCOME_SILENCED = "silenced"
        const val OUTCOME_ALLOWED = "allowed"
    }
}

object Presets {
    fun forCountry(code: String): List<PrefixRule> = when (code) {
        "56" -> listOf(
            PrefixRule("800", true),
            PrefixRule("809", true),
            PrefixRule("600", false),
            PrefixRule("702", false),
        )
        else -> listOf(PrefixRule("800", true))
    }

    /** Agrega los prefijos sugeridos del país que todavía no estén en la lista. */
    fun merge(existing: List<PrefixRule>, code: String): List<PrefixRule> {
        val have = existing.map { normalizePrefix(it.prefix) }.toSet()
        return existing + forCountry(code).filter { normalizePrefix(it.prefix) !in have }
    }
}

/** Limpia lo que escribe el usuario: deja dígitos y un "+" inicial. */
fun normalizePrefix(input: String): String {
    val t = input.trim()
    val d = Classifier.digitsOf(t)
    if (d.isEmpty()) return ""
    return when {
        t.startsWith("+") -> "+$d"
        d.startsWith("00") && d.length > 2 -> "+" + d.drop(2)
        else -> d
    }
}

object Rules {

    fun prefixMatches(prefix: String, info: NumberInfo, homeCode: String): Boolean {
        val p = normalizePrefix(prefix)
        if (p.isEmpty() || info.kind == Kind.HIDDEN) return false
        if (p.startsWith("+")) return info.intlDigits.startsWith(p.drop(1))
        // "56800" escrito sin "+": se entiende como código de país + prefijo.
        if (homeCode.isNotEmpty() && p.startsWith(homeCode) && p.length >= homeCode.length + 3) {
            if (info.intlDigits.startsWith(p)) return true
        }
        if (!info.isDomestic) return false
        val national = info.national ?: return false
        val trimmed = p.trimStart('0').ifEmpty { p }
        return national.startsWith(trimmed)
    }

    fun decide(info: NumberInfo, c: Config): Verdict {
        if (!c.active) return Verdict(false, "Filtro en pausa")
        if (info.kind == Kind.HIDDEN) return Verdict(false, "")
        if (info.key in c.allowed) return Verdict(false, "Siempre permitido")
        if (info.key in c.blocked) return Verdict(true, "Bloqueado por ti")
        val rule = c.prefixes.firstOrNull { it.enabled && prefixMatches(it.prefix, info, c.countryCode) }
        if (rule != null) return Verdict(true, "Prefijo ${normalizePrefix(rule.prefix)}")
        if (c.blockInternational && info.kind == Kind.INTERNATIONAL) return Verdict(true, "Llamada internacional")
        return Verdict(false, "")
    }

    /** Texto bajo cada prefijo en la pantalla de reglas. */
    fun describePrefix(prefix: String, homeCode: String): String {
        val p = normalizePrefix(prefix)
        if (p.startsWith("+")) {
            val split = Countries.split(p.drop(1))
            val name = when {
                split == null -> Countries.name(p.drop(1))
                split.first == "1" && split.second.length >= 3 ->
                    Countries.nanpRegion(split.second) ?: Countries.name("1")
                else -> Countries.name(split.first)
            }
            return if (name != null) "Llamadas desde $name" else "Prefijo internacional"
        }
        if (homeCode == "56") {
            return when {
                p == "800" -> "Líneas 800 de empresas y call centers"
                p == "809" -> "Telemarketing que no pediste"
                p == "600" -> "Empresas con las que tienes un servicio"
                p == "702" -> "Llamadas masivas de organismos públicos"
                p == "44" -> "Telefonía por internet (call centers)"
                p == "2" -> "Teléfonos fijos de Santiago"
                p == "9" -> "Todos los celulares"
                p.length == 2 && Classifier.clArea(p) != null -> "Teléfonos fijos de ${Classifier.clArea(p)}"
                else -> "Prefijo personalizado"
            }
        }
        return if (p == "800") "Líneas gratuitas de empresas" else "Prefijo personalizado"
    }
}
