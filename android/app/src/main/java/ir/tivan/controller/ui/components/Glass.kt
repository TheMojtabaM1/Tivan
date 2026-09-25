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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ir.tivan.controller.ui.theme.CurrentLayout
import ir.tivan.controller.ui.theme.Tivan

/**
 * The basic "کاشی" block every screen is built from: a solid, rounded tile.
 * No gradients or hairline borders — in the tile design the fill color itself
 * carries the meaning, so a border is only drawn when a caller asks for one
 * (e.g. to mark a selected tile).
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    corner: Dp? = null,
    tint: Color? = null,
    borderTint: Color? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val c = Tivan
    val shape: Shape = RoundedCornerShape(corner ?: CurrentLayout.cardCorner)

    var m = modifier
        .clip(shape)
        .background(tint ?: c.glass)
    if (borderTint != null && borderTint != c.stroke) m = m.border(2.dp, borderTint, shape)

    if (onClick != null) {
        m = m.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = rememberRipple(color = c.text),
            onClick = onClick
        )
    }

    Column(modifier = m, content = content)
}

/** Solid rounded status chip: "روشن", "منتظر تأیید", "۲۴°C". */
@Composable
fun StatusPill(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier
            .clip(RoundedCornerShape(99.dp))
            .background(color)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            color = if (color.luminance() > 0.5f) Color(0xFF1B1F16) else Color.White
        )
    }
}

/** Rounded square that holds an emoji, used as the leading icon everywhere. */
@Composable
fun IconTile(
    emoji: String,
    size: Dp = 48.dp,
    corner: Dp = 16.dp,
    tint: Color? = null,
    borderTint: Color? = null
) {
    val c = Tivan
    var m = Modifier
        .size(size)
        .clip(RoundedCornerShape(corner))
        .background(tint ?: c.tileOff)
    if (borderTint != null && borderTint != c.stroke) m = m.border(2.dp, borderTint, RoundedCornerShape(corner))
    Box(m, contentAlignment = Alignment.Center) {
        Text(emoji, style = MaterialTheme.typography.titleLarge)
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
        Text(title, style = MaterialTheme.typography.headlineSmall, color = c.text)
        if (hint != null) {
            Text(hint, style = MaterialTheme.typography.labelMedium, color = c.dim)
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
