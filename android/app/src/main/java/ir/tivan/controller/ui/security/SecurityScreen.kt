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
import androidx.compose.material3.Surface
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
import ir.tivan.controller.ui.theme.CurrentLayout
import ir.tivan.controller.ui.theme.Tivan
import ir.tivan.controller.ui.theme.TivanLayout
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
    val isArmed = armed == true
    val stateFill by animateColorAsState(
        when {
            pending != null -> c.pending
            isArmed -> c.alarm
            else -> c.ink
        },
        tween(320), label = "secFill"
    )
    val stateInk = if (pending != null || isArmed) androidx.compose.ui.graphics.Color.White else c.onInk

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        header()
        SectionHeader("دزدگیر", "رنگ کاشی = وضعیت دزدگیر")

        // The state itself as one big tile: dark when off, red when armed,
        // amber while the device hasn't confirmed yet.
        Column(
            Modifier
                .fillMaxWidth()
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(CurrentLayout.cardCorner))
                .background(stateFill)
                .padding(22.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    when {
                        pending != null -> "⏳"
                        isArmed -> "🔒"
                        else -> "🔓"
                    },
                    style = MaterialTheme.typography.displaySmall
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("سیستم دزدگیر", style = MaterialTheme.typography.labelLarge, color = stateInk.copy(alpha = 0.75f))
                    Text(
                        when {
                            pending != null -> "منتظر تأیید دستگاه"
                            isArmed -> "فعال است"
                            armed == false -> "غیرفعال است"
                            else -> "وضعیت نامشخص"
                        },
                        style = MaterialTheme.typography.headlineMedium,
                        color = stateInk
                    )
                    if (armed != null && pending == null) {
                        Text(
                            "آخرین گزارش: " + RelativeTime.ago(status?.securityAt ?: 0L),
                            style = MaterialTheme.typography.labelMedium,
                            color = stateInk.copy(alpha = 0.75f)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BigActionTile(
                emoji = "🔒",
                text = "فعال کن",
                selected = isArmed,
                selectedFill = c.alarm,
                modifier = Modifier.weight(1f),
                enabled = pending == null
            ) { viewModel.setSecurity(true) }
            BigActionTile(
                emoji = "🔓",
                text = "غیرفعال کن",
                selected = armed == false,
                selectedFill = c.ink,
                modifier = Modifier.weight(1f),
                enabled = pending == null
            ) { viewModel.setSecurity(false) }
        }

        SectionHeader("زون‌ها", "کدام ورودی‌ها دزدگیر باشند")
        GlassCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("خاموش" to 0, "تک زون" to 1, "دو زون" to 2).forEach { (label, z) ->
                        SegmentButton(
                            text = label,
                            selected = zones == z,
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.setSecurityZones(z) }
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    when (zones) {
                        0 -> "دزدگیر خاموش است و هیچ ورودی آژیر را فعال نمی‌کند"
                        1 -> "فقط ورودی ۱ به‌عنوان زون دزدگیر عمل می‌کند"
                        else -> "ورودی ۱ و ۲ به‌عنوان زون دزدگیر عمل می‌کنند"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = c.dim
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

/** A large square-ish action tile: emoji over a label, filled when it's the current state. */
@Composable
private fun BigActionTile(
    emoji: String,
    text: String,
    selected: Boolean,
    selectedFill: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val c = Tivan
    val ink = if (selected) androidx.compose.ui.graphics.Color.White.takeIf { selectedFill != c.ink } ?: c.onInk else c.text
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(96.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
        color = if (selected) selectedFill else c.tileOff
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(emoji, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(4.dp))
            Text(text, style = MaterialTheme.typography.titleSmall, color = ink)
        }
    }
}

@Composable
fun LogRow(incoming: Boolean, body: String, age: String) {
    val c = Tivan
    GlassCard(Modifier.fillMaxWidth(), corner = 16.dp) {
        Row(Modifier.padding(14.dp)) {
            Box(
                Modifier
                    .padding(top = 5.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (incoming) c.on else c.primary)
            )
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(body, style = MaterialTheme.typography.titleSmall, color = c.text)
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
            Text(text, style = MaterialTheme.typography.bodyMedium, color = c.dim)
        }
    }
}
