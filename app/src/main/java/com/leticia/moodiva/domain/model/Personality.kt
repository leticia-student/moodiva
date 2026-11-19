package com.leticia.moodiva.domain.model

/** Os cinco traços medidos pelo TIPI (RN04). */
enum class Trait {
    EXTRAVERSION,
    AGREEABLENESS,
    CONSCIENTIOUSNESS,
    EMOTIONAL_STABILITY,
    OPENNESS,
}

/**
 * Resultado do teste de personalidade (RF05).
 *
 * @property answers as 10 respostas, na ordem dos itens, de 1 a 7.
 * @property scores a pontuação de cada traço, de 1,0 a 7,0.
 */
data class PersonalityProfile(
    val answers: List<Int>,
    val scores: Map<Trait, Double>,
) {
    fun score(trait: Trait): Double = scores.getValue(trait)
}

/** Perfil de descoberta derivado da Abertura a experiências (RN06). */
enum class OpennessLevel {
    DISCOVERY,
    BALANCED,
    FAMILIARITY,
}
