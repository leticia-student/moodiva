package com.leticia.moodiva.di

import androidx.room.Room
import com.leticia.moodiva.BuildConfig
import com.leticia.moodiva.data.auth.PasswordHasher
import com.leticia.moodiva.data.auth.SessionStore
import com.leticia.moodiva.data.auth.SharedPrefsSessionStore
import com.leticia.moodiva.data.local.AppDatabase
import com.leticia.moodiva.data.remote.ApiKeyInterceptor
import com.leticia.moodiva.data.remote.LastFmApi
import com.leticia.moodiva.data.repository.AuthRepositoryImpl
import com.leticia.moodiva.data.repository.MusicRepositoryImpl
import com.leticia.moodiva.data.repository.ProfileRepositoryImpl
import com.leticia.moodiva.data.repository.RecommendationRepositoryImpl
import com.leticia.moodiva.data.repository.SavedTrackRepositoryImpl
import com.leticia.moodiva.domain.repository.AuthRepository
import com.leticia.moodiva.domain.repository.MusicRepository
import com.leticia.moodiva.domain.repository.ProfileRepository
import com.leticia.moodiva.domain.repository.RecommendationRepository
import com.leticia.moodiva.domain.repository.SavedTrackRepository
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/** API do Last.fm, banco Room, autenticação local e repositórios. */
val dataModule = module {

    single {
        Json {
            ignoreUnknownKeys = true // o Last.fm devolve muitos campos que o app não usa
            coerceInputValues = true // `null` em campo não nulo usa o valor padrão do DTO
        }
    }

    single {
        val builder = OkHttpClient.Builder()
            .addInterceptor(ApiKeyInterceptor(BuildConfig.LASTFM_API_KEY))
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)

        // Log só no build de depuração (método, URL e status). O Last.fm exige a chave na
        // URL, então o parâmetro `api_key` é ocultado no log ("██") e o log nunca liga no release.
        if (BuildConfig.DEBUG) {
            builder.addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                    redactQueryParams(ApiKeyInterceptor.PARAM_API_KEY)
                },
            )
        }
        builder.build()
    }

    single {
        Retrofit.Builder()
            .baseUrl(LastFmApi.BASE_URL)
            .client(get())
            .addConverterFactory(get<Json>().asConverterFactory("application/json".toMediaType()))
            .build()
    }
    single<LastFmApi> { get<Retrofit>().create(LastFmApi::class.java) }

    single {
        Room.databaseBuilder(androidContext(), AppDatabase::class.java, AppDatabase.NAME)
            // Versão 1: ainda não há migrações. Rede de segurança para builds de desenvolvimento.
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }
    single { get<AppDatabase>().userDao() }
    single { get<AppDatabase>().profileDao() }
    single { get<AppDatabase>().recommendationDao() }
    single { get<AppDatabase>().artistTagsDao() }
    single { get<AppDatabase>().savedTrackDao() }

    // Autenticação local
    single { PasswordHasher() }
    single<SessionStore> { SharedPrefsSessionStore(androidContext()) }

    // Repositórios, registrados pela interface do domain.
    single<AuthRepository> { AuthRepositoryImpl(get(), get(), get(), get(), get()) }
    single<ProfileRepository> { ProfileRepositoryImpl(get(), get(), get()) }
    single<MusicRepository> { MusicRepositoryImpl(get(), get(), get(), get()) }
    single<RecommendationRepository> { RecommendationRepositoryImpl(get(), get(), get()) }
    single<SavedTrackRepository> { SavedTrackRepositoryImpl(get(), get()) }
}
