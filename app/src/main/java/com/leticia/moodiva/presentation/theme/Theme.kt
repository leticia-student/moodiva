package com.leticia.moodiva.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = PrimaryLight, onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight, onPrimaryContainer = OnPrimaryContainerLight,
    inversePrimary = InversePrimaryLight,
    secondary = SecondaryLight, onSecondary = OnSecondaryLight,
    secondaryContainer = SecondaryContainerLight, onSecondaryContainer = OnSecondaryContainerLight,
    tertiary = TertiaryLight, onTertiary = OnTertiaryLight,
    tertiaryContainer = TertiaryContainerLight, onTertiaryContainer = OnTertiaryContainerLight,
    background = BackgroundLight, onBackground = OnBackgroundLight,
    surface = BackgroundLight, onSurface = OnBackgroundLight,
    surfaceVariant = SurfaceVariantLight, onSurfaceVariant = OnSurfaceVariantLight,
    surfaceTint = PrimaryLight,
    surfaceDim = SurfaceDimLight, surfaceBright = BackgroundLight,
    surfaceContainerLowest = SurfaceContainerLowestLight,
    surfaceContainerLow = SurfaceContainerLowLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    surfaceContainerHighest = SurfaceContainerHighestLight,
    inverseSurface = InverseSurfaceLight, inverseOnSurface = InverseOnSurfaceLight,
    outline = OutlineLight, outlineVariant = OutlineVariantLight,
    error = ErrorLight, onError = OnErrorLight,
    errorContainer = ErrorContainerLight, onErrorContainer = OnErrorContainerLight,
)

private val DarkColors = darkColorScheme(
    primary = PrimaryDark, onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark, onPrimaryContainer = OnPrimaryContainerDark,
    inversePrimary = InversePrimaryDark,
    secondary = SecondaryDark, onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryContainerDark, onSecondaryContainer = OnSecondaryContainerDark,
    tertiary = TertiaryDark, onTertiary = OnTertiaryDark,
    tertiaryContainer = TertiaryContainerDark, onTertiaryContainer = OnTertiaryContainerDark,
    background = BackgroundDark, onBackground = OnBackgroundDark,
    surface = BackgroundDark, onSurface = OnBackgroundDark,
    surfaceVariant = SurfaceVariantDark, onSurfaceVariant = OnSurfaceVariantDark,
    surfaceTint = PrimaryDark,
    surfaceDim = BackgroundDark, surfaceBright = SurfaceBrightDark,
    surfaceContainerLowest = SurfaceContainerLowestDark,
    surfaceContainerLow = SurfaceContainerLowDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    surfaceContainerHighest = SurfaceContainerHighestDark,
    inverseSurface = InverseSurfaceDark, inverseOnSurface = InverseOnSurfaceDark,
    outline = OutlineDark, outlineVariant = OutlineVariantDark,
    error = ErrorDark, onError = OnErrorDark,
    errorContainer = ErrorContainerDark, onErrorContainer = OnErrorContainerDark,
)

private val MoodivaShapes = Shapes(
    small = RoundedCornerShape(12.dp),   // capas de álbum e chips
    medium = RoundedCornerShape(20.dp),  // cards de música e itens de lista
    large = RoundedCornerShape(28.dp),   // painéis, bottom sheets e diálogos
)

/**
 * Formas dos balões do chat. Não existem no Shapes do Material 3,
 * por isso ficam aqui. O canto de 4dp "aponta" para quem falou.
 * São as ÚNICAS formas extras permitidas.
 */
object ChatBubbleShapes {
    val App = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomEnd = 20.dp, bottomStart = 4.dp)
    val User = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomEnd = 4.dp, bottomStart = 20.dp)
}

@Composable
fun MoodivaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = MoodivaTypography,
        shapes = MoodivaShapes,
        content = content,
    )
}
