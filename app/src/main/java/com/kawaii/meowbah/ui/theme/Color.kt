package com.kawaii.meowbah.ui.theme

import androidx.compose.ui.graphics.Color

// Define our Kawaii Pink Palette
val PinkAesthetic = Color(0xFFFFC0CB) // Light Pink - Primary candidate
val PinkAccent = Color(0xFFF9A8D4) // Softer, slightly more saturated pink
val PinkDeep = Color(0xFFF472B6)  // Deeper Pink
val PinkPastel = Color(0xFFFFE4E1) // Misty Rose - very light, for backgrounds/containers
val PinkVibrant = Color(0xFFEC4899) // Vibrant Pink - Secondary candidate

val TextOnPink = Color(0xFFFFFFFF) // White text for darker pinks
val TextOnLightPink = Color(0xFF88394A) // Darker text for very light pinks

// Material 3 Light Theme Colors
val md_theme_light_primary = PinkAesthetic // Light Pink
val md_theme_light_onPrimary = TextOnLightPink
val md_theme_light_primaryContainer = PinkPastel
val md_theme_light_onPrimaryContainer = TextOnLightPink

val md_theme_light_secondary = PinkVibrant // Vibrant Pink
val md_theme_light_onSecondary = TextOnPink
val md_theme_light_secondaryContainer = Color(0xFFFFD9E2) // Lighter version of PinkVibrant or PinkPastel
val md_theme_light_onSecondaryContainer = TextOnLightPink

val md_theme_light_tertiary = PinkAccent // Softer Accent
val md_theme_light_onTertiary = TextOnPink
val md_theme_light_tertiaryContainer = Color(0xFFFFD9F2)
val md_theme_light_onTertiaryContainer = TextOnLightPink

val md_theme_light_error = Color(0xFFB00020) // Standard Error Red
val md_theme_light_onError = Color.White
val md_theme_light_errorContainer = Color(0xFFFCD8DF)
val md_theme_light_onErrorContainer = Color(0xFFB00020)

val md_theme_light_background = PinkPastel // Very light pink background
val md_theme_light_onBackground = TextOnLightPink
val md_theme_light_surface = PinkPastel // Can be same as background or slightly different
val md_theme_light_onSurface = TextOnLightPink
val md_theme_light_surfaceVariant = Color(0xFFFDE7EA)
val md_theme_light_onSurfaceVariant = TextOnLightPink

val md_theme_light_outline = PinkDeep // For outlines, borders

// Material 3 Dark Theme Colors (Let's make them pink-tinted darks)
val md_theme_dark_primary = PinkDeep // Deeper Pink for dark theme primary
val md_theme_dark_onPrimary = TextOnPink
val md_theme_dark_primaryContainer = Color(0xFF88394A) // Darker pink container
val md_theme_dark_onPrimaryContainer = PinkPastel

val md_theme_dark_secondary = PinkAccent // Accent can be similar
val md_theme_dark_onSecondary = TextOnLightPink
val md_theme_dark_secondaryContainer = Color(0xFFB95A7E)
val md_theme_dark_onSecondaryContainer = PinkPastel

val md_theme_dark_tertiary = PinkAesthetic // Light Pink as tertiary in dark
val md_theme_dark_onTertiary = TextOnLightPink
val md_theme_dark_tertiaryContainer = Color(0xFFCF859A)
val md_theme_dark_onTertiaryContainer = TextOnPink

val md_theme_dark_error = Color(0xFFCF6679) // Lighter error for dark theme
val md_theme_dark_onError = Color(0xFF141213)
val md_theme_dark_errorContainer = Color(0xFFB00020)
val md_theme_dark_onErrorContainer = Color(0xFFFCD8DF)

val md_theme_dark_background = Color(0xFF3E272E) // Dark pinkish-brown/grey
val md_theme_dark_onBackground = PinkPastel
val md_theme_dark_surface = Color(0xFF3E272E)
val md_theme_dark_onSurface = PinkPastel
val md_theme_dark_surfaceVariant = Color(0xFF5A3B44)
val md_theme_dark_onSurfaceVariant = PinkPastel

val md_theme_dark_outline = PinkAccent