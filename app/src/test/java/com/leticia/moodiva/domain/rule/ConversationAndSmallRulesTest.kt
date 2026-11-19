package com.leticia.moodiva.domain.rule

import com.leticia.moodiva.domain.model.ConversationAnswer
import com.leticia.moodiva.domain.model.ConversationMode
import com.leticia.moodiva.domain.model.ConversationStep
import com.leticia.moodiva.domain.model.CredentialProblem
import com.leticia.moodiva.domain.model.ListenStatus
import com.leticia.moodiva.domain.model.Mood
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime

class ConversationMachineTest {

    @Test
    fun `saudacao deve levar a pergunta do humor`() {
        assertEquals(
            ConversationStep.AskMood,
            ConversationMachine.next(ConversationMachine.START, ConversationAnswer.Start),
        )
    }

    @Test
    fun `feliz deve ir direto as recomendacoes sem perguntar o modo`() {
        val next = ConversationMachine.next(ConversationStep.AskMood, ConversationAnswer.ChooseMood(Mood.HAPPY))
        assertEquals(ConversationStep.Done(Mood.HAPPY, ConversationMode.MATCH), next)
    }

    @Test
    fun `triste deve perguntar se quer acompanhar ou mudar o clima`() {
        val askMode = ConversationMachine.next(ConversationStep.AskMood, ConversationAnswer.ChooseMood(Mood.SAD))
        assertEquals(ConversationStep.AskMode(Mood.SAD), askMode)

        val done = ConversationMachine.next(askMode!!, ConversationAnswer.ChooseMode(ConversationMode.SHIFT))
        assertEquals(ConversationStep.Done(Mood.SAD, ConversationMode.SHIFT), done)
    }

    @Test
    fun `resposta que nao pertence ao passo deve ser recusada`() {
        assertNull(ConversationMachine.next(ConversationStep.Greeting, ConversationAnswer.ChooseMood(Mood.SAD)))
        assertNull(
            ConversationMachine.next(ConversationStep.Done(Mood.SAD, ConversationMode.MATCH), ConversationAnswer.Start),
        )
    }

    @Test
    fun `pergunta do humor deve oferecer os oito humores`() {
        assertEquals(8, ConversationMachine.answersFor(ConversationStep.AskMood).size)
        assertEquals(2, ConversationMachine.answersFor(ConversationStep.AskMode(Mood.TIRED)).size)
    }
}

class SavedTrackRulesTest {

    @Test
    fun `avaliacao deve aceitar so de um a cinco`() {
        assertFalse(SavedTrackRules.isValidRating(0))
        assertTrue(SavedTrackRules.isValidRating(1))
        assertTrue(SavedTrackRules.isValidRating(5))
        assertFalse(SavedTrackRules.isValidRating(6))
    }

    @Test
    fun `so musicas ouvidas podem ser avaliadas`() {
        assertFalse(SavedTrackRules.canRate(ListenStatus.WANT_TO_LISTEN))
        assertTrue(SavedTrackRules.canRate(ListenStatus.LISTENED))
    }
}

class CredentialRulesTest {

    @Test
    fun `cadastro valido nao deve ter problemas`() {
        assertEquals(emptySet<CredentialProblem>(), CredentialRules.validateSignUp(" Ana@Mail.com ", "123456", "123456"))
    }

    @Test
    fun `senha curta e confirmacao diferente devem ser apontadas`() {
        val problems = CredentialRules.validateSignUp("ana@mail.com", "12345", "1234")
        assertEquals(setOf(CredentialProblem.PASSWORD_TOO_SHORT, CredentialProblem.CONFIRMATION_MISMATCH), problems)
    }

    @Test
    fun `email vazio ou sem formato deve ser apontado`() {
        assertEquals(setOf(CredentialProblem.EMAIL_BLANK, CredentialProblem.PASSWORD_BLANK), CredentialRules.validateSignIn(" ", ""))
        assertEquals(setOf(CredentialProblem.EMAIL_INVALID), CredentialRules.validateSignIn("ana@mail", "x"))
    }

    @Test
    fun `email deve ser comparado sem maiusculas e espacos`() {
        assertEquals("ana@mail.com", CredentialRules.normalizeEmail("  Ana@Mail.COM "))
    }
}

class CachePolicyTest {

    private val now = Instant.parse("2026-11-19T12:00:00Z")

    @Test
    fun `cache com menos de vinte e quatro horas deve ser valido`() {
        assertFalse(CachePolicy.isExpired(now.minus(Duration.ofHours(23)), now))
    }

    @Test
    fun `cache com exatamente vinte e quatro horas deve estar expirado`() {
        assertTrue(CachePolicy.isExpired(now.minus(Duration.ofHours(24)), now))
    }

    @Test
    fun `cache nunca buscado ou no futuro deve estar expirado`() {
        assertTrue(CachePolicy.isExpired(null, now))
        assertTrue(CachePolicy.isExpired(now.plusSeconds(60), now))
    }
}

class NotificationWindowTest {

    private fun at(hour: Int, minute: Int = 0) = LocalDateTime.of(2026, 11, 19, hour, minute)

    @Test
    fun `antes das dezenove deve esperar ate as dezenove do mesmo dia`() {
        assertEquals(Duration.ofHours(4), NotificationWindow.delayUntilNextWindow(at(15)))
    }

    @Test
    fun `dentro da janela o atraso deve ser zero`() {
        assertEquals(Duration.ZERO, NotificationWindow.delayUntilNextWindow(at(19)))
        assertEquals(Duration.ZERO, NotificationWindow.delayUntilNextWindow(at(20, 59)))
    }

    @Test
    fun `a partir das vinte e uma deve esperar ate o dia seguinte`() {
        assertEquals(Duration.ofHours(22), NotificationWindow.delayUntilNextWindow(at(21)))
    }
}
