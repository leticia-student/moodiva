package com.leticia.moodiva.presentation.util

import androidx.annotation.StringRes
import com.leticia.moodiva.R
import com.leticia.moodiva.core.result.AppError
import com.leticia.moodiva.domain.model.ConversationMode
import com.leticia.moodiva.domain.model.Era
import com.leticia.moodiva.domain.model.Genre
import com.leticia.moodiva.domain.model.Mood
import com.leticia.moodiva.domain.model.OriginPreference
import com.leticia.moodiva.domain.model.TrackSource
import com.leticia.moodiva.domain.model.Trait
import java.time.Duration
import java.time.Instant

// Textos dos enums do domain. O domain não conhece `R`: a tradução acontece aqui.

/** Humor com emoji, para os chips da conversa. */
@get:StringRes
val Mood.labelRes: Int
    get() = when (this) {
        Mood.HAPPY -> R.string.mood_happy
        Mood.SAD -> R.string.mood_sad
        Mood.EXCITED -> R.string.mood_excited
        Mood.ANXIOUS -> R.string.mood_anxious
        Mood.TIRED -> R.string.mood_tired
        Mood.ROMANTIC -> R.string.mood_romantic
        Mood.NOSTALGIC -> R.string.mood_nostalgic
        Mood.FOCUSED -> R.string.mood_focused
    }

/** Humor sem emoji, para títulos. */
@get:StringRes
val Mood.plainLabelRes: Int
    get() = when (this) {
        Mood.HAPPY -> R.string.mood_happy_plain
        Mood.SAD -> R.string.mood_sad_plain
        Mood.EXCITED -> R.string.mood_excited_plain
        Mood.ANXIOUS -> R.string.mood_anxious_plain
        Mood.TIRED -> R.string.mood_tired_plain
        Mood.ROMANTIC -> R.string.mood_romantic_plain
        Mood.NOSTALGIC -> R.string.mood_nostalgic_plain
        Mood.FOCUSED -> R.string.mood_focused_plain
    }

@get:StringRes
val ConversationMode.labelRes: Int
    get() = when (this) {
        ConversationMode.MATCH -> R.string.mode_match
        ConversationMode.SHIFT -> R.string.mode_shift
    }

@get:StringRes
val ConversationMode.shortLabelRes: Int
    get() = when (this) {
        ConversationMode.MATCH -> R.string.mode_match_short
        ConversationMode.SHIFT -> R.string.mode_shift_short
    }

@get:StringRes
val Genre.labelRes: Int
    get() = when (this) {
        Genre.POP -> R.string.genre_pop
        Genre.ROCK -> R.string.genre_rock
        Genre.INDIE -> R.string.genre_indie
        Genre.ELECTRONIC -> R.string.genre_electronic
        Genre.HIP_HOP -> R.string.genre_hip_hop
        Genre.RNB_SOUL -> R.string.genre_rnb_soul
        Genre.JAZZ -> R.string.genre_jazz
        Genre.BLUES -> R.string.genre_blues
        Genre.CLASSICAL -> R.string.genre_classical
        Genre.METAL -> R.string.genre_metal
        Genre.REGGAE -> R.string.genre_reggae
        Genre.COUNTRY -> R.string.genre_country
        Genre.LATIN -> R.string.genre_latin
        Genre.K_POP -> R.string.genre_k_pop
        Genre.GOSPEL -> R.string.genre_gospel
        Genre.MPB -> R.string.genre_mpb
        Genre.SAMBA_PAGODE -> R.string.genre_samba_pagode
        Genre.BOSSA_NOVA -> R.string.genre_bossa_nova
        Genre.SERTANEJO -> R.string.genre_sertanejo
        Genre.FORRO -> R.string.genre_forro
        Genre.BRAZILIAN_FUNK -> R.string.genre_brazilian_funk
        Genre.AXE -> R.string.genre_axe
    }

@get:StringRes
val Era.labelRes: Int
    get() = when (this) {
        Era.SIXTIES -> R.string.era_sixties
        Era.SEVENTIES -> R.string.era_seventies
        Era.EIGHTIES -> R.string.era_eighties
        Era.NINETIES -> R.string.era_nineties
        Era.TWO_THOUSANDS -> R.string.era_two_thousands
        Era.TWENTY_TENS -> R.string.era_twenty_tens
        Era.TWENTY_TWENTIES -> R.string.era_twenty_twenties
    }

@get:StringRes
val OriginPreference.labelRes: Int
    get() = when (this) {
        OriginPreference.NATIONAL -> R.string.origin_national
        OriginPreference.INTERNATIONAL -> R.string.origin_international
        OriginPreference.BOTH -> R.string.origin_both
    }

@get:StringRes
val OriginPreference.hintRes: Int
    get() = when (this) {
        OriginPreference.NATIONAL -> R.string.origin_national_hint
        OriginPreference.INTERNATIONAL -> R.string.origin_international_hint
        OriginPreference.BOTH -> R.string.origin_both_hint
    }

@get:StringRes
val Trait.labelRes: Int
    get() = when (this) {
        Trait.EXTRAVERSION -> R.string.trait_extraversion
        Trait.AGREEABLENESS -> R.string.trait_agreeableness
        Trait.CONSCIENTIOUSNESS -> R.string.trait_conscientiousness
        Trait.EMOTIONAL_STABILITY -> R.string.trait_emotional_stability
        Trait.OPENNESS -> R.string.trait_openness
    }

@get:StringRes
val TrackSource.labelRes: Int
    get() = when (this) {
        TrackSource.MOOD -> R.string.source_mood
        TrackSource.TASTE -> R.string.source_taste
    }

/** Itens do TIPI, na ordem do questionário (RN05). */
val TIPI_ITEMS: List<Int> = listOf(
    R.string.tipi_item_1, R.string.tipi_item_2, R.string.tipi_item_3, R.string.tipi_item_4, R.string.tipi_item_5,
    R.string.tipi_item_6, R.string.tipi_item_7, R.string.tipi_item_8, R.string.tipi_item_9, R.string.tipi_item_10,
)

/** Rótulos da escala de 1 a 7 do TIPI. */
val TIPI_SCALE: List<Int> = listOf(
    R.string.tipi_scale_1, R.string.tipi_scale_2, R.string.tipi_scale_3, R.string.tipi_scale_4,
    R.string.tipi_scale_5, R.string.tipi_scale_6, R.string.tipi_scale_7,
)

/**
 * Traduz um [AppError] na mensagem mostrada ao usuário (planejamento, seção 7.2).
 * Devolve o ID do texto, para o ViewModel não precisar de `Context`.
 */
object ErrorMessageMapper {

    @StringRes
    fun messageFor(error: AppError): Int = when (error) {
        AppError.Network -> R.string.error_network
        AppError.InvalidApiKey -> R.string.error_invalid_api_key
        AppError.RateLimited -> R.string.error_rate_limited
        is AppError.Http -> if (error.code in 500..599) R.string.error_server else R.string.error_generic
        is AppError.Api -> R.string.error_server
        AppError.NotSignedIn -> R.string.error_not_signed_in
        AppError.Serialization, AppError.Unknown -> R.string.error_generic
    }
}

/** "Atualizado há…" das recomendações. */
sealed interface UpdatedAtText {
    data object JustNow : UpdatedAtText
    data class MinutesAgo(val minutes: Int) : UpdatedAtText
    data class HoursAgo(val hours: Int) : UpdatedAtText
}

object UpdatedAtFormatter {
    fun format(updatedAt: Instant, now: Instant): UpdatedAtText {
        val age = Duration.between(updatedAt, now).coerceAtLeast(Duration.ZERO)
        return when {
            age < Duration.ofMinutes(1) -> UpdatedAtText.JustNow
            age < Duration.ofHours(1) -> UpdatedAtText.MinutesAgo(age.toMinutes().toInt())
            else -> UpdatedAtText.HoursAgo(age.toHours().toInt())
        }
    }
}

/** Duração "m:ss" (ex.: 261 s → "4:21"). */
fun formatDuration(seconds: Int): String = "%d:%02d".format(seconds / 60, seconds % 60)
