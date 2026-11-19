package com.leticia.moodiva.domain.fake

import com.leticia.moodiva.core.network.NetworkMonitor
import com.leticia.moodiva.core.result.AppError
import com.leticia.moodiva.core.result.AppResult
import com.leticia.moodiva.domain.model.ArtistTag
import com.leticia.moodiva.domain.model.ConversationMode
import com.leticia.moodiva.domain.model.Era
import com.leticia.moodiva.domain.model.Genre
import com.leticia.moodiva.domain.model.Mood
import com.leticia.moodiva.domain.model.OriginPreference
import com.leticia.moodiva.domain.model.Recommendations
import com.leticia.moodiva.domain.model.RecommendedTrack
import com.leticia.moodiva.domain.model.SavedTrack
import com.leticia.moodiva.domain.model.SignInResult
import com.leticia.moodiva.domain.model.SignUpResult
import com.leticia.moodiva.domain.model.SimilarArtist
import com.leticia.moodiva.domain.model.TagTrack
import com.leticia.moodiva.domain.model.TasteProfile
import com.leticia.moodiva.domain.model.Track
import com.leticia.moodiva.domain.model.TrackDetails
import com.leticia.moodiva.domain.model.User
import com.leticia.moodiva.domain.model.UserProfile
import com.leticia.moodiva.domain.repository.AuthRepository
import com.leticia.moodiva.domain.repository.MusicRepository
import com.leticia.moodiva.domain.repository.ProfileRepository
import com.leticia.moodiva.domain.repository.RecommendationRepository
import com.leticia.moodiva.domain.repository.SavedTrackRepository
import com.leticia.moodiva.domain.rule.TextNormalizer
import com.leticia.moodiva.domain.rule.TipiCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

// Implementações em memória dos contratos do domain. Elas se comportam como as reais
// (o Flow emite quando algo é gravado), então os testes verificam comportamento.

object TestData {
    val NOW: Instant = Instant.parse("2026-11-19T12:00:00Z")
    val clock: Clock = Clock.fixed(NOW, ZoneOffset.UTC)
    val USER = User(id = "uid-1", email = "ana@mail.com")

    /** Respostas do CA08: Extroversão 5,5 e Abertura 6,0 (descoberta). */
    val CA08_ANSWERS = listOf(5, 3, 6, 2, 7, 2, 6, 1, 5, 3)

    fun profile(
        answers: List<Int> = CA08_ANSWERS,
        genres: Set<Genre> = setOf(Genre.POP),
        favorites: List<String> = listOf("Fav"),
        origin: OriginPreference = OriginPreference.BOTH,
    ) = UserProfile(
        personality = TipiCalculator.calculate(answers)!!,
        taste = TasteProfile(genres, favorites, setOf(Era.NINETIES), origin),
    )

    fun track(n: Int, artist: String = "Artist $n") = Track(name = "Song $n", artist = artist)

    fun recommended(n: Int, source: com.leticia.moodiva.domain.model.TrackSource = com.leticia.moodiva.domain.model.TrackSource.MOOD) =
        RecommendedTrack(track(n), source)
}

class FakeNetworkMonitor(online: Boolean = true) : NetworkMonitor {
    override val isOnline = MutableStateFlow(online)
}

class FakeAuthRepository(initialUser: User? = TestData.USER) : AuthRepository {
    private val accounts = mutableMapOf<String, Pair<User, String>>() // e-mail → (usuário, senha)
    override val session = MutableStateFlow(initialUser)

    override suspend fun signUp(email: String, password: String): SignUpResult {
        if (email in accounts) return SignUpResult.EmailAlreadyUsed
        val user = User(id = "uid-${accounts.size + 100}", email = email)
        accounts[email] = user to password
        session.value = user
        return SignUpResult.Success(user)
    }

    override suspend fun signIn(email: String, password: String): SignInResult {
        val (user, stored) = accounts[email] ?: return SignInResult.InvalidCredentials
        if (stored != password) return SignInResult.InvalidCredentials
        session.value = user
        return SignInResult.Success(user)
    }

    override suspend fun signOut() {
        session.value = null
    }
}

class FakeProfileRepository : ProfileRepository {
    val profiles = MutableStateFlow<Map<String, UserProfile>>(emptyMap())
    var saveResult: AppResult<Unit> = AppResult.Success(Unit)

    override fun observeProfile(userId: String): Flow<UserProfile?> = profiles.map { it[userId] }

    override suspend fun saveProfile(userId: String, profile: UserProfile): AppResult<Unit> {
        if (saveResult is AppResult.Success) profiles.value += (userId to profile)
        return saveResult
    }
}

class FakeRecommendationRepository : RecommendationRepository {
    private data class Key(val userId: String, val mood: Mood, val mode: ConversationMode)

    private val cache = MutableStateFlow<Map<Key, Recommendations>>(emptyMap())
    private val covers = MutableStateFlow<Map<String, String?>>(emptyMap())
    var replaceCalls = 0
        private set
    var clearCalls = 0
        private set

    fun givenCache(userId: String, mood: Mood, mode: ConversationMode, value: Recommendations) {
        cache.value += (Key(userId, mood, mode) to value)
    }

    override fun observeRecommendations(userId: String, mood: Mood, mode: ConversationMode): Flow<Recommendations> =
        kotlinx.coroutines.flow.combine(cache, covers) { all, coverMap ->
            val recs = all[Key(userId, mood, mode)] ?: Recommendations(emptyList(), null)
            recs.copy(
                tracks = recs.tracks.map { t ->
                    if (t.track.key in coverMap) t.copy(coverUrl = coverMap[t.track.key], coverChecked = true) else t
                },
            )
        }

    override suspend fun replaceRecommendations(
        userId: String,
        mood: Mood,
        mode: ConversationMode,
        tracks: List<RecommendedTrack>,
        updatedAt: Instant,
    ): AppResult<Unit> {
        replaceCalls++
        cache.value += (Key(userId, mood, mode) to Recommendations(tracks, updatedAt))
        return AppResult.Success(Unit)
    }

    override suspend fun clearRecommendations(userId: String): AppResult<Unit> {
        clearCalls++
        cache.value = cache.value.filterKeys { it.userId != userId }
        return AppResult.Success(Unit)
    }

    override suspend fun isCoverChecked(trackKey: String): Boolean = trackKey in covers.value

    override suspend fun saveCover(trackKey: String, coverUrl: String?): AppResult<Unit> {
        covers.value += (trackKey to coverUrl)
        return AppResult.Success(Unit)
    }
}

class FakeSavedTrackRepository : SavedTrackRepository {
    private val saved = MutableStateFlow<Map<String, Map<String, SavedTrack>>>(emptyMap()) // uid → chave → música

    fun given(userId: String, track: SavedTrack) {
        saved.value += (userId to (saved.value[userId].orEmpty() + (track.track.key to track)))
    }

    override fun observeSavedTracks(userId: String): Flow<List<SavedTrack>> =
        saved.map { it[userId].orEmpty().values.toList() }

    override suspend fun getSavedTrack(userId: String, trackKey: String): SavedTrack? = saved.value[userId]?.get(trackKey)

    override suspend fun insert(userId: String, savedTrack: SavedTrack): AppResult<Unit> {
        given(userId, savedTrack)
        return AppResult.Success(Unit)
    }

    override suspend fun update(userId: String, savedTrack: SavedTrack): AppResult<Unit> = insert(userId, savedTrack)

    override suspend fun delete(userId: String, trackKey: String): AppResult<Unit> {
        saved.value += (userId to (saved.value[userId].orEmpty() - trackKey))
        return AppResult.Success(Unit)
    }
}

/**
 * Last.fm falso: respostas configuráveis por tag/artista/música, com registro de chamadas.
 * Artistas sem tags configuradas não têm gênero (lista vazia, como o erro 6).
 */
class FakeMusicRepository : MusicRepository {
    val tagTracks = mutableMapOf<String, List<TagTrack>>()
    val artistTagsByName = mutableMapOf<String, List<ArtistTag>>()
    val similar = mutableMapOf<String, List<SimilarArtist>>()
    val topTracks = mutableMapOf<String, List<Track>>()
    val details = mutableMapOf<String, TrackDetails>()
    var failure: AppError? = null
    var failTasteSource = false
    val calls = mutableListOf<String>()

    private fun <T> respond(call: String, value: T): AppResult<T> {
        synchronized(calls) { calls += call }
        return failure?.let { AppResult.Failure(it) } ?: AppResult.Success(value)
    }

    override suspend fun tagTopTracks(tag: String, limit: Int) = respond("tag:$tag", tagTracks[tag].orEmpty().take(limit))

    override suspend fun artistTags(artist: String) =
        respond("tags:$artist", artistTagsByName[TextNormalizer.normalize(artist)].orEmpty())

    override suspend fun similarArtists(artist: String, limit: Int): AppResult<List<SimilarArtist>> =
        if (failTasteSource) AppResult.Failure(AppError.Network)
        else respond("similar:$artist", similar[artist].orEmpty().take(limit))

    override suspend fun artistTopTracks(artist: String, limit: Int) = respond("top:$artist", topTracks[artist].orEmpty().take(limit))

    override suspend fun trackDetails(track: Track) = respond("info:${track.key}", details[track.key])
}
