package com.leticia.moodiva.domain.usecase

import com.leticia.moodiva.core.result.AppResult
import com.leticia.moodiva.domain.model.ArtistProfile
import com.leticia.moodiva.domain.model.ConversationMode
import com.leticia.moodiva.domain.model.Mood
import com.leticia.moodiva.domain.model.OriginPreference
import com.leticia.moodiva.domain.model.RecommendedTrack
import com.leticia.moodiva.domain.model.SimilarArtist
import com.leticia.moodiva.domain.model.Trait
import com.leticia.moodiva.domain.model.TrackDetails
import com.leticia.moodiva.domain.model.UserProfile
import com.leticia.moodiva.domain.repository.MusicRepository
import com.leticia.moodiva.domain.rule.ArtistClassifier
import com.leticia.moodiva.domain.rule.MoodSourceRanker
import com.leticia.moodiva.domain.rule.MoodTagMapper
import com.leticia.moodiva.domain.rule.OpennessProfile
import com.leticia.moodiva.domain.rule.OriginFilter
import com.leticia.moodiva.domain.rule.RecommendationMerger
import com.leticia.moodiva.domain.rule.TasteSourceRanker
import com.leticia.moodiva.domain.rule.TextNormalizer
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/**
 * Motor de recomendação com duas fontes (planejamento, seção 3.4; RN18–RN24).
 *
 * 1. **"Pelo seu humor"**: músicas das tags do humor, filtradas pelos gêneros do perfil
 *    e pela preferência de origem.
 * 2. **"Pelo seu gosto"**: só se a primeira trouxer menos de 10 músicas. Usa artistas
 *    similares aos favoritos, na faixa de `match` da Abertura.
 *
 * As chamadas independentes rodam em paralelo, com no máximo [MAX_PARALLEL_REQUESTS]
 * ao mesmo tempo (RNF10). Se a fonte principal falhar, a geração falha (o cache anterior
 * fica intacto). Se só o complemento falhar e já houver músicas pelo humor, elas são usadas.
 */
class GenerateRecommendationsUseCase(
    private val musicRepository: MusicRepository,
) {

    suspend operator fun invoke(
        profile: UserProfile,
        mood: Mood,
        mode: ConversationMode,
    ): AppResult<List<RecommendedTrack>> = coroutineScope {
        val semaphore = Semaphore(MAX_PARALLEL_REQUESTS)
        val extraversion = profile.personality.score(Trait.EXTRAVERSION)
        val moodTags = MoodTagMapper.tagsFor(mood, MoodTagMapper.effectiveMode(mood, mode))

        // ---------- Fonte principal ----------
        val tagResults = moodTags.map { tag ->
            async { tag to semaphore.withPermit { musicRepository.tagTopTracks(tag, MoodSourceRanker.MAX_TRACKS_PER_TAG) } }
        }.awaitAll()
        tagResults.firstFailure { it.second }?.let { return@coroutineScope it }

        val candidates = MoodSourceRanker.collect(tagResults.associate { (tag, result) -> tag to result.successData() })
        val profiles = when (val result = classifyArtists(MoodSourceRanker.artistsOf(candidates), semaphore)) {
            is AppResult.Success -> result.data
            is AppResult.Failure -> return@coroutineScope result
        }
        val moodTracks = MoodSourceRanker.rank(candidates, profiles, profile.taste, extraversion)

        if (!RecommendationMerger.needsTasteSource(moodTracks.size)) {
            return@coroutineScope AppResult.Success(RecommendationMerger.merge(moodTracks, emptyList()))
        }

        // ---------- Fonte de complemento ----------
        when (val taste = tasteSource(profile, moodTags, extraversion, semaphore)) {
            is AppResult.Success -> AppResult.Success(RecommendationMerger.merge(moodTracks, taste.data))
            is AppResult.Failure ->
                if (moodTracks.isNotEmpty()) AppResult.Success(RecommendationMerger.merge(moodTracks, emptyList()))
                else taste
        }
    }

    private suspend fun tasteSource(
        profile: UserProfile,
        moodTags: List<String>,
        extraversion: Double,
        semaphore: Semaphore,
    ): AppResult<List<RecommendedTrack>> = coroutineScope {
        val favorites = profile.taste.favoriteArtists
        val similarResults = favorites.map { favorite ->
            async { semaphore.withPermit { musicRepository.similarArtists(favorite, TasteSourceRanker.SIMILAR_LIMIT) } }
        }.awaitAll()
        similarResults.firstFailure { it }?.let { return@coroutineScope it }

        val merged = TasteSourceRanker.mergeSimilar(similarResults.map { it.successData() }, favorites)
        val level = OpennessProfile.levelFor(profile.personality.score(Trait.OPENNESS))
        val ordered = TasteSourceRanker.preferenceOrder(merged, level)

        val chosen = when (val result = chooseArtistsByOrigin(ordered, profile.taste.origin, semaphore)) {
            is AppResult.Success -> result.data
            is AppResult.Failure -> return@coroutineScope result
        }

        val topTracks = chosen.map { artist ->
            async { artist to semaphore.withPermit { musicRepository.artistTopTracks(artist.name, TasteSourceRanker.TRACKS_PER_ARTIST) } }
        }.awaitAll()
        topTracks.firstFailure { it.second }?.let { return@coroutineScope it }

        val candidates = TasteSourceRanker.interleave(topTracks.map { (artist, result) -> artist to result.successData() })
        val details = candidates.map { candidate ->
            async { candidate.track.key to semaphore.withPermit { musicRepository.trackDetails(candidate.track) } }
        }.awaitAll()
        details.firstFailure { it.second }?.let { return@coroutineScope it }

        val detailsByKey: Map<String, TrackDetails?> = details.associate { (key, result) -> key to result.successData() }
        AppResult.Success(TasteSourceRanker.rank(candidates, detailsByKey, moodTags, extraversion))
    }

    /**
     * Percorre os artistas na ordem de preferência, em blocos, até ter [TasteSourceRanker.MAX_ARTISTS]
     * que respeitem a preferência de origem (RN17). Com "Ambas", não precisa consultar nada.
     */
    private suspend fun chooseArtistsByOrigin(
        ordered: List<SimilarArtist>,
        origin: OriginPreference,
        semaphore: Semaphore,
    ): AppResult<List<SimilarArtist>> {
        val max = TasteSourceRanker.MAX_ARTISTS
        if (origin == OriginPreference.BOTH) return AppResult.Success(ordered.take(max))

        val chosen = mutableListOf<SimilarArtist>()
        for (block in ordered.chunked(max).take(MAX_ORIGIN_BLOCKS)) {
            val profiles = when (val result = classifyArtists(block.map { it.name }, semaphore)) {
                is AppResult.Success -> result.data
                is AppResult.Failure -> return result
            }
            chosen += block.filter { artist ->
                val profile = profiles[TextNormalizer.normalize(artist.name)]
                profile != null && OriginFilter.accepts(origin, profile.isBrazilian)
            }
            if (chosen.size >= max) break
        }
        return AppResult.Success(chosen.take(max))
    }

    /** Gêneros e nacionalidade de cada artista (RN13, RN14), por nome normalizado. */
    private suspend fun classifyArtists(
        artists: List<String>,
        semaphore: Semaphore,
    ): AppResult<Map<String, ArtistProfile>> = coroutineScope {
        val results = artists.map { artist ->
            async { artist to semaphore.withPermit { musicRepository.artistTags(artist) } }
        }.awaitAll()
        results.firstFailure { it.second }?.let { return@coroutineScope it }
        AppResult.Success(
            results.associate { (artist, result) ->
                TextNormalizer.normalize(artist) to ArtistClassifier.classify(result.successData())
            },
        )
    }

    private companion object {
        /** Limite próprio de requisições simultâneas ao Last.fm (RNF10). */
        const val MAX_PARALLEL_REQUESTS = 4

        /** Blocos de artistas consultados para achar os da origem preferida. */
        const val MAX_ORIGIN_BLOCKS = 3
    }
}

/** Primeira falha da lista, se houver. */
private fun <T> List<T>.firstFailure(result: (T) -> AppResult<*>): AppResult.Failure? =
    firstNotNullOfOrNull { result(it) as? AppResult.Failure }

/** Dado de um resultado que já se sabe ser sucesso. */
private fun <T> AppResult<T>.successData(): T = (this as AppResult.Success).data
