package com.leticia.moodiva.data.repository

import com.leticia.moodiva.core.dispatcher.AppDispatchers
import com.leticia.moodiva.core.result.AppError
import com.leticia.moodiva.core.result.AppResult
import com.leticia.moodiva.data.auth.PasswordHasher
import com.leticia.moodiva.data.auth.SessionStore
import com.leticia.moodiva.data.local.dao.ArtistTagsDao
import com.leticia.moodiva.data.local.dao.RecommendationDao
import com.leticia.moodiva.data.local.dao.UserDao
import com.leticia.moodiva.data.local.entity.ArtistTagsEntity
import com.leticia.moodiva.data.local.entity.StoredTag
import com.leticia.moodiva.data.local.entity.UserEntity
import com.leticia.moodiva.data.mapper.toDomain
import com.leticia.moodiva.data.mapper.toEntity
import com.leticia.moodiva.data.remote.LastFmApi
import com.leticia.moodiva.data.remote.dto.ArtistTopTagsResponse
import com.leticia.moodiva.data.remote.dto.TagDto
import com.leticia.moodiva.data.remote.dto.TagListDto
import com.leticia.moodiva.data.remote.dto.TagTopTracksResponse
import com.leticia.moodiva.domain.fake.TestData
import com.leticia.moodiva.domain.model.ArtistTag
import com.leticia.moodiva.domain.model.ConversationMode
import com.leticia.moodiva.domain.model.Mood
import com.leticia.moodiva.domain.model.RecommendedTrack
import com.leticia.moodiva.domain.model.SignInResult
import com.leticia.moodiva.domain.model.SignUpResult
import com.leticia.moodiva.domain.model.TrackSource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

private val dispatcher = StandardTestDispatcher()
private val dispatchers = AppDispatchers(io = dispatcher, default = dispatcher, main = dispatcher)
private fun runRepoTest(block: suspend TestScope.() -> Unit) = TestScope(dispatcher).runTest { block() }

class FakeSessionStore : SessionStore {
    override val userId = MutableStateFlow<String?>(null)
    override fun save(userId: String) { this.userId.value = userId }
    override fun clear() { userId.value = null }
}

class PasswordHasherTest {

    private val hasher = PasswordHasher(iterations = 1_000)

    @Test
    fun `senha correta deve ser aceita e errada recusada`() {
        val hashed = hasher.hash("segredo123")
        assertTrue(hasher.verify("segredo123", hashed.hash, hashed.salt))
        assertFalse(hasher.verify("segredo124", hashed.hash, hashed.salt))
    }

    @Test
    fun `mesma senha deve gerar hashes diferentes e nunca o texto puro`() {
        val a = hasher.hash("segredo123")
        val b = hasher.hash("segredo123")
        assertNotEquals(a.hash, b.hash)
        assertFalse(a.hash.contains("segredo"))
    }

    @Test
    fun `hash corrompido deve ser recusado sem excecao`() {
        assertFalse(hasher.verify("x", "zz", "00"))
    }
}

class AuthRepositoryImplTest {

    private val userDao = mockk<UserDao>(relaxUnitFun = true)
    private val session = FakeSessionStore()
    private val hasher = PasswordHasher(iterations = 1_000)
    private val repository = AuthRepositoryImpl(userDao, hasher, session, TestData.clock, dispatchers)

    @Test
    fun `cadastro deve guardar hash em vez da senha e abrir a sessao`() = runRepoTest {
        coEvery { userDao.findByEmail("ana@mail.com") } returns null
        val saved = slot<UserEntity>()
        coEvery { userDao.insert(capture(saved)) } returns Unit

        val result = repository.signUp("ana@mail.com", "123456")

        assertTrue(result is SignUpResult.Success)
        assertNotEquals("123456", saved.captured.passwordHash)
        assertTrue(hasher.verify("123456", saved.captured.passwordHash, saved.captured.passwordSalt))
        assertEquals(saved.captured.uid, session.userId.value)
    }

    @Test
    fun `email ja cadastrado deve ser recusado`() = runRepoTest {
        coEvery { userDao.findByEmail(any()) } returns UserEntity("u", "ana@mail.com", "h", "s", 0)
        assertEquals(SignUpResult.EmailAlreadyUsed, repository.signUp("ana@mail.com", "123456"))
        coVerify(exactly = 0) { userDao.insert(any()) }
    }

    @Test
    fun `login deve validar a senha pelo hash`() = runRepoTest {
        val hashed = hasher.hash("123456")
        val user = UserEntity("uid-9", "ana@mail.com", hashed.hash, hashed.salt, 0)
        coEvery { userDao.findByEmail("ana@mail.com") } returns user
        coEvery { userDao.findByEmail("nao@existe.com") } returns null

        assertEquals(SignInResult.InvalidCredentials, repository.signIn("ana@mail.com", "errada"))
        assertEquals(SignInResult.InvalidCredentials, repository.signIn("nao@existe.com", "123456"))
        assertNull(session.userId.value)

        assertTrue(repository.signIn("ana@mail.com", "123456") is SignInResult.Success)
        assertEquals("uid-9", session.userId.value)
    }

    @Test
    fun `sessao deve refletir o usuario do banco e sumir no logout`() = runRepoTest {
        every { userDao.observeById("uid-9") } returns flowOf(UserEntity("uid-9", "ana@mail.com", "h", "s", 0))
        session.save("uid-9")
        assertEquals("ana@mail.com", repository.session.first()?.email)

        repository.signOut()
        assertNull(repository.session.first())
    }

    @Test
    fun `falha do banco no cadastro deve virar erro`() = runRepoTest {
        coEvery { userDao.findByEmail(any()) } throws IllegalStateException("disco cheio")
        assertEquals(SignUpResult.Failure(AppError.Unknown), repository.signUp("ana@mail.com", "123456"))
    }
}

class MusicRepositoryImplTest {

    private val api = mockk<LastFmApi>()
    private val artistTagsDao = mockk<ArtistTagsDao>(relaxUnitFun = true)
    private val repository = MusicRepositoryImpl(api, artistTagsDao, TestData.clock, dispatchers)

    private fun tagsResponse(vararg tags: Pair<String, Int>) =
        ArtistTopTagsResponse(toptags = TagListDto(tags.map { TagDto(it.first, it.second.toString()) }))

    @Test
    fun `tags em cache nao devem chamar a api`() = runRepoTest {
        coEvery { artistTagsDao.get("caetano veloso") } returns
            ArtistTagsEntity("caetano veloso", listOf(StoredTag("mpb", 100)), 0)

        val result = repository.artistTags("Caetano Veloso")

        assertEquals(AppResult.Success(listOf(ArtistTag("mpb", 100))), result)
        coVerify(exactly = 0) { api.artistTopTags(any(), any(), any()) }
    }

    @Test
    fun `tags fora do cache devem ser buscadas e gravadas sem validade`() = runRepoTest {
        coEvery { artistTagsDao.get(any()) } returns null
        coEvery { api.artistTopTags("Gal Costa", any(), any()) } returns tagsResponse("mpb" to 100)
        val stored = slot<ArtistTagsEntity>()
        coEvery { artistTagsDao.upsert(capture(stored)) } returns Unit

        repository.artistTags("Gal Costa")

        assertEquals("gal costa", stored.captured.artistKey)
        assertEquals(listOf(StoredTag("mpb", 100)), stored.captured.tags)
    }

    @Test
    fun `artista nao encontrado deve virar lista vazia`() = runRepoTest {
        coEvery { artistTagsDao.get(any()) } returns null
        coEvery { api.artistTopTags(any(), any(), any()) } returns ArtistTopTagsResponse(error = 6, message = "not found")

        assertEquals(AppResult.Success(emptyList<ArtistTag>()), repository.artistTags("Ninguém"))
    }

    @Test
    fun `erros do last fm devem virar AppError`() = runRepoTest {
        coEvery { api.tagTopTracks("happy", any(), any(), any()) } returns TagTopTracksResponse(error = 29)
        coEvery { api.tagTopTracks("sad", any(), any(), any()) } returns TagTopTracksResponse(error = 10)
        coEvery { api.tagTopTracks("calm", any(), any(), any()) } throws IOException()
        coEvery { api.tagTopTracks("love", any(), any(), any()) } throws
            HttpException(Response.error<Any>(403, "".toResponseBody()))

        assertEquals(AppResult.Failure(AppError.RateLimited), repository.tagTopTracks("happy", 30))
        assertEquals(AppResult.Failure(AppError.InvalidApiKey), repository.tagTopTracks("sad", 30))
        assertEquals(AppResult.Failure(AppError.Network), repository.tagTopTracks("calm", 30))
        assertEquals(AppResult.Failure(AppError.InvalidApiKey), repository.tagTopTracks("love", 30))
    }
}

class RecommendationRepositoryImplTest {

    private val dao = mockk<RecommendationDao>(relaxUnitFun = true)
    private val repository = RecommendationRepositoryImpl(dao, TestData.clock, dispatchers)

    @Test
    fun `substituir deve gravar posicoes sem duplicadas e as capas ja consultadas`() = runRepoTest {
        val tracks = listOf(
            TestData.recommended(1),
            TestData.recommended(1), // repetida
            RecommendedTrack(TestData.track(2), TrackSource.TASTE, coverUrl = "https://c.png", coverChecked = true),
        )
        coEvery { dao.replace(any(), any(), any(), any(), any(), any()) } returns Unit

        repository.replaceRecommendations("uid", Mood.SAD, ConversationMode.SHIFT, tracks, TestData.NOW)

        coVerify {
            dao.replace(
                uid = "uid",
                mood = "SAD",
                mode = "SHIFT",
                rows = match { rows -> rows.map { it.position } == listOf(0, 1) && rows[1].source == "TASTE" },
                meta = match { it.updatedAt == TestData.NOW.toEpochMilli() },
                covers = match { it.single().coverUrl == "https://c.png" },
            )
        }
    }

    @Test
    fun `falha do banco ao substituir deve virar erro sem excecao`() = runRepoTest {
        coEvery { dao.replace(any(), any(), any(), any(), any(), any()) } throws IllegalStateException()
        val result = repository.replaceRecommendations("uid", Mood.HAPPY, ConversationMode.MATCH, emptyList(), TestData.NOW)
        assertEquals(AppResult.Failure(AppError.Unknown), result)
    }
}

class SafeCallsAndMappersTest {

    @Test(expected = CancellationException::class)
    fun `cancelamento deve ser relancado pelo safeApiCall`() = runRepoTest {
        safeApiCall<Unit> { throw CancellationException() }
    }

    @Test(expected = CancellationException::class)
    fun `cancelamento deve ser relancado pelo safeDbCall`() = runRepoTest {
        safeDbCall<Unit> { throw CancellationException() }
    }

    @Test
    fun `perfil deve ida e volta pela entidade sem perder dados`() {
        val profile = TestData.profile(favorites = listOf("Caetano Veloso", "Gal Costa"))
        val back = profile.toEntity("uid", TestData.NOW).toDomain()
        assertEquals(profile, back)
    }
}
