package com.v2ray.ang.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/** Matix net: the corner radius shared by every card, so the whole app has one rounded, iOS-like look. */
val MatixCardShape = RoundedCornerShape(24.dp)

/** Matix net: the blue of the logo, used by buttons, the connect knob and highlights. */
val MatixBlue = Color(0xFF1B6BFF)
val MatixBlueDark = Color(0xFF1456D9)

/**
 * Matix net: an iOS-style card. A plain white card in the day theme (a deep navy one at night) with a hairline
 * border, so it reads as a clean, flat sheet. [accent] tints the card blue, for the cards that should stand out.
 * No blur and no shadow layer, so it costs nothing on weak phones.
 */
@Composable
fun Modifier.glassCard(
    shape: Shape = MatixCardShape,
    accent: Boolean = false
): Modifier {
    val dark = LocalDarkTheme.current
    val fill = when {
        dark && accent -> Color(0xFF0B1F3F)
        dark -> Color(0xFF0E1420)
        accent -> Color(0xFFE8F0FF)
        else -> Color.White
    }
    val edge = when {
        dark && accent -> MatixBlue.copy(alpha = 0.35f)
        dark -> Color.White.copy(alpha = 0.07f)
        accent -> MatixBlue.copy(alpha = 0.28f)
        else -> Color(0xFFE1E8F5)
    }
    return this
        .clip(shape)
        .background(fill)
        .border(1.dp, edge, shape)
}

/** Matix net: the app background, a clean page with a faint blue wash at the top (black with a dark blue glow at night). */
@Composable
fun Modifier.matixBackdrop(): Modifier {
    val dark = LocalDarkTheme.current
    val colors = if (dark) {
        listOf(Color(0xFF071A3F), Color(0xFF000000), Color(0xFF000000))
    } else {
        listOf(Color(0xFFDCE8FF), Color(0xFFF2F6FF), Color(0xFFF7F9FF))
    }
    return this.background(Brush.verticalGradient(colors))
}

/** Matix net: the blue gradient of the main buttons and the logo. */
val MatixBlueBrush: Brush
    get() = Brush.linearGradient(listOf(Color(0xFF4C9BFF), Color(0xFF1456D9)))
