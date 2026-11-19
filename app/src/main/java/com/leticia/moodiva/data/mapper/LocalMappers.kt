package com.leticia.moodiva.data.mapper

import com.leticia.moodiva.data.local.entity.ProfileEntity
import com.leticia.moodiva.data.local.entity.RecommendationEntity
import com.leticia.moodiva.data.local.entity.RecommendationRow
import com.leticia.moodiva.data.local.entity.SavedTrackEntity
import com.leticia.moodiva.data.local.entity.StoredTag
import com.leticia.moodiva.data.local.entity.UserEntity
import com.leticia.moodiva.domain.model.ArtistTag
import com.leticia.moodiva.domain.model.ConversationMode
import com.leticia.moodiva.domain.model.Era
import com.leticia.moodiva.domain.model.Genre
import com.leticia.moodiva.domain.model.ListenStatus
import com.leticia.moodiva.domain.model.Mood
import com.leticia.moodiva.domain.model.OriginPreference
import com.leticia.moodiva.domain.model.PersonalityProfile
import com.leticia.moodiva.domain.model.RecommendedTrack
import com.leticia.moodiva.domain.model.SavedTrack
import com.leticia.moodiva.domain.model.TasteProfile
import com.leticia.moodiva.domain.model.Track
import com.leticia.moodiva.domain.model.TrackSource
import com.leticia.moodiva.domain.model.Trait
import com.leticia.moodiva.domain.model.User
import com.leticia.moodiva.domain.model.UserProfile
import java.time.Instant

// Entidade do Room ↔ modelo do domain. Valores de enum desconhecidos (ex.: gravados por uma
// versão futura) são ignorados em vez de derrubar o app.

private const val SEPARATOR = ","

private inline fun <reified E : Enum<E>> parseEnum(name: String): E? = enumValues<E>().firstOrNull { it.name == name }

private inline fun <reified E : Enum<E>> parseEnumSet(text: String): Set<E> =
    text.split(SEPARATOR).filter { it.isNotBlank() }.mapNotNull { parseEnum<E>(it) }.toSet()

fun UserEntity.toDomain() = User(id = uid, email = email)

fun UserProfile.toEntity(uid: String, updatedAt: Instant) = ProfileEntity(
    uid = uid,
    answers = personality.answers.joinToString(SEPARATOR),
    extraversion = personality.score(Trait.EXTRAVERSION),
    agreeableness = personality.score(Trait.AGREEABLENESS),
    conscientiousness = personality.score(Trait.CONSCIENTIOUSNESS),
    emotionalStability = personality.score(Trait.EMOTIONAL_STABILITY),
    openness = personality.score(Trait.OPENNESS),
    genres = taste.genres.joinToString(SEPARATOR) { it.name },
    favoriteArtists = taste.favoriteArtists,
    eras = taste.eras.joinToString(SEPARATOR) { it.name },
    origin = taste.origin.name,
    updatedAt = updatedAt.toEpochMilli(),
)

fun ProfileEntity.toDomain() = UserProfile(
    personality = PersonalityProfile(
        answers = answers.split(SEPARATOR).mapNotNull { it.trim().toIntOrNull() },
        scores = mapOf(
            Trait.EXTRAVERSION to extraversion,
            Trait.AGREEABLENESS to agreeableness,
            Trait.CONSCIENTIOUSNESS to conscientiousness,
            Trait.EMOTIONAL_STABILITY to emotionalStability,
            Trait.OPENNESS to openness,
        ),
    ),
    taste = TasteProfile(
        genres = parseEnumSet<Genre>(genres),
        favoriteArtists = favoriteArtists,
        eras = parseEnumSet<Era>(eras),
        origin = parseEnum<OriginPreference>(origin) ?: OriginPreference.BOTH,
    ),
)

fun RecommendedTrack.toEntity(uid: String, mood: Mood, mode: ConversationMode, position: Int) = RecommendationEntity(
    uid = uid,
    mood = mood.name,
    mode = mode.name,
    trackKey = track.key,
    position = position,
    name = track.name,
    artist = track.artist,
    durationSeconds = track.durationSeconds,
    source = source.name,
)

fun RecommendationRow.toDomain() = RecommendedTrack(
    track = Track(recommendation.name, recommendation.artist, recommendation.durationSeconds),
    source = parseEnum<TrackSource>(recommendation.source) ?: TrackSource.MOOD,
    coverUrl = coverUrl,
    coverChecked = coverChecked,
)

fun SavedTrack.toEntity(uid: String) = SavedTrackEntity(
    uid = uid,
    trackKey = track.key,
    name = track.name,
    artist = track.artist,
    durationSeconds = track.durationSeconds,
    coverUrl = coverUrl,
    source = source.name,
    status = status.name,
    rating = rating,
    savedAt = savedAt.toEpochMilli(),
)

fun SavedTrackEntity.toDomain() = SavedTrack(
    track = Track(name, artist, durationSeconds),
    coverUrl = coverUrl,
    source = parseEnum<TrackSource>(source) ?: TrackSource.MOOD,
    status = parseEnum<ListenStatus>(status) ?: ListenStatus.WANT_TO_LISTEN,
    rating = rating,
    savedAt = Instant.ofEpochMilli(savedAt),
)

fun StoredTag.toDomain() = ArtistTag(name, count)

fun ArtistTag.toStored() = StoredTag(name, count)
