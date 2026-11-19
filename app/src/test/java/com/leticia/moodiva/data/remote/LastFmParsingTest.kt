package com.leticia.moodiva.data.remote

import com.leticia.moodiva.data.mapper.LASTFM_ERROR_NOT_FOUND
import com.leticia.moodiva.data.mapper.durationFromMillis
import com.leticia.moodiva.data.mapper.durationFromSeconds
import com.leticia.moodiva.data.mapper.toArtistTags
import com.leticia.moodiva.data.mapper.toSimilarArtists
import com.leticia.moodiva.data.mapper.toTagTracks
import com.leticia.moodiva.data.mapper.toTrackDetails
import com.leticia.moodiva.data.mapper.toTracks
import com.leticia.moodiva.data.mapper.validCoverUrl
import com.leticia.moodiva.data.remote.dto.ArtistTopTagsResponse
import com.leticia.moodiva.data.remote.dto.ArtistTopTracksResponse
import com.leticia.moodiva.data.remote.dto.SimilarArtistsResponse
import com.leticia.moodiva.data.remote.dto.TagTopTracksResponse
import com.leticia.moodiva.data.remote.dto.TrackInfoResponse
import com.leticia.moodiva.domain.model.ArtistTag
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.Request
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Respostas reais do Last.fm (capturadas em 2026-11-19) → modelos do domain. */
class LastFmParsingTest {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private fun resource(name: String): String =
        javaClass.classLoader!!.getResource("lastfm/$name")!!.readText()

    @Test
    fun `tag getTopTracks deve ler a duracao em segundos e a posicao`() {
        val tracks = json.decodeFromString<TagTopTracksResponse>(resource("tag.json")).toTagTracks()

        assertEquals(3, tracks.size)
        val first = tracks.first()
        assertEquals("Send Me On My Way", first.track.name)
        assertEquals("Rusted Root", first.track.artist)
        assertEquals(261, first.track.durationSeconds) // "261" = segundos
        assertEquals(1, first.rank)
    }

    @Test
    fun `track getInfo deve ler a duracao em milissegundos e a capa extralarge`() {
        val details = json.decodeFromString<TrackInfoResponse>(resource("trackinfo.json")).toTrackDetails()!!

        assertEquals("Happy", details.track.name)
        assertEquals(232, details.track.durationSeconds) // "232000" = milissegundos
        assertTrue(details.coverUrl!!.contains("300x300"))
        assertTrue(details.tags.isEmpty()) // "toptags": {"tag": []}
    }

    @Test
    fun `duracao zero ou invalida deve ser desconhecida nos dois endpoints`() {
        assertNull(durationFromSeconds("0"))
        assertNull(durationFromMillis("0"))
        assertNull(durationFromSeconds(""))
        assertEquals(312, durationFromSeconds("312"))
        assertEquals(250, durationFromMillis("250000"))
    }

    @Test
    fun `musica nao encontrada deve virar null`() {
        val response = json.decodeFromString<TrackInfoResponse>(resource("trackerr.json"))
        assertEquals(LASTFM_ERROR_NOT_FOUND, response.error)
        assertNull(response.toTrackDetails())
    }

    @Test
    fun `capa placeholder ou sem album deve ser indisponivel`() {
        assertNull(validCoverUrl("https://lastfm.freetls.fastly.net/i/u/300x300/2a96cbd8b46e442fc41c2b86b821562f.png"))
        assertNull(validCoverUrl(""))
        assertNull(validCoverUrl(null))
        val semAlbum = """{"track":{"name":"X","duration":"0","artist":{"name":"Y"},"toptags":""}}"""
        val details = json.decodeFromString<TrackInfoResponse>(semAlbum).toTrackDetails()!!
        assertNull(details.coverUrl)
        assertNull(details.track.durationSeconds)
    }

    @Test
    fun `artist getTopTags deve aceitar count como numero ou texto`() {
        val tags = json.decodeFromString<ArtistTopTagsResponse>(resource("artisttags.json")).toArtistTags()
        assertEquals(ArtistTag("mpb", 100), tags.first())
        assertEquals(ArtistTag("Bossa Nova", 81), tags[1])

        val asText = """{"toptags":{"tag":[{"name":"rock","count":"42"}]}}"""
        assertEquals(listOf(ArtistTag("rock", 42)), json.decodeFromString<ArtistTopTagsResponse>(asText).toArtistTags())
    }

    @Test
    fun `objeto unico no lugar da lista deve virar lista de um item`() {
        val single = """{"toptags":{"tag":{"name":"jazz","count":90}}}"""
        assertEquals(listOf(ArtistTag("jazz", 90)), json.decodeFromString<ArtistTopTagsResponse>(single).toArtistTags())
    }

    @Test
    fun `artista nao encontrado deve ficar sem tags`() {
        val notFound = """{"error":6,"message":"The artist you supplied could not be found","links":[]}"""
        val response = json.decodeFromString<ArtistTopTagsResponse>(notFound)
        assertEquals(LASTFM_ERROR_NOT_FOUND, response.error)
        assertTrue(response.toArtistTags().isEmpty())
    }

    @Test
    fun `artist getSimilar deve ler o match em texto`() {
        val similar = json.decodeFromString<SimilarArtistsResponse>(resource("similar.json")).toSimilarArtists()
        assertEquals("Gilberto Gil", similar.first().name)
        assertEquals(1.0, similar.first().match, 0.0)
        assertEquals(0.690513, similar[1].match, 0.000001)
    }

    @Test
    fun `artist getTopTracks deve trazer nome e artista`() {
        val tracks = json.decodeFromString<ArtistTopTracksResponse>(resource("artisttop.json")).toTracks()
        assertEquals(listOf("Palco", "Andar Com Fé"), tracks.map { it.name })
        assertTrue(tracks.all { it.artist == "Gilberto Gil" })
    }
}

class ApiKeyInterceptorTest {

    @Test
    fun `deve acrescentar a chave e o formato json a requisicao`() {
        val chain = mockk<Interceptor.Chain>()
        val sent = slot<Request>()
        every { chain.request() } returns Request.Builder().url("https://ws.audioscrobbler.com/2.0/?method=tag.gettoptracks").build()
        every { chain.proceed(capture(sent)) } returns mockk(relaxed = true)

        ApiKeyInterceptor("chave-de-teste").intercept(chain)

        val url = sent.captured.url
        assertEquals("chave-de-teste", url.queryParameter("api_key"))
        assertEquals("json", url.queryParameter("format"))
        assertEquals("tag.gettoptracks", url.queryParameter("method"))
    }
}
