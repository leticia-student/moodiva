package com.leticia.moodiva.presentation.screens.recommendations

import app.cash.turbine.test
import com.leticia.moodiva.R
import com.leticia.moodiva.domain.fake.FakeAuthRepository
import com.leticia.moodiva.domain.fake.FakeMusicRepository
import com.leticia.moodiva.domain.fake.FakeNetworkMonitor
import com.leticia.moodiva.domain.fake.FakeProfileRepository
import com.leticia.moodiva.domain.fake.FakeRecommendationRepository
import com.leticia.moodiva.domain.fake.FakeSavedTrackRepository
import com.leticia.moodiva.domain.fake.TestData
import com.leticia.moodiva.domain.model.ArtistTag
import com.leticia.moodiva.domain.model.ConversationMode
import com.leticia.moodiva.domain.model.ListenStatus
import com.leticia.moodiva.domain.model.Mood
import com.leticia.moodiva.domain.model.Recommendations
import com.leticia.moodiva.domain.model.SavedTrack
import com.leticia.moodiva.domain.model.TagTrack
import com.leticia.moodiva.domain.model.TrackDetails
import com.leticia.moodiva.domain.model.TrackSource
import com.leticia.moodiva.domain.usecase.GenerateRecommendationsUseCase
import com.leticia.moodiva.domain.usecase.LoadCoverUseCase
import com.leticia.moodiva.domain.usecase.ObserveRecommendationsUseCase
import com.leticia.moodiva.domain.usecase.ObserveSavedTracksUseCase
import com.leticia.moodiva.domain.usecase.RefreshRecommendationsUseCase
import com.leticia.moodiva.domain.usecase.SaveTrackUseCase
import com.leticia.moodiva.presentation.MainDispatcherRule
import com.leticia.moodiva.presentation.util.UpdatedAtText
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Duration

@OptIn(ExperimentalCoroutinesApi::class) // advanceUntilIdle
class RecommendationsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val auth = FakeAuthRepository()
    private val profiles = FakeProfileRepository().apply {
        profiles.value = mapOf(TestData.USER.id to TestData.profile())
    }
    private val recommendations = FakeRecommendationRepository()
    private val saved = FakeSavedTrackRepository()
    private val music = FakeMusicRepository()
    private val network = FakeNetworkMonitor(online = true)

    private fun createViewModel() = RecommendationsViewModel(
        mood = Mood.HAPPY,
        mode = ConversationMode.MATCH,
        observeRecommendations = ObserveRecommendationsUseCase(auth, recommendations, saved),
        observeSavedTracks = ObserveSavedTracksUseCase(auth, saved),
        refreshRecommendations = RefreshRecommendationsUseCase(
            auth, profiles, recommendations, GenerateRecommendationsUseCase(music), network, TestData.clock,
        ),
        loadCover = LoadCoverUseCase(recommendations, music, network),
        saveTrack = SaveTrackUseCase(auth, saved, TestData.clock),
        clock = TestData.clock,
    )

    private fun apiHasSongs(count: Int = 12) {
        music.tagTracks["happy"] = (1..count).map { TagTrack(TestData.track(it, "pop$it"), it) }
        (1..count).forEach { music.artistTagsByName["pop$it"] = listOf(ArtistTag("pop", 100)) }
    }

    private fun givenCache(age: Duration, vararg ids: Int) = recommendations.givenCache(
        TestData.USER.id, Mood.HAPPY, ConversationMode.MATCH,
        Recommendations(ids.map { TestData.recommended(it) }, TestData.NOW.minus(age)),
    )

    private fun RecommendationsContent.names() = (this as RecommendationsContent.Success).tracks.map { it.track.name }

    @Test
    fun `sem cache deve ir de loading para success com as musicas da api`() = runTest {
        apiHasSongs()
        createViewModel().uiState.test {
            assertEquals(RecommendationsContent.Loading, awaitItem().content)
            val content = awaitItem().content as RecommendationsContent.Success
            assertEquals(12, content.tracks.size)
            assertEquals(UpdatedAtText.JustNow, content.updatedAt)
        }
    }

    @Test
    fun `sem cache e sem internet deve mostrar vazio com tentar novamente`() = runTest {
        network.isOnline.value = false
        createViewModel().uiState.test {
            assertEquals(RecommendationsContent.Loading, awaitItem().content)
            assertEquals(
                RecommendationsContent.Empty(R.string.recommendations_empty_offline, canRetry = true),
                awaitItem().content,
            )
        }
    }

    @Test
    fun `tentar novamente com internet deve carregar as musicas`() = runTest {
        network.isOnline.value = false
        val viewModel = createViewModel()
        viewModel.uiState.test {
            skipItems(2) // Loading, Empty
            network.isOnline.value = true
            apiHasSongs()
            viewModel.onRetry()
            assertEquals(RecommendationsContent.Loading, awaitItem().content)
            assertEquals(12, (awaitItem().content as RecommendationsContent.Success).tracks.size)
        }
    }

    @Test
    fun `cache valido deve aparecer sem chamar a api`() = runTest {
        givenCache(Duration.ofHours(3), 1, 2)
        createViewModel().uiState.test {
            skipItems(1)
            val content = awaitItem().content as RecommendationsContent.Success
            assertEquals(UpdatedAtText.HoursAgo(3), content.updatedAt)
        }
        assertTrue(music.calls.isEmpty())
    }

    @Test
    fun `falha com cache deve manter as musicas e mostrar snackbar`() = runTest {
        givenCache(Duration.ofHours(30), 1, 2)
        network.isOnline.value = false
        val viewModel = createViewModel()

        viewModel.messages.test { assertEquals(R.string.error_network, awaitItem()) }
        viewModel.uiState.test {
            skipItems(1)
            assertEquals(listOf("Song 1", "Song 2"), awaitItem().content.names())
        }
    }

    @Test
    fun `api sem musicas para o perfil deve mostrar vazio sem tentar novamente`() = runTest {
        // A tag existe, mas nenhum artista combina com o gênero do perfil.
        music.tagTracks["happy"] = listOf(TagTrack(TestData.track(1, "rockstar"), 1))
        createViewModel().uiState.test {
            skipItems(1)
            assertEquals(RecommendationsContent.Empty(R.string.recommendations_empty, canRetry = false), awaitItem().content)
        }
    }

    @Test
    fun `pull to refresh deve buscar mesmo com cache valido`() = runTest {
        givenCache(Duration.ofMinutes(10), 1)
        apiHasSongs()
        val viewModel = createViewModel()
        viewModel.uiState.test {
            skipItems(1)
            assertEquals(listOf("Song 1"), awaitItem().content.names())

            viewModel.onRefresh()
            assertTrue(awaitItem().isRefreshing)
            advanceUntilIdle()
            val state = expectMostRecentItem()
            assertFalse(state.isRefreshing)
            assertEquals(12, (state.content as RecommendationsContent.Success).tracks.size)
        }
    }

    @Test
    fun `salvar deve marcar a musica e avisar`() = runTest {
        givenCache(Duration.ofHours(1), 1)
        val viewModel = createViewModel()
        viewModel.uiState.test {
            skipItems(1)
            val track = (awaitItem().content as RecommendationsContent.Success).tracks.first()
            viewModel.messages.test {
                viewModel.onSave(track)
                assertEquals(R.string.track_saved_message, awaitItem())
                viewModel.onSave(track)
                assertEquals(R.string.track_already_saved, awaitItem())
            }
            assertTrue(track.track.key in expectMostRecentItem().savedKeys)
        }
    }

    @Test
    fun `musica marcada como ouvida deve sumir da lista`() = runTest {
        givenCache(Duration.ofHours(1), 1, 2)
        val viewModel = createViewModel()
        viewModel.uiState.test {
            skipItems(1)
            assertEquals(2, (awaitItem().content as RecommendationsContent.Success).tracks.size)
            saved.given(
                TestData.USER.id,
                SavedTrack(TestData.track(2), null, TrackSource.MOOD, ListenStatus.LISTENED, null, TestData.NOW),
            )
            // A lista de salvas e as recomendações emitem separadamente: vale o estado final.
            advanceUntilIdle()
            assertEquals(listOf("Song 1"), expectMostRecentItem().content.names())
        }
    }

    @Test
    fun `card visivel deve buscar a capa uma vez`() = runTest {
        givenCache(Duration.ofHours(1), 1)
        val track = TestData.track(1)
        music.details[track.key] = TrackDetails(track, "https://capa.png", emptyList())
        val viewModel = createViewModel()
        viewModel.uiState.test {
            skipItems(2)
            viewModel.onTrackVisible(track)
            viewModel.onTrackVisible(track)
            val content = awaitItem().content as RecommendationsContent.Success
            assertEquals("https://capa.png", content.tracks.single().coverUrl)
        }
        assertEquals(1, music.calls.count { it.startsWith("info:") })
    }
}
