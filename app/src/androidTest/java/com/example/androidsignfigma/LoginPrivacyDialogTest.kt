package com.example.androidsignfigma

import com.example.androidsignfigma.privacy.obtenerTextoEsencialPrivacidad
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class PrivacyUtilsTest {

    @Test
    fun obtenerTextoEsencialPrivacidad_devuelveTextoCorrecto() {
        val texto = obtenerTextoEsencialPrivacidad()
        assertEquals("Esto es lo esencial para usar SignLearn:", texto)
    }

    @Test
    fun obtenerTextoEsencialPrivacidad_noDevuelveTextoIncorrecto() {
        val texto = obtenerTextoEsencialPrivacidad()
        assertNotEquals("Texto incorrecto", texto)
    }
}
