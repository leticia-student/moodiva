package com.leticia.moodiva.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.leticia.moodiva.data.local.dao.ArtistTagsDao
import com.leticia.moodiva.data.local.dao.ProfileDao
import com.leticia.moodiva.data.local.dao.RecommendationDao
import com.leticia.moodiva.data.local.dao.SavedTrackDao
import com.leticia.moodiva.data.local.dao.UserDao
import com.leticia.moodiva.data.local.entity.ArtistTagsEntity
import com.leticia.moodiva.data.local.entity.ProfileEntity
import com.leticia.moodiva.data.local.entity.RecommendationEntity
import com.leticia.moodiva.data.local.entity.RecommendationMetaEntity
import com.leticia.moodiva.data.local.entity.SavedTrackEntity
import com.leticia.moodiva.data.local.entity.StoredTag
import com.leticia.moodiva.data.local.entity.TrackCoverEntity
import com.leticia.moodiva.data.local.entity.UserEntity
import kotlinx.serialization.json.Json

/** Banco local: a única fonte de verdade do app (RNF01; planejamento, seção 5). */
@Database(
    entities = [
        UserEntity::class,
        ProfileEntity::class,
        RecommendationEntity::class,
        RecommendationMetaEntity::class,
        ArtistTagsEntity::class,
        TrackCoverEntity::class,
        SavedTrackEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun profileDao(): ProfileDao
    abstract fun recommendationDao(): RecommendationDao
    abstract fun artistTagsDao(): ArtistTagsDao
    abstract fun savedTrackDao(): SavedTrackDao

    companion object {
        const val NAME = "moodiva.db"
    }
}

/** Listas guardadas como JSON numa coluna de texto (sem tabela extra). */
class Converters {

    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun stringsToJson(value: List<String>): String = json.encodeToString(value)

    @TypeConverter
    fun jsonToStrings(value: String): List<String> = json.decodeFromString(value)

    @TypeConverter
    fun tagsToJson(value: List<StoredTag>): String = json.encodeToString(value)

    @TypeConverter
    fun jsonToTags(value: String): List<StoredTag> = json.decodeFromString(value)
}
