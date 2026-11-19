package com.leticia.moodiva.domain.rule

import com.leticia.moodiva.domain.model.ConversationAnswer
import com.leticia.moodiva.domain.model.ConversationMode
import com.leticia.moodiva.domain.model.ConversationStep
import com.leticia.moodiva.domain.model.Mood

/**
 * Máquina de estados da conversa guiada (RN11), totalmente offline.
 *
 * saudação → humor → (se aplicável, RN10) modo → concluída.
 * Cada passo aceita só as respostas de [answersFor]; qualquer outra é recusada (`null`).
 */
object ConversationMachine {

    val START: ConversationStep = ConversationStep.Greeting

    fun answersFor(step: ConversationStep): List<ConversationAnswer> = when (step) {
        ConversationStep.Greeting -> listOf(ConversationAnswer.Start)
        ConversationStep.AskMood -> Mood.entries.map { ConversationAnswer.ChooseMood(it) }
        is ConversationStep.AskMode -> ConversationMode.entries.map { ConversationAnswer.ChooseMode(it) }
        is ConversationStep.Done -> emptyList()
    }

    /** Próximo passo, ou `null` se a resposta não pertence ao passo atual. */
    fun next(step: ConversationStep, answer: ConversationAnswer): ConversationStep? {
        if (answer !in answersFor(step)) return null
        return when (step) {
            ConversationStep.Greeting -> ConversationStep.AskMood
            ConversationStep.AskMood -> {
                val mood = (answer as ConversationAnswer.ChooseMood).mood
                if (MoodTagMapper.asksForMode(mood)) {
                    ConversationStep.AskMode(mood)
                } else {
                    ConversationStep.Done(mood, ConversationMode.MATCH)
                }
            }
            is ConversationStep.AskMode ->
                ConversationStep.Done(step.mood, (answer as ConversationAnswer.ChooseMode).mode)
            is ConversationStep.Done -> null
        }
    }
}
