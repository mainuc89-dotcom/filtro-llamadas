package app.filtro.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NumbersTest {

    private val cl = Config(countryCode = "56", prefixes = Presets.forCountry("56"))

    private fun info(n: String?) = Classifier.analyze(n, "56")

    @Test
    fun linea800EnTodosLosFormatos() {
        for (raw in listOf("800123456", "+56800123456", "56800123456", "0056800123456", "800 123 456")) {
            val i = info(raw)
            assertEquals(raw, Kind.TOLL_FREE, i.kind)
            assertEquals(raw, "+56800123456", i.key)
            assertEquals(raw, "800 123 456", i.display)
            assertTrue(raw, Rules.decide(i, cl).block)
            assertEquals(raw, "Prefijo 800", Rules.decide(i, cl).reason)
        }
    }

    @Test
    fun telemarketing809SeBloqueaPorDefecto() {
        val i = info("+568091234567")
        assertEquals(Kind.TELEMARKETING, i.kind)
        assertTrue(Rules.decide(i, cl).block)
    }

    @Test
    fun empresa600SeIdentificaPeroNoSeBloquea() {
        val i = info("6006002020")
        assertEquals(Kind.COMPANY, i.kind)
        assertEquals("600 600 2020", i.display)
        assertFalse(Rules.decide(i, cl).block)
    }

    @Test
    fun celularYFijos() {
        val m = info("+56912345678")
        assertEquals(Kind.MOBILE, m.kind)
        assertEquals("+56 9 1234 5678", m.display)
        assertFalse(Rules.decide(m, cl).block)

        val s = info("+56223456789")
        assertEquals(Kind.LANDLINE, s.kind)
        assertEquals("Teléfono fijo · Santiago", s.title)

        val t = info("452123456")
        assertEquals("Teléfono fijo · Temuco", t.title)
        assertEquals("+56 45 212 3456", t.display)

        val ip = info("+56441234567")
        assertEquals(Kind.VOIP, ip.kind)
    }

    @Test
    fun internacionales() {
        val ng = info("+2348012345678")
        assertEquals(Kind.INTERNATIONAL, ng.kind)
        assertEquals("Internacional · Nigeria", ng.title)
        assertEquals("+234 801 234 5678", ng.display)
        assertFalse(Rules.decide(ng, cl).block)
        assertTrue(Rules.decide(ng, cl.copy(blockInternational = true)).block)

        val jm = info("+18765551234")
        assertEquals("Internacional · Jamaica", jm.title)
        assertEquals(Tone.WARNING, jm.tone)

        val us = info("+16505551234")
        assertEquals("Internacional · Estados Unidos / Canadá", us.title)

        // Un "800" de otro país no cae en la regla nacional 800.
        val us800 = info("+18005551234")
        assertFalse(Rules.decide(us800, cl).block)
    }

    @Test
    fun listasYPrioridades() {
        val banco = info("600 200 7000")
        val conBloqueo = cl.copy(blocked = listOf(banco.key))
        assertTrue(Rules.decide(banco, conBloqueo).block)
        assertEquals("Bloqueado por ti", Rules.decide(banco, conBloqueo).reason)

        val amigo800 = info("800 111 222")
        val permitido = cl.copy(allowed = listOf(amigo800.key))
        assertFalse(Rules.decide(amigo800, permitido).block)

        assertFalse(Rules.decide(info("800123456"), cl.copy(active = false)).block)

        val apagado = cl.copy(prefixes = cl.prefixes.map { it.copy(enabled = false) })
        assertFalse(Rules.decide(info("800123456"), apagado).block)
    }

    @Test
    fun prefijosEscritosDeDistintasFormas() {
        val n = info("+56800123456")
        assertTrue(Rules.prefixMatches("800", n, "56"))
        assertTrue(Rules.prefixMatches(" 800 ", n, "56"))
        assertTrue(Rules.prefixMatches("+56800", n, "56"))
        assertTrue(Rules.prefixMatches("56800", n, "56"))
        assertTrue(Rules.prefixMatches("0056800", n, "56"))
        assertFalse(Rules.prefixMatches("600", n, "56"))
        assertTrue(Rules.prefixMatches("+234", info("+2348012345678"), "56"))
        assertEquals("+234", normalizePrefix("00 234"))
    }

    @Test
    fun ocultoYVacio() {
        val h = info(null)
        assertEquals(Kind.HIDDEN, h.kind)
        assertFalse(Rules.decide(h, cl).block)
        assertEquals(Kind.HIDDEN, info("Restricted").kind)
    }

    @Test
    fun otrosPaises() {
        // Argentina: las líneas 0800 se escriben con 0 inicial.
        val ar = Config(countryCode = "54", prefixes = Presets.forCountry("54"))
        val a = Classifier.analyze("08001234567", "54")
        assertEquals(Kind.TOLL_FREE, a.kind)
        assertTrue(Rules.decide(a, ar).block)
        assertTrue(Rules.prefixMatches("0800", a, "54"))

        // Sin país detectado no se cae.
        val u = Classifier.analyze("800123456", "")
        assertEquals(Kind.TOLL_FREE, u.kind)
        assertEquals("800123456", u.key)
    }

    @Test
    fun presetsYDescripciones() {
        assertEquals(listOf("800", "809", "600", "702"), Presets.forCountry("56").map { it.prefix })
        val merged = Presets.merge(listOf(PrefixRule("800", true), PrefixRule("+234", true)), "56")
        assertEquals(listOf("800", "+234", "809", "600", "702"), merged.map { it.prefix })
        assertEquals("Llamadas desde Nigeria", Rules.describePrefix("+234", "56"))
        assertEquals("Llamadas desde Jamaica", Rules.describePrefix("+1876", "56"))
        assertEquals("Teléfonos fijos de Concepción", Rules.describePrefix("41", "56"))
        assertEquals("56", Countries.codeForIso("CL"))
        assertEquals("1", Countries.codeForIso("do"))
    }

    @Test
    fun agrupacion() {
        assertEquals("123 456 7890", Classifier.group("1234567890"))
        assertEquals("123 4567", Classifier.group("1234567"))
        assertEquals("1234 5678", Classifier.group("12345678"))
    }
}
