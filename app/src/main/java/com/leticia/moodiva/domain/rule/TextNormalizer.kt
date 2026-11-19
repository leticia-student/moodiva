package com.leticia.moodiva.domain.rule

import java.text.Normalizer

/**
 * Normalização de textos antes de qualquer comparação (RN12).
 *
 * 1. Minúsculas.
 * 2. Sem acentos: decompõe "é" em "e" + acento (forma NFD) e remove os acentos.
 * 3. Sem espaços nas pontas e com espaços internos repetidos reduzidos a um.
 *
 * Exemplo: `"  Bossa   Nova "` → `"bossa nova"`; `"Forró"` → `"forro"`.
 */
object TextNormalizer {

    private val COMBINING_MARKS = Regex("\\p{Mn}+")
    private val WHITESPACE = Regex("\\s+")

    fun normalize(text: String): String =
        Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD)
            .replace(COMBINING_MARKS, "")
            .replace(WHITESPACE, " ")
            .trim()
}

/** Identidade de uma música: artista + nome, normalizados (RN23). */
object TrackKey {

    private const val SEPARATOR = "|"

    fun of(artist: String, name: String): String =
        TextNormalizer.normalize(artist) + SEPARATOR + TextNormalizer.normalize(name)
}
