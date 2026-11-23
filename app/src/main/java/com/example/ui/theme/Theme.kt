package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val FitVersoLightScheme = lightColorScheme(
    primary = TealPrimary,
    onPrimary = Color.White,
    primaryContainer = TealSoft,
    onPrimaryContainer = DarkInk,
    secondary = SidebarNavy,
    onSecondary = Color.White,
    secondaryContainer = LightCardBg,
    onSecondaryContainer = DarkInk,
    tertiary = TealMint,
    onTertiary = Color.White,
    background = LightCanvasBg,
    onBackground = DarkInk,
    surface = LightCardBg,
    onSurface = DarkInk,
    surfaceVariant = LightFieldBg,
    onSurfaceVariant = MutedInk,
    outline = LightCardBorder,
    error = StopCoral,
    onError = Color.White
)

private val FitVersoDarkScheme = darkColorScheme(
    primary = CyanBox,
    onPrimary = ObsidianBg,
    secondary = TerracottaMesh,
    onSecondary = ObsidianBg,
    tertiary = GoldBone,
    background = ArenaDarkBg,
    onBackground = TextPrimary,
    surface = LabSurface,
    onSurface = TextPrimary,
    surfaceVariant = LabSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = LabBorder,
    error = CoralError,
    onError = Color.White
)

val InstantHmrShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp)
)

@Composable
fun InstantHmrTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FitVersoLightScheme,
        typography = InstantHmrTypography,
        shapes = InstantHmrShapes,
        content = content
    )
}

@Composable
fun FitVersoDarkArenaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FitVersoDarkScheme,
        typography = InstantHmrTypography,
        shapes = InstantHmrShapes,
        content = content
    )
}
