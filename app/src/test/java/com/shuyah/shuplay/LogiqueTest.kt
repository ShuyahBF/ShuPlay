package com.shuyah.shuplay

import com.shuyah.shuplay.Logique.Piste
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/** Tests de la logique pure de ShuPlay (lancés sur l'ordinateur : `gradle :app:testDebugUnitTest`). */
class LogiqueTest {
    @Test fun titreDepuisNom() {
        assertEquals("Mon super titre", Logique.titreDepuisNom("Mon_super_titre.mp3"))
        assertEquals("film.v2", Logique.titreDepuisNom("film.v2.mkv"))
        assertEquals("Sans titre", Logique.titreDepuisNom(null))
        assertEquals("Sans titre", Logique.titreDepuisNom(""))
        assertEquals("chanson", Logique.titreDepuisNom("chanson"))
    }

    @Test fun formaterDuree() {
        assertEquals("0:00", Logique.formaterDuree(0))
        assertEquals("1:05", Logique.formaterDuree(65_000))
        assertEquals("1:02:03", Logique.formaterDuree(3_723_000))
        assertEquals("--:--", Logique.formaterDuree(-1))
    }

    @Test fun positionDeReprise() {
        assertEquals(0L, Logique.positionDeReprise(2_000, 100_000))          // tout début : on repart de zéro
        assertEquals(60_000L, Logique.positionDeReprise(60_000, 100_000))    // milieu : on reprend
        assertEquals(0L, Logique.positionDeReprise(95_000, 100_000))         // presque fini : on repart de zéro
        assertEquals(60_000L, Logique.positionDeReprise(60_000, -1))         // durée inconnue : on reprend
    }

    @Test fun lienValide() {
        assertEquals("https://a.bf/v.mp4", Logique.lienValide("  https://a.bf/v.mp4 "))
        assertEquals("http://a.bf", Logique.lienValide("http://a.bf"))
        assertNull(Logique.lienValide("ftp://a.bf"))
        assertNull(Logique.lienValide("https://"))
        assertNull(Logique.lienValide("https:///x"))
        assertNull(Logique.lienValide("https://a b"))
        assertNull(Logique.lienValide(null))
    }

    @Test fun modeRepetition() {
        assertEquals(2, Logique.modeRepetitionSuivant(0))
        assertEquals(1, Logique.modeRepetitionSuivant(2))
        assertEquals(0, Logique.modeRepetitionSuivant(1))
    }

    @Test fun deplacer() {
        val l = listOf(Piste("a", "A"), Piste("b", "B"), Piste("c", "C"))
        assertEquals(listOf("b", "a", "c"), Logique.deplacer(l, 0, 1).map { it.uri })
        assertEquals(listOf("c", "a", "b"), Logique.deplacer(l, 2, 0).map { it.uri })
        assertEquals(l, Logique.deplacer(l, 0, -5))                          // borné : reste en tête, rien ne change
        assertEquals(l, Logique.deplacer(l, 9, 0))                           // position inexistante : rien ne change
    }

    @Test fun jsonAllerRetour() {
        val l = listOf(Piste("content://x/1", "Un"), Piste("https://a.bf/2.mp4", "Deux \"cité\""))
        val (nom, pistes) = Logique.depuisJson(Logique.versJson("Ma liste", l))!!
        assertEquals("Ma liste", nom); assertEquals(l, pistes)
    }

    @Test fun jsonAbimeNePlantePas() {
        assertNull(Logique.depuisJson("pas du json"))
        assertNull(Logique.depuisJson("{\"nom\":\"x\"}"))
        assertNotNull(Logique.depuisJson("{\"nom\":\"x\",\"pistes\":[]}"))
    }
}
