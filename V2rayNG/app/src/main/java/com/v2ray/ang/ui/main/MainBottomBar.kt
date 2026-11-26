package com.v2ray.ang.ui.main

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v2ray.ang.ui.compose.MatixBlue
import com.v2ray.ang.ui.compose.MatixLogo
import com.v2ray.ang.ui.compose.glassCard

private val BAR_MAX_WIDTH = 480.dp

/**
 * Matix net: the bottom panel, an iOS-style rounded sheet that holds the slide-to-connect control. It sits at the
 * bottom of the screen, where a thumb reaches it, and on wide screens keeps a phone-like width.
 */
@Composable
fun MainBottomBar(
    displayText: String,
    isRunning: Boolean,
    isDarkTheme: Boolean,
    onAction: (MainAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .widthIn(max = BAR_MAX_WIDTH)
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 14.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard(shape = RoundedCornerShape(34.dp), accent = isRunning)
                .padding(8.dp)
                .semantics { contentDescription = displayText }
        ) {
            SlideToConnect(
                isRunning = isRunning,
                onToggle = { onAction(MainAction.ToggleService) }
            )
        }
    }
}

/**
 * Matix net: the top of the home screen, the logo with a blue ring that spreads from it while connected, and the
 * status under it. A tap on the status tests the current server, as a tap on the old bar did.
 */
@Composable
fun MainConnectHero(
    displayText: String,
    isRunning: Boolean,
    onAction: (MainAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val pulse = rememberInfiniteTransition(label = "connected-pulse")
    val ring by pulse.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing)),
        label = "ring"
    )
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.size(132.dp), contentAlignment = Alignment.Center) {
            if (isRunning) {
                Canvas(Modifier.size(132.dp)) {
                    drawCircle(
                        color = MatixBlue.copy(alpha = (1f - ring) * 0.45f),
                        radius = (size.minDimension / 2f) * (0.62f + 0.38f * ring),
                        style = Stroke(width = 3.dp.toPx())
                    )
                }
            }
            MatixLogo(size = 84.dp, interactive = true)
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = displayText,
            color = if (isRunning) MatixBlue else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .clip(CircleShape)
                .clickable { onAction(MainAction.TestCurrentServer) }
                .padding(horizontal = 14.dp, vertical = 6.dp)
        )
    }
}
