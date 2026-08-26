package ir.tivan.controller.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ir.tivan.controller.ui.theme.Tivan

/**
 * The frosted-card look, faked with layered translucent gradients rather than a
 * real blur. `RenderEffect.createBlurEffect` only exists on API 31+ and costs a
 * full-screen readback every frame; these cards are cheap enough to scroll at
 * 120 Hz on low-end hardware and look identical against the app's own gradient
 * background.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    corner: Dp = 22.dp,
    tint: Color? = null,
    borderTint: Color? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val c = Tivan
    val shape: Shape = RoundedCornerShape(corner)
    val base = tint ?: c.glass

    var m = modifier
        .clip(shape)
        .background(
            Brush.verticalGradient(
                listOf(
                    base.copy(alpha = (base.alpha * 1.35f).coerceAtMost(1f)),
                    base
                )
            )
        )
        .border(1.dp, borderTint ?: c.stroke, shape)

    if (onClick != null) {
        m = m.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = rememberRipple(color = c.primary),
            onClick = onClick
        )
    }

    Column(modifier = m, content = content)
}

/** Small rounded status chip: "روشن", "منتظر تأیید", "۲۴°C". */
@Composable
fun StatusPill(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier
            .clip(RoundedCornerShape(9.dp))
            .background(color.copy(alpha = 0.16f))
            .border(1.dp, color.copy(alpha = 0.34f), RoundedCornerShape(9.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

/** Rounded square that holds an emoji, used as the leading icon everywhere. */
@Composable
fun IconTile(
    emoji: String,
    size: Dp = 42.dp,
    corner: Dp = 14.dp,
    tint: Color? = null,
    borderTint: Color? = null
) {
    val c = Tivan
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(corner))
            .background(tint ?: c.glassStrong)
            .border(1.dp, borderTint ?: c.stroke, RoundedCornerShape(corner)),
        contentAlignment = Alignment.Center
    ) {
        Text(emoji, style = MaterialTheme.typography.titleMedium)
    }
}

/** Section heading with an optional trailing hint on the far side. */
@Composable
fun SectionHeader(title: String, hint: String? = null) {
    val c = Tivan
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, top = 20.dp, bottom = 10.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = c.text)
        if (hint != null) {
            Text(hint, style = MaterialTheme.typography.labelSmall, color = c.dim2)
        }
    }
}

/**
 * Value with the "as of when" caption underneath — the app never shows cached
 * device data without saying how old it is.
 */
@Composable
fun ValueWithAge(
    value: String,
    age: String,
    stale: Boolean = false,
    modifier: Modifier = Modifier,
    align: Alignment.Horizontal = Alignment.Start
) {
    val c = Tivan
    Column(modifier, horizontalAlignment = align) {
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            color = if (stale) c.dim else c.text
        )
        Text(
            age,
            style = MaterialTheme.typography.labelSmall,
            color = if (stale) c.dim2.copy(alpha = 0.75f) else c.dim2
        )
    }
}
