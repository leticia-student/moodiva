package com.leticia.moodiva.domain.rule

import com.leticia.moodiva.domain.model.ArtistProfile
import com.leticia.moodiva.domain.model.OpennessLevel
import com.leticia.moodiva.domain.model.RecommendedTrack
import com.leticia.moodiva.domain.model.SimilarArtist
import com.leticia.moodiva.domain.model.TagTrack
import com.leticia.moodiva.domain.model.TasteProfile
import com.leticia.moodiva.domain.model.Track
import com.leticia.moodiva.domain.model.TrackDetails
import com.leticia.moodiva.domain.model.TrackSource

/** Uma música candidata da fonte principal, com as tags do humor em que apareceu. */
data class MoodCandidate(
    val track: Track,
    val moodTags: Set<String>,
    val bestRank: Int,
)

/**
 * Fonte principal "Pelo seu humor" (RN18, RN19).
 */
object MoodSourceRanker {

    /** Analisa no máximo as 30 primeiras músicas de cada tag (RN18). */
    const val MAX_TRACKS_PER_TAG = 30

    /**
     * Junta as listas de cada tag numa lista de candidatas sem repetição (RN23).
     * A mesma música em várias tags acumula as tags e guarda a melhor posição.
     */
    fun collect(tracksByTag: Map<String, List<TagTrack>>): List<MoodCandidate> {
        val byKey = linkedMapOf<String, MoodCandidate>()
        for ((tag, tracks) in tracksByTag) {
            for (tagTrack in tracks.sortedBy { it.rank }.take(MAX_TRACKS_PER_TAG)) {
                val key = tagTrack.track.key
                val current = byKey[key]
                byKey[key] = if (current == null) {
                    MoodCandidate(tagTrack.track, setOf(tag), tagTrack.rank)
                } else {
                    current.copy(
                        moodTags = current.moodTags + tag,
                        bestRank = minOf(current.bestRank, tagTrack.rank),
                    )
                }
            }
        }
        return byKey.values.toList()
    }

    /** Artistas distintos (normalizados) das candidatas, para buscar gêneros e nacionalidade. */
    fun artistsOf(candidates: List<MoodCandidate>): List<String> =
        candidates.map { it.track.artist }.distinctBy(TextNormalizer::normalize)

    /**
     * Filtra e ordena (RN18, RN19):
     * - só artistas com algum gênero em comum com o perfil e que respeitam a origem;
     * - artistas sem perfil conhecido (falha ao classificar) ficam de fora;
     * - ordem: mais tags do humor ↓, melhor posição ↑, desempate da Extroversão ↓ (RN07).
     *
     * @param artistProfiles perfis por nome de artista **normalizado**.
     */
    fun rank(
        candidates: List<MoodCandidate>,
        artistProfiles: Map<String, ArtistProfile>,
        taste: TasteProfile,
        extraversion: Double,
    ): List<Track> =
        candidates
            .filter { candidate ->
                val profile = artistProfiles[TextNormalizer.normalize(candidate.track.artist)]
                profile != null &&
                    profile.genres.any { it in taste.genres } &&
                    OriginFilter.accepts(taste.origin, profile.isBrazilian)
            }
            .sortedWith(
                compareByDescending<MoodCandidate> { it.moodTags.size }
                    .thenBy { it.bestRank }
                    .thenByDescending { ExtraversionTiebreak.score(extraversion, it.moodTags) },
            )
            .map { it.track }
}

/** Uma música candidata da fonte de complemento, com o `match` do artista similar. */
data class TasteCandidate(
    val track: Track,
    val artistMatch: Double,
)

/**
 * Fonte de complemento "Pelo seu gosto" (RN06, RN20).
 */
object TasteSourceRanker {

    const val SIMILAR_LIMIT = 30
    const val MAX_ARTISTS = 8
    const val TRACKS_PER_ARTIST = 3
    const val MAX_CANDIDATES = 20

    /**
     * Junta os similares de todos os favoritos: tira os próprios favoritos e repetidos
     * (fica o maior `match`) e ordena por `match` decrescente.
     */
    fun mergeSimilar(similarByFavorite: List<List<SimilarArtist>>, favorites: List<String>): List<SimilarArtist> {
        val favoriteKeys = favorites.map(TextNormalizer::normalize).toSet()
        return similarByFavorite.flatten()
            .filter { TextNormalizer.normalize(it.name) !in favoriteKeys }
            .groupBy { TextNormalizer.normalize(it.name) }
            .map { (_, sameArtist) -> sameArtist.maxBy { it.match } }
            .sortedByDescending { it.match }
    }

    /**
     * Ordem de preferência dos artistas dentro da faixa de `match` da Abertura (RN06).
     *
     * Primeiro vêm até [MAX_ARTISTS] artistas **espaçados uniformemente** na lista
     * (do mais próximo ao mais distante da faixa), depois os demais por `match`.
     * Assim o perfil "descoberta" recebe também artistas distantes, e quem consome
     * a lista pode pular artistas recusados pela origem sem perder a variedade.
     */
    fun preferenceOrder(similar: List<SimilarArtist>, level: OpennessLevel): List<SimilarArtist> {
        val inRange = similar
            .filter { OpennessProfile.acceptsMatch(level, it.match) }
            .sortedByDescending { it.match }
        if (inRange.size <= MAX_ARTISTS) return inRange
        val spacedIndices = (0 until MAX_ARTISTS)
            .map { i -> i * (inRange.size - 1) / (MAX_ARTISTS - 1) }
            .distinct()
        val spaced = spacedIndices.map { inRange[it] }
        return spaced + inRange.filterIndexed { index, _ -> index !in spacedIndices }
    }

    /**
     * Intercala as músicas por artista (1ª de cada, 2ª de cada…) para variar,
     * sem repetir músicas, até [MAX_CANDIDATES].
     */
    fun interleave(tracksByArtist: List<Pair<SimilarArtist, List<Track>>>): List<TasteCandidate> {
        val result = mutableListOf<TasteCandidate>()
        val seen = mutableSetOf<String>()
        val maxDepth = tracksByArtist.maxOfOrNull { it.second.size } ?: 0
        for (depth in 0 until maxDepth) {
            for ((artist, tracks) in tracksByArtist) {
                val track = tracks.getOrNull(depth) ?: continue
                if (seen.add(track.key)) result += TasteCandidate(track, artist.match)
                if (result.size == MAX_CANDIDATES) return result
            }
        }
        return result
    }

    /**
     * Ordena os candidatos já com os detalhes do `track.getInfo` (RN20):
     * tem alguma tag do humor ↓, `match` do artista ↓, desempate da Extroversão ↓.
     * A capa vem dos mesmos detalhes (RN28).
     *
     * @param detailsByKey detalhes por [Track.key]; `null` = música não encontrada.
     */
    fun rank(
        candidates: List<TasteCandidate>,
        detailsByKey: Map<String, TrackDetails?>,
        moodTags: List<String>,
        extraversion: Double,
    ): List<RecommendedTrack> {
        val normalizedMoodTags = moodTags.map(TextNormalizer::normalize).toSet()
        fun tagsOf(candidate: TasteCandidate) =
            detailsByKey[candidate.track.key]?.tags.orEmpty().map(TextNormalizer::normalize)

        return candidates
            .sortedWith(
                compareByDescending<TasteCandidate> { candidate -> tagsOf(candidate).any { it in normalizedMoodTags } }
                    .thenByDescending { it.artistMatch }
                    .thenByDescending { ExtraversionTiebreak.score(extraversion, tagsOf(it)) },
            )
            .map { candidate ->
                val details = detailsByKey[candidate.track.key]
                RecommendedTrack(
                    // O artist.getTopTracks não traz a duração; o track.getInfo traz.
                    track = candidate.track.copy(
                        durationSeconds = details?.track?.durationSeconds ?: candidate.track.durationSeconds,
                    ),
                    source = TrackSource.TASTE,
                    coverUrl = details?.coverUrl,
                    coverChecked = candidate.track.key in detailsByKey,
                )
            }
    }
}

/**
 * Junção das duas fontes (RN20–RN24).
 */
object RecommendationMerger {

    /** Abaixo disso, a fonte de complemento é usada (RN20). */
    const val MIN_MOOD_TRACKS = 10

    /** Tamanho máximo da lista (RN21). */
    const val MAX_TRACKS = 20

    fun needsTasteSource(moodTrackCount: Int): Boolean = moodTrackCount < MIN_MOOD_TRACKS

    /**
     * Principal primeiro, complemento depois, sem duplicadas (RN23; a mesma música
     * nas duas fontes fica como `MOOD`) e no máximo [MAX_TRACKS].
     */
    fun merge(moodTracks: List<Track>, tasteTracks: List<RecommendedTrack>): List<RecommendedTrack> {
        val seen = mutableSetOf<String>()
        val fromMood = moodTracks
            .filter { seen.add(it.key) }
            .map { RecommendedTrack(track = it, source = TrackSource.MOOD) }
        val fromTaste = tasteTracks.filter { seen.add(it.track.key) }
        return (fromMood + fromTaste).take(MAX_TRACKS)
    }

    /** RN24: músicas salvas como ouvidas não aparecem nas recomendações. */
    fun withoutListened(tracks: List<RecommendedTrack>, listenedKeys: Set<String>): List<RecommendedTrack> =
        tracks.filter { it.track.key !in listenedKeys }
}
