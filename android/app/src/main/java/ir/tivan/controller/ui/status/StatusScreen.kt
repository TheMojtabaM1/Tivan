package ir.tivan.controller.ui.status

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ir.tivan.controller.data.LogDirection
import ir.tivan.controller.ui.MainViewModel
import ir.tivan.controller.ui.components.*
import ir.tivan.controller.ui.outputs.ActionRow
import ir.tivan.controller.ui.security.EmptyHint
import ir.tivan.controller.ui.security.LogRow
import ir.tivan.controller.ui.theme.AppTheme
import ir.tivan.controller.ui.theme.Tivan
import ir.tivan.controller.ui.theme.TivanLayout
import ir.tivan.controller.util.RelativeTime

@Composable
fun StatusScreen(viewModel: MainViewModel, header: @Composable () -> Unit) {
    val c = Tivan
    val flat = TivanLayout == AppTheme.OBSIDIAN
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
                .border(1.dp, c.stroke, androidx.compose.foundation.shape.RoundedCornerShape(c.cardCorner))
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
                        .border(1.dp, c.stroke, androidx.compose.foundation.shape.RoundedCornerShape(c.cardCorner))
                ) { cellContent() }
            } else {
                GlassCard(Modifier.weight(1f), corner = 10.dp, content = { cellContent() })
            }
        }
    }
}
