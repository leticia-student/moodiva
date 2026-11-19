package com.leticia.moodiva.domain.rule

import com.leticia.moodiva.domain.model.ConversationMode
import com.leticia.moodiva.domain.model.Mood

/**
 * Humor + modo → tags do Last.fm, somente em inglês (RN09, RN10).
 *
 * O `when` é exaustivo: um humor novo no enum só compila depois de ganhar suas tags.
 */
object MoodTagMapper {

    fun tagsFor(mood: Mood, mode: ConversationMode): List<String> = when (mood) {
        Mood.HAPPY -> listOf("happy", "feel good", "upbeat")
        Mood.SAD -> when (mode) {
            ConversationMode.MATCH -> listOf("sad", "melancholy")
            ConversationMode.SHIFT -> listOf("feel good", "uplifting")
        }
        Mood.EXCITED -> listOf("energetic", "party", "dance")
        // "Ansioso" sempre recebe músicas calmas, em qualquer modo.
        Mood.ANXIOUS -> when (mode) {
            ConversationMode.MATCH -> listOf("calm", "relaxing", "chill")
            ConversationMode.SHIFT -> listOf("calm", "relaxing")
        }
        Mood.TIRED -> when (mode) {
            ConversationMode.MATCH -> listOf("mellow", "acoustic", "chill")
            ConversationMode.SHIFT -> listOf("energetic", "upbeat")
        }
        Mood.ROMANTIC -> listOf("love", "romantic")
        Mood.NOSTALGIC -> listOf("oldies", "80s", "90s")
        Mood.FOCUSED -> listOf("instrumental", "ambient")
    }

    /** RN10: a conversa só pergunta o modo quando ele muda as tags (Triste, Ansioso, Cansado). */
    fun asksForMode(mood: Mood): Boolean =
        tagsFor(mood, ConversationMode.MATCH) != tagsFor(mood, ConversationMode.SHIFT)

    /** Modo efetivo: humores que não perguntam o modo usam sempre "acompanhar". */
    fun effectiveMode(mood: Mood, mode: ConversationMode): ConversationMode =
        if (asksForMode(mood)) mode else ConversationMode.MATCH
}
