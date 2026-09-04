package ir.tivan.controller.ui.outputs

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import ir.tivan.controller.data.Device
import ir.tivan.controller.ui.MainViewModel
import ir.tivan.controller.ui.OutputUi
import ir.tivan.controller.ui.components.*
import ir.tivan.controller.ui.theme.Tivan
import ir.tivan.controller.util.RelativeTime

@Composable
fun OutputsScreen(viewModel: MainViewModel, header: @Composable () -> Unit) {
    val c = Tivan
    val outputs by viewModel.outputs.collectAsState()
    val device by viewModel.selectedDevice.collectAsState()
    var renaming by remember { mutableStateOf<Int?>(null) }
    var timerFor by remember { mutableStateOf<Int?>(null) }

    // One scroll container for the whole tab. Every screen does this — the old
    // build nested a fixed-height grid inside a static Column, so anything past
    // the fold was simply unreachable.
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        header()

        SectionHeader("خروجی‌ها", "لمس برای روشن یا خاموش")

        // 2-per-row — a LazyVerticalGrid inside a scrolling column needs a hard
        // height, and up to 8 items never need lazy layout anyway.
        for (rowStart in outputs.indices step 2) {
            Row(horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                for (i in rowStart until minOf(rowStart + 2, outputs.size)) {
                    OutputTile(
                        state = outputs[i],
                        modifier = Modifier.weight(1f),
                        onToggle = {
                            val target = outputs[i].pendingTarget ?: (outputs[i].on != true)
                            viewModel.toggleOutput(i, target)
                        },
                        onRename = { renaming = i },
                        onTimer = { timerFor = i }
                    )
                }
                if (outputs.size - rowStart == 1) Spacer(Modifier.weight(1f))
            }
            if (rowStart + 2 < outputs.size) Spacer(Modifier.height(11.dp))
        }

        SectionHeader("میان‌بر")
        ActionRow(
            "⚡", "خاموش کردن همه خروجی‌ها",
            "ارسال ${outputs.indices.joinToString(" و ") { "${it + 1}0" }}"
        ) {
            outputs.indices.forEach { viewModel.toggleOutput(it, false) }
        }
        Spacer(Modifier.height(9.dp))
        ActionRow("🔄", "درخواست گزارش وضعیت", "ارسال REPORT به دستگاه") {
            viewModel.sendCommand("REPORT")
        }
        Spacer(Modifier.height(9.dp))
        ActionRow("📶", "استعلام آنتن‌دهی", "ارسال ANTEN") {
            viewModel.sendCommand("ANTEN")
        }

        Spacer(Modifier.height(24.dp))
    }

    renaming?.let { index ->
        val d = device
        RenameDialog(
            title = "نام و آیکون خروجی ${RelativeTime.fa(index + 1)}",
            hint = "نام باید انگلیسی و حداکثر ۱۴ کاراکتر باشد — همین نام در گزارش دستگاه برمی‌گردد",
            initialName = d?.outputName(index).orEmpty(),
            initialIcon = d?.outputIcon(index) ?: "🔌",
            maxLength = 14,
            onDismiss = { renaming = null },
            onConfirm = { n, ic -> viewModel.renameOutput(index, n, ic); renaming = null }
        )
    }

    timerFor?.let { index ->
        TimerDialog(
            outputNumber = index + 1,
            onDismiss = { timerFor = null },
            onSend = { cmd -> viewModel.sendCommand(cmd); timerFor = null }
        )
    }
}

@Composable
private fun OutputTile(
    state: OutputUi,
    modifier: Modifier = Modifier,
    onToggle: () -> Unit,
    onRename: () -> Unit,
    onTimer: () -> Unit
) {
    val c = Tivan
    val accent = when {
        state.pending -> c.pending
        state.on == true -> c.on
        else -> c.dim2
    }
    val tint by animateColorAsState(
        when {
            state.pending -> c.pending.copy(alpha = 0.15f)
            state.on == true -> c.on.copy(alpha = 0.17f)
            else -> c.glass
        },
        tween(280), label = "tileTint"
    )
    val border by animateColorAsState(
        if (state.pending || state.on == true) accent.copy(alpha = 0.42f) else c.stroke,
        tween(280), label = "tileBorder"
    )

    GlassCard(modifier = modifier, tint = tint, borderTint = border, onClick = onToggle) {
        Column(Modifier.padding(14.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                IconTile(
                    state.icon,
                    size = 40.dp,
                    corner = 13.dp,
                    tint = if (state.on == true || state.pending) accent.copy(alpha = 0.18f) else c.glassStrong,
                    borderTint = if (state.on == true || state.pending) accent.copy(alpha = 0.4f) else c.stroke
                )
                MiniSwitch(on = state.on == true, pending = state.pending, accent = accent)
            }
            Spacer(Modifier.height(12.dp))
            Text(
                state.name,
                style = MaterialTheme.typography.titleSmall,
                color = c.text,
                maxLines = 1
            )
            Text(
                when {
                    state.pending -> "منتظر تأیید دستگاه…"
                    state.on == true -> "روشن"
                    state.on == false -> "خاموش"
                    else -> "وضعیت نامشخص"
                },
                style = MaterialTheme.typography.labelSmall,
                color = accent
            )
            if (state.on != null && !state.pending) {
                Text(
                    RelativeTime.ago(state.updatedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = c.dim2.copy(alpha = 0.8f)
                )
            }

            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TinyButton("⏱ تایمر", Modifier.weight(1f), onTimer)
                TinyButton("✎ نام", Modifier.weight(1f), onRename)
            }
        }
    }
}

@Composable
private fun MiniSwitch(on: Boolean, pending: Boolean, accent: androidx.compose.ui.graphics.Color) {
    val c = Tivan
    val knob by animateFloatAsState(
        targetValue = when {
            pending -> 0.5f
            on -> 1f
            else -> 0f
        },
        animationSpec = tween(260), label = "knob"
    )
    Box(
        Modifier
            .size(width = 46.dp, height = 27.dp)
            .clip(CircleShape)
            .background(if (on && !pending) accent else accent.copy(alpha = 0.18f))
            .border(1.dp, if (on && !pending) accent else c.stroke, CircleShape)
    ) {
        val travel = 19.dp
        Box(
            Modifier
                .padding(3.dp)
                .offset(x = -(travel * knob))
                .align(Alignment.CenterEnd)
                .size(21.dp)
                .clip(CircleShape)
                .background(if (pending) accent else androidx.compose.ui.graphics.Color.White)
        )
    }
}

@Composable
private fun TinyButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = Tivan
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = c.glassStrong,
        border = androidx.compose.foundation.BorderStroke(1.dp, c.stroke)
    ) {
        Box(Modifier.padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
            Text(text, style = MaterialTheme.typography.labelSmall, color = c.dim)
        }
    }
}

@Composable
fun ActionRow(emoji: String, title: String, subtitle: String, onClick: () -> Unit) {
    val c = Tivan
    GlassCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            IconTile(emoji, size = 42.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = c.text)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = c.dim2)
            }
            StatusPill("اجرا", c.primary)
        }
    }
}

/** Shared by outputs and inputs: edit the label and pick an emoji together. */
@Composable
fun RenameDialog(
    title: String,
    hint: String,
    initialName: String,
    initialIcon: String,
    maxLength: Int,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    val c = Tivan
    var name by remember { mutableStateOf(initialName) }
    var icon by remember { mutableStateOf(initialIcon) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (c.dark) androidx.compose.ui.graphics.Color(0xFF141828)
        else androidx.compose.ui.graphics.Color.White,
        title = { Text(title, style = MaterialTheme.typography.titleMedium, color = c.text) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(hint, style = MaterialTheme.typography.labelSmall, color = c.dim)
                Spacer(Modifier.height(12.dp))
                LabeledField(
                    "نام",
                    name,
                    { if (it.length <= maxLength) name = it },
                    "PUMP"
                )
                Text(
                    "${RelativeTime.fa(name.length)} از ${RelativeTime.fa(maxLength)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = c.dim2,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Spacer(Modifier.height(14.dp))
                Text("آیکون", style = MaterialTheme.typography.labelSmall, color = c.dim)
                Spacer(Modifier.height(8.dp))
                EmojiPicker(Device.ICON_CHOICES, icon, { icon = it })
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name, icon) }) { Text("ذخیره و ارسال") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}

@Composable
private fun TimerDialog(outputNumber: Int, onDismiss: () -> Unit, onSend: (String) -> Unit) {
    val c = Tivan
    var minutes by remember { mutableStateOf(true) }
    var value by remember { mutableStateOf("5") }
    val n = value.toIntOrNull() ?: 0
    val max = if (minutes) 999 else 99
    val valid = n in 1..max

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (c.dark) androidx.compose.ui.graphics.Color(0xFF141828)
        else androidx.compose.ui.graphics.Color.White,
        title = {
            Text(
                "تایمر خروجی ${RelativeTime.fa(outputNumber)}",
                style = MaterialTheme.typography.titleMedium,
                color = c.text
            )
        },
        text = {
            Column {
                Text(
                    "خروجی برای مدت تعیین‌شده روشن می‌ماند و سپس خودکار خاموش می‌شود.",
                    style = MaterialTheme.typography.labelSmall,
                    color = c.dim
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(minutes, { minutes = true }, { Text("دقیقه") })
                    FilterChip(!minutes, { minutes = false }, { Text("ثانیه") })
                }
                Spacer(Modifier.height(12.dp))
                LabeledField(
                    if (minutes) "چند دقیقه؟ (۱ تا ۹۹۹)" else "چند ثانیه؟ (۱ تا ۹۹)",
                    value,
                    { value = it.filter { ch -> ch.isDigit() }.take(3) },
                    "5",
                    androidx.compose.ui.text.input.KeyboardType.Number
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = {
                    // Manual §"روشن کردن تایمر": seconds = "<out><2 digits>",
                    // minutes = "<out><3 digits>".
                    val cmd = if (minutes) "$outputNumber${n.toString().padStart(3, '0')}"
                    else "$outputNumber${n.toString().padStart(2, '0')}"
                    onSend(cmd)
                }
            ) { Text("ارسال") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}
