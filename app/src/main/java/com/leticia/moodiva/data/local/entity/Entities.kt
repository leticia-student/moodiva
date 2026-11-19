package com.leticia.moodiva.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

// Tabelas do banco `moodiva.db` (planejamento, seção 5). Datas em epoch millis.
// Toda tabela de dado pessoal tem `uid`: o isolamento por conta é garantido nas consultas (RNF08).

/** Conta local. A senha nunca é guardada: só o hash PBKDF2 e o sal (RNF14). */
@Entity(tableName = "users", indices = [Index(value = ["email"], unique = true)])
data class UserEntity(
    @PrimaryKey val uid: String,
    val email: String,
    @ColumnInfo(name = "password_hash") val passwordHash: String,
    @ColumnInfo(name = "password_salt") val passwordSalt: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

/** Perfil (TIPI + gostos) de uma conta. Listas e enums guardados como texto. */
@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey val uid: String,
    /** As 10 respostas, separadas por vírgula ("5,3,6,…"). */
    val answers: String,
    val extraversion: Double,
    val agreeableness: Double,
    val conscientiousness: Double,
    @ColumnInfo(name = "emotional_stability") val emotionalStability: Double,
    val openness: Double,
    /** Nomes do enum Genre, separados por vírgula. */
    val genres: String,
    @ColumnInfo(name = "favorite_artists") val favoriteArtists: List<String>,
    /** Nomes do enum Era, separados por vírgula (pode ser vazio). */
    val eras: String,
    val origin: String,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

/** Uma música recomendada para (conta, humor, modo), na posição da lista. */
@Entity(tableName = "recommendations", primaryKeys = ["uid", "mood", "mode", "track_key"])
data class RecommendationEntity(
    val uid: String,
    val mood: String,
    val mode: String,
    @ColumnInfo(name = "track_key") val trackKey: String,
    val position: Int,
    val name: String,
    val artist: String,
    @ColumnInfo(name = "duration_seconds") val durationSeconds: Int?,
    val source: String,
)

/** Quando as recomendações de (conta, humor, modo) foram buscadas (RN25). */
@Entity(tableName = "recommendation_meta", primaryKeys = ["uid", "mood", "mode"])
data class RecommendationMetaEntity(
    val uid: String,
    val mood: String,
    val mode: String,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

/** Recomendação + capa do cache de capas (LEFT JOIN). */
data class RecommendationRow(
    @Embedded val recommendation: RecommendationEntity,
    @ColumnInfo(name = "cover_url") val coverUrl: String?,
    @ColumnInfo(name = "cover_checked") val coverChecked: Boolean,
)

/** Tags brutas de um artista, sem prazo de validade (RN16). Público: não pertence a uma conta. */
@Entity(tableName = "artist_tags")
data class ArtistTagsEntity(
    /** Nome do artista normalizado (RN12). */
    @PrimaryKey @ColumnInfo(name = "artist_key") val artistKey: String,
    val tags: List<StoredTag>,
    @ColumnInfo(name = "fetched_at") val fetchedAt: Long,
)

@Serializable
data class StoredTag(val name: String, val count: Int)

/** Capa consultada de uma música; `cover_url` nulo = indisponível (RN27). Público. */
@Entity(tableName = "track_covers")
data class TrackCoverEntity(
    @PrimaryKey @ColumnInfo(name = "track_key") val trackKey: String,
    @ColumnInfo(name = "cover_url") val coverUrl: String?,
    @ColumnInfo(name = "fetched_at") val fetchedAt: Long,
)

/** Música salva por uma conta. A chave composta impede salvar duas vezes (RN29). */
@Entity(tableName = "saved_tracks", primaryKeys = ["uid", "track_key"])
data class SavedTrackEntity(
    val uid: String,
    @ColumnInfo(name = "track_key") val trackKey: String,
    val name: String,
    val artist: String,
    @ColumnInfo(name = "duration_seconds") val durationSeconds: Int?,
    @ColumnInfo(name = "cover_url") val coverUrl: String?,
    val source: String,
    val status: String,
    val rating: Int?,
    @ColumnInfo(name = "saved_at") val savedAt: Long,
)
