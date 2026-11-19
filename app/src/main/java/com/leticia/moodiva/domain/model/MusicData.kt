package com.leticia.moodiva.domain.model

// Dados vindos do Last.fm, já convertidos pela camada data (DTOs nunca chegam aqui).

/** Tag de um artista com o peso relativo `count` (0 a 100), de `artist.getTopTags`. */
data class ArtistTag(
    val name: String,
    val count: Int,
)

/** Artista similar, com o grau de semelhança `match` (0 a 1), de `artist.getSimilar`. */
data class SimilarArtist(
    val name: String,
    val match: Double,
)

/** Música de uma tag de humor, com a posição [rank] (1 = mais associada), de `tag.getTopTracks`. */
data class TagTrack(
    val track: Track,
    val rank: Int,
)

/** Detalhes de uma música (`track.getInfo`): capa válida ou `null`, e tags da própria música. */
data class TrackDetails(
    val track: Track,
    val coverUrl: String?,
    val tags: List<String>,
)
