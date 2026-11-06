package app.filtro.core

enum class Kind {
    MOBILE, LANDLINE, TOLL_FREE, COMPANY, TELEMARKETING, GOVERNMENT,
    VOIP, SHORT, NATIONAL, INTERNATIONAL, HIDDEN
}

/** Qué tan atento conviene estar. Se usa para el color del identificador. */
enum class Tone { NEUTRAL, INFO, CAUTION, WARNING }

data class NumberInfo(
    /** Texto tal como llegó. */
    val raw: String,
    /** Clave canónica "+56800123456" para comparar listas. Vacía si es oculto. */
    val key: String,
    /** Dígitos con código de país, sin "+": "56800123456". */
    val intlDigits: String,
    /** Número nacional (sin código de país) cuando la llamada es del mismo país. */
    val national: String?,
    val isDomestic: Boolean,
    /** Código del país de origen, p. ej. "234". */
    val countryCode: String?,
    val kind: Kind,
    val tone: Tone,
    /** Título corto: "Línea 800 · Empresa". */
    val title: String,
    /** Explicación de una línea. */
    val detail: String,
    /** Número formateado para mostrar. */
    val display: String,
)

object Classifier {

    /** Códigos de área de telefonía fija en Chile (fuente: Subtel / UIT). */
    private val CL_AREAS = mapOf(
        "32" to "Valparaíso / Viña del Mar", "33" to "Quillota", "34" to "Los Andes / San Felipe",
        "35" to "San Antonio", "41" to "Concepción", "42" to "Chillán", "43" to "Los Ángeles",
        "45" to "Temuco", "51" to "La Serena / Coquimbo", "52" to "Copiapó", "53" to "Ovalle",
        "55" to "Antofagasta", "57" to "Iquique", "58" to "Arica", "61" to "Punta Arenas",
        "63" to "Valdivia", "64" to "Osorno", "65" to "Puerto Montt", "67" to "Coyhaique",
        "71" to "Talca", "72" to "Rancagua", "73" to "Linares", "75" to "Curicó",
    )

    fun clArea(code: String): String? = CL_AREAS[code]

    fun digitsOf(text: String): String = text.filter { it in '0'..'9' }

    fun analyze(raw: String?, homeCode: String): NumberInfo {
        val text = raw?.trim().orEmpty()
        var digits = digitsOf(text)
        if (digits.isEmpty()) return hidden(text)

        var intl = text.startsWith("+")
        if (!intl && digits.startsWith("00") && digits.length > 6) {
            intl = true
            digits = digits.drop(2)
        }
        // Algunas compañías entregan "56912345678" sin el "+".
        if (!intl && homeCode.isNotEmpty() && digits.startsWith(homeCode)) {
            val rest = digits.length - homeCode.length
            if (rest in 8..11 && digits[homeCode.length] != '0') intl = true
        }

        if (intl) {
            val split = Countries.split(digits)
            if (split != null && split.first == homeCode) {
                return domestic(text, split.second, homeCode)
            }
            return international(text, digits, split)
        }

        var national = digits
        while (national.length > 3 && national.startsWith("0")) national = national.drop(1)
        return domestic(text, national, homeCode)
    }

    private fun hidden(text: String) = NumberInfo(
        raw = text, key = "", intlDigits = "", national = null, isDomestic = false,
        countryCode = null, kind = Kind.HIDDEN, tone = Tone.CAUTION,
        title = "Número oculto", detail = "Quien llama no muestra su número",
        display = "Privado",
    )

    private fun domestic(text: String, n: String, home: String): NumberInfo {
        val key = if (home.isEmpty()) n else "+$home$n"
        val intlDigits = home + n
        fun make(kind: Kind, tone: Tone, title: String, detail: String, display: String) = NumberInfo(
            raw = text, key = key, intlDigits = intlDigits, national = n, isDomestic = true,
            countryCode = home.ifEmpty { null }, kind = kind, tone = tone,
            title = title, detail = detail, display = display,
        )

        if (home == "56") {
            val area2 = if (n.length >= 2) n.substring(0, 2) else ""
            return when {
                n.startsWith("809") -> make(
                    Kind.TELEMARKETING, Tone.WARNING, "Telemarketing",
                    "Prefijo 809: llamada comercial que no pediste", service(n),
                )
                n.startsWith("800") -> make(
                    Kind.TOLL_FREE, Tone.CAUTION, "Línea 800 · Empresa",
                    "Número gratuito de empresas y call centers", service(n),
                )
                n.startsWith("600") -> make(
                    Kind.COMPANY, Tone.INFO, "Empresa · Servicio contratado",
                    "Prefijo 600: empresas con las que tienes un servicio", service(n),
                )
                n.startsWith("702") -> make(
                    Kind.GOVERNMENT, Tone.INFO, "Organismo público",
                    "Prefijo 702: llamadas masivas del Estado", service(n),
                )
                n.length == 9 && n[0] == '9' -> make(
                    Kind.MOBILE, Tone.NEUTRAL, "Celular",
                    "Número móvil de Chile", "+56 9 ${n.substring(1, 5)} ${n.substring(5)}",
                )
                n.length == 9 && n[0] == '2' -> make(
                    Kind.LANDLINE, Tone.NEUTRAL, "Teléfono fijo · Santiago",
                    "Región Metropolitana", "+56 2 ${n.substring(1, 5)} ${n.substring(5)}",
                )
                n.length == 9 && area2 == "44" -> make(
                    Kind.VOIP, Tone.CAUTION, "Telefonía por internet",
                    "Prefijo 44: líneas IP, comunes en call centers",
                    "+56 44 ${n.substring(2, 5)} ${n.substring(5)}",
                )
                n.length == 9 && CL_AREAS.containsKey(area2) -> make(
                    Kind.LANDLINE, Tone.NEUTRAL, "Teléfono fijo · ${CL_AREAS[area2]}",
                    "Línea fija de regiones", "+56 $area2 ${n.substring(2, 5)} ${n.substring(5)}",
                )
                n.length <= 5 -> make(
                    Kind.SHORT, Tone.INFO, "Número corto de servicio",
                    "Emergencias, compañías o servicios especiales", n,
                )
                else -> make(Kind.NATIONAL, Tone.NEUTRAL, "Número de Chile", "Número nacional", group(n))
            }
        }

        return when {
            n.startsWith("800") -> make(
                Kind.TOLL_FREE, Tone.CAUTION, "Línea 800 · Empresa",
                "Número gratuito de empresas y call centers", service(n),
            )
            n.length <= 5 -> make(Kind.SHORT, Tone.INFO, "Número corto de servicio", "Servicio especial", n)
            else -> make(Kind.NATIONAL, Tone.NEUTRAL, "Número nacional", "Llamada de tu mismo país", group(n))
        }
    }

    private fun international(text: String, digits: String, split: Pair<String, String>?): NumberInfo {
        val code = split?.first
        val rest = split?.second ?: digits
        var country = code?.let { Countries.name(it) }
        var detail = "Si no esperas llamadas de otro país, desconfía"
        var tone = Tone.CAUTION
        if (code == "1") {
            val other = Countries.nanpRegion(rest)
            if (other != null) {
                country = other
                detail = "Parece de EE.UU., pero es del Caribe. No devuelvas la llamada"
                tone = Tone.WARNING
            }
        }
        if (code != null && code in Countries.NON_GEOGRAPHIC) {
            detail = "Red satelital o internacional: suele tener costo muy alto"
            tone = Tone.WARNING
        }
        val display = if (code != null) "+$code ${group(rest)}" else "+${group(digits)}"
        return NumberInfo(
            raw = text, key = "+$digits", intlDigits = digits, national = null, isDomestic = false,
            countryCode = code, kind = Kind.INTERNATIONAL, tone = tone,
            title = if (country != null) "Internacional · $country" else "Llamada internacional",
            detail = detail, display = display,
        )
    }

    /** Números de servicio (600, 800, 809, 702): "800 123 456" / "600 600 2020". */
    private fun service(n: String): String =
        if (n.length > 3) n.substring(0, 3) + " " + group(n.substring(3)) else n

    /** Agrupa dígitos en bloques legibles. */
    fun group(d: String): String = when (d.length) {
        in 0..4 -> d
        5 -> d.substring(0, 2) + " " + d.substring(2)
        6, 7 -> d.substring(0, 3) + " " + d.substring(3)
        8 -> d.substring(0, 4) + " " + d.substring(4)
        else -> {
            val parts = d.chunked(3).toMutableList()
            if (parts.size > 1 && parts.last().length == 1) {
                val last = parts.removeAt(parts.size - 1)
                parts[parts.size - 1] = parts.last() + last
            }
            parts.joinToString(" ")
        }
    }
}
