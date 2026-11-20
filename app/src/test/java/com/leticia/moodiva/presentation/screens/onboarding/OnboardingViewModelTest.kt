package com.leticia.moodiva.presentation.screens.onboarding

import app.cash.turbine.test
import com.leticia.moodiva.domain.fake.FakeAuthRepository
import com.leticia.moodiva.domain.fake.FakeProfileRepository
import com.leticia.moodiva.domain.fake.FakeRecommendationRepository
import com.leticia.moodiva.domain.fake.TestData
import com.leticia.moodiva.domain.model.Era
import com.leticia.moodiva.domain.model.Genre
import com.leticia.moodiva.domain.model.OriginPreference
import com.leticia.moodiva.domain.model.Trait
import com.leticia.moodiva.domain.usecase.CalculatePersonalityUseCase
import com.leticia.moodiva.domain.usecase.ObserveProfileUseCase
import com.leticia.moodiva.domain.usecase.SaveProfileUseCase
import com.leticia.moodiva.presentation.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class) // advanceUntilIdle
class OnboardingViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val auth = FakeAuthRepository()
    private val profiles = FakeProfileRepository()
    private val recommendations = FakeRecommendationRepository()

    private fun createViewModel(mode: OnboardingMode) = OnboardingViewModel(
        mode = mode,
        observeProfile = ObserveProfileUseCase(auth, profiles),
        calculatePersonality = CalculatePersonalityUseCase(),
        saveProfile = SaveProfileUseCase(auth, profiles, recommendations),
    )

    private fun OnboardingViewModel.answerAll(answers: List<Int> = TestData.CA08_ANSWERS) = answers.forEach(::onAnswer)

    @Test
    fun `fluxo completo deve salvar o perfil com tracos e gostos`() = runTest {
        val viewModel = createViewModel(OnboardingMode.FULL)
        assertEquals(OnboardingStep.INTRO, viewModel.uiState.value.step)

        viewModel.onContinue() // começar teste
        viewModel.answerAll()
        assertEquals(OnboardingStep.GENRES, viewModel.uiState.value.step)

        viewModel.onToggleGenre(Genre.MPB)
        viewModel.onContinue()
        viewModel.onArtistInputChange("Caetano Veloso")
        viewModel.onAddArtist()
        viewModel.onContinue()
        viewModel.onToggleEra(Era.SEVENTIES)
        viewModel.onContinue()
        viewModel.onSelectOrigin(OriginPreference.NATIONAL)
        viewModel.onContinue()
        assertEquals(OnboardingStep.RESULT, viewModel.uiState.value.step)
        assertEquals(6.0, viewModel.uiState.value.result!!.score(Trait.OPENNESS), 0.0)

        viewModel.onContinue() // "Conversar com a Moodiva": salva
        advanceUntilIdle()

        val saved = profiles.profiles.value[TestData.USER.id]!!
        assertEquals(setOf(Genre.MPB), saved.taste.genres)
        assertEquals(listOf("Caetano Veloso"), saved.taste.favoriteArtists)
        assertEquals(OriginPreference.NATIONAL, saved.taste.origin)
        assertEquals(5.5, saved.personality.score(Trait.EXTRAVERSION), 0.0)
    }

    @Test
    fun `nao deve avancar sem genero sem artista ou sem origem`() {
        val viewModel = createViewModel(OnboardingMode.EDIT_TASTE)
        assertEquals(OnboardingStep.GENRES, viewModel.uiState.value.step)
        assertFalse(viewModel.uiState.value.canContinue)
        viewModel.onContinue()
        assertEquals(OnboardingStep.GENRES, viewModel.uiState.value.step)
    }

    @Test
    fun `artistas devem ir de um a cinco sem repetidos`() {
        val viewModel = createViewModel(OnboardingMode.FULL)
        listOf("Gal Costa", "gal  costa", "A", "B", "C", "D", "E").forEach {
            viewModel.onArtistInputChange(it)
            viewModel.onAddArtist()
        }
        assertEquals(listOf("Gal Costa", "A", "B", "C", "D"), viewModel.uiState.value.artists)
        assertFalse(viewModel.uiState.value.canAddArtist)
    }

    @Test
    fun `voltar no teste deve ir ao item anterior mantendo a resposta`() {
        val viewModel = createViewModel(OnboardingMode.FULL)
        viewModel.onContinue()
        viewModel.onAnswer(5)
        viewModel.onAnswer(3)
        assertEquals(2, viewModel.uiState.value.tipiIndex)

        assertTrue(viewModel.onBack())
        assertEquals(1, viewModel.uiState.value.tipiIndex)
        assertEquals(3, viewModel.uiState.value.answers[1])
    }

    @Test
    fun `resposta fora da escala deve ser ignorada`() {
        val viewModel = createViewModel(OnboardingMode.FULL)
        viewModel.onContinue()
        viewModel.onAnswer(8)
        assertEquals(0, viewModel.uiState.value.tipiIndex)
        assertNull(viewModel.uiState.value.answers[0])
    }

    @Test
    fun `refazer teste deve manter os gostos, limpar o cache e fechar`() = runTest {
        profiles.profiles.value = mapOf(TestData.USER.id to TestData.profile(genres = setOf(Genre.JAZZ)))
        val viewModel = createViewModel(OnboardingMode.RETAKE_TEST)
        advanceUntilIdle()

        viewModel.closeEvents.test {
            viewModel.onContinue()
            viewModel.answerAll(List(10) { 7 })
            assertEquals(OnboardingStep.RESULT, viewModel.uiState.value.step)
            viewModel.onContinue()
            awaitItem()
        }

        val saved = profiles.profiles.value[TestData.USER.id]!!
        assertEquals(setOf(Genre.JAZZ), saved.taste.genres)
        assertEquals(List(10) { 7 }, saved.personality.answers)
        assertEquals(1, recommendations.clearCalls)
    }

    @Test
    fun `editar gostos deve comecar com os gostos atuais`() = runTest {
        profiles.profiles.value = mapOf(TestData.USER.id to TestData.profile(genres = setOf(Genre.ROCK), favorites = listOf("X")))
        val viewModel = createViewModel(OnboardingMode.EDIT_TASTE)
        advanceUntilIdle()

        assertEquals(setOf(Genre.ROCK), viewModel.uiState.value.genres)
        assertEquals(listOf("X"), viewModel.uiState.value.artists)
        assertFalse(viewModel.onBack()) // primeira etapa: a tela fecha
    }
}
