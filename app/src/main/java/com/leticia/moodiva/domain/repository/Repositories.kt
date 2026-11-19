package com.leticia.moodiva.domain.repository

import com.leticia.moodiva.core.result.AppResult
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
import kotlinx.coroutines.flow.Flow
import java.time.Instant

// Contratos do domain. A camada data os implementa (inversão de dependência):
// quem usa um repositório não sabe que existe Room, Retrofit ou SharedPreferences.
// Convenção: observe* → Flow do banco; ações → suspend + AppResult.

/** Contas locais e sessão (RF01–RF03). As credenciais já chegam validadas pelo caso de uso. */
interface AuthRepository {

    /** Usuário logado, ou `null`. Emite de novo a cada login/logout. */
    val session: Flow<User?>

    suspend fun signUp(email: String, password: String): SignUpResult

    suspend fun signIn(email: String, password: String): SignInResult

    suspend fun signOut()
}

/** Perfil (personalidade + gostos) de cada conta, só no aparelho (RNF09). */
interface ProfileRepository {

    /** `null` enquanto a conta não concluiu o onboarding (RN33). */
    fun observeProfile(userId: String): Flow<UserProfile?>

    suspend fun saveProfile(userId: String, profile: UserProfile): AppResult<Unit>
}

/**
 * Acesso ao catálogo do Last.fm. "Não encontrado" (erro 6) não é falha:
 * vira lista vazia ou `null`, conforme o método.
 */
interface MusicRepository {

    suspend fun tagTopTracks(tag: String, limit: Int): AppResult<List<TagTrack>>

    /** Tags brutas do artista. Usa o cache sem validade antes da rede (RN16). */
    suspend fun artistTags(artist: String): AppResult<List<ArtistTag>>

    suspend fun similarArtists(artist: String, limit: Int): AppResult<List<SimilarArtist>>

    suspend fun artistTopTracks(artist: String, limit: Int): AppResult<List<Track>>

    /** `null` se a música não existe no Last.fm. */
    suspend fun trackDetails(track: Track): AppResult<TrackDetails?>
}

/** Cache de recomendações por (conta, humor, modo) e cache público de capas. */
interface RecommendationRepository {

    fun observeRecommendations(userId: String, mood: Mood, mode: ConversationMode): Flow<Recommendations>

    /** Substitui, numa transação, as recomendações de (conta, humor, modo) (seção 5). */
    suspend fun replaceRecommendations(
        userId: String,
        mood: Mood,
        mode: ConversationMode,
        tracks: List<RecommendedTrack>,
        updatedAt: Instant,
    ): AppResult<Unit>

    /** Descarta todo o cache de recomendações da conta (RN26). */
    suspend fun clearRecommendations(userId: String): AppResult<Unit>

    /** A capa desta música já foi consultada (com ou sem sucesso)? */
    suspend fun isCoverChecked(trackKey: String): Boolean

    /** Grava a capa consultada; `null` = indisponível (RN27). */
    suspend fun saveCover(trackKey: String, coverUrl: String?): AppResult<Unit>
}

/** Músicas salvas de cada conta (RF18–RF21, RF26). */
interface SavedTrackRepository {

    fun observeSavedTracks(userId: String): Flow<List<SavedTrack>>

    suspend fun getSavedTrack(userId: String, trackKey: String): SavedTrack?

    suspend fun insert(userId: String, savedTrack: SavedTrack): AppResult<Unit>

    suspend fun update(userId: String, savedTrack: SavedTrack): AppResult<Unit>

    suspend fun delete(userId: String, trackKey: String): AppResult<Unit>
}
