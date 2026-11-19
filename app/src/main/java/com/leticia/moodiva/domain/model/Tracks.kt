package com.leticia.moodiva.domain.model

import com.leticia.moodiva.domain.rule.TrackKey
import java.time.Instant

/**
 * Uma música, identificada por artista + nome.
 *
 * [key] é a identidade normalizada (RN23): "Águas de Março" e "aguas de marco "
 * do mesmo artista têm a mesma chave. A duração não faz parte da identidade.
 *
 * @property durationSeconds duração em segundos, ou `null` se desconhecida.
 */
data class Track(
    val name: String,
    val artist: String,
    val durationSeconds: Int? = null,
) {
    val key: String get() = TrackKey.of(artist = artist, name = name)
}

/** Fonte que trouxe a recomendação (RN22). */
enum class TrackSource {
    /** "Pelo seu humor": tags do humor. */
    MOOD,

    /** "Pelo seu gosto": artistas similares aos favoritos. */
    TASTE,
}

/**
 * Uma música recomendada.
 *
 * @property coverUrl URL da capa válida, ou `null` se ainda não buscada ou indisponível.
 * @property coverChecked `true` quando a capa já foi consultada: `coverUrl == null`
 * significa então "capa indisponível" (RN27, RN28).
 */
data class RecommendedTrack(
    val track: Track,
    val source: TrackSource,
    val coverUrl: String? = null,
    val coverChecked: Boolean = false,
)

/** Recomendações em cache para (conta, humor, modo). `updatedAt == null` = nunca buscadas. */
data class Recommendations(
    val tracks: List<RecommendedTrack>,
    val updatedAt: Instant?,
)

/** Status de uma música salva (RN30). */
enum class ListenStatus {
    WANT_TO_LISTEN,
    LISTENED,
}

/** Música da lista de salvas (RF18–RF21). [rating] só existe para `LISTENED` (RN31). */
data class SavedTrack(
    val track: Track,
    val coverUrl: String?,
    val source: TrackSource,
    val status: ListenStatus,
    val rating: Int?,
    val savedAt: Instant,
)
