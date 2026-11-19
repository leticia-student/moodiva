package com.leticia.moodiva.domain.rule

import com.leticia.moodiva.domain.model.Trait
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TipiCalculatorTest {

    // Respostas do CA08 (especificação, seção 9).
    private val ca08Answers = listOf(5, 3, 6, 2, 7, 2, 6, 1, 5, 3)

    @Test
    fun `respostas do CA08 devem gerar os cinco tracos esperados`() {
        val profile = TipiCalculator.calculate(ca08Answers)!!

        assertEquals(5.5, profile.score(Trait.EXTRAVERSION), 0.0)
        assertEquals(5.5, profile.score(Trait.AGREEABLENESS), 0.0)
        assertEquals(6.5, profile.score(Trait.CONSCIENTIOUSNESS), 0.0)
        assertEquals(5.5, profile.score(Trait.EMOTIONAL_STABILITY), 0.0)
        assertEquals(6.0, profile.score(Trait.OPENNESS), 0.0)
        assertEquals(ca08Answers, profile.answers)
    }

    @Test
    fun `itens invertidos devem valer oito menos a resposta`() {
        listOf(2, 4, 6, 8, 10).forEach { item -> assertEquals(7, TipiCalculator.itemScore(item, 1)) }
        listOf(1, 3, 5, 7, 9).forEach { item -> assertEquals(1, TipiCalculator.itemScore(item, 1)) }
    }

    @Test
    fun `respostas extremas devem gerar tracos entre um e sete`() {
        val allOnes = TipiCalculator.calculate(List(10) { 1 })!!
        val allSevens = TipiCalculator.calculate(List(10) { 7 })!!

        // Cada traço mistura um item direto e um invertido: 1 e 7 viram média 4.
        Trait.entries.forEach {
            assertEquals(4.0, allOnes.score(it), 0.0)
            assertEquals(4.0, allSevens.score(it), 0.0)
        }
    }

    @Test
    fun `teste com nove respostas nao deve ser concluido`() {
        assertNull(TipiCalculator.calculate(ca08Answers.dropLast(1)))
        assertFalse(TipiCalculator.isComplete(ca08Answers.dropLast(1) + null))
    }

    @Test
    fun `respostas fora da escala devem ser rejeitadas`() {
        assertFalse(TipiCalculator.isValidAnswer(0))
        assertFalse(TipiCalculator.isValidAnswer(8))
        assertTrue(TipiCalculator.isValidAnswer(1))
        assertTrue(TipiCalculator.isValidAnswer(7))
        assertNull(TipiCalculator.calculate(ca08Answers.toMutableList().also { it[3] = 8 }))
    }

    @Test
    fun `teste completo e valido deve ser aceito`() {
        assertTrue(TipiCalculator.isComplete(ca08Answers))
    }
}
