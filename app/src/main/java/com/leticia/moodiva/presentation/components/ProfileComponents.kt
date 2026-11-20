package com.leticia.moodiva.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.leticia.moodiva.R
import com.leticia.moodiva.domain.model.PersonalityProfile
import com.leticia.moodiva.domain.model.Trait
import com.leticia.moodiva.domain.rule.TipiCalculator
import com.leticia.moodiva.presentation.theme.Dimens
import com.leticia.moodiva.presentation.theme.MoodivaTheme
import com.leticia.moodiva.presentation.util.labelRes
import java.util.Locale

/** Os cinco traços com uma barra de 1 a 7 cada (resultado do teste e perfil, RF11). */
@Composable
fun TraitBars(personality: PersonalityProfile, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)) {
        Trait.entries.forEach { trait -> TraitBar(trait, personality.score(trait)) }
    }
}

@Composable
private fun TraitBar(trait: Trait, score: Double) {
    val label = stringResource(trait.labelRes)
    val value = stringResource(R.string.trait_score, String.format(Locale("pt", "BR"), "%.1f", score))
    // Leitores de tela leem "Extroversão, 5,5 de 7" de uma vez.
    Column(
        modifier = Modifier.clearAndSetSemantics { contentDescription = "$label, $value" },
        verticalArrangement = Arrangement.spacedBy(Dimens.SmallSpacing),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LinearProgressIndicator(
            progress = {
                ((score - TipiCalculator.MIN_ANSWER) / (TipiCalculator.MAX_ANSWER - TipiCalculator.MIN_ANSWER)).toFloat()
            },
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.primaryContainer,
            drawStopIndicator = {},
            modifier = Modifier
                .fillMaxWidth()
                .height(Dimens.TraitBarHeight)
                .clip(MaterialTheme.shapes.small),
        )
    }
}

@PreviewLightDark
@Composable
private fun TraitBarsPreview() {
    MoodivaTheme {
        Surface {
            TraitBars(TipiCalculator.calculate(listOf(5, 3, 6, 2, 7, 2, 6, 1, 5, 3))!!)
        }
    }
}
