package com.bakehub.app.util

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the multi-language support: every supported language must translate every string,
 * with the same format placeholders, and must be registered for Android 13+ per-app language.
 * Unit tests run with the module directory (app/) as the working directory.
 */
class StringResourcesTest {

    private val resDir = File("src/main/res")

    private fun load(file: File): Map<String, String> {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val nodes = doc.getElementsByTagName("string")
        val out = mutableMapOf<String, String>()
        for (i in 0 until nodes.length) {
            val el = nodes.item(i) as org.w3c.dom.Element
            if (el.getAttribute("translatable") == "false") continue
            out[el.getAttribute("name")] = el.textContent
        }
        return out
    }

    private val base get() = load(File(resDir, "values/strings.xml"))
    private val translated get() = LanguageTags.SUPPORTED.filter { it != "en" }

    private fun placeholders(s: String) =
        Regex("%\\d+\\$[sd]").findAll(s).map { it.value }.sorted().toList()

    @Test
    fun `every supported language has a strings file`() {
        translated.forEach { lang ->
            assertTrue("values-$lang/strings.xml is missing", File(resDir, "values-$lang/strings.xml").exists())
        }
    }

    @Test
    fun `no translation is missing or stale`() {
        val english = base.keys
        translated.forEach { lang ->
            val keys = load(File(resDir, "values-$lang/strings.xml")).keys
            assertEquals("Missing in $lang", emptySet<String>(), english - keys)
            assertEquals("Not in the default strings (stale) in $lang", emptySet<String>(), keys - english)
        }
    }

    @Test
    fun `translations keep the same format placeholders as English`() {
        val english = base
        translated.forEach { lang ->
            load(File(resDir, "values-$lang/strings.xml")).forEach { (key, value) ->
                assertEquals("Placeholders differ for '$key' in $lang", placeholders(english.getValue(key)), placeholders(value))
            }
        }
    }

    @Test
    fun `no translation is blank`() {
        translated.forEach { lang ->
            load(File(resDir, "values-$lang/strings.xml")).forEach { (key, value) ->
                assertTrue("'$key' is blank in $lang", value.isNotBlank())
            }
        }
    }

    @Test
    fun `locales_config lists exactly the supported languages`() {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(File(resDir, "xml/locales_config.xml"))
        val nodes = doc.getElementsByTagName("locale")
        val declared = (0 until nodes.length)
            .map { (nodes.item(it) as org.w3c.dom.Element).getAttribute("android:name") }
            .toSet()
        assertEquals(LanguageTags.SUPPORTED.toSet(), declared)
    }
}
