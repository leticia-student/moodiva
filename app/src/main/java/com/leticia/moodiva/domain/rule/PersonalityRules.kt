package com.leticia.moodiva.domain.rule

import com.leticia.moodiva.domain.model.OpennessLevel

/**
 * Abertura a experiências → faixa de `match` dos artistas similares (RN06).
 *
 * - ≥ 5,0: descoberta, aceita `match` ≥ 0,10 (artistas mais distantes dos favoritos).
 * - ≤ 3,0: familiaridade, aceita `match` ≥ 0,50 (só artistas próximos).
 * - Entre os dois: equilibrado, aceita `match` ≥ 0,25.
 */
object OpennessProfile {

    const val DISCOVERY_THRESHOLD = 5.0
    const val FAMILIARITY_THRESHOLD = 3.0

    fun levelFor(openness: Double): OpennessLevel = when {
        openness >= DISCOVERY_THRESHOLD -> OpennessLevel.DISCOVERY
        openness <= FAMILIARITY_THRESHOLD -> OpennessLevel.FAMILIARITY
        else -> OpennessLevel.BALANCED
    }

    fun minMatch(level: OpennessLevel): Double = when (level) {
        OpennessLevel.DISCOVERY -> 0.10
        OpennessLevel.BALANCED -> 0.25
        OpennessLevel.FAMILIARITY -> 0.50
    }

    fun acceptsMatch(level: OpennessLevel, match: Double): Boolean = match in minMatch(level)..1.0
}

/**
 * Extroversão como **desempate** no ranking (RN07). Nunca muda quais humores são buscados.
 *
 * - ≥ 5,0: cada tag energética da música vale 1 ponto.
 * - ≤ 3,0: cada tag calma da música vale 1 ponto.
 * - Entre os dois: 0 (sem desempate por personalidade).
 */
object ExtraversionTiebreak {

    const val HIGH_THRESHOLD = 5.0
    const val LOW_THRESHOLD = 3.0

    val ENERGETIC_TAGS = setOf("energetic", "upbeat", "party", "dance")
    val CALM_TAGS = setOf("calm", "relaxing", "chill", "mellow", "acoustic")

    fun score(extraversion: Double, trackTags: Collection<String>): Int {
        val preferred = when {
            extraversion >= HIGH_THRESHOLD -> ENERGETIC_TAGS
            extraversion <= LOW_THRESHOLD -> CALM_TAGS
            else -> return 0
        }
        return trackTags.map(TextNormalizer::normalize).toSet().count { it in preferred }
    }
}
