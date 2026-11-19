package com.leticia.moodiva.data.repository

import com.leticia.moodiva.core.dispatcher.AppDispatchers
import com.leticia.moodiva.core.result.AppResult
import com.leticia.moodiva.data.auth.PasswordHasher
import com.leticia.moodiva.data.auth.SessionStore
import com.leticia.moodiva.data.local.dao.ArtistTagsDao
import com.leticia.moodiva.data.local.dao.ProfileDao
import com.leticia.moodiva.data.local.dao.RecommendationDao
import com.leticia.moodiva.data.local.dao.SavedTrackDao
import com.leticia.moodiva.data.local.dao.UserDao
import com.leticia.moodiva.data.local.entity.ArtistTagsEntity
import com.leticia.moodiva.data.local.entity.RecommendationMetaEntity
import com.leticia.moodiva.data.local.entity.TrackCoverEntity
import com.leticia.moodiva.data.local.entity.UserEntity
import com.leticia.moodiva.data.mapper.LASTFM_ERROR_NOT_FOUND
import com.leticia.moodiva.data.mapper.toArtistTags
import com.leticia.moodiva.data.mapper.toDomain
import com.leticia.moodiva.data.mapper.toEntity
import com.leticia.moodiva.data.mapper.toSimilarArtists
import com.leticia.moodiva.data.mapper.toStored
import com.leticia.moodiva.data.mapper.toTagTracks
import com.leticia.moodiva.data.mapper.toTrackDetails
import com.leticia.moodiva.data.mapper.toTracks
import com.leticia.moodiva.data.remote.LastFmApi
import com.leticia.moodiva.domain.model.ArtistTag
import com.leticia.moodiva.domain.model.ConversationMode
import com.leticia.moodiva.domain.model.Mood
import com.leticia.moodiva.domain.model.Recommendations
import com.leticia.moodiva.domain.model.RecommendedTrack
import com.leticia.moodiva.domain.model.SavedTrack
import com.leticia.moodiva.domain.model.SignInResult
import com.leticia.moodiva.domain.model.SignUpResult
import com.leticia.moodiva.domain.model.SimilarArtist
import com.leticia.moodiva.domain.model.TagTrack
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Clock
import java.time.Instant
import java.util.UUID

/**
 * Contas locais (RF01–RF03, RNF14). As senhas viram hash PBKDF2; a sessão guarda só o uid.
 * Se o uid salvo não existir mais no banco, a sessão é tratada como vazia.
 */
class AuthRepositoryImpl(
    private val userDao: UserDao,
    private val passwordHasher: PasswordHasher,
    private val sessionStore: SessionStore,
    private val clock: Clock,
    private val dispatchers: AppDispatchers,
) : AuthRepository {

    @OptIn(ExperimentalCoroutinesApi::class) // flatMapLatest
    override val session: Flow<User?> = sessionStore.userId.flatMapLatest { uid ->
        if (uid == null) flowOf(null) else userDao.observeById(uid).map { it?.toDomain() }
    }

    override suspend fun signUp(email: String, password: String): SignUpResult = withContext(dispatchers.io) {
        val existing = when (val result = safeDbCall { userDao.findByEmail(email) }) {
            is AppResult.Success -> result.data
            is AppResult.Failure -> return@withContext SignUpResult.Failure(result.error)
        }
        if (existing != null) return@withContext SignUpResult.EmailAlreadyUsed

        val hashed = passwordHasher.hash(password)
        val entity = UserEntity(
            uid = UUID.randomUUID().toString(),
            email = email,
            passwordHash = hashed.hash,
            passwordSalt = hashed.salt,
            createdAt = clock.millis(),
        )
        when (val result = safeDbCall { userDao.insert(entity) }) {
            is AppResult.Success -> {
                sessionStore.save(entity.uid)
                SignUpResult.Success(entity.toDomain())
            }
            is AppResult.Failure -> SignUpResult.Failure(result.error)
        }
    }

    override suspend fun signIn(email: String, password: String): SignInResult = withContext(dispatchers.io) {
        val user = when (val result = safeDbCall { userDao.findByEmail(email) }) {
            is AppResult.Success -> result.data
            is AppResult.Failure -> return@withContext SignInResult.Failure(result.error)
        }
        // A mesma resposta para e-mail inexistente e senha errada: não revela quais e-mails existem.
        if (user == null || !passwordHasher.verify(password, user.passwordHash, user.passwordSalt)) {
            return@withContext SignInResult.InvalidCredentials
        }
        sessionStore.save(user.uid)
        SignInResult.Success(user.toDomain())
    }

    override suspend fun signOut() {
        sessionStore.clear()
    }
}

class ProfileRepositoryImpl(
    private val profileDao: ProfileDao,
    private val clock: Clock,
    private val dispatchers: AppDispatchers,
) : ProfileRepository {

    override fun observeProfile(userId: String): Flow<UserProfile?> =
        profileDao.observe(userId).map { it?.toDomain() }

    override suspend fun saveProfile(userId: String, profile: UserProfile): AppResult<Unit> =
        withContext(dispatchers.io) {
            safeDbCall { profileDao.upsert(profile.toEntity(userId, clock.instant())) }
        }
}

/**
 * Catálogo do Last.fm (planejamento, seção 3). As tags dos artistas ficam em cache
 * sem validade (RN16): cada artista é consultado na rede uma vez só por aparelho.
 */
class MusicRepositoryImpl(
    private val api: LastFmApi,
    private val artistTagsDao: ArtistTagsDao,
    private val clock: Clock,
    private val dispatchers: AppDispatchers,
) : MusicRepository {

    override suspend fun tagTopTracks(tag: String, limit: Int): AppResult<List<TagTrack>> =
        lastFmCall({ api.tagTopTracks(tag, limit) }, { it.error }, emptyList()) { it.toTagTracks() }

    override suspend fun artistTags(artist: String): AppResult<List<ArtistTag>> = withContext(dispatchers.io) {
        val key = TextNormalizer.normalize(artist)
        val cached = (safeDbCall { artistTagsDao.get(key) } as? AppResult.Success)?.data
        if (cached != null) return@withContext AppResult.Success(cached.tags.map { it.toDomain() })

        val remote = lastFmCall({ api.artistTopTags(artist) }, { it.error }, emptyList()) { it.toArtistTags() }
        if (remote is AppResult.Success) {
            // Falha ao gravar o cache não impede a recomendação: só será buscado de novo depois.
            safeDbCall { artistTagsDao.upsert(ArtistTagsEntity(key, remote.data.map { it.toStored() }, clock.millis())) }
        }
        remote
    }

    override suspend fun similarArtists(artist: String, limit: Int): AppResult<List<SimilarArtist>> =
        lastFmCall({ api.similarArtists(artist, limit) }, { it.error }, emptyList()) { it.toSimilarArtists() }

    override suspend fun artistTopTracks(artist: String, limit: Int): AppResult<List<Track>> =
        lastFmCall({ api.artistTopTracks(artist, limit) }, { it.error }, emptyList()) { it.toTracks() }

    override suspend fun trackDetails(track: Track): AppResult<TrackDetails?> =
        lastFmCall({ api.trackInfo(track.artist, track.name) }, { it.error }, null) { it.toTrackDetails() }

    /**
     * Chamada ao Last.fm tratando o `error` do corpo: "não encontrado" (6) vira [notFound];
     * os demais códigos viram falha (planejamento, seção 3.3).
     */
    private suspend fun <R, T> lastFmCall(
        request: suspend () -> R,
        errorOf: (R) -> Int?,
        notFound: T,
        map: (R) -> T,
    ): AppResult<T> = withContext(dispatchers.io) {
        when (val result = safeApiCall { request() }) {
            is AppResult.Failure -> result
            is AppResult.Success -> when (val code = errorOf(result.data)) {
                null -> AppResult.Success(map(result.data))
                LASTFM_ERROR_NOT_FOUND -> AppResult.Success(notFound)
                else -> AppResult.Failure(lastFmError(code))
            }
        }
    }
}

class RecommendationRepositoryImpl(
    private val recommendationDao: RecommendationDao,
    private val clock: Clock,
    private val dispatchers: AppDispatchers,
) : RecommendationRepository {

    override fun observeRecommendations(userId: String, mood: Mood, mode: ConversationMode): Flow<Recommendations> =
        combine(
            recommendationDao.observeRows(userId, mood.name, mode.name),
            recommendationDao.observeUpdatedAt(userId, mood.name, mode.name),
        ) { rows, updatedAt ->
            Recommendations(
                tracks = rows.map { it.toDomain() },
                updatedAt = updatedAt?.let(Instant::ofEpochMilli),
            )
        }

    /** As capas que já vieram junto (fonte "Pelo seu gosto") vão para o cache de capas. */
    override suspend fun replaceRecommendations(
        userId: String,
        mood: Mood,
        mode: ConversationMode,
        tracks: List<RecommendedTrack>,
        updatedAt: Instant,
    ): AppResult<Unit> = withContext(dispatchers.io) {
        val unique = tracks.distinctBy { it.track.key }
        safeDbCall {
            recommendationDao.replace(
                uid = userId,
                mood = mood.name,
                mode = mode.name,
                rows = unique.mapIndexed { index, track -> track.toEntity(userId, mood, mode, index) },
                meta = RecommendationMetaEntity(userId, mood.name, mode.name, updatedAt.toEpochMilli()),
                covers = unique.filter { it.coverChecked }
                    .map { TrackCoverEntity(it.track.key, it.coverUrl, updatedAt.toEpochMilli()) },
            )
        }
    }

    override suspend fun clearRecommendations(userId: String): AppResult<Unit> = withContext(dispatchers.io) {
        safeDbCall { recommendationDao.clearForUser(userId) }
    }

    override suspend fun isCoverChecked(trackKey: String): Boolean = withContext(dispatchers.io) {
        (safeDbCall { recommendationDao.isCoverChecked(trackKey) } as? AppResult.Success)?.data ?: false
    }

    override suspend fun saveCover(trackKey: String, coverUrl: String?): AppResult<Unit> =
        withContext(dispatchers.io) {
            safeDbCall { recommendationDao.upsertCover(TrackCoverEntity(trackKey, coverUrl, clock.millis())) }
        }
}

class SavedTrackRepositoryImpl(
    private val savedTrackDao: SavedTrackDao,
    private val dispatchers: AppDispatchers,
) : SavedTrackRepository {

    override fun observeSavedTracks(userId: String): Flow<List<SavedTrack>> =
        savedTrackDao.observe(userId).map { list -> list.map { it.toDomain() } }

    override suspend fun getSavedTrack(userId: String, trackKey: String): SavedTrack? = withContext(dispatchers.io) {
        (safeDbCall { savedTrackDao.get(userId, trackKey) } as? AppResult.Success)?.data?.toDomain()
    }

    override suspend fun insert(userId: String, savedTrack: SavedTrack): AppResult<Unit> =
        withContext(dispatchers.io) { safeDbCall { savedTrackDao.insert(savedTrack.toEntity(userId)) } }

    override suspend fun update(userId: String, savedTrack: SavedTrack): AppResult<Unit> =
        withContext(dispatchers.io) { safeDbCall { savedTrackDao.update(savedTrack.toEntity(userId)) } }

    override suspend fun delete(userId: String, trackKey: String): AppResult<Unit> =
        withContext(dispatchers.io) { safeDbCall { savedTrackDao.delete(userId, trackKey) } }
}
