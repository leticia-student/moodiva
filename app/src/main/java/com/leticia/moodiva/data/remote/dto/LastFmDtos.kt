package com.leticia.moodiva.data.remote.dto

import com.leticia.moodiva.data.remote.LenientStringSerializer
import com.leticia.moodiva.data.remote.ObjectOrEmptySerializer
import com.leticia.moodiva.data.remote.SingleOrListSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// DTOs do Last.fm (formato verificado em 2026-11-19, planejamento seção 3.3).
// Todos os campos têm valor padrão: campos ausentes não quebram a leitura.
// `error`/`message` aparecem no corpo quando há erro, inclusive com HTTP 200 (erro 6).

@Serializable
data class ImageDto(
    @SerialName("#text") val url: String = "",
    val size: String = "",
)

@Serializable
data class ArtistRefDto(
    val name: String = "",
)

@Serializable
data class RankAttrDto(
    @Serializable(with = LenientStringSerializer::class) val rank: String = "",
)

// ---------- tag.getTopTracks ----------

@Serializable
data class TagTopTracksResponse(
    val tracks: TagTracksDto? = null,
    val error: Int? = null,
    val message: String? = null,
)

@Serializable
data class TagTracksDto(
    @Serializable(with = TagTrackListSerializer::class) val track: List<TagTrackDto> = emptyList(),
)

/** Duração em **segundos**, como texto ("261"). "0" = desconhecida. */
@Serializable
data class TagTrackDto(
    val name: String = "",
    @Serializable(with = LenientStringSerializer::class) val duration: String = "",
    val artist: ArtistRefDto = ArtistRefDto(),
    @SerialName("@attr") val attr: RankAttrDto = RankAttrDto(),
)

object TagTrackListSerializer : SingleOrListSerializer<TagTrackDto>(TagTrackDto.serializer())

// ---------- artist.getTopTags ----------

@Serializable
data class ArtistTopTagsResponse(
    @Serializable(with = TagListObjectSerializer::class) val toptags: TagListDto = TagListDto(),
    val error: Int? = null,
    val message: String? = null,
)

@Serializable
data class TagListDto(
    @Serializable(with = TagListSerializer::class) val tag: List<TagDto> = emptyList(),
)

/** `count` chega como número em artist.getTopTags; o serializador aceita número ou texto. */
@Serializable
data class TagDto(
    val name: String = "",
    @Serializable(with = LenientStringSerializer::class) val count: String = "",
)

object TagListSerializer : SingleOrListSerializer<TagDto>(TagDto.serializer())

object TagListObjectSerializer : ObjectOrEmptySerializer<TagListDto>(TagListDto.serializer())

// ---------- artist.getSimilar ----------

@Serializable
data class SimilarArtistsResponse(
    @SerialName("similarartists") val similarArtists: SimilarArtistListDto? = null,
    val error: Int? = null,
    val message: String? = null,
)

@Serializable
data class SimilarArtistListDto(
    @Serializable(with = SimilarArtistListSerializer::class) val artist: List<SimilarArtistDto> = emptyList(),
)

/** `match` chega como texto ("0.690513"). As imagens são sempre placeholder e não são lidas. */
@Serializable
data class SimilarArtistDto(
    val name: String = "",
    @Serializable(with = LenientStringSerializer::class) val match: String = "",
)

object SimilarArtistListSerializer : SingleOrListSerializer<SimilarArtistDto>(SimilarArtistDto.serializer())

// ---------- artist.getTopTracks ----------

@Serializable
data class ArtistTopTracksResponse(
    @SerialName("toptracks") val topTracks: ArtistTrackListDto? = null,
    val error: Int? = null,
    val message: String? = null,
)

@Serializable
data class ArtistTrackListDto(
    @Serializable(with = ArtistTrackListSerializer::class) val track: List<ArtistTrackDto> = emptyList(),
)

@Serializable
data class ArtistTrackDto(
    val name: String = "",
    val artist: ArtistRefDto = ArtistRefDto(),
)

object ArtistTrackListSerializer : SingleOrListSerializer<ArtistTrackDto>(ArtistTrackDto.serializer())

// ---------- track.getInfo ----------

@Serializable
data class TrackInfoResponse(
    val track: TrackInfoDto? = null,
    val error: Int? = null,
    val message: String? = null,
)

/** Duração em **milissegundos**, como texto ("232000"). "0" = desconhecida. */
@Serializable
data class TrackInfoDto(
    val name: String = "",
    @Serializable(with = LenientStringSerializer::class) val duration: String = "",
    val artist: ArtistRefDto = ArtistRefDto(),
    val album: AlbumDto? = null,
    @Serializable(with = TagListObjectSerializer::class) val toptags: TagListDto = TagListDto(),
)

@Serializable
data class AlbumDto(
    @Serializable(with = ImageListSerializer::class) val image: List<ImageDto> = emptyList(),
)

object ImageListSerializer : SingleOrListSerializer<ImageDto>(ImageDto.serializer())
