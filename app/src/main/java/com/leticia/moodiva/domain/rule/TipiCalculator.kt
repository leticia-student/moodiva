package com.leticia.moodiva.domain.rule

import com.leticia.moodiva.domain.model.PersonalityProfile
import com.leticia.moodiva.domain.model.Trait

/**
 * Cálculo do TIPI (RN01–RN04).
 *
 * - 10 respostas, cada uma de 1 a 7.
 * - Itens invertidos (R) valem `8 - resposta`: são os itens 2, 4, 6, 8 e 10.
 * - Cada traço é a média dos seus dois itens:
 *   Extroversão (1, 6R), Amabilidade (2R, 7), Conscienciosidade (3, 8R),
 *   Estabilidade emocional (4R, 9), Abertura (5, 10R).
 */
object TipiCalculator {

    const val ITEM_COUNT = 10
    const val MIN_ANSWER = 1
    const val MAX_ANSWER = 7

    /** Itens invertidos, numerados de 1 a 10 como no questionário. */
    private val REVERSED_ITEMS = setOf(2, 4, 6, 8, 10)

    /** Os dois itens (1 a 10) de cada traço. */
    private val TRAIT_ITEMS: Map<Trait, Pair<Int, Int>> = mapOf(
        Trait.EXTRAVERSION to (1 to 6),
        Trait.AGREEABLENESS to (2 to 7),
        Trait.CONSCIENTIOUSNESS to (3 to 8),
        Trait.EMOTIONAL_STABILITY to (4 to 9),
        Trait.OPENNESS to (5 to 10),
    )

    /** RN01: resposta dentro da escala de 1 a 7. */
    fun isValidAnswer(answer: Int): Boolean = answer in MIN_ANSWER..MAX_ANSWER

    /** RN02: o teste só está completo com as 10 respostas preenchidas e válidas. */
    fun isComplete(answers: List<Int?>): Boolean =
        answers.size == ITEM_COUNT && answers.all { it != null && isValidAnswer(it) }

    /** Pontuação de um item (1 a 10), aplicando a inversão quando for o caso (RN03). */
    fun itemScore(item: Int, answer: Int): Int =
        if (item in REVERSED_ITEMS) (MAX_ANSWER + 1) - answer else answer

    /**
     * Calcula o perfil (RN04). Devolve `null` se as respostas forem incompletas ou
     * tiverem valores fora da escala (RN01, RN02).
     */
    fun calculate(answers: List<Int>): PersonalityProfile? {
        if (!isComplete(answers)) return null
        val scores = TRAIT_ITEMS.mapValues { (_, items) ->
            val (first, second) = items
            (itemScore(first, answers[first - 1]) + itemScore(second, answers[second - 1])) / 2.0
        }
        return PersonalityProfile(answers = answers, scores = scores)
    }
}
