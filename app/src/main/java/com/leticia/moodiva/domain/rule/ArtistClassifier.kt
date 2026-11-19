package com.leticia.moodiva.domain.rule

import com.leticia.moodiva.domain.model.ArtistProfile
import com.leticia.moodiva.domain.model.ArtistTag
import com.leticia.moodiva.domain.model.Genre
import com.leticia.moodiva.domain.model.OriginPreference

/**
 * Gêneros e nacionalidade de um artista a partir das tags do `artist.getTopTags`.
 *
 * - RN14: uma tag só conta como gênero se, normalizada, pertencer à lista fechada
 *   ([Genre]) **e** tiver `count` ≥ [MIN_GENRE_COUNT]. "female vocalists" ou "british"
 *   não são gêneros e são ignoradas.
 * - RN13: o artista é brasileiro se tiver alguma das tags `brazilian`, `brasil` ou `brazil`
 *   (com qualquer peso). Sem nenhuma delas, conta como internacional.
 */
object ArtistClassifier {

    const val MIN_GENRE_COUNT = 10
    val BRAZILIAN_TAGS = setOf("brazilian", "brasil", "brazil")

    fun genresOf(tags: List<ArtistTag>): Set<Genre> =
        tags.asSequence()
            .filter { it.count >= MIN_GENRE_COUNT }
            .map { TextNormalizer.normalize(it.name) }
            .flatMap { tag -> Genre.entries.filter { tag in it.tags } }
            .toSet()

    fun isBrazilian(tags: List<ArtistTag>): Boolean =
        tags.any { TextNormalizer.normalize(it.name) in BRAZILIAN_TAGS }

    fun classify(tags: List<ArtistTag>): ArtistProfile =
        ArtistProfile(genres = genresOf(tags), isBrazilian = isBrazilian(tags))
}

/** Preferência de origem (RN17). */
object OriginFilter {

    fun accepts(origin: OriginPreference, isBrazilian: Boolean): Boolean = when (origin) {
        OriginPreference.NATIONAL -> isBrazilian
        OriginPreference.INTERNATIONAL -> !isBrazilian
        OriginPreference.BOTH -> true
    }
}
