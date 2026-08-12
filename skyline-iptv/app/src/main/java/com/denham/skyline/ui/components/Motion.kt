package com.denham.skyline.ui.components

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Sky's signature entrance: fade in while sliding up, decelerating (ease-out) so
 * elements "arrive and settle". Pass an increasing [delayMs] down a list of
 * sections to get the staggered top-to-bottom reveal. Runs once, on first
 * composition; uses a graphics-layer transform so it never affects layout.
 */
fun Modifier.enterReveal(delayMs: Int = 0): Modifier = composed {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (delayMs > 0) delay(delayMs.toLong())
        shown = true
    }
    val progress by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(durationMillis = 340, easing = LinearOutSlowInEasing),
        label = "enterReveal",
    )
    graphicsLayer {
        alpha = progress
        translationY = (1f - progress) * 22.dp.toPx()
    }
}

/** Per-item stagger delay, capped so items scrolled in later don't wait too long. */
fun revealDelay(index: Int, stepMs: Int = 55, maxMs: Int = 330): Int =
    (index * stepMs).coerceAtMost(maxMs)
