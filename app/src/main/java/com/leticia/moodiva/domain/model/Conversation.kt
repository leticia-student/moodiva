package com.leticia.moodiva.domain.model

/** Os oito humores da conversa (RN09). */
enum class Mood {
    HAPPY,
    SAD,
    EXCITED,
    ANXIOUS,
    TIRED,
    ROMANTIC,
    NOSTALGIC,
    FOCUSED,
}

/** Acompanhar (`MATCH`) ou mudar (`SHIFT`) o clima. */
enum class ConversationMode {
    MATCH,
    SHIFT,
}

/** Passos da conversa guiada (RN11). Cada passo tem um conjunto fechado de respostas. */
sealed interface ConversationStep {

    /** Saudação: a única resposta é "Vamos lá". */
    data object Greeting : ConversationStep

    /** Pergunta o humor: uma resposta por [Mood]. */
    data object AskMood : ConversationStep

    /** Pergunta se o usuário quer acompanhar ou mudar o clima (só para alguns humores, RN10). */
    data class AskMode(val mood: Mood) : ConversationStep

    /** Conversa concluída: hora das recomendações. */
    data class Done(val mood: Mood, val mode: ConversationMode) : ConversationStep
}

/** Respostas possíveis da conversa, sempre escolhidas por chips. */
sealed interface ConversationAnswer {
    data object Start : ConversationAnswer
    data class ChooseMood(val mood: Mood) : ConversationAnswer
    data class ChooseMode(val mode: ConversationMode) : ConversationAnswer
}
