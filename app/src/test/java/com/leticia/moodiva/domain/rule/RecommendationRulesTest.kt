package com.leticia.moodiva.domain.rule

import com.leticia.moodiva.domain.model.ArtistProfile
import com.leticia.moodiva.domain.model.Genre
import com.leticia.moodiva.domain.model.OpennessLevel
import com.leticia.moodiva.domain.model.OriginPreference
import com.leticia.moodiva.domain.model.RecommendedTrack
import com.leticia.moodiva.domain.model.SimilarArtist
import com.leticia.moodiva.domain.model.TagTrack
import com.leticia.moodiva.domain.model.TasteProfile
import com.leticia.moodiva.domain.model.Track
import com.leticia.moodiva.domain.model.TrackDetails
import com.leticia.moodiva.domain.model.TrackSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private fun track(name: String, artist: String = "Artista") = Track(name = name, artist = artist)

private fun taste(
    genres: Set<Genre> = setOf(Genre.POP),
    origin: OriginPreference = OriginPreference.BOTH,
) = TasteProfile(genres = genres, favoriteArtists = listOf("Fav"), eras = emptySet(), origin = origin)

class MoodSourceRankerTest {

    private val pop = ArtistProfile(setOf(Genre.POP), isBrazilian = false)
    private val mpbBr = ArtistProfile(setOf(Genre.MPB), isBrazilian = true)
    private val popBr = ArtistProfile(setOf(Genre.POP), isBrazilian = true)

    @Test
    fun `deve analisar no maximo trinta musicas por tag`() {
        val tracks = (1..40).map { TagTrack(track("M$it"), rank = it) }
        val candidates = MoodSourceRanker.collect(mapOf("happy" to tracks))
        assertEquals(30, candidates.size)
        assertEquals(30, candidates.maxOf { it.bestRank })
    }

    @Test
    fun `musica em varias tags deve acumular tags e guardar a melhor posicao`() {
        val candidates = MoodSourceRanker.collect(
            mapOf(
                "happy" to listOf(TagTrack(track("Song"), 5)),
                "upbeat" to listOf(TagTrack(track("song "), 2)),
            ),
        )
        assertEquals(1, candidates.size)
        assertEquals(setOf("happy", "upbeat"), candidates.single().moodTags)
        assertEquals(2, candidates.single().bestRank)
    }

    @Test
    fun `deve manter so artistas com genero em comum e ordenar por tags e posicao`() {
        val candidates = listOf(
            MoodCandidate(track("A", "Pop Star"), setOf("happy"), 1),
            MoodCandidate(track("B", "Pop Star"), setOf("happy", "upbeat"), 9),
            MoodCandidate(track("C", "Mpb Star"), setOf("happy"), 2),
            MoodCandidate(track("D", "Unknown"), setOf("happy"), 3),
        )
        val profiles = mapOf("pop star" to pop, "mpb star" to mpbBr)

        val ranked = MoodSourceRanker.rank(candidates, profiles, taste(setOf(Genre.POP)), extraversion = 4.0)

        assertEquals(listOf("B", "A"), ranked.map { it.name })
    }

    @Test
    fun `preferencia nacional deve manter so artistas brasileiros`() {
        val candidates = listOf(
            MoodCandidate(track("A", "Gringo"), setOf("happy"), 1),
            MoodCandidate(track("B", "Brasileiro"), setOf("happy"), 2),
        )
        val profiles = mapOf("gringo" to pop, "brasileiro" to popBr)

        val ranked = MoodSourceRanker.rank(candidates, profiles, taste(origin = OriginPreference.NATIONAL), 4.0)

        assertEquals(listOf("B"), ranked.map { it.name })
    }

    @Test
    fun `extroversao deve desempatar musicas com mesmas tags e posicao`() {
        val candidates = listOf(
            MoodCandidate(track("Calma", "Pop Star"), setOf("chill"), 1),
            MoodCandidate(track("Agitada", "Pop Star"), setOf("energetic"), 1),
        )
        val profiles = mapOf("pop star" to pop)

        val extrovert = MoodSourceRanker.rank(candidates, profiles, taste(), extraversion = 6.0)
        val introvert = MoodSourceRanker.rank(candidates, profiles, taste(), extraversion = 2.0)

        assertEquals("Agitada", extrovert.first().name)
        assertEquals("Calma", introvert.first().name)
    }

    @Test
    fun `artistas distintos devem ser listados uma vez`() {
        val candidates = listOf(
            MoodCandidate(track("A", "Elis Regina"), setOf("sad"), 1),
            MoodCandidate(track("B", "ELIS REGINA"), setOf("sad"), 2),
        )
        assertEquals(1, MoodSourceRanker.artistsOf(candidates).size)
    }
}

class TasteSourceRankerTest {

    @Test
    fun `deve juntar similares sem favoritos e com o maior match`() {
        val merged = TasteSourceRanker.mergeSimilar(
            listOf(
                listOf(SimilarArtist("Gal Costa", 0.6), SimilarArtist("Fav", 0.9)),
                listOf(SimilarArtist("gal costa", 0.8), SimilarArtist("Chico", 0.7)),
            ),
            favorites = listOf("fav"),
        )
        assertEquals(listOf("gal costa" to 0.8, "Chico" to 0.7), merged.map { it.name to it.match })
    }

    @Test
    fun `familiaridade deve usar so artistas com match alto`() {
        val similar = listOf(0.9, 0.6, 0.4, 0.2).mapIndexed { i, m -> SimilarArtist("A$i", m) }
        val order = TasteSourceRanker.preferenceOrder(similar, OpennessLevel.FAMILIARITY)
        assertEquals(listOf(0.9, 0.6), order.map { it.match })
    }

    @Test
    fun `descoberta deve espalhar a escolha por toda a faixa de match`() {
        val similar = (0 until 30).map { SimilarArtist("A$it", 1.0 - it * 0.03) } // 1,0 … 0,13
        val order = TasteSourceRanker.preferenceOrder(similar, OpennessLevel.DISCOVERY)

        val firstEight = order.take(TasteSourceRanker.MAX_ARTISTS)
        assertEquals(1.0, firstEight.first().match, 0.0001)
        assertTrue("deve incluir artistas distantes", firstEight.last().match < 0.2)
        assertEquals(30, order.size)
        assertEquals(30, order.map { it.name }.toSet().size)
    }

    @Test
    fun `deve intercalar musicas por artista sem repetir e limitar a vinte`() {
        val artists = (1..10).map { a ->
            SimilarArtist("Art$a", 0.5) to (1..3).map { t -> track("T$t", "Art$a") }
        }
        val candidates = TasteSourceRanker.interleave(artists)

        assertEquals(20, candidates.size)
        assertEquals(listOf("Art1", "Art2", "Art3"), candidates.take(3).map { it.track.artist })
        assertEquals("T1", candidates[9].track.name)
        assertEquals("T2", candidates[10].track.name)
    }

    @Test
    fun `musica com tag do humor deve subir no ranking e trazer a capa`() {
        val semTag = TasteCandidate(track("Sem tag", "X"), artistMatch = 0.9)
        val comTag = TasteCandidate(track("Com tag", "Y"), artistMatch = 0.3)
        val details = mapOf(
            semTag.track.key to TrackDetails(semTag.track, null, listOf("rock")),
            comTag.track.key to TrackDetails(comTag.track, "https://capa.png", listOf("Sad")),
        )

        val ranked = TasteSourceRanker.rank(listOf(semTag, comTag), details, listOf("sad"), 4.0)

        assertEquals(listOf("Com tag", "Sem tag"), ranked.map { it.track.name })
        assertEquals("https://capa.png", ranked.first().coverUrl)
        assertTrue(ranked.all { it.coverChecked && it.source == TrackSource.TASTE })
    }
}

class RecommendationMergerTest {

    @Test
    fun `menos de dez musicas da fonte principal deve pedir o complemento`() {
        assertTrue(RecommendationMerger.needsTasteSource(9))
        assertFalse(RecommendationMerger.needsTasteSource(10))
    }

    @Test
    fun `musica repetida nas duas fontes deve aparecer uma vez como MOOD`() {
        val mood = listOf(track("Águas de Março", "Elis Regina"))
        val taste = listOf(
            RecommendedTrack(track("aguas de marco ", "Elis Regina"), TrackSource.TASTE),
            RecommendedTrack(track("Outra", "Elis Regina"), TrackSource.TASTE),
        )

        val merged = RecommendationMerger.merge(mood, taste)

        assertEquals(2, merged.size)
        assertEquals(TrackSource.MOOD, merged.first().source)
        assertEquals("Outra", merged.last().track.name)
    }

    @Test
    fun `lista final deve ter no maximo vinte musicas com a principal primeiro`() {
        val mood = (1..15).map { track("M$it") }
        val taste = (1..15).map { RecommendedTrack(track("T$it"), TrackSource.TASTE) }

        val merged = RecommendationMerger.merge(mood, taste)

        assertEquals(20, merged.size)
        assertEquals(15, merged.count { it.source == TrackSource.MOOD })
    }

    @Test
    fun `musicas ouvidas devem sair das recomendacoes`() {
        val tracks = listOf(track("A"), track("B")).map { RecommendedTrack(it, TrackSource.MOOD) }
        val result = RecommendationMerger.withoutListened(tracks, setOf(track("b").key))
        assertEquals(listOf("A"), result.map { it.track.name })
    }
}
