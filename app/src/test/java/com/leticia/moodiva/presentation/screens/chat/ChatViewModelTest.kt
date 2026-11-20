package com.leticia.moodiva.presentation.screens.chat

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
import com.leticia.moodiva.domain.model.ConversationAnswer
import com.leticia.moodiva.domain.model.ConversationMode
import com.leticia.moodiva.domain.model.Mood
import com.leticia.moodiva.domain.model.Recommendations
import com.leticia.moodiva.domain.model.TagTrack
import com.leticia.moodiva.domain.usecase.GenerateRecommendationsUseCase
import com.leticia.moodiva.domain.usecase.ObserveRecommendationsUseCase
import com.leticia.moodiva.domain.usecase.RefreshRecommendationsUseCase
import com.leticia.moodiva.presentation.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Duration

@OptIn(ExperimentalCoroutinesApi::class) // advanceUntilIdle
class ChatViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val auth = FakeAuthRepository()
    private val profiles = FakeProfileRepository().apply {
        profiles.value = mapOf(TestData.USER.id to TestData.profile())
    }
    private val recommendations = FakeRecommendationRepository()
    private val music = FakeMusicRepository()
    private val network = FakeNetworkMonitor(online = true)

    private val viewModel = ChatViewModel(
        RefreshRecommendationsUseCase(auth, profiles, recommendations, GenerateRecommendationsUseCase(music), network, TestData.clock),
        ObserveRecommendationsUseCase(auth, recommendations, FakeSavedTrackRepository()),
    )

    private fun texts() = viewModel.uiState.value.messages.filterIsInstance<ChatMessage.Text>().map { it.textRes }

    private fun choose(answer: ConversationAnswer) {
        val option = viewModel.uiState.value.options.filterIsInstance<ChatOption.Answer>().first { it.answer == answer }
        viewModel.onOption(option)
    }

    private fun apiHasSongs() {
        music.tagTracks["happy"] = (1..12).map { TagTrack(TestData.track(it, "pop$it"), it) }
        (1..12).forEach { music.artistTagsByName["pop$it"] = listOf(ArtistTag("pop", 100)) }
    }

    @Test
    fun `conversa deve comecar pela saudacao com uma resposta`() {
        assertEquals(listOf(R.string.chat_greeting), texts())
        assertEquals(1, viewModel.uiState.value.options.size)
    }

    @Test
    fun `feliz deve ir direto as recomendacoes sem perguntar o modo`() = runTest {
        apiHasSongs()
        viewModel.showRecommendations.test {
            choose(ConversationAnswer.Start)
            assertEquals(8, viewModel.uiState.value.options.size) // oito humores
            choose(ConversationAnswer.ChooseMood(Mood.HAPPY))

            assertEquals(Mood.HAPPY to ConversationMode.MATCH, awaitItem())
        }
        assertEquals(R.string.chat_ready, texts().last())
        assertTrue(viewModel.uiState.value.options.contains(ChatOption.SeeSongs))
    }

    @Test
    fun `triste deve perguntar o modo antes das recomendacoes`() {
        choose(ConversationAnswer.Start)
        choose(ConversationAnswer.ChooseMood(Mood.SAD))

        assertEquals(R.string.chat_ask_mode, texts().last())
        assertEquals(2, viewModel.uiState.value.options.size)
    }

    @Test
    fun `ansioso deve avisar que as musicas serao calmas`() {
        choose(ConversationAnswer.Start)
        choose(ConversationAnswer.ChooseMood(Mood.ANXIOUS))
        assertTrue(R.string.chat_anxious_note in texts())
    }

    @Test
    fun `sem cache e sem internet deve explicar e oferecer tentar novamente`() = runTest {
        network.isOnline.value = false
        choose(ConversationAnswer.Start)
        choose(ConversationAnswer.ChooseMood(Mood.HAPPY))
        assertTrue(viewModel.uiState.value.messages.last() is ChatMessage.Loading)

        advanceUntilIdle()

        assertEquals(R.string.chat_error_first_search, texts().last())
        assertEquals(listOf(ChatOption.Retry, ChatOption.Restart), viewModel.uiState.value.options)
    }

    @Test
    fun `sem internet mas com cache antigo deve abrir as recomendacoes`() = runTest {
        network.isOnline.value = false
        recommendations.givenCache(
            TestData.USER.id, Mood.HAPPY, ConversationMode.MATCH,
            Recommendations(listOf(TestData.recommended(1)), TestData.NOW.minus(Duration.ofDays(3))),
        )
        viewModel.showRecommendations.test {
            choose(ConversationAnswer.Start)
            choose(ConversationAnswer.ChooseMood(Mood.HAPPY))
            assertEquals(Mood.HAPPY to ConversationMode.MATCH, awaitItem())
        }
    }

    @Test
    fun `conversar de novo deve voltar a saudacao`() {
        choose(ConversationAnswer.Start)
        viewModel.onOption(ChatOption.Restart)
        assertEquals(listOf(R.string.chat_greeting), texts())
    }
}
