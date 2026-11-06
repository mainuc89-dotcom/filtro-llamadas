package app.filtro.core

/**
 * Códigos telefónicos de país (E.164) con nombres en español.
 * Los códigos son "libres de prefijo": ninguno es el comienzo de otro,
 * así que basta probar con 1, 2 y 3 dígitos.
 */
object Countries {

    private const val DATA = """
1|Estados Unidos / Canadá|us,ca,pr,do,jm,tt,bs,bb,ag,ai,bm,dm,gd,kn,ky,lc,ms,sx,tc,vc,vg,vi,as,gu,mp
7|Rusia / Kazajistán|ru,kz
20|Egipto|eg
27|Sudáfrica|za
30|Grecia|gr
31|Países Bajos|nl
32|Bélgica|be
33|Francia|fr
34|España|es
36|Hungría|hu
39|Italia|it,va
40|Rumania|ro
41|Suiza|ch
43|Austria|at
44|Reino Unido|gb,gg,im,je
45|Dinamarca|dk
46|Suecia|se
47|Noruega|no
48|Polonia|pl
49|Alemania|de
51|Perú|pe
52|México|mx
53|Cuba|cu
54|Argentina|ar
55|Brasil|br
56|Chile|cl
57|Colombia|co
58|Venezuela|ve
60|Malasia|my
61|Australia|au
62|Indonesia|id
63|Filipinas|ph
64|Nueva Zelanda|nz
65|Singapur|sg
66|Tailandia|th
81|Japón|jp
82|Corea del Sur|kr
84|Vietnam|vn
86|China|cn
90|Turquía|tr
91|India|in
92|Pakistán|pk
93|Afganistán|af
94|Sri Lanka|lk
95|Myanmar|mm
98|Irán|ir
211|Sudán del Sur|ss
212|Marruecos|ma
213|Argelia|dz
216|Túnez|tn
218|Libia|ly
220|Gambia|gm
221|Senegal|sn
222|Mauritania|mr
223|Malí|ml
224|Guinea|gn
225|Costa de Marfil|ci
226|Burkina Faso|bf
227|Níger|ne
228|Togo|tg
229|Benín|bj
230|Mauricio|mu
231|Liberia|lr
232|Sierra Leona|sl
233|Ghana|gh
234|Nigeria|ng
235|Chad|td
236|República Centroafricana|cf
237|Camerún|cm
238|Cabo Verde|cv
239|Santo Tomé y Príncipe|st
240|Guinea Ecuatorial|gq
241|Gabón|ga
242|República del Congo|cg
243|República Democrática del Congo|cd
244|Angola|ao
245|Guinea-Bisáu|gw
246|Diego García|io
248|Seychelles|sc
249|Sudán|sd
250|Ruanda|rw
251|Etiopía|et
252|Somalia|so
253|Yibuti|dj
254|Kenia|ke
255|Tanzania|tz
256|Uganda|ug
257|Burundi|bi
258|Mozambique|mz
260|Zambia|zm
261|Madagascar|mg
262|Reunión / Mayotte|re,yt
263|Zimbabue|zw
264|Namibia|na
265|Malaui|mw
266|Lesoto|ls
267|Botsuana|bw
268|Esuatini|sz
269|Comoras|km
290|Santa Elena|sh
291|Eritrea|er
297|Aruba|aw
298|Islas Feroe|fo
299|Groenlandia|gl
350|Gibraltar|gi
351|Portugal|pt
352|Luxemburgo|lu
353|Irlanda|ie
354|Islandia|is
355|Albania|al
356|Malta|mt
357|Chipre|cy
358|Finlandia|fi,ax
359|Bulgaria|bg
370|Lituania|lt
371|Letonia|lv
372|Estonia|ee
373|Moldavia|md
374|Armenia|am
375|Bielorrusia|by
376|Andorra|ad
377|Mónaco|mc
378|San Marino|sm
380|Ucrania|ua
381|Serbia|rs
382|Montenegro|me
383|Kosovo|xk
385|Croacia|hr
386|Eslovenia|si
387|Bosnia y Herzegovina|ba
389|Macedonia del Norte|mk
420|República Checa|cz
421|Eslovaquia|sk
423|Liechtenstein|li
500|Islas Malvinas|fk
501|Belice|bz
502|Guatemala|gt
503|El Salvador|sv
504|Honduras|hn
505|Nicaragua|ni
506|Costa Rica|cr
507|Panamá|pa
508|San Pedro y Miquelón|pm
509|Haití|ht
590|Guadalupe|gp,bl,mf
591|Bolivia|bo
592|Guyana|gy
593|Ecuador|ec
594|Guayana Francesa|gf
595|Paraguay|py
596|Martinica|mq
597|Surinam|sr
598|Uruguay|uy
599|Curazao / Caribe Neerlandés|cw,bq
670|Timor Oriental|tl
673|Brunéi|bn
674|Nauru|nr
675|Papúa Nueva Guinea|pg
676|Tonga|to
677|Islas Salomón|sb
678|Vanuatu|vu
679|Fiyi|fj
680|Palaos|pw
681|Wallis y Futuna|wf
682|Islas Cook|ck
683|Niue|nu
685|Samoa|ws
686|Kiribati|ki
687|Nueva Caledonia|nc
688|Tuvalu|tv
689|Polinesia Francesa|pf
690|Tokelau|tk
691|Micronesia|fm
692|Islas Marshall|mh
850|Corea del Norte|kp
852|Hong Kong|hk
853|Macao|mo
855|Camboya|kh
856|Laos|la
870|Red satelital Inmarsat|
880|Bangladés|bd
881|Red satelital|
882|Red internacional|
883|Red internacional|
886|Taiwán|tw
960|Maldivas|mv
961|Líbano|lb
962|Jordania|jo
963|Siria|sy
964|Irak|iq
965|Kuwait|kw
966|Arabia Saudita|sa
967|Yemen|ye
968|Omán|om
970|Palestina|ps
971|Emiratos Árabes Unidos|ae
972|Israel|il
973|Baréin|bh
974|Catar|qa
975|Bután|bt
976|Mongolia|mn
977|Nepal|np
992|Tayikistán|tj
993|Turkmenistán|tm
994|Azerbaiyán|az
995|Georgia|ge
996|Kirguistán|kg
998|Uzbekistán|uz
"""

    /** Códigos de área del plan norteamericano (+1) que NO son EE.UU. ni Canadá. */
    private val NANP_OTHER = mapOf(
        "242" to "Bahamas", "246" to "Barbados", "264" to "Anguila",
        "268" to "Antigua y Barbuda", "284" to "Islas Vírgenes Británicas",
        "340" to "Islas Vírgenes de EE.UU.", "345" to "Islas Caimán",
        "441" to "Bermudas", "473" to "Granada", "649" to "Islas Turcas y Caicos",
        "658" to "Jamaica", "664" to "Montserrat", "670" to "Islas Marianas del Norte",
        "671" to "Guam", "684" to "Samoa Americana", "721" to "Sint Maarten",
        "758" to "Santa Lucía", "767" to "Dominica", "784" to "San Vicente y las Granadinas",
        "787" to "Puerto Rico", "809" to "República Dominicana", "829" to "República Dominicana",
        "849" to "República Dominicana", "868" to "Trinidad y Tobago",
        "869" to "San Cristóbal y Nieves", "876" to "Jamaica", "939" to "Puerto Rico",
    )

    /** Redes que no pertenecen a un país (satelitales, internacionales). */
    val NON_GEOGRAPHIC = setOf("870", "881", "882", "883")

    private val names = HashMap<String, String>()
    private val isoToCode = HashMap<String, String>()

    init {
        for (line in DATA.lines()) {
            val parts = line.trim().split("|")
            if (parts.size < 2 || parts[0].isEmpty()) continue
            names[parts[0]] = parts[1]
            if (parts.size > 2) {
                for (iso in parts[2].split(",")) {
                    if (iso.isNotBlank()) isoToCode.putIfAbsent(iso.trim().lowercase(), parts[0])
                }
            }
        }
    }

    fun name(code: String): String? = names[code]

    fun codeForIso(iso: String?): String? = iso?.trim()?.lowercase()?.let { isoToCode[it] }

    /** Para +1: distingue el Caribe de EE.UU./Canadá según el código de área. */
    fun nanpRegion(nationalAfterOne: String): String? =
        if (nationalAfterOne.length >= 3) NANP_OTHER[nationalAfterOne.substring(0, 3)] else null

    /** "56912345678" → ("56", "912345678"). Null si el código no se reconoce. */
    fun split(intlDigits: String): Pair<String, String>? {
        for (len in 1..3) {
            if (intlDigits.length <= len) return null
            val code = intlDigits.substring(0, len)
            if (names.containsKey(code)) return code to intlDigits.substring(len)
        }
        return null
    }
}
