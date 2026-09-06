package ir.tivan.controller.ui.outputs

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import ir.tivan.controller.ui.theme.CurrentLayout
import ir.tivan.controller.ui.theme.Tivan
import ir.tivan.controller.ui.theme.TivanLayout
import ir.tivan.controller.util.RelativeTime

@Composable
fun OutputsScreen(viewModel: MainViewModel, header: @Composable () -> Unit) {
    val c = Tivan
    val layout = CurrentLayout
    val outputs by viewModel.outputs.collectAsState()
    val device by viewModel.selectedDevice.collectAsState()
    val status by viewModel.status.collectAsState()
    val pendingSecurity by viewModel.pendingSecurity.collectAsState()
    var renaming by remember { mutableStateOf<Int?>(null) }
    var timerFor by remember { mutableStateOf<Int?>(null) }
    var scheduleFor by remember { mutableStateOf<Int?>(null) }

    val onSet: (Int, Boolean) -> Unit = { i, target -> viewModel.toggleOutput(i, target) }
    val isManager = device?.isManager != false

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

        TinyButton(
            "🔄 بروزرسانی اطلاعات",
            Modifier.fillMaxWidth(),
            onClick = { viewModel.sendCommand("REPORT", "درخواست گزارش ارسال شد") }
        )
        Spacer(Modifier.height(14.dp))

        SectionHeader("خروجی‌ها", "لمس برای روشن یا خاموش")

        when (layout) {
            TivanLayout.CARD ->
                // 2-per-row — a LazyVerticalGrid inside a scrolling column needs a
                // hard height, and up to 8 items never need lazy layout anyway.
                // Row uses IntrinsicSize.Min so both tiles in a pair share one
                // height even when one output's name wraps to a second line.
                for (rowStart in outputs.indices step 2) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(11.dp),
                        modifier = Modifier.height(IntrinsicSize.Min)
                    ) {
                        for (i in rowStart until minOf(rowStart + 2, outputs.size)) {
                            OutputTile(
                                state = outputs[i],
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                onSet = { on -> onSet(i, on) },
                                onRename = if (isManager) { { renaming = i } } else null,
                                onTimer = if (isManager) { { timerFor = i } } else null,
                                onSchedule = if (isManager) { { scheduleFor = i } } else null
                            )
                        }
                        if (outputs.size - rowStart == 1) Spacer(Modifier.weight(1f))
                    }
                    if (rowStart + 2 < outputs.size) Spacer(Modifier.height(11.dp))
                }

            TivanLayout.FLAT ->
                // Borderless hairline list — no cards.
                Column {
                    outputs.forEachIndexed { i, o ->
                        FlatOutputRow(
                            state = o,
                            onSet = { on -> onSet(i, on) },
                            onRename = if (isManager) { { renaming = i } } else null,
                            onTimer = if (isManager) { { timerFor = i } } else null,
                            onSchedule = if (isManager) { { scheduleFor = i } } else null
                        )
                        if (i < outputs.lastIndex) {
                            HorizontalDivider(c.stroke)
                        }
                    }
                }
        }

        SectionHeader("امنیت")
        SecurityTeaser(
            armed = status?.securityArmed,
            pending = pendingSecurity,
            onSet = { target -> viewModel.setSecurity(target) }
        )

        SectionHeader("همه خروجی‌ها")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TinyButton(
                "روشن کردن همه",
                Modifier.weight(1f),
                onClick = { outputs.indices.forEach { viewModel.toggleOutput(it, true) } },
                emphasis = c.on
            )
            TinyButton(
                "خاموش کردن همه",
                Modifier.weight(1f),
                onClick = { outputs.indices.forEach { viewModel.toggleOutput(it, false) } },
                emphasis = c.alarm
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(
            "دستور از طریق پیامک به دستگاه ارسال می‌شود",
            style = MaterialTheme.typography.labelSmall,
            color = c.dim2,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(Modifier.height(24.dp))
    }

    renaming?.let { index ->
        val d = device
        RenameDialog(
            title = "نام و آیکون خروجی ${RelativeTime.fa(index + 1)}",
            hint = "نام دستگاه باید انگلیسی و حداکثر ۱۴ کاراکتر باشد — همین نام در گزارش دستگاه برمی‌گردد و با پیامک روی خود دستگاه هم تغییر می‌کند",
            initialName = d?.outputName(index).orEmpty(),
            initialIcon = d?.outputIcon(index) ?: "🔌",
            initialDisplayName = outputs.getOrNull(index)?.let { if (it.name != it.deviceName) it.name else "" }.orEmpty(),
            maxLength = 14,
            onDismiss = { renaming = null },
            onConfirm = { n, ic, disp ->
                viewModel.renameOutput(index, n, ic)
                d?.let { viewModel.setDisplayName(it.id, true, index, disp) }
                renaming = null
            }
        )
    }

    timerFor?.let { index ->
        TimerDialog(
            outputNumber = index + 1,
            onDismiss = { timerFor = null },
            onSend = { cmd -> viewModel.sendCommand(cmd); timerFor = null }
        )
    }

    scheduleFor?.let { index ->
        ScheduleDialog(
            outputName = outputs.getOrNull(index)?.name ?: "خروجی ${index + 1}",
            schedules = viewModel.schedulesFor(index).collectAsState(initial = emptyList()).value,
            onAdd = { days, sh, sm, eh, em -> viewModel.addSchedule(index, days, sh, sm, eh, em) },
            onDelete = { viewModel.deleteSchedule(it) },
            onDismiss = { scheduleFor = null }
        )
    }
}

/**
 * A tap-to-arm/disarm teaser at the bottom of the home screen, matching the
 * mockup's inline "امنیت" block — full zone management still lives on the
 * dedicated Security tab, but arming from here needs no extra tap.
 */
@Composable
private fun SecurityTeaser(armed: Boolean?, pending: Boolean?, onSet: (Boolean) -> Unit) {
    val c = Tivan
    val layout = CurrentLayout
    val isArmed = armed == true
    val statusText = when {
        pending != null -> "در انتظار تأیید…"
        isArmed -> "فعال"
        else -> "غیرفعال"
    }
    val accent = when {
        pending != null -> c.pending
        isArmed -> c.alarm
        else -> c.dim2
    }
    val bg by animateColorAsState(accent.copy(alpha = if (pending != null) 0.12f else 0.1f), tween(280), label = "secBg")

    val buttons: @Composable () -> Unit = {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            TinyButton(
                "فعال کردن",
                Modifier.weight(1f),
                emphasis = if (isArmed) c.alarm else null,
                onClick = { onSet(true) }
            )
            TinyButton(
                "غیرفعال کردن",
                Modifier.weight(1f),
                emphasis = if (armed == false) c.on else null,
                onClick = { onSet(false) }
            )
        }
    }

    when (layout) {
        TivanLayout.FLAT ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(bg)
                    .border(1.dp, c.stroke)
                    .padding(vertical = 18.dp, horizontal = 18.dp)
            ) {
                Text("سیستم امنیتی", style = MaterialTheme.typography.labelSmall, color = c.dim2)
                Spacer(Modifier.height(8.dp))
                Text(statusText, style = MaterialTheme.typography.headlineSmall, color = c.text)
                Spacer(Modifier.height(14.dp))
                buttons()
            }

        TivanLayout.CARD ->
            GlassCard(Modifier.fillMaxWidth(), tint = bg) {
                Column(Modifier.padding(15.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(accent.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) { Text(if (isArmed) "🔒" else "🔓", style = MaterialTheme.typography.titleMedium) }
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(
                                if (isArmed) "دزدگیر فعال" else "دزدگیر غیرفعال",
                                style = MaterialTheme.typography.titleSmall,
                                color = c.text
                            )
                            Text(statusText, style = MaterialTheme.typography.labelSmall, color = c.dim2)
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    buttons()
                }
            }
    }
}

@Composable
private fun OutputTile(
    state: OutputUi,
    modifier: Modifier = Modifier,
    onSet: (Boolean) -> Unit,
    onRename: (() -> Unit)?,
    onTimer: (() -> Unit)?,
    onSchedule: (() -> Unit)? = null
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
            state.on == false -> c.alarm.copy(alpha = 0.08f)
            else -> c.glass
        },
        tween(280), label = "tileTint"
    )
    val border by animateColorAsState(
        if (state.pending || state.on == true) accent.copy(alpha = 0.42f) else c.stroke,
        tween(280), label = "tileBorder"
    )

    GlassCard(modifier = modifier, tint = tint, borderTint = border) {
        Column(Modifier.padding(14.dp).fillMaxHeight()) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(state.icon, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            state.name,
                            style = MaterialTheme.typography.titleSmall,
                            color = c.text,
                            maxLines = 2
                        )
                        Text(
                            if (state.on != null && !state.pending) RelativeTime.ago(state.updatedAt) else "خروجی",
                            style = MaterialTheme.typography.labelSmall,
                            color = c.dim2
                        )
                    }
                }
                StatusPill(
                    when {
                        state.pending -> "در انتظار"
                        state.on == true -> "روشن"
                        state.on == false -> "خاموش"
                        else -> "نامشخص"
                    },
                    accent
                )
            }
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TinyButton(
                    "روشن کن",
                    Modifier.weight(1f),
                    emphasis = if (state.on == true) c.on else null,
                    onClick = { onSet(true) }
                )
                TinyButton(
                    "خاموش کن",
                    Modifier.weight(1f),
                    emphasis = if (state.on == false) c.alarm else null,
                    onClick = { onSet(false) }
                )
            }
            if (onTimer != null || onRename != null || onSchedule != null) {
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (onTimer != null) TinyButton("⏱", Modifier.weight(1f), onClick = onTimer)
                    if (onSchedule != null) TinyButton("📅", Modifier.weight(1f), onClick = onSchedule)
                    if (onRename != null) TinyButton("✎ نام", Modifier.weight(1f), onClick = onRename)
                }
            }
        }
    }
}

@Composable
private fun FlatOutputRow(
    state: OutputUi,
    onSet: (Boolean) -> Unit,
    onRename: (() -> Unit)?,
    onTimer: (() -> Unit)?,
    onSchedule: (() -> Unit)? = null
) {
    val c = Tivan
    val accent = when {
        state.pending -> c.pending
        state.on == true -> c.on
        else -> c.dim2
    }
    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(state.icon, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(state.name, style = MaterialTheme.typography.titleSmall, color = c.text, maxLines = 2)
                Text(
                    when {
                        state.pending -> "منتظر تأیید…"
                        state.on == true -> "روشن"
                        state.on == false -> "خاموش"
                        else -> "نامشخص"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = accent
                )
            }
            if (onTimer != null) {
                Text(
                    "⏱",
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onTimer)
                        .padding(8.dp),
                    color = c.dim2
                )
            }
            if (onSchedule != null) {
                Text(
                    "📅",
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onSchedule)
                        .padding(8.dp),
                    color = c.dim2
                )
            }
            if (onRename != null) {
                Text(
                    "✎",
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onRename)
                        .padding(8.dp),
                    color = c.dim2
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            TinyButton(
                "روشن کن",
                Modifier.weight(1f),
                emphasis = if (state.on == true) c.on else null,
                onClick = { onSet(true) }
            )
            TinyButton(
                "خاموش کن",
                Modifier.weight(1f),
                emphasis = if (state.on == false) c.alarm else null,
                onClick = { onSet(false) }
            )
        }
    }
}

@Composable
private fun TinyButton(
    text: String,
    modifier: Modifier = Modifier,
    emphasis: androidx.compose.ui.graphics.Color? = null,
    onClick: () -> Unit
) {
    val c = Tivan
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = c.glassStrong,
        border = androidx.compose.foundation.BorderStroke(1.dp, c.stroke)
    ) {
        Box(Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
            Text(text, style = MaterialTheme.typography.labelSmall, color = emphasis ?: c.dim)
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

/**
 * Shared by outputs and inputs: edit the on-device name (sent to the
 * controller by SMS, so it stays ASCII), the icon, and an optional local
 * display name — shown only inside the app, never sent anywhere, so it can
 * be Persian or anything else.
 */
@Composable
fun RenameDialog(
    title: String,
    hint: String,
    initialName: String,
    initialIcon: String,
    initialDisplayName: String = "",
    maxLength: Int,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    val c = Tivan
    var name by remember { mutableStateOf(initialName) }
    var icon by remember { mutableStateOf(initialIcon) }
    var displayName by remember { mutableStateOf(initialDisplayName) }

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
                    "نام روی دستگاه",
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
                LabeledField(
                    "نام نمایشی در برنامه (اختیاری)",
                    displayName,
                    { if (it.length <= 24) displayName = it },
                    "مثلاً «پمپ استخر» — فقط همینجا دیده می‌شود"
                )
                Text(
                    "خالی بگذارید تا همون نام دستگاه نمایش داده شود",
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
            TextButton(onClick = { onConfirm(name, icon, displayName) }) { Text("ذخیره") }
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

private val WEEK_DAYS = listOf(
    java.util.Calendar.SATURDAY to "ش",
    java.util.Calendar.SUNDAY to "ی",
    java.util.Calendar.MONDAY to "د",
    java.util.Calendar.TUESDAY to "س",
    java.util.Calendar.WEDNESDAY to "چ",
    java.util.Calendar.THURSDAY to "پ",
    java.util.Calendar.FRIDAY to "ج"
)

private fun timeLabel(h: Int, m: Int) = "${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}"

private fun daysLabel(mask: Int): String {
    val all = WEEK_DAYS.all { (day, _) -> mask and (1 shl day) != 0 }
    if (all) return "همه روزها"
    return WEEK_DAYS.filter { (day, _) -> mask and (1 shl day) != 0 }.joinToString("، ") { it.second }
}

/**
 * Manages the weekly on/off timers for one output — a list of what's
 * already scheduled, and a form to add another. Each entry covers whichever
 * days are checked with one start/end time; adding several entries with
 * different day sets is how a different time per day of the week is built.
 */
@Composable
private fun ScheduleDialog(
    outputName: String,
    schedules: List<ir.tivan.controller.data.Schedule>,
    onAdd: (days: Int, startHour: Int, startMinute: Int, endHour: Int, endMinute: Int) -> Unit,
    onDelete: (ir.tivan.controller.data.Schedule) -> Unit,
    onDismiss: () -> Unit
) {
    val c = Tivan
    var selectedDays by remember { mutableStateOf(setOf<Int>()) }
    var startHour by remember { mutableStateOf("8") }
    var startMinute by remember { mutableStateOf("0") }
    var endHour by remember { mutableStateOf("18") }
    var endMinute by remember { mutableStateOf("0") }

    fun hh(v: String) = v.toIntOrNull()?.coerceIn(0, 23)
    fun mm(v: String) = v.toIntOrNull()?.coerceIn(0, 59)
    val valid = selectedDays.isNotEmpty() && hh(startHour) != null && mm(startMinute) != null &&
        hh(endHour) != null && mm(endMinute) != null

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (c.dark) androidx.compose.ui.graphics.Color(0xFF141828)
        else androidx.compose.ui.graphics.Color.White,
        title = { Text("زمان‌بندی «$outputName»", style = MaterialTheme.typography.titleMedium, color = c.text) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "برای اجرای درست، گوشی باید روشن، برنامه نصب و آنتن‌دهی داشته باشد — زمان‌بندی از طریق همین گوشی پیامک می‌فرستد.",
                    style = MaterialTheme.typography.labelSmall,
                    color = c.pending
                )
                Spacer(Modifier.height(12.dp))

                if (schedules.isNotEmpty()) {
                    Text("برنامه‌های فعلی", style = MaterialTheme.typography.labelSmall, color = c.dim)
                    Spacer(Modifier.height(6.dp))
                    schedules.forEach { s ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "${timeLabel(s.startHour, s.startMinute)} تا ${timeLabel(s.endHour, s.endMinute)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = c.text
                                )
                                Text(daysLabel(s.days), style = MaterialTheme.typography.labelSmall, color = c.dim2)
                            }
                            TextButton(onClick = { onDelete(s) }) { Text("حذف", color = c.alarm) }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider(c.stroke)
                    Spacer(Modifier.height(10.dp))
                }

                Text("افزودن زمان‌بندی جدید", style = MaterialTheme.typography.labelSmall, color = c.dim)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    WEEK_DAYS.forEach { (day, label) ->
                        val sel = day in selectedDays
                        Box(
                            Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(if (sel) c.primary.copy(alpha = 0.25f) else c.glassStrong)
                                .border(1.dp, if (sel) c.primary else c.stroke, CircleShape)
                                .clickable {
                                    selectedDays = if (sel) selectedDays - day else selectedDays + day
                                },
                            contentAlignment = Alignment.Center
                        ) { Text(label, style = MaterialTheme.typography.labelSmall, color = c.text) }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("ساعت روشن شدن", style = MaterialTheme.typography.labelSmall, color = c.dim)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LabeledField("ساعت (۰-۲۳)", startHour, { startHour = it.filter(Char::isDigit).take(2) }, "8", androidx.compose.ui.text.input.KeyboardType.Number)
                    LabeledField("دقیقه (۰-۵۹)", startMinute, { startMinute = it.filter(Char::isDigit).take(2) }, "0", androidx.compose.ui.text.input.KeyboardType.Number)
                }
                Spacer(Modifier.height(10.dp))
                Text("ساعت خاموش شدن", style = MaterialTheme.typography.labelSmall, color = c.dim)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LabeledField("ساعت (۰-۲۳)", endHour, { endHour = it.filter(Char::isDigit).take(2) }, "18", androidx.compose.ui.text.input.KeyboardType.Number)
                    LabeledField("دقیقه (۰-۵۹)", endMinute, { endMinute = it.filter(Char::isDigit).take(2) }, "0", androidx.compose.ui.text.input.KeyboardType.Number)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = {
                    val mask = selectedDays.sumOf { 1 shl it }
                    onAdd(mask, hh(startHour)!!, mm(startMinute)!!, hh(endHour)!!, mm(endMinute)!!)
                    selectedDays = emptySet()
                }
            ) { Text("افزودن") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("بستن") } }
    )
}
