package com.leticia.moodiva.domain.rule

import com.leticia.moodiva.domain.model.ArtistTag
import com.leticia.moodiva.domain.model.ConversationMode.MATCH
import com.leticia.moodiva.domain.model.ConversationMode.SHIFT
import com.leticia.moodiva.domain.model.Genre
import com.leticia.moodiva.domain.model.Mood
import com.leticia.moodiva.domain.model.OpennessLevel
import com.leticia.moodiva.domain.model.OriginPreference
import com.leticia.moodiva.domain.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TextNormalizerTest {

    @Test
    fun `deve ignorar maiusculas acentos e espacos extras`() {
        assertEquals("bossa nova", TextNormalizer.normalize("  Bossa   Nova "))
        assertEquals("forro", TextNormalizer.normalize("Forró"))
        assertEquals("aguas de marco", TextNormalizer.normalize("Águas de Março"))
        assertEquals("mpb", TextNormalizer.normalize("MPB"))
    }

    @Test
    fun `musicas iguais apos normalizacao devem ter a mesma chave`() {
        val a = Track(name = "Águas de Março", artist = "Elis Regina")
        val b = Track(name = "aguas de marco ", artist = "ELIS  REGINA")
        assertEquals(a.key, b.key)
    }

    @Test
    fun `musicas com nomes iguais e artistas diferentes devem ter chaves diferentes`() {
        assertNotEquals(Track("Happy", "Pharrell Williams").key, Track("Happy", "Marina").key)
    }
}

class MoodTagMapperTest {

    @Test
    fun `cansado no modo mudar deve usar tags energeticas`() {
        assertEquals(listOf("energetic", "upbeat"), MoodTagMapper.tagsFor(Mood.TIRED, SHIFT))
    }

    @Test
    fun `triste deve mudar de tags conforme o modo`() {
        assertEquals(listOf("sad", "melancholy"), MoodTagMapper.tagsFor(Mood.SAD, MATCH))
        assertEquals(listOf("feel good", "uplifting"), MoodTagMapper.tagsFor(Mood.SAD, SHIFT))
    }

    @Test
    fun `ansioso deve receber musicas calmas em qualquer modo`() {
        val calm = setOf("calm", "relaxing", "chill")
        assertTrue(calm.containsAll(MoodTagMapper.tagsFor(Mood.ANXIOUS, MATCH)))
        assertTrue(calm.containsAll(MoodTagMapper.tagsFor(Mood.ANXIOUS, SHIFT)))
    }

    @Test
    fun `so triste ansioso e cansado devem perguntar o modo`() {
        val asking = Mood.entries.filter(MoodTagMapper::asksForMode).toSet()
        assertEquals(setOf(Mood.SAD, Mood.ANXIOUS, Mood.TIRED), asking)
    }

    @Test
    fun `humor que nao pergunta o modo deve usar sempre acompanhar`() {
        assertEquals(MATCH, MoodTagMapper.effectiveMode(Mood.HAPPY, SHIFT))
        assertEquals(SHIFT, MoodTagMapper.effectiveMode(Mood.SAD, SHIFT))
    }

    @Test
    fun `todas as tags devem estar em ingles e sem a tag study`() {
        val all = Mood.entries.flatMap { MoodTagMapper.tagsFor(it, MATCH) + MoodTagMapper.tagsFor(it, SHIFT) }
        assertFalse("study" in all)
        assertFalse("triste" in all)
    }
}

class PersonalityRulesTest {

    @Test
    fun `abertura deve definir o perfil de descoberta nas bordas`() {
        assertEquals(OpennessLevel.DISCOVERY, OpennessProfile.levelFor(5.0))
        assertEquals(OpennessLevel.BALANCED, OpennessProfile.levelFor(4.5))
        assertEquals(OpennessLevel.BALANCED, OpennessProfile.levelFor(3.5))
        assertEquals(OpennessLevel.FAMILIARITY, OpennessProfile.levelFor(3.0))
    }

    @Test
    fun `faixa de match deve ser mais ampla na descoberta`() {
        assertTrue(OpennessProfile.acceptsMatch(OpennessLevel.DISCOVERY, 0.10))
        assertFalse(OpennessProfile.acceptsMatch(OpennessLevel.DISCOVERY, 0.09))
        assertFalse(OpennessProfile.acceptsMatch(OpennessLevel.BALANCED, 0.24))
        assertTrue(OpennessProfile.acceptsMatch(OpennessLevel.BALANCED, 0.25))
        assertFalse(OpennessProfile.acceptsMatch(OpennessLevel.FAMILIARITY, 0.49))
        assertTrue(OpennessProfile.acceptsMatch(OpennessLevel.FAMILIARITY, 1.0))
    }

    @Test
    fun `extroversao alta deve favorecer tags energeticas`() {
        assertEquals(2, ExtraversionTiebreak.score(5.0, listOf("Energetic", "dance", "chill")))
        assertEquals(1, ExtraversionTiebreak.score(2.0, listOf("Energetic", "dance", "chill")))
        assertEquals(0, ExtraversionTiebreak.score(4.0, listOf("energetic", "chill")))
    }
}

class ArtistClassifierTest {

    // Tags reais de Caetano Veloso (artist.getTopTags, 2026-11-19).
    private val caetano = listOf(
        ArtistTag("mpb", 100),
        ArtistTag("Bossa Nova", 81),
        ArtistTag("tropicalia", 50),
        ArtistTag("brazilian", 48),
        ArtistTag("singer-songwriter", 5),
        ArtistTag("brazil", 4),
        ArtistTag("male vocalists", 1),
    )

    @Test
    fun `deve reconhecer generos da lista fechada com count minimo`() {
        assertEquals(setOf(Genre.MPB, Genre.BOSSA_NOVA), ArtistClassifier.genresOf(caetano))
    }

    @Test
    fun `tag de genero com count abaixo de dez deve ser ignorada`() {
        assertEquals(emptySet<Genre>(), ArtistClassifier.genresOf(listOf(ArtistTag("rock", 9))))
        assertEquals(setOf(Genre.ROCK), ArtistClassifier.genresOf(listOf(ArtistTag("Rock", 10))))
    }

    @Test
    fun `tags que nao sao generos devem ser ignoradas`() {
        val tags = listOf(ArtistTag("female vocalists", 100), ArtistTag("british", 90))
        assertEquals(emptySet<Genre>(), ArtistClassifier.genresOf(tags))
    }

    @Test
    fun `nacionalidade brasileira deve ser identificada pelas tags`() {
        assertTrue(ArtistClassifier.isBrazilian(caetano))
        assertTrue(ArtistClassifier.isBrazilian(listOf(ArtistTag("Brasil", 3))))
        assertFalse(ArtistClassifier.isBrazilian(listOf(ArtistTag("british", 90))))
    }

    @Test
    fun `artista sem tag de nacionalidade deve contar como internacional`() {
        val profile = ArtistClassifier.classify(listOf(ArtistTag("pop", 100)))
        assertFalse(profile.isBrazilian)
        assertFalse(OriginFilter.accepts(OriginPreference.NATIONAL, profile.isBrazilian))
        assertTrue(OriginFilter.accepts(OriginPreference.INTERNATIONAL, profile.isBrazilian))
    }

    @Test
    fun `preferencia ambas deve aceitar qualquer origem`() {
        assertTrue(OriginFilter.accepts(OriginPreference.BOTH, true))
        assertTrue(OriginFilter.accepts(OriginPreference.BOTH, false))
        assertFalse(OriginFilter.accepts(OriginPreference.INTERNATIONAL, true))
    }
}
