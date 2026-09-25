package ir.tivan.controller.ui.status

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import ir.tivan.controller.data.LogDirection
import ir.tivan.controller.data.MessageLog
import ir.tivan.controller.ui.MainViewModel
import ir.tivan.controller.ui.OutputUi
import ir.tivan.controller.ui.components.*
import ir.tivan.controller.ui.outputs.ActionRow
import ir.tivan.controller.ui.security.EmptyHint
import ir.tivan.controller.ui.security.LogRow
import ir.tivan.controller.ui.theme.CurrentLayout
import ir.tivan.controller.ui.theme.Tivan
import ir.tivan.controller.ui.theme.TivanLayout
import ir.tivan.controller.util.RelativeTime
import ir.tivan.controller.util.UsageHistory

@Composable
fun StatusScreen(viewModel: MainViewModel, header: @Composable () -> Unit) {
    val c = Tivan
    val flat = CurrentLayout == TivanLayout.FLAT
    val status by viewModel.status.collectAsState()
    val outputs by viewModel.outputs.collectAsState()
    val logs by viewModel.logs.collectAsState()
    val st = status

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        header()
        SectionHeader("وضعیت و گزارش", "آخرین گزارش‌گیری از دستگاه")

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(Modifier.weight(1f), "آنتن‌دهی", st?.antenna ?: "—", good = st?.antenna != null, flat = flat)
            StatTile(Modifier.weight(1f), "دمای محیط", st?.temperature?.let { "$it°C" } ?: "—", flat = flat)
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(
                Modifier.weight(1f), "گزارش کامل",
                if ((st?.lastReportAt ?: 0L) > 0) "دریافت شده" else "—",
                flat = flat
            )
            StatTile(
                Modifier.weight(1f), "آخرین ارتباط",
                if ((st?.lastContactAt ?: 0L) > 0) RelativeTime.ago(st!!.lastContactAt) else "—",
                flat = flat
            )
        }

        SectionHeader("وضعیت خروجی/ورودی لحظه‌ای")
        IoStrip(outputs = outputs, flat = flat)

        Spacer(Modifier.height(14.dp))
        ActionRow("🔄", "بروزرسانی گزارش", "ارسال REPORT به دستگاه") {
            viewModel.sendCommand("REPORT")
        }
        Spacer(Modifier.height(9.dp))
        ActionRow("📶", "تست آنتن", "ارسال ANTEN") {
            viewModel.sendCommand("ANTEN")
        }

        SectionHeader("خروجی‌ها", "طبق آخرین گزارش")
        @Composable
        fun outputRowContent(o: ir.tivan.controller.ui.OutputUi) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                IconTile(o.icon, size = 38.dp, corner = 12.dp)
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Text(o.name, style = MaterialTheme.typography.titleSmall, color = c.text)
                    Text(
                        RelativeTime.ago(o.updatedAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = c.dim2
                    )
                }
                StatusPill(
                    when (o.on) {
                        true -> "روشن"
                        false -> "خاموش"
                        null -> "نامشخص"
                    },
                    when (o.on) {
                        true -> c.on
                        false -> c.dim2
                        null -> c.dim2
                    }
                )
            }
        }
        if (flat) {
            Column {
                outputs.forEachIndexed { i, o ->
                    outputRowContent(o)
                    if (i < outputs.lastIndex) HorizontalDivider(c.stroke)
                }
            }
        } else {
            outputs.forEach { o ->
                GlassCard(Modifier.fillMaxWidth(), corner = 16.dp) { outputRowContent(o) }
                Spacer(Modifier.height(8.dp))
            }
        }

        SectionHeader("تاریخچه مصرف", "ساعت روشن‌بودن خروجی‌ها")
        UsageHistorySection(viewModel = viewModel, outputs = outputs)

        SectionHeader("تاریخچه پیامک", "${RelativeTime.fa(logs.size)} مورد")
        if (logs.isEmpty()) {
            EmptyHint("هنوز پیامکی رد و بدل نشده است")
        } else {
            logs.take(25).forEach { log ->
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
 * Approximate on-time history reconstructed from the outgoing SMS log (see
 * [UsageHistory]): pick a week or a month, see one output's hours per day,
 * and compare every output's total over the same period side by side.
 */
@Composable
private fun UsageHistorySection(viewModel: MainViewModel, outputs: List<OutputUi>) {
    val c = Tivan
    if (outputs.isEmpty()) return
    var days by remember { mutableStateOf(7) }
    var selected by remember { mutableStateOf(0) }
    LaunchedEffect(outputs.size) { if (selected >= outputs.size) selected = 0 }
    val logs by remember(days) { viewModel.usageLogs(days) }.collectAsState(initial = emptyList())

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ir.tivan.controller.ui.inputs.SegmentButton("۷ روز اخیر", days == 7, Modifier.weight(1f)) { days = 7 }
        ir.tivan.controller.ui.inputs.SegmentButton("۳۰ روز اخیر", days == 30, Modifier.weight(1f)) { days = 30 }
    }
    Spacer(Modifier.height(12.dp))

    val perOutput = remember(logs, days, outputs.size) {
        outputs.indices.map { UsageHistory.computeDailyHours(logs, it, days) }
    }
    val totals = perOutput.map { list -> list.sumOf { it.hours.toDouble() }.toFloat() }

    // Compare: every output's total for the period as one horizontal bar each.
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CurrentLayout.cardCorner))
            .background(c.glass)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("مقایسه خروجی‌ها · جمع ساعت روشن", style = MaterialTheme.typography.titleSmall, color = c.text)
        val maxTotal = (totals.maxOrNull() ?: 0f).coerceAtLeast(1f)
        outputs.forEachIndexed { i, o ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { selected = i }
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${o.icon} ${o.name}", style = MaterialTheme.typography.labelLarge, color = c.text, modifier = Modifier.width(110.dp), maxLines = 1)
                Box(
                    Modifier
                        .weight(1f)
                        .height(22.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(c.tileOff)
                ) {
                    Box(
                        Modifier
                            .fillMaxHeight()
                            .fillMaxWidth((totals[i] / maxTotal).coerceIn(0f, 1f))
                            .background(if (i == selected) c.ink else c.tileOn)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    String.format("%.1f س", totals[i]),
                    style = MaterialTheme.typography.labelLarge,
                    color = c.text,
                    modifier = Modifier.width(56.dp)
                )
            }
        }
    }
    Spacer(Modifier.height(12.dp))

    // Daily breakdown for the selected output.
    val daily = perOutput.getOrElse(selected) { emptyList() }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CurrentLayout.cardCorner))
            .background(c.glass)
            .padding(16.dp)
    ) {
        Text(
            "هر روز · ${outputs.getOrNull(selected)?.name.orEmpty()}",
            style = MaterialTheme.typography.titleSmall,
            color = c.text
        )
        Spacer(Modifier.height(10.dp))
        if (daily.none { it.hours > 0.01f }) {
            EmptyHint("در این بازه داده‌ای برای این خروجی ثبت نشده")
        } else {
            val maxHours = daily.maxOf { it.hours }.coerceAtLeast(1f)
            val labelEvery = if (days <= 7) 1 else 5
            Row(
                Modifier.fillMaxWidth().height(140.dp),
                horizontalArrangement = Arrangement.spacedBy(if (days <= 7) 8.dp else 2.dp)
            ) {
                daily.forEachIndexed { i, d ->
                    Column(
                        Modifier.weight(1f).fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        if (days <= 7 && d.hours >= 0.1f) {
                            Text(String.format("%.1f", d.hours), style = MaterialTheme.typography.labelSmall, color = c.dim)
                            Spacer(Modifier.height(4.dp))
                        }
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .fillMaxHeight((d.hours / maxHours).coerceIn(0.03f, 1f) * 0.8f)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(if (d.isToday) c.ink else c.tileOn)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (i % labelEvery == 0 || d.isToday) d.label else "",
                            style = MaterialTheme.typography.labelSmall,
                            color = c.dim2,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatTile(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    good: Boolean = false,
    flat: Boolean = false
) {
    val c = Tivan
    val content: @Composable () -> Unit = {
        Column(Modifier.padding(14.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = c.dim2)
            Spacer(Modifier.height(6.dp))
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                color = if (good) c.on else c.text
            )
        }
    }
    if (flat) {
        Box(
            modifier
                .fillMaxWidth()
                .border(1.dp, c.stroke, androidx.compose.foundation.shape.RoundedCornerShape(CurrentLayout.cardCorner))
        ) { content() }
    } else {
        GlassCard(modifier.fillMaxWidth(), corner = 14.dp, content = { content() })
    }
}

@Composable
private fun IoStrip(outputs: List<ir.tivan.controller.ui.OutputUi>, flat: Boolean = false) {
    val c = Tivan
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        outputs.forEachIndexed { i, o ->
            val cellContent: @Composable () -> Unit = {
                Column(
                    Modifier.padding(vertical = 10.dp, horizontal = 4.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "OUT${i + 1}",
                        style = MaterialTheme.typography.labelSmall,
                        color = c.dim2
                    )
                    Spacer(Modifier.height(6.dp))
                    Box(
                        Modifier
                            .size(12.dp)
                            .background(
                                if (o.on == true) c.on else c.dim2.copy(alpha = 0.35f),
                                androidx.compose.foundation.shape.CircleShape
                            )
                    )
                }
            }
            if (flat) {
                Box(
                    Modifier
                        .weight(1f)
                        .border(1.dp, c.stroke, androidx.compose.foundation.shape.RoundedCornerShape(CurrentLayout.cardCorner))
                ) { cellContent() }
            } else {
                GlassCard(Modifier.weight(1f), corner = 10.dp, content = { cellContent() })
            }
        }
    }
}
