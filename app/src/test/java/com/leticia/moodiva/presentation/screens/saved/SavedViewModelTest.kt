package com.leticia.moodiva.presentation.screens.saved

import app.cash.turbine.test
import com.leticia.moodiva.R
import com.leticia.moodiva.domain.fake.FakeAuthRepository
import com.leticia.moodiva.domain.fake.FakeSavedTrackRepository
import com.leticia.moodiva.domain.fake.TestData
import com.leticia.moodiva.domain.model.ListenStatus
import com.leticia.moodiva.domain.model.SavedTrack
import com.leticia.moodiva.domain.model.TrackSource
import com.leticia.moodiva.domain.usecase.MarkAsListenedUseCase
import com.leticia.moodiva.domain.usecase.ObserveSavedTracksUseCase
import com.leticia.moodiva.domain.usecase.RateTrackUseCase
import com.leticia.moodiva.domain.usecase.RemoveSavedTrackUseCase
import com.leticia.moodiva.presentation.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class) // advanceUntilIdle
class SavedViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val auth = FakeAuthRepository()
    private val repository = FakeSavedTrackRepository()
    private val uid = TestData.USER.id

    private fun saved(n: Int, status: ListenStatus = ListenStatus.WANT_TO_LISTEN, minute: Long = n.toLong()) =
        SavedTrack(TestData.track(n), null, TrackSource.MOOD, status, null, TestData.NOW.plusSeconds(minute * 60))

    private fun createViewModel() = SavedViewModel(
        ObserveSavedTracksUseCase(auth, repository),
        MarkAsListenedUseCase(auth, repository),
        RateTrackUseCase(auth, repository),
        RemoveSavedTrackUseCase(auth, repository),
    )

    private fun SavedContent.names() = (this as SavedContent.Success).tracks.map { it.track.name }

    @Test
    fun `lista vazia deve oferecer conversar com a moodiva`() = runTest {
        createViewModel().uiState.test {
            assertEquals(SavedContent.Loading, awaitItem().content)
            assertEquals(SavedContent.Empty(R.string.saved_empty, showChatAction = true), awaitItem().content)
        }
    }

    @Test
    fun `deve listar da mais recente para a mais antiga e filtrar por status`() = runTest {
        repository.given(uid, saved(1))
        repository.given(uid, saved(2, ListenStatus.LISTENED))
        val viewModel = createViewModel()

        viewModel.uiState.test {
            skipItems(1)
            assertEquals(listOf("Song 2", "Song 1"), awaitItem().content.names())

            viewModel.onFilter(SavedFilter.LISTENED)
            assertEquals(listOf("Song 2"), awaitItem().content.names())

            viewModel.onFilter(SavedFilter.WANT_TO_LISTEN)
            assertEquals(listOf("Song 1"), awaitItem().content.names())
        }
    }

    @Test
    fun `filtro sem resultados nao deve oferecer a conversa`() = runTest {
        repository.given(uid, saved(1))
        val viewModel = createViewModel()
        viewModel.uiState.test {
            skipItems(2)
            viewModel.onFilter(SavedFilter.LISTENED)
            assertEquals(SavedContent.Empty(R.string.saved_empty_filter_listened, showChatAction = false), awaitItem().content)
        }
    }

    @Test
    fun `ja ouvi e avaliar devem atualizar a musica`() = runTest {
        repository.given(uid, saved(1))
        val viewModel = createViewModel()
        val track = saved(1)

        viewModel.onMarkListened(track)
        advanceUntilIdle()
        viewModel.onRate(track, 5)
        advanceUntilIdle()

        val updated = repository.getSavedTrack(uid, track.track.key)!!
        assertEquals(ListenStatus.LISTENED, updated.status)
        assertEquals(5, updated.rating)
    }

    @Test
    fun `remover so deve acontecer depois de confirmar`() = runTest {
        repository.given(uid, saved(1))
        val viewModel = createViewModel()
        viewModel.uiState.test {
            skipItems(2)
            viewModel.onRequestRemove(saved(1))
            assertEquals("Song 1", awaitItem().pendingRemoval?.track?.name)

            viewModel.onDismissRemove()
            assertNull(awaitItem().pendingRemoval)
            assertEquals("Song 1", repository.getSavedTrack(uid, saved(1).track.key)?.track?.name)

            viewModel.onRequestRemove(saved(1))
            skipItems(1)
            viewModel.onConfirmRemove()
            advanceUntilIdle()
            assertEquals(SavedContent.Empty(R.string.saved_empty, showChatAction = true), expectMostRecentItem().content)
        }
    }
}
