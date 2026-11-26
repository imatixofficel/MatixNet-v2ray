package com.v2ray.ang.ui.main

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v2ray.ang.R
import com.v2ray.ang.ui.compose.LocalDarkTheme
import com.v2ray.ang.ui.compose.MatixBlue
import com.v2ray.ang.ui.compose.MatixBlueBrush
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val TRACK_HEIGHT = 68.dp
private val KNOB_SIZE = 56.dp
private val KNOB_INSET = 6.dp

/** How far the knob must be dragged, as a share of the track, before the release counts as "on" (or "off"). */
private const val CONNECT_THRESHOLD = 0.7f
private const val DISCONNECT_THRESHOLD = 0.3f

/** How long a connect or disconnect may take before the knob goes back to what the service really is. */
private const val SETTLE_TIMEOUT_MS = 7000L

/**
 * Matix net: the connect control, a rounded track with a round knob. Drag the knob to the right to connect (the
 * track fills with blue as it goes); while connected the knob rests on the right, and dragging it back to the left
 * disconnects. Always left-to-right, in every language, so the "right" is the same physical side for everyone.
 *
 * [isRunning] is the truth of the service; the knob follows it, so a connection started from the widget or the
 * notification moves the knob too, and a refused or failed start sends the knob back after a moment.
 */
@Composable
fun SlideToConnect(
    isRunning: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dark = LocalDarkTheme.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val runningNow by rememberUpdatedState(isRunning)
    val toggleNow by rememberUpdatedState(onToggle)

    val progress = remember { Animatable(if (isRunning) 1f else 0f) }
    var dragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }
    // The state the last release asked for (true: connect, false: disconnect), until the service answers.
    var expecting by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(isRunning) {
        expecting = null
        if (!dragging) {
            progress.animateTo(
                if (isRunning) 1f else 0f,
                spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)
            )
        }
    }
    LaunchedEffect(expecting) {
        val asked = expecting ?: return@LaunchedEffect
        delay(SETTLE_TIMEOUT_MS)
        if (runningNow != asked) {
            expecting = null
            progress.animateTo(if (runningNow) 1f else 0f, spring())
        }
    }

    val shown = if (dragging) dragFraction else progress.value
    val waiting = expecting != null && expecting != isRunning

    val idle = rememberInfiniteTransition(label = "slide-hint")
    val hint by idle.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Reverse),
        label = "hint"
    )

    val connectLabel = stringResource(R.string.matix_slide_connect)
    val disconnectLabel = stringResource(R.string.matix_slide_disconnect)
    val trackColor = if (dark) Color(0xFF182033) else Color(0xFFE4EAF6)

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        BoxWithConstraints(
            modifier = modifier
                .fillMaxWidth()
                .height(TRACK_HEIGHT)
                .clip(CircleShape)
                .background(trackColor)
                .semantics(mergeDescendants = true) {
                    contentDescription = if (isRunning) disconnectLabel else connectLabel
                    onClick(label = if (isRunning) disconnectLabel else connectLabel) {
                        onToggle()
                        true
                    }
                }
        ) {
            val trackPx = with(density) { maxWidth.toPx() }
            val knobPx = with(density) { KNOB_SIZE.toPx() }
            val insetPx = with(density) { KNOB_INSET.toPx() }
            val travel = (trackPx - knobPx - 2f * insetPx).coerceAtLeast(1f)

            // The blue that follows the knob.
            val fillWidth = with(density) { (insetPx * 2f + knobPx + travel * shown).toDp() }
            Box(
                Modifier
                    .width(fillWidth)
                    .fillMaxHeight()
                    .alpha((shown * 5f).coerceIn(0f, 1f))
                    .clip(CircleShape)
                    .background(MatixBlueBrush)
            )

            // "Slide to connect", fading away as the knob moves; "slide back to disconnect" fading in on the blue.
            Text(
                text = connectLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(start = KNOB_SIZE + KNOB_INSET * 2, end = 16.dp)
                    .alpha(((1f - shown * 2f).coerceIn(0f, 1f)) * hint)
            )
            Text(
                text = disconnectLabel,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(start = 16.dp, end = KNOB_SIZE + KNOB_INSET * 2)
                    .alpha(((shown - 0.5f) * 2f).coerceIn(0f, 1f))
            )

            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset { IntOffset((insetPx + travel * shown).roundToInt(), 0) }
                    .size(KNOB_SIZE)
                    .shadow(elevation = 3.dp, shape = CircleShape)
                    .background(Color.White, CircleShape)
                    .pointerInput(travel) {
                        fun settle() {
                            val released = dragFraction
                            scope.launch {
                                progress.snapTo(released)
                                dragging = false
                                val running = runningNow
                                when {
                                    !running && released > CONNECT_THRESHOLD -> {
                                        progress.animateTo(1f, tween(140))
                                        expecting = true
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        toggleNow()
                                    }

                                    running && released < DISCONNECT_THRESHOLD -> {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        expecting = false
                                        toggleNow()
                                        progress.animateTo(0f, spring())
                                    }

                                    else -> progress.animateTo(if (running) 1f else 0f, spring())
                                }
                            }
                        }
                        detectHorizontalDragGestures(
                            onDragStart = {
                                dragFraction = progress.value
                                dragging = true
                            },
                            onDragEnd = { settle() },
                            onDragCancel = { settle() },
                            onHorizontalDrag = { change, amount ->
                                change.consume()
                                dragFraction = (dragFraction + amount / travel).coerceIn(0f, 1f)
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                if (waiting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.5.dp,
                        color = MatixBlue
                    )
                } else {
                    Icon(
                        painter = painterResource(if (shown > 0.5f) R.drawable.ic_stop_24dp else R.drawable.ic_play_24dp),
                        contentDescription = null,
                        tint = MatixBlue,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }
}
