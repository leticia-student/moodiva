package com.leticia.moodiva.domain.usecase

import app.cash.turbine.test
import com.leticia.moodiva.core.result.AppError
import com.leticia.moodiva.core.result.AppResult
import com.leticia.moodiva.domain.fake.FakeAuthRepository
import com.leticia.moodiva.domain.fake.FakeMusicRepository
import com.leticia.moodiva.domain.fake.FakeNetworkMonitor
import com.leticia.moodiva.domain.fake.FakeProfileRepository
import com.leticia.moodiva.domain.fake.FakeRecommendationRepository
import com.leticia.moodiva.domain.fake.FakeSavedTrackRepository
import com.leticia.moodiva.domain.fake.TestData
import com.leticia.moodiva.domain.model.ArtistTag
import com.leticia.moodiva.domain.model.ConversationMode
import com.leticia.moodiva.domain.model.CredentialProblem
import com.leticia.moodiva.domain.model.ListenStatus
import com.leticia.moodiva.domain.model.Mood
import com.leticia.moodiva.domain.model.Recommendations
import com.leticia.moodiva.domain.model.SignInResult
import com.leticia.moodiva.domain.model.SignUpResult
import com.leticia.moodiva.domain.model.TagTrack
import com.leticia.moodiva.domain.model.TrackDetails
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration

class AccountUseCasesTest {

    private val auth = FakeAuthRepository(initialUser = null)

    @Test
    fun `cadastro com campos invalidos nao deve chegar ao repositorio`() = runTest {
        val result = SignUpUseCase(auth)("ana", "123", "123")
        assertEquals(
            SignUpResult.Invalid(setOf(CredentialProblem.EMAIL_INVALID, CredentialProblem.PASSWORD_TOO_SHORT)),
            result,
        )
        assertNull(auth.session.value)
    }

    @Test
    fun `cadastro valido deve logar e email repetido deve ser recusado`() = runTest {
        val first = SignUpUseCase(auth)(" Ana@Mail.com ", "123456", "123456")
        assertTrue(first is SignUpResult.Success)
        assertEquals("ana@mail.com", auth.session.value?.email)

        assertEquals(SignUpResult.EmailAlreadyUsed, SignUpUseCase(auth)("ana@mail.com", "654321", "654321"))
    }

    @Test
    fun `login com senha errada deve ser recusado e logout deve limpar a sessao`() = runTest {
        SignUpUseCase(auth)("ana@mail.com", "123456", "123456")
        SignOutUseCase(auth)()
        assertNull(auth.session.value)

        assertEquals(SignInResult.InvalidCredentials, SignInUseCase(auth)("ana@mail.com", "errada"))
        assertTrue(SignInUseCase(auth)("ANA@mail.com", "123456") is SignInResult.Success)
    }
}

class ProfileUseCasesTest {

    private val auth = FakeAuthRepository()
    private val profiles = FakeProfileRepository()
    private val recommendations = FakeRecommendationRepository()

    @Test
    fun `salvar o perfil deve descartar o cache de recomendacoes`() = runTest {
        recommendations.givenCache(TestData.USER.id, Mood.HAPPY, ConversationMode.MATCH, Recommendations(listOf(TestData.recommended(1)), TestData.NOW))

        val result = SaveProfileUseCase(auth, profiles, recommendations)(TestData.profile())

        assertEquals(AppResult.Success(Unit), result)
        assertEquals(1, recommendations.clearCalls)
        val cached = recommendations.observeRecommendations(TestData.USER.id, Mood.HAPPY, ConversationMode.MATCH).first()
        assertTrue(cached.tracks.isEmpty())
    }

    @Test
    fun `perfil deve ser isolado por conta`() = runTest {
        SaveProfileUseCase(auth, profiles, recommendations)(TestData.profile())
        val observe = ObserveProfileUseCase(auth, profiles)

        observe().test {
            assertNotNull(awaitItem())
            auth.session.value = TestData.USER.copy(id = "outra-conta")
            assertNull(awaitItem())
        }
    }

    @Test
    fun `salvar sem sessao deve falhar`() = runTest {
        auth.session.value = null
        assertEquals(
            AppResult.Failure(AppError.NotSignedIn),
            SaveProfileUseCase(auth, profiles, recommendations)(TestData.profile()),
        )
    }
}

class RecommendationUseCasesTest {

    private val auth = FakeAuthRepository()
    private val profiles = FakeProfileRepository()
    private val recommendations = FakeRecommendationRepository()
    private val saved = FakeSavedTrackRepository()
    private val music = FakeMusicRepository()
    private val network = FakeNetworkMonitor(online = true)

    private val refresh = RefreshRecommendationsUseCase(
        auth, profiles, recommendations, GenerateRecommendationsUseCase(music), network, TestData.clock,
    )
    private val observe = ObserveRecommendationsUseCase(auth, recommendations, saved)
    private val uid = TestData.USER.id

    private fun givenProfileAndApi() {
        profiles.profiles.value = mapOf(uid to TestData.profile())
        music.tagTracks["happy"] = (1..12).map { TagTrack(TestData.track(it, "pop$it"), it) }
        (1..12).forEach { music.artistTagsByName["pop$it"] = listOf(ArtistTag("pop", 100)) }
    }

    private fun givenCache(age: Duration) = recommendations.givenCache(
        uid, Mood.HAPPY, ConversationMode.MATCH,
        Recommendations(listOf(TestData.recommended(1)), TestData.NOW.minus(age)),
    )

    @Test
    fun `cache valido nao deve chamar a api`() = runTest {
        givenProfileAndApi()
        givenCache(Duration.ofHours(23))

        assertEquals(AppResult.Success(Unit), refresh(Mood.HAPPY, ConversationMode.MATCH))
        assertEquals(0, recommendations.replaceCalls)
        assertTrue(music.calls.isEmpty())
    }

    @Test
    fun `cache expirado e online deve buscar e substituir o cache`() = runTest {
        givenProfileAndApi()
        givenCache(Duration.ofHours(24))

        refresh(Mood.HAPPY, ConversationMode.MATCH)

        val cached = observe(Mood.HAPPY, ConversationMode.MATCH).first()
        assertEquals(12, cached.tracks.size)
        assertEquals(TestData.NOW, cached.updatedAt)
    }

    @Test
    fun `sem conexao deve falhar sem apagar o cache`() = runTest {
        givenProfileAndApi()
        givenCache(Duration.ofHours(30))
        network.isOnline.value = false

        assertEquals(AppResult.Failure(AppError.Network), refresh(Mood.HAPPY, ConversationMode.MATCH))
        assertEquals(1, observe(Mood.HAPPY, ConversationMode.MATCH).first().tracks.size)
    }

    @Test
    fun `pull to refresh deve buscar mesmo com cache valido`() = runTest {
        givenProfileAndApi()
        givenCache(Duration.ofMinutes(5))

        refresh(Mood.HAPPY, ConversationMode.MATCH, force = true)

        assertEquals(1, recommendations.replaceCalls)
    }

    @Test
    fun `humor que nao pergunta o modo deve usar o cache de acompanhar`() = runTest {
        givenProfileAndApi()

        refresh(Mood.HAPPY, ConversationMode.SHIFT)

        assertEquals(12, observe(Mood.HAPPY, ConversationMode.MATCH).first().tracks.size)
    }

    @Test
    fun `musica marcada como ouvida deve sumir das recomendacoes`() = runTest {
        givenCache(Duration.ofHours(1))

        observe(Mood.HAPPY, ConversationMode.MATCH).test {
            assertEquals(1, awaitItem().tracks.size)
            SaveTrackUseCase(auth, saved, TestData.clock)(TestData.recommended(1))
            skipItems(1) // salva como "quero ouvir": continua na lista
            MarkAsListenedUseCase(auth, saved)(TestData.track(1).key)
            assertTrue(awaitItem().tracks.isEmpty())
        }
    }

    @Test
    fun `capa deve ser buscada uma vez e marcada como indisponivel quando faltar`() = runTest {
        val loadCover = LoadCoverUseCase(recommendations, music, network)
        val withCover = TestData.track(1)
        music.details[withCover.key] = TrackDetails(withCover, "https://capa.png", emptyList())

        loadCover(withCover)
        loadCover(withCover)
        loadCover(TestData.track(2)) // não existe no Last.fm

        assertEquals(1, music.calls.count { it == "info:${withCover.key}" })
        assertTrue(recommendations.isCoverChecked(TestData.track(2).key))
    }

    @Test
    fun `capa sem conexao nao deve ser marcada como consultada`() = runTest {
        network.isOnline.value = false
        val result = LoadCoverUseCase(recommendations, music, network)(TestData.track(1))

        assertEquals(AppResult.Failure(AppError.Network), result)
        assertTrue(!recommendations.isCoverChecked(TestData.track(1).key))
    }
}

class SavedTrackUseCasesTest {

    private val auth = FakeAuthRepository()
    private val saved = FakeSavedTrackRepository()
    private val save = SaveTrackUseCase(auth, saved, TestData.clock)
    private val rate = RateTrackUseCase(auth, saved)
    private val key = TestData.track(1).key

    @Test
    fun `musica ja salva nao deve ser duplicada`() = runTest {
        assertEquals(SaveTrackResult.Saved, save(TestData.recommended(1)))
        assertEquals(SaveTrackResult.AlreadySaved, save(TestData.recommended(1)))
        assertEquals(1, ObserveSavedTracksUseCase(auth, saved)().first().size)
    }

    @Test
    fun `musica salva deve comecar como quero ouvir`() = runTest {
        save(TestData.recommended(1))
        val track = saved.getSavedTrack(TestData.USER.id, key)!!
        assertEquals(ListenStatus.WANT_TO_LISTEN, track.status)
        assertNull(track.rating)
        assertEquals(TestData.NOW, track.savedAt)
    }

    @Test
    fun `musica nao ouvida nao pode ser avaliada`() = runTest {
        save(TestData.recommended(1))
        assertEquals(RateTrackResult.NotListened, rate(key, 4))
    }

    @Test
    fun `musica ouvida deve aceitar nota de um a cinco`() = runTest {
        save(TestData.recommended(1))
        MarkAsListenedUseCase(auth, saved)(key)

        assertEquals(RateTrackResult.InvalidRating, rate(key, 6))
        assertEquals(RateTrackResult.Rated, rate(key, 4))
        assertEquals(4, saved.getSavedTrack(TestData.USER.id, key)?.rating)
    }

    @Test
    fun `remover deve tirar a musica da lista`() = runTest {
        save(TestData.recommended(1))
        RemoveSavedTrackUseCase(auth, saved)(key)
        assertTrue(ObserveSavedTracksUseCase(auth, saved)().first().isEmpty())
    }

    @Test
    fun `musicas salvas devem ser isoladas por conta`() = runTest {
        save(TestData.recommended(1))
        auth.session.value = TestData.USER.copy(id = "outra")
        assertTrue(ObserveSavedTracksUseCase(auth, saved)().first().isEmpty())
    }
}
