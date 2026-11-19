package com.leticia.moodiva.domain.model

/**
 * Lista fechada de gêneros do onboarding (especificação, seção 8.8).
 *
 * [tags] são as tags do Last.fm aceitas para o gênero, já normalizadas
 * (minúsculas, sem acentos e sem espaços extras, RN12).
 */
enum class Genre(val tags: Set<String>) {
    POP(setOf("pop")),
    ROCK(setOf("rock", "classic rock", "alternative rock")),
    INDIE(setOf("indie", "indie pop", "indie rock")),
    ELECTRONIC(setOf("electronic", "dance", "house", "edm")),
    HIP_HOP(setOf("hip-hop", "hip hop", "rap")),
    RNB_SOUL(setOf("rnb", "r&b", "soul")),
    JAZZ(setOf("jazz")),
    BLUES(setOf("blues")),
    CLASSICAL(setOf("classical")),
    METAL(setOf("metal", "heavy metal")),
    REGGAE(setOf("reggae")),
    COUNTRY(setOf("country")),
    LATIN(setOf("latin", "reggaeton")),
    K_POP(setOf("k-pop", "kpop")),
    GOSPEL(setOf("gospel")),
    MPB(setOf("mpb")),
    SAMBA_PAGODE(setOf("samba", "pagode")),
    BOSSA_NOVA(setOf("bossa nova")),
    SERTANEJO(setOf("sertanejo")),
    FORRO(setOf("forro")),
    BRAZILIAN_FUNK(setOf("funk carioca", "brazilian funk")),
    AXE(setOf("axe")),
}

/** Épocas musicais preferidas (RN36). Aparecem só no perfil. */
enum class Era {
    SIXTIES,
    SEVENTIES,
    EIGHTIES,
    NINETIES,
    TWO_THOUSANDS,
    TWENTY_TENS,
    TWENTY_TWENTIES,
}

/** Preferência de origem das músicas (RN17). */
enum class OriginPreference {
    NATIONAL,
    INTERNATIONAL,
    BOTH,
}

/** Gostos musicais informados no onboarding (RF07–RF10). */
data class TasteProfile(
    val genres: Set<Genre>,
    val favoriteArtists: List<String>,
    val eras: Set<Era>,
    val origin: OriginPreference,
)

/** Perfil completo de uma conta: personalidade + gostos. Fica só no aparelho (RNF09). */
data class UserProfile(
    val personality: PersonalityProfile,
    val taste: TasteProfile,
)
