package com.leticia.moodiva.data.remote

import com.leticia.moodiva.data.remote.dto.ArtistTopTagsResponse
import com.leticia.moodiva.data.remote.dto.ArtistTopTracksResponse
import com.leticia.moodiva.data.remote.dto.SimilarArtistsResponse
import com.leticia.moodiva.data.remote.dto.TagTopTracksResponse
import com.leticia.moodiva.data.remote.dto.TrackInfoResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Endpoints do Last.fm usados pelo app (planejamento, seção 3.3).
 *
 * Toda chamada vai para a mesma URL base e muda só o parâmetro `method`.
 * `api_key` e `format=json` são acrescentados pelo [ApiKeyInterceptor].
 * `autocorrect=1` deixa o Last.fm corrigir nomes de artistas escritos de outro jeito
 * (ex.: "caetano veloso" → "Caetano Veloso").
 */
interface LastFmApi {

    @GET(".")
    suspend fun tagTopTracks(
        @Query("tag") tag: String,
        @Query("limit") limit: Int,
        @Query("page") page: Int = 1,
        @Query("method") method: String = "tag.gettoptracks",
    ): TagTopTracksResponse

    @GET(".")
    suspend fun artistTopTags(
        @Query("artist") artist: String,
        @Query("autocorrect") autocorrect: Int = 1,
        @Query("method") method: String = "artist.gettoptags",
    ): ArtistTopTagsResponse

    @GET(".")
    suspend fun similarArtists(
        @Query("artist") artist: String,
        @Query("limit") limit: Int,
        @Query("autocorrect") autocorrect: Int = 1,
        @Query("method") method: String = "artist.getsimilar",
    ): SimilarArtistsResponse

    @GET(".")
    suspend fun artistTopTracks(
        @Query("artist") artist: String,
        @Query("limit") limit: Int,
        @Query("autocorrect") autocorrect: Int = 1,
        @Query("method") method: String = "artist.gettoptracks",
    ): ArtistTopTracksResponse

    @GET(".")
    suspend fun trackInfo(
        @Query("artist") artist: String,
        @Query("track") track: String,
        @Query("autocorrect") autocorrect: Int = 1,
        @Query("method") method: String = "track.getinfo",
    ): TrackInfoResponse

    companion object {
        const val BASE_URL = "https://ws.audioscrobbler.com/2.0/"
    }
}
