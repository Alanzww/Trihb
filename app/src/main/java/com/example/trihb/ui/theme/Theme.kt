package com.example.trihb.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

/**
 * Esquema de cores claro TriHub. A identidade é deliberadamente clara e calma,
 * por isso não usamos cores dinâmicas (Material You) para preservar a marca.
 */
private val TriHubLightColorScheme = lightColorScheme(
    primary = TriPrimary,
    onPrimary = TriOnPrimary,
    primaryContainer = TriPrimaryContainer,
    onPrimaryContainer = TriOnPrimaryContainer,
    secondary = TriSecondary,
    onSecondary = TriOnSecondary,
    secondaryContainer = TriSecondaryContainer,
    onSecondaryContainer = TriOnSecondaryContainer,
    tertiary = TriTertiary,
    onTertiary = TriOnTertiary,
    tertiaryContainer = TriTertiaryContainer,
    onTertiaryContainer = TriOnTertiaryContainer,
    background = TriBackground,
    onBackground = TriOnSurface,
    surface = TriSurface,
    onSurface = TriOnSurface,
    surfaceVariant = TriSurfaceVariant,
    onSurfaceVariant = TriOnSurfaceVariant,
    surfaceContainerLowest = TriSurface,
    surfaceContainerLow = TriSurface,
    surfaceContainer = TriSurface,
    surfaceContainerHigh = TriSurfaceVariant,
    surfaceContainerHighest = TriSurfaceVariant,
    outline = TriOutline,
    outlineVariant = TriOutlineVariant,
    error = TriError,
    onError = TriOnError,
    errorContainer = TriErrorContainer,
    onErrorContainer = TriOnErrorContainer,
)

/** Geometria TriHub: cards 16–20 dp, botões/chips/campos 12–16 dp. */
val TriHubShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),   // chips, campos de texto
    medium = RoundedCornerShape(16.dp),  // botões, cards secundários
    large = RoundedCornerShape(20.dp),   // cards principais
    extraLarge = RoundedCornerShape(28.dp), // diálogos
)

@Composable
fun TriHubTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TriHubLightColorScheme,
        typography = Typography,
        shapes = TriHubShapes,
        content = content,
    )
}
