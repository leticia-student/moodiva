package com.leticia.moodiva.domain.usecase

import com.leticia.moodiva.core.result.AppError
import com.leticia.moodiva.core.result.AppResult
import com.leticia.moodiva.domain.fake.FakeMusicRepository
import com.leticia.moodiva.domain.fake.TestData
import com.leticia.moodiva.domain.model.ArtistTag
import com.leticia.moodiva.domain.model.ConversationMode
import com.leticia.moodiva.domain.model.Genre
import com.leticia.moodiva.domain.model.Mood
import com.leticia.moodiva.domain.model.OriginPreference
import com.leticia.moodiva.domain.model.RecommendedTrack
import com.leticia.moodiva.domain.model.SimilarArtist
import com.leticia.moodiva.domain.model.TagTrack
import com.leticia.moodiva.domain.model.Track
import com.leticia.moodiva.domain.model.TrackDetails
import com.leticia.moodiva.domain.model.TrackSource
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GenerateRecommendationsUseCaseTest {

    private val music = FakeMusicRepository()
    private val generate = GenerateRecommendationsUseCase(music)

    private fun popArtist(name: String) {
        music.artistTagsByName[name.lowercase()] = listOf(ArtistTag("pop", 100))
    }

    private fun brazilianPopArtist(name: String) {
        music.artistTagsByName[name.lowercase()] = listOf(ArtistTag("pop", 100), ArtistTag("brazilian", 50))
    }

    /** Coloca [count] músicas de artistas pop na tag "happy". */
    private fun happyTracks(count: Int, artistPrefix: String = "pop") {
        music.tagTracks["happy"] = (1..count).map { i ->
            popArtist("$artistPrefix$i")
            TagTrack(Track("Happy $i", "$artistPrefix$i"), rank = i)
        }
    }

    /** Similares do favorito "Fav", cada um com 3 músicas e detalhes. */
    private fun tasteSource(artists: Int, moodTagOn: Set<Int> = emptySet()) {
        music.similar["Fav"] = (1..artists).map { SimilarArtist("sim$it", 1.0 - it * 0.05) }
        (1..artists).forEach { a ->
            brazilianPopArtist("sim$a")
            val tracks = (1..3).map { t -> Track("Sim $a-$t", "sim$a") }
            music.topTracks["sim$a"] = tracks
            tracks.forEach { track ->
                val tags = if (a in moodTagOn) listOf("happy") else listOf("rock")
                music.details[track.key] = TrackDetails(track, "https://capa/$a.png", tags)
            }
        }
    }

    private suspend fun run(
        origin: OriginPreference = OriginPreference.BOTH,
        genres: Set<Genre> = setOf(Genre.POP),
    ): List<RecommendedTrack> {
        val profile = TestData.profile(genres = genres, origin = origin)
        val result = generate(profile, Mood.HAPPY, ConversationMode.MATCH)
        return (result as AppResult.Success).data
    }

    @Test
    fun `fonte principal com dez ou mais musicas nao deve usar o complemento`() = runTest {
        happyTracks(12)
        tasteSource(3)

        val tracks = run()

        assertEquals(12, tracks.size)
        assertTrue(tracks.all { it.source == TrackSource.MOOD })
        assertFalse(music.calls.any { it.startsWith("similar:") })
    }

    @Test
    fun `fonte principal fraca deve ser completada pelo gosto com o selo TASTE`() = runTest {
        happyTracks(4)
        tasteSource(10, moodTagOn = setOf(10))

        val tracks = run()

        assertEquals(4, tracks.count { it.source == TrackSource.MOOD })
        assertEquals(TrackSource.MOOD, tracks.first().source)
        val taste = tracks.filter { it.source == TrackSource.TASTE }
        assertEquals(16, taste.size) // até completar 20
        assertEquals("sim10", taste.first().track.artist) // menor match, mas com tag do humor: sobe
        assertTrue(taste.all { it.coverChecked && it.coverUrl != null })
    }

    @Test
    fun `generos que nao batem com o perfil devem ser filtrados`() = runTest {
        happyTracks(12)
        tasteSource(0)

        val tracks = run(genres = setOf(Genre.MPB))

        assertTrue(tracks.isEmpty())
    }

    @Test
    fun `preferencia nacional deve excluir artistas sem tag brasileira`() = runTest {
        happyTracks(12) // artistas sem nacionalidade = internacionais
        tasteSource(4) // similares brasileiros

        val tracks = run(origin = OriginPreference.NATIONAL)

        assertTrue(tracks.isNotEmpty())
        assertTrue(tracks.all { it.track.artist.startsWith("sim") })
    }

    @Test
    fun `falha de rede na fonte principal deve falhar a geracao`() = runTest {
        happyTracks(3)
        music.failure = AppError.Network

        val result = generate(TestData.profile(), Mood.HAPPY, ConversationMode.MATCH)

        assertEquals(AppResult.Failure(AppError.Network), result)
    }

    @Test
    fun `falha so no complemento deve manter as musicas pelo humor`() = runTest {
        happyTracks(3)
        music.failTasteSource = true

        val tracks = run()

        assertEquals(3, tracks.size)
    }

    @Test
    fun `sem nenhuma musica e com falha no complemento deve falhar`() = runTest {
        music.failTasteSource = true

        val result = generate(TestData.profile(), Mood.HAPPY, ConversationMode.MATCH)

        assertEquals(AppResult.Failure(AppError.Network), result)
    }

    @Test
    fun `cansado no modo mudar deve buscar as tags energeticas`() = runTest {
        generate(TestData.profile(), Mood.TIRED, ConversationMode.SHIFT)

        assertTrue("tag:energetic" in music.calls)
        assertTrue("tag:upbeat" in music.calls)
        assertFalse("tag:mellow" in music.calls)
    }

    @Test
    fun `cada artista deve ter as tags consultadas uma vez so`() = runTest {
        music.tagTracks["happy"] = listOf(
            TagTrack(Track("A", "Same"), 1),
            TagTrack(Track("B", "same"), 2),
        )
        popArtist("same")
        tasteSource(0)

        run()

        assertEquals(1, music.calls.count { it.startsWith("tags:") && it.endsWith("ame") })
    }
}
