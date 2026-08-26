package ir.tivan.controller.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import ir.tivan.controller.ui.theme.Tivan

enum class HeroMood { Normal, Pending, Alarm }

data class HeroStat(val label: String, val value: String, val age: String? = null)

/**
 * The one card at the top of every tab. It reflects whatever matters most right
 * now — an armed alarm, a command awaiting confirmation, or a calm summary —
 * so the user never has to hunt for the app's current state.
 */
@Composable
fun AdaptiveHero(
    mood: HeroMood,
    emoji: String,
    title: String,
    subtitle: String,
    stats: List<HeroStat>,
    modifier: Modifier = Modifier
) {
    val c = Tivan
    val accent = when (mood) {
        HeroMood.Normal -> c.on
        HeroMood.Pending -> c.pending
        HeroMood.Alarm -> c.alarm
    }

    // A single low-frequency pulse drives the alarm glow; nothing animates at all
    // in the calm state, which keeps the tab cheap to render while idle.
    val pulse by rememberInfiniteTransition(label = "heroPulse").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "heroPulseValue"
    )
    val alarmAlpha = if (mood == HeroMood.Alarm) 0.10f + pulse * 0.12f else 0f

    val tint by animateColorAsState(
        targetValue = when (mood) {
            HeroMood.Normal -> c.glass
            HeroMood.Pending -> c.pending.copy(alpha = 0.13f)
            HeroMood.Alarm -> c.alarm.copy(alpha = 0.15f + alarmAlpha)
        },
        animationSpec = tween(350),
        label = "heroTint"
    )
    val border by animateColorAsState(
        targetValue = when (mood) {
            HeroMood.Normal -> c.stroke
            else -> accent.copy(alpha = 0.42f)
        },
        animationSpec = tween(350),
        label = "heroBorder"
    )

    GlassCard(modifier = modifier.fillMaxWidth(), tint = tint, borderTint = border) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(contentAlignment = Alignment.Center) {
                    IconTile(
                        emoji = emoji,
                        size = 58.dp,
                        corner = 20.dp,
                        tint = accent.copy(alpha = 0.16f),
                        borderTint = accent.copy(alpha = 0.38f)
                    )
                    if (mood == HeroMood.Pending) PendingRing(accent)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleLarge, color = c.text)
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = c.dim
                    )
                }
            }

            if (stats.isNotEmpty()) {
                Spacer(Modifier.height(15.dp))
                HorizontalDivider(c.stroke)
                Spacer(Modifier.height(13.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    stats.forEach { s ->
                        Column(
                            Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                s.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = c.dim2
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                s.value,
                                style = MaterialTheme.typography.titleSmall,
                                color = c.text
                            )
                            if (s.age != null) {
                                Text(
                                    s.age,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = c.dim2.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Thin rotating arc drawn around the hero icon while a command is unconfirmed. */
@Composable
private fun PendingRing(color: Color) {
    val angle by rememberInfiniteTransition(label = "ring").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing)),
        label = "ringAngle"
    )
    Canvas(Modifier.size(70.dp)) {
        drawArc(
            color = color,
            startAngle = angle,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(2.dp.toPx(), 2.dp.toPx()),
            size = Size(size.width - 4.dp.toPx(), size.height - 4.dp.toPx()),
            style = Stroke(width = 2.5.dp.toPx())
        )
    }
}

@Composable
internal fun HorizontalDivider(color: Color) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .then(Modifier)
    ) {
        Canvas(Modifier.fillMaxSize()) { drawRect(color) }
    }
}
