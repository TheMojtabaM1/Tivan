package ir.tivan.controller.ui.security

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import ir.tivan.controller.data.LogDirection
import ir.tivan.controller.ui.MainViewModel
import ir.tivan.controller.ui.components.*
import ir.tivan.controller.ui.inputs.SegmentButton
import ir.tivan.controller.ui.theme.Tivan
import ir.tivan.controller.util.RelativeTime

@Composable
fun SecurityScreen(viewModel: MainViewModel, header: @Composable () -> Unit) {
    val c = Tivan
    val status by viewModel.status.collectAsState()
    val device by viewModel.selectedDevice.collectAsState()
    val pending by viewModel.pendingSecurity.collectAsState()
    val logs by viewModel.logs.collectAsState()

    val armed = status?.securityArmed
    val zones = device?.securityZones ?: 2

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        header()
        SectionHeader("دزدگیر", "زون‌ها و آژیر")

        GlassCard(Modifier.fillMaxWidth()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp, horizontal = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ArmDial(
                    armed = armed,
                    pending = pending,
                    onClick = {
                        val target = pending ?: (armed != true)
                        viewModel.setSecurity(target)
                    }
                )

                if (armed != null && pending == null) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        RelativeTime.ago(status?.securityAt ?: 0L),
                        style = MaterialTheme.typography.labelSmall,
                        color = c.dim2
                    )
                }

                Spacer(Modifier.height(20.dp))
                Text(
                    "تعداد زون",
                    style = MaterialTheme.typography.labelSmall,
                    color = c.dim
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("غیرفعال" to 0, "تک زون" to 1, "دو زون" to 2).forEach { (label, z) ->
                        SegmentButton(
                            text = label,
                            selected = zones == z,
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.setSecurityZones(z) }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    when (zones) {
                        0 -> "دزدگیر خاموش است"
                        1 -> "فقط ورودی ۱ به‌عنوان زون دزدگیر عمل می‌کند"
                        else -> "ورودی ۱ و ۲ به‌عنوان زون دزدگیر عمل می‌کنند"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = c.dim2
                )
            }
        }

        SectionHeader("رویدادهای اخیر", "${RelativeTime.fa(logs.size)} مورد")
        if (logs.isEmpty()) {
            EmptyHint("هنوز پیامکی رد و بدل نشده است")
        } else {
            logs.take(12).forEach { log ->
                LogRow(
                    incoming = log.direction == LogDirection.IN,
                    body = log.body,
                    age = RelativeTime.ago(log.timestamp)
                )
                Spacer(Modifier.height(8.dp))
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

/**
 * The arm/disarm control. Its ring is the clearest place to show that a command
 * is out but unconfirmed, so it holds an amber spinner until the controller
 * answers rather than flipping straight to "armed".
 */
@Composable
private fun ArmDial(armed: Boolean?, pending: Boolean?, onClick: () -> Unit) {
    val c = Tivan
    val accent = when {
        pending != null -> c.pending
        armed == true -> c.alarm
        else -> c.dim
    }
    val fill by animateColorAsState(
        when {
            pending != null -> c.pending.copy(alpha = 0.18f)
            armed == true -> c.alarm.copy(alpha = 0.22f)
            else -> c.glassStrong
        },
        tween(320), label = "dialFill"
    )

    Box(contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(184.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(fill, fill.copy(alpha = fill.alpha * 0.35f))
                    )
                )
                .border(1.dp, accent.copy(alpha = 0.45f), CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    when {
                        pending != null -> "⏳"
                        armed == true -> "🔒"
                        else -> "🔓"
                    },
                    style = MaterialTheme.typography.displaySmall
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    when {
                        pending != null -> "در انتظار تأیید"
                        armed == true -> "فعال"
                        armed == false -> "غیرفعال"
                        else -> "نامشخص"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = c.text
                )
                Text(
                    when {
                        pending != null -> "پیامک ارسال شد…"
                        armed == true -> "برای غیرفعال کردن لمس کنید"
                        else -> "برای فعال کردن لمس کنید"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = c.dim2
                )
            }
        }
        if (pending != null) {
            val angle by rememberInfiniteTransition(label = "dialRing").animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)),
                label = "dialAngle"
            )
            Canvas(Modifier.size(196.dp)) {
                drawArc(
                    color = c.pending,
                    startAngle = angle,
                    sweepAngle = 100f,
                    useCenter = false,
                    topLeft = Offset(2.dp.toPx(), 2.dp.toPx()),
                    size = Size(size.width - 4.dp.toPx(), size.height - 4.dp.toPx()),
                    style = Stroke(width = 3.dp.toPx())
                )
            }
        }
    }
}

@Composable
fun LogRow(incoming: Boolean, body: String, age: String) {
    val c = Tivan
    GlassCard(Modifier.fillMaxWidth(), corner = 16.dp) {
        Row(Modifier.padding(13.dp)) {
            Box(
                Modifier
                    .padding(top = 5.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (incoming) c.on else c.primary)
            )
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(body, style = MaterialTheme.typography.labelMedium, color = c.text)
                Text(
                    "${if (incoming) "دریافتی" else "ارسالی"} · $age",
                    style = MaterialTheme.typography.labelSmall,
                    color = c.dim2
                )
            }
        }
    }
}

@Composable
fun EmptyHint(text: String) {
    val c = Tivan
    GlassCard(Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 26.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text, style = MaterialTheme.typography.bodySmall, color = c.dim2)
        }
    }
}
