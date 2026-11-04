package com.bakehub.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LanguageTagsTest {

    @Test
    fun `supported languages are normalised to the bare language`() {
        assertEquals("es", LanguageTags.normalize("es"))
        assertEquals("fr", LanguageTags.normalize("fr-CA"))
        assertEquals("ar", LanguageTags.normalize("AR_EG"))
        assertEquals("en", LanguageTags.normalize("  en-GB "))
    }

    @Test
    fun `unsupported or empty tags mean system default`() {
        assertEquals("", LanguageTags.normalize("de"))
        assertEquals("", LanguageTags.normalize(""))
        assertEquals("", LanguageTags.normalize(null))
        assertEquals("", LanguageTags.normalize("zz-ZZ"))
    }

    @Test
    fun `english is always available`() {
        assertTrue("en" in LanguageTags.SUPPORTED)
    }
}
