package com.kawaii.meowbah.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.saveable.Saver
import androidx.compose.ui.graphics.Color // Keep import in case it's needed by other md_theme_... colors
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

internal val PinkDarkColorScheme = darkColorScheme(
    primary = md_theme_dark_primary,
    onPrimary = md_theme_dark_onPrimary,
    primaryContainer = md_theme_dark_primaryContainer,
    onPrimaryContainer = md_theme_dark_onPrimaryContainer,
    secondary = md_theme_dark_secondary,
    onSecondary = md_theme_dark_onSecondary,
    secondaryContainer = md_theme_dark_secondaryContainer,
    onSecondaryContainer = md_theme_dark_onSecondaryContainer,
    tertiary = md_theme_dark_tertiary,
    onTertiary = md_theme_dark_onTertiary,
    tertiaryContainer = md_theme_dark_tertiaryContainer,
    onTertiaryContainer = md_theme_dark_onTertiaryContainer,
    error = md_theme_dark_error,
    onError = md_theme_dark_onError,
    errorContainer = md_theme_dark_errorContainer,
    onErrorContainer = md_theme_dark_onErrorContainer,
    background = md_theme_dark_background, // This is the intended dark pink background
    onBackground = md_theme_dark_onBackground,
    surface = md_theme_dark_surface,
    onSurface = md_theme_dark_onSurface,
    surfaceVariant = md_theme_dark_surfaceVariant,
    onSurfaceVariant = md_theme_dark_onSurfaceVariant,
    outline = md_theme_dark_outline
)

internal val PinkLightColorScheme = lightColorScheme(
    primary = md_theme_light_primary,
    onPrimary = md_theme_light_onPrimary,
    primaryContainer = md_theme_light_primaryContainer,
    onPrimaryContainer = md_theme_light_onPrimaryContainer,
    secondary = md_theme_light_secondary,
    onSecondary = md_theme_light_onSecondary,
    secondaryContainer = md_theme_light_secondaryContainer,
    onSecondaryContainer = md_theme_light_onSecondaryContainer,
    tertiary = md_theme_light_tertiary,
    onTertiary = md_theme_light_onTertiary,
    tertiaryContainer = md_theme_light_tertiaryContainer,
    onTertiaryContainer = md_theme_light_onTertiaryContainer,
    error = md_theme_light_error,
    onError = md_theme_light_onError,
    errorContainer = md_theme_light_errorContainer,
    onErrorContainer = md_theme_light_onErrorContainer,
    background = md_theme_light_background,
    onBackground = md_theme_light_onBackground,
    surface = md_theme_light_surface,
    onSurface = md_theme_light_onSurface,
    surfaceVariant = md_theme_light_surfaceVariant,
    onSurfaceVariant = md_theme_light_onSurfaceVariant,
    outline = md_theme_light_outline
)

internal val LavenderLightColorScheme = lightColorScheme(
    primary = LavenderPrimary,
    onPrimary = LavenderOnPrimary,
    primaryContainer = LavenderPrimaryContainer,
    onPrimaryContainer = LavenderOnPrimaryContainer,
    secondary = LavenderSecondary,
    onSecondary = LavenderOnSecondary,
    secondaryContainer = LavenderSecondaryContainer,
    onSecondaryContainer = LavenderOnSecondaryContainer,
    tertiary = LavenderTertiary,
    onTertiary = LavenderOnTertiary,
    tertiaryContainer = LavenderTertiaryContainer,
    onTertiaryContainer = LavenderOnTertiaryContainer,
    error = LavenderError,
    onError = LavenderOnError,
    errorContainer = LavenderErrorContainer,
    onErrorContainer = LavenderOnErrorContainer,
    background = LavenderBackground,
    onBackground = LavenderOnBackground,
    surface = LavenderSurface,
    onSurface = LavenderOnSurface,
    surfaceVariant = LavenderSurfaceVariant,
    onSurfaceVariant = LavenderOnSurfaceVariant,
    outline = LavenderOutline
)

internal val LavenderDarkColorScheme = darkColorScheme(
    primary = LavenderDarkPrimary,
    onPrimary = LavenderDarkOnPrimary,
    primaryContainer = LavenderDarkPrimaryContainer,
    onPrimaryContainer = LavenderDarkOnPrimaryContainer,
    secondary = LavenderDarkSecondary,
    onSecondary = LavenderDarkOnSecondary,
    secondaryContainer = LavenderDarkSecondaryContainer,
    onSecondaryContainer = LavenderOnSecondaryContainer,
    tertiary = LavenderDarkTertiary,
    onTertiary = LavenderDarkOnTertiary,
    tertiaryContainer = LavenderDarkTertiaryContainer,
    onTertiaryContainer = LavenderDarkOnTertiaryContainer,
    error = LavenderDarkError,
    onError = LavenderDarkOnError,
    errorContainer = LavenderDarkErrorContainer,
    onErrorContainer = LavenderDarkOnErrorContainer,
    background = LavenderDarkBackground,
    onBackground = LavenderDarkOnBackground,
    surface = LavenderDarkSurface,
    onSurface = LavenderDarkOnSurface,
    surfaceVariant = LavenderDarkSurfaceVariant,
    onSurfaceVariant = LavenderDarkOnSurfaceVariant,
    outline = LavenderDarkOutline
)

internal val MintLightColorScheme = lightColorScheme(
    primary = MintPrimary,
    onPrimary = MintOnPrimary,
    primaryContainer = MintPrimaryContainer,
    onPrimaryContainer = MintOnPrimaryContainer,
    secondary = MintSecondary,
    onSecondary = MintOnSecondary,
    secondaryContainer = MintSecondaryContainer,
    onSecondaryContainer = MintOnSecondaryContainer,
    tertiary = MintTertiary,
    onTertiary = MintOnTertiary,
    tertiaryContainer = MintTertiaryContainer,
    onTertiaryContainer = MintOnTertiaryContainer,
    error = MintError,
    onError = MintOnError,
    errorContainer = MintErrorContainer,
    onErrorContainer = MintOnErrorContainer,
    background = MintBackground,
    onBackground = MintOnBackground,
    surface = MintSurface,
    onSurface = MintOnSurface,
    surfaceVariant = MintSurfaceVariant,
    onSurfaceVariant = MintOnSurfaceVariant,
    outline = MintOutline
)

internal val MintDarkColorScheme = darkColorScheme(
    primary = MintDarkPrimary,
    onPrimary = MintDarkOnPrimary,
    primaryContainer = MintDarkPrimaryContainer,
    onPrimaryContainer = MintDarkOnPrimaryContainer,
    secondary = MintDarkSecondary,
    onSecondary = MintDarkOnSecondary,
    secondaryContainer = MintDarkSecondaryContainer,
    onSecondaryContainer = MintOnSecondaryContainer,
    tertiary = MintDarkTertiary,
    onTertiary = MintDarkOnTertiary,
    tertiaryContainer = MintDarkTertiaryContainer,
    onTertiaryContainer = MintDarkOnTertiaryContainer,
    error = MintDarkError,
    onError = MintDarkOnError,
    errorContainer = MintDarkErrorContainer,
    onErrorContainer = MintDarkOnErrorContainer,
    background = MintDarkBackground,
    onBackground = MintDarkOnBackground,
    surface = MintDarkSurface,
    onSurface = MintDarkOnSurface,
    surfaceVariant = MintDarkSurfaceVariant,
    onSurfaceVariant = MintDarkOnSurfaceVariant,
    outline = MintDarkOutline
)

private const val THEME_ANIMATION_DURATION = 600 // ms

@Composable
fun AnimatedColorScheme(targetColorScheme: ColorScheme): ColorScheme {
    return targetColorScheme.copy(
        primary = animateColorAsState(targetColorScheme.primary, tween(THEME_ANIMATION_DURATION), label = "primary").value,
        onPrimary = animateColorAsState(targetColorScheme.onPrimary, tween(THEME_ANIMATION_DURATION), label = "onPrimary").value,
        primaryContainer = animateColorAsState(targetColorScheme.primaryContainer, tween(THEME_ANIMATION_DURATION), label = "primaryContainer").value,
        onPrimaryContainer = animateColorAsState(targetColorScheme.onPrimaryContainer, tween(THEME_ANIMATION_DURATION), label = "onPrimaryContainer").value,
        secondary = animateColorAsState(targetColorScheme.secondary, tween(THEME_ANIMATION_DURATION), label = "secondary").value,
        onSecondary = animateColorAsState(targetColorScheme.onSecondary, tween(THEME_ANIMATION_DURATION), label = "onSecondary").value,
        secondaryContainer = animateColorAsState(targetColorScheme.secondaryContainer, tween(THEME_ANIMATION_DURATION), label = "secondaryContainer").value,
        onSecondaryContainer = animateColorAsState(targetColorScheme.onSecondaryContainer, tween(THEME_ANIMATION_DURATION), label = "onSecondaryContainer").value,
        tertiary = animateColorAsState(targetColorScheme.tertiary, tween(THEME_ANIMATION_DURATION), label = "tertiary").value,
        onTertiary = animateColorAsState(targetColorScheme.onTertiary, tween(THEME_ANIMATION_DURATION), label = "onTertiary").value,
        tertiaryContainer = animateColorAsState(targetColorScheme.tertiaryContainer, tween(THEME_ANIMATION_DURATION), label = "tertiaryContainer").value,
        onTertiaryContainer = animateColorAsState(targetColorScheme.onTertiaryContainer, tween(THEME_ANIMATION_DURATION), label = "onTertiaryContainer").value,
        background = animateColorAsState(targetColorScheme.background, tween(THEME_ANIMATION_DURATION), label = "background").value,
        onBackground = animateColorAsState(targetColorScheme.onBackground, tween(THEME_ANIMATION_DURATION), label = "onBackground").value,
        surface = animateColorAsState(targetColorScheme.surface, tween(THEME_ANIMATION_DURATION), label = "surface").value,
        onSurface = animateColorAsState(targetColorScheme.onSurface, tween(THEME_ANIMATION_DURATION), label = "onSurface").value,
        surfaceVariant = animateColorAsState(targetColorScheme.surfaceVariant, tween(THEME_ANIMATION_DURATION), label = "surfaceVariant").value,
        onSurfaceVariant = animateColorAsState(targetColorScheme.onSurfaceVariant, tween(THEME_ANIMATION_DURATION), label = "onSurfaceVariant").value,
        error = animateColorAsState(targetColorScheme.error, tween(THEME_ANIMATION_DURATION), label = "error").value,
        onError = animateColorAsState(targetColorScheme.onError, tween(THEME_ANIMATION_DURATION), label = "onError").value,
        errorContainer = animateColorAsState(targetColorScheme.errorContainer, tween(THEME_ANIMATION_DURATION), label = "errorContainer").value,
        onErrorContainer = animateColorAsState(targetColorScheme.onErrorContainer, tween(THEME_ANIMATION_DURATION), label = "onErrorContainer").value,
        outline = animateColorAsState(targetColorScheme.outline, tween(THEME_ANIMATION_DURATION), label = "outline").value,
        inversePrimary = animateColorAsState(targetColorScheme.inversePrimary, tween(THEME_ANIMATION_DURATION), label = "inversePrimary").value,
        inverseSurface = animateColorAsState(targetColorScheme.inverseSurface, tween(THEME_ANIMATION_DURATION), label = "inverseSurface").value,
        inverseOnSurface = animateColorAsState(targetColorScheme.inverseOnSurface, tween(THEME_ANIMATION_DURATION), label = "inverseOnSurface").value,
        surfaceTint = animateColorAsState(targetColorScheme.surfaceTint, tween(THEME_ANIMATION_DURATION), label = "surfaceTint").value,
        outlineVariant = animateColorAsState(targetColorScheme.outlineVariant, tween(THEME_ANIMATION_DURATION), label = "outlineVariant").value,
        scrim = animateColorAsState(targetColorScheme.scrim, tween(THEME_ANIMATION_DURATION), label = "scrim").value
    )
}

@Composable
fun MeowbahTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    currentSelectedTheme: AvailableTheme = AvailableTheme.Pink, 
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when (currentSelectedTheme) {
        AvailableTheme.MaterialYou -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else {
                if (darkTheme) PinkDarkColorScheme else PinkLightColorScheme
            }
        }
        AvailableTheme.Lavender -> if (darkTheme) LavenderDarkColorScheme else LavenderLightColorScheme
        AvailableTheme.Mint -> if (darkTheme) MintDarkColorScheme else MintLightColorScheme
        AvailableTheme.Pink -> { 
             if (darkTheme) PinkDarkColorScheme else PinkLightColorScheme
        }
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme, 
        typography = AppTypography, 
        shapes = AppShapes,
        content = content
    )
}

sealed class AvailableTheme(val displayName: String) {
    object Pink : AvailableTheme("Pink") 
    object Lavender : AvailableTheme("Lavender")
    object Mint : AvailableTheme("Mint")
    object MaterialYou : AvailableTheme("Material You (System)")
}

val allThemes = listOf(AvailableTheme.Pink, AvailableTheme.Lavender, AvailableTheme.Mint, AvailableTheme.MaterialYou)

val AvailableThemeSaver = Saver<AvailableTheme, String>(
    save = { it.displayName },
    restore = { displayName ->
        allThemes.firstOrNull { it.displayName == displayName } ?: AvailableTheme.Pink
    }
)
