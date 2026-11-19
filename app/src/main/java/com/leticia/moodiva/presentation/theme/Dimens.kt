package com.leticia.moodiva.presentation.theme

import androidx.compose.ui.unit.dp

/**
 * Espaçamentos e tamanhos definidos em docs/identidade_visual.md ("Espaçamento e toque").
 * Centralizados aqui para nenhuma tela repetir números soltos.
 */
object Dimens {
    val ScreenHorizontalPadding = 20.dp
    val ItemSpacing = 16.dp
    val SectionSpacing = 24.dp

    // Chat: 8dp entre mensagens do mesmo autor, 16dp quando o autor muda.
    val BubbleSameAuthorSpacing = 8.dp
    val BubbleAuthorChangeSpacing = 16.dp
    const val BUBBLE_MAX_WIDTH_FRACTION = 0.8f

    // Capas de álbum
    val CoverCard = 56.dp
    val CoverDetails = 240.dp

    // Valores internos dos componentes, derivados da escala acima.
    val SmallSpacing = 4.dp
    val MediumSpacing = 8.dp
    val ContentPadding = 12.dp
    val BorderWidth = 1.dp
    val SelectedBorderWidth = 2.dp
    val IconSmall = 18.dp
    val IconMedium = 24.dp
    val ScaleOptionSize = 48.dp // alvo de toque mínimo da escala 1–7 do teste
    val TraitBarHeight = 8.dp
    val ProgressStrokeWidth = 2.dp
    val ContentMaxWidth = 600.dp // limita a largura em telas grandes e tablets
}
