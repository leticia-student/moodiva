package com.leticia.moodiva.data.mapper

import com.leticia.moodiva.data.remote.dto.ArtistTopTagsResponse
import com.leticia.moodiva.data.remote.dto.ArtistTopTracksResponse
import com.leticia.moodiva.data.remote.dto.SimilarArtistsResponse
import com.leticia.moodiva.data.remote.dto.TagTopTracksResponse
import com.leticia.moodiva.data.remote.dto.TrackInfoResponse
import com.leticia.moodiva.domain.model.ArtistTag
import com.leticia.moodiva.domain.model.SimilarArtist
import com.leticia.moodiva.domain.model.TagTrack
import com.leticia.moodiva.domain.model.Track
import com.leticia.moodiva.domain.model.TrackDetails

// DTO do Last.fm → modelo do domain. Cada endpoint tem o seu mapper, porque cada um
// tem particularidades (planejamento, seção 3.3). Itens sem nome são descartados.

/** Hash do arquivo de imagem genérico que o Last.fm usa quando não tem a imagem (RN27). */
const val LASTFM_PLACEHOLDER_HASH = "2a96cbd8b46e442fc41c2b86b821562f"

/** Código de erro do Last.fm para "não encontrado" (chega com HTTP 200). */
const val LASTFM_ERROR_NOT_FOUND = 6

/** Duração em **segundos** (tag.getTopTracks). "0", vazio ou inválido = desconhecida. */
fun durationFromSeconds(raw: String): Int? = raw.trim().toIntOrNull()?.takeIf { it > 0 }

/** Duração em **milissegundos** (track.getInfo), convertida para segundos. "0" = desconhecida. */
fun durationFromMillis(raw: String): Int? = raw.trim().toLongOrNull()?.takeIf { it > 0 }?.let { (it / 1000).toInt() }

/** Capa válida: URL não vazia e que não seja o placeholder do Last.fm (RN27). */
fun validCoverUrl(url: String?): String? =
    url?.trim()?.takeIf { it.isNotEmpty() && !it.contains(LASTFM_PLACEHOLDER_HASH) }

fun TagTopTracksResponse.toTagTracks(): List<TagTrack> =
    tracks?.track.orEmpty()
        .filter { it.name.isNotBlank() && it.artist.name.isNotBlank() }
        .mapIndexed { index, dto ->
            TagTrack(
                track = Track(dto.name, dto.artist.name, durationFromSeconds(dto.duration)),
                rank = dto.attr.rank.toIntOrNull() ?: (index + 1),
            )
        }

fun ArtistTopTagsResponse.toArtistTags(): List<ArtistTag> =
    toptags.tag
        .filter { it.name.isNotBlank() }
        .map { ArtistTag(name = it.name, count = it.count.trim().toIntOrNull() ?: 0) }

fun SimilarArtistsResponse.toSimilarArtists(): List<SimilarArtist> =
    similarArtists?.artist.orEmpty()
        .filter { it.name.isNotBlank() }
        .map { SimilarArtist(name = it.name, match = it.match.trim().toDoubleOrNull()?.coerceIn(0.0, 1.0) ?: 0.0) }

/** As imagens deste endpoint são sempre placeholder: não são lidas. */
fun ArtistTopTracksResponse.toTracks(): List<Track> =
    topTracks?.track.orEmpty()
        .filter { it.name.isNotBlank() && it.artist.name.isNotBlank() }
        .map { Track(it.name, it.artist.name) }

/**
 * Detalhes da música, ou `null` se ela não existe (erro 6). A capa é a `extralarge`
 * de `album.image`; sem álbum ou com placeholder, fica `null` (capa indisponível).
 */
fun TrackInfoResponse.toTrackDetails(): TrackDetails? {
    val dto = track?.takeIf { it.name.isNotBlank() } ?: return null
    val image = dto.album?.image.orEmpty()
    val cover = image.firstOrNull { it.size == "extralarge" }?.url
    return TrackDetails(
        track = Track(dto.name, dto.artist.name, durationFromMillis(dto.duration)),
        coverUrl = validCoverUrl(cover),
        tags = dto.toptags.tag.map { it.name }.filter { it.isNotBlank() },
    )
}
