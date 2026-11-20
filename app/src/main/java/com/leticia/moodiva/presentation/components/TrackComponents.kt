package com.leticia.moodiva.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import coil3.compose.AsyncImage
import com.leticia.moodiva.R
import com.leticia.moodiva.domain.model.TrackSource
import com.leticia.moodiva.domain.rule.SavedTrackRules
import com.leticia.moodiva.presentation.theme.Dimens
import com.leticia.moodiva.presentation.theme.MoodivaTheme
import com.leticia.moodiva.presentation.util.labelRes

/**
 * Capa do álbum. Sem URL (ainda não buscada, indisponível ou placeholder do Last.fm) ou
 * com falha ao carregar, mostra o estado "capa indisponível": `primaryContainer` com a
 * nota musical em `primary` (identidade visual). O Coil guarda as capas em disco, então
 * as já vistas aparecem offline.
 */
@Composable
fun TrackCover(url: String?, modifier: Modifier = Modifier, size: Dp = Dimens.CoverCard) {
    var failed by remember(url) { mutableStateOf(false) }
    Box(
        modifier = modifier
            .size(size)
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        if (url == null || failed) {
            Icon(
                painter = painterResource(R.drawable.ic_music_note),
                contentDescription = stringResource(R.string.track_cover_unavailable),
                tint = MaterialTheme.colorScheme.primary,
            )
        } else {
            AsyncImage(
                model = url,
                contentDescription = null, // decorativa: o nome da música está ao lado
                contentScale = ContentScale.Crop,
                onError = { failed = true },
                modifier = Modifier.size(size),
            )
        }
    }
}

/**
 * Selo da fonte da recomendação (RN22): "Pelo seu humor" em `primaryContainer` e
 * "Pelo seu gosto" em `secondaryContainer`, com a forma `small` dos chips.
 */
@Composable
fun SourceBadge(source: TrackSource, modifier: Modifier = Modifier) {
    val (container, content) = when (source) {
        TrackSource.MOOD -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        TrackSource.TASTE -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
    }
    Surface(color = container, contentColor = content, shape = MaterialTheme.shapes.small, modifier = modifier) {
        Text(
            text = stringResource(source.labelRes),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = Dimens.MediumSpacing, vertical = Dimens.SmallSpacing),
        )
    }
}

/**
 * Estrelas de 1 a 5 (RF21): cheias em `tertiary`, vazias em `outline`.
 * Com [onRate], cada estrela é um botão de 48dp; sem ele, só exibe a nota.
 */
@Composable
fun RatingBar(rating: Int?, modifier: Modifier = Modifier, onRate: ((Int) -> Unit)? = null) {
    val summary = rating?.let { stringResource(R.string.saved_rating_cd, it) } ?: stringResource(R.string.saved_rate_hint)
    Row(modifier = modifier.semantics { if (onRate == null) contentDescription = summary }) {
        (SavedTrackRules.MIN_RATING..SavedTrackRules.MAX_RATING).forEach { star ->
            val filled = rating != null && star <= rating
            val tint = if (filled) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline
            if (onRate != null) {
                IconButton(onClick = { onRate(star) }) {
                    Icon(Icons.Rounded.Star, contentDescription = stringResource(R.string.saved_rate_cd, star), tint = tint)
                }
            } else {
                Icon(Icons.Rounded.Star, contentDescription = null, tint = tint, modifier = Modifier.size(Dimens.IconSmall))
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun TrackComponentsPreview() {
    MoodivaTheme {
        Surface {
            Row(Modifier.padding(Dimens.ItemSpacing)) {
                TrackCover(url = null)
                SourceBadge(TrackSource.MOOD, Modifier.padding(Dimens.MediumSpacing))
                SourceBadge(TrackSource.TASTE, Modifier.padding(Dimens.MediumSpacing))
            }
            RatingBar(rating = 3, onRate = {}, modifier = Modifier.padding(top = Dimens.CoverDetails))
        }
    }
}
