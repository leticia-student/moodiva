package com.leticia.moodiva.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import com.leticia.moodiva.data.local.entity.ArtistTagsEntity
import com.leticia.moodiva.data.local.entity.ProfileEntity
import com.leticia.moodiva.data.local.entity.RecommendationEntity
import com.leticia.moodiva.data.local.entity.RecommendationMetaEntity
import com.leticia.moodiva.data.local.entity.RecommendationRow
import com.leticia.moodiva.data.local.entity.SavedTrackEntity
import com.leticia.moodiva.data.local.entity.TrackCoverEntity
import com.leticia.moodiva.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    /** `ABORT`: e-mail repetido viola o índice único e lança exceção (nunca sobrescreve). */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(user: UserEntity)

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun findByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE uid = :uid LIMIT 1")
    fun observeById(uid: String): Flow<UserEntity?>
}

@Dao
interface ProfileDao {

    @Query("SELECT * FROM profiles WHERE uid = :uid LIMIT 1")
    fun observe(uid: String): Flow<ProfileEntity?>

    @Upsert
    suspend fun upsert(profile: ProfileEntity)
}

@Dao
abstract class RecommendationDao {

    @Query(
        """
        SELECT r.*, c.cover_url AS cover_url, (c.track_key IS NOT NULL) AS cover_checked
        FROM recommendations r
        LEFT JOIN track_covers c ON c.track_key = r.track_key
        WHERE r.uid = :uid AND r.mood = :mood AND r.mode = :mode
        ORDER BY r.position
        """,
    )
    abstract fun observeRows(uid: String, mood: String, mode: String): Flow<List<RecommendationRow>>

    @Query("SELECT updated_at FROM recommendation_meta WHERE uid = :uid AND mood = :mood AND mode = :mode")
    abstract fun observeUpdatedAt(uid: String, mood: String, mode: String): Flow<Long?>

    /**
     * Substitui as recomendações de (conta, humor, modo) numa única transação:
     * a tela nunca vê a lista pela metade e, se algo falhar, o cache anterior fica intacto.
     */
    @Transaction
    open suspend fun replace(
        uid: String,
        mood: String,
        mode: String,
        rows: List<RecommendationEntity>,
        meta: RecommendationMetaEntity,
        covers: List<TrackCoverEntity>,
    ) {
        deleteRecommendations(uid, mood, mode)
        insertRecommendations(rows)
        upsertMeta(meta)
        upsertCovers(covers)
    }

    /** Descarta todo o cache de recomendações de uma conta (RN26). */
    @Transaction
    open suspend fun clearForUser(uid: String) {
        deleteAllRecommendations(uid)
        deleteAllMeta(uid)
    }

    @Query("SELECT EXISTS(SELECT 1 FROM track_covers WHERE track_key = :trackKey)")
    abstract suspend fun isCoverChecked(trackKey: String): Boolean

    @Upsert
    abstract suspend fun upsertCover(cover: TrackCoverEntity)

    @Upsert
    protected abstract suspend fun upsertCovers(covers: List<TrackCoverEntity>)

    @Query("DELETE FROM recommendations WHERE uid = :uid AND mood = :mood AND mode = :mode")
    protected abstract suspend fun deleteRecommendations(uid: String, mood: String, mode: String)

    @Insert
    protected abstract suspend fun insertRecommendations(rows: List<RecommendationEntity>)

    @Upsert
    protected abstract suspend fun upsertMeta(meta: RecommendationMetaEntity)

    @Query("DELETE FROM recommendations WHERE uid = :uid")
    protected abstract suspend fun deleteAllRecommendations(uid: String)

    @Query("DELETE FROM recommendation_meta WHERE uid = :uid")
    protected abstract suspend fun deleteAllMeta(uid: String)
}

@Dao
interface ArtistTagsDao {

    @Query("SELECT * FROM artist_tags WHERE artist_key = :artistKey LIMIT 1")
    suspend fun get(artistKey: String): ArtistTagsEntity?

    @Upsert
    suspend fun upsert(entity: ArtistTagsEntity)
}

@Dao
interface SavedTrackDao {

    @Query("SELECT * FROM saved_tracks WHERE uid = :uid ORDER BY saved_at DESC")
    fun observe(uid: String): Flow<List<SavedTrackEntity>>

    @Query("SELECT * FROM saved_tracks WHERE uid = :uid AND track_key = :trackKey LIMIT 1")
    suspend fun get(uid: String, trackKey: String): SavedTrackEntity?

    /** `ABORT`: a chave composta (uid, música) impede salvar duas vezes (RN29). */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: SavedTrackEntity)

    @Update
    suspend fun update(entity: SavedTrackEntity)

    @Query("DELETE FROM saved_tracks WHERE uid = :uid AND track_key = :trackKey")
    suspend fun delete(uid: String, trackKey: String)
}
