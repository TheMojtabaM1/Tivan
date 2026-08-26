package ir.tivan.controller.ui.status

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
import ir.tivan.controller.ui.theme.Tivan
import ir.tivan.controller.util.RelativeTime

@Composable
fun StatusScreen(viewModel: MainViewModel, header: @Composable () -> Unit) {
    val c = Tivan
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
        SectionHeader("وضعیت دستگاه", "آخرین داده‌های ذخیره‌شده")

        // Every cached fact carries its own age, because these arrive in
        // separate SMS replies and can be hours apart.
        CachedRow(
            emoji = "📶",
            title = "آنتن‌دهی",
            command = "ANTEN",
            value = st?.antenna ?: "—",
            at = st?.antennaAt ?: 0L,
            onRefresh = { viewModel.sendCommand("ANTEN") }
        )
        Spacer(Modifier.height(9.dp))
        CachedRow(
            emoji = "🌡",
            title = "دمای محیط",
            command = "?temp",
            value = st?.temperature?.let { "$it°C" } ?: "—",
            at = st?.temperatureAt ?: 0L,
            onRefresh = { viewModel.sendCommand("?temp") }
        )
        Spacer(Modifier.height(9.dp))
        CachedRow(
            emoji = "📋",
            title = "گزارش کامل",
            command = "REPORT",
            value = if ((st?.lastReportAt ?: 0L) > 0) "دریافت شده" else "—",
            at = st?.lastReportAt ?: 0L,
            onRefresh = { viewModel.sendCommand("REPORT") }
        )

        SectionHeader("خروجی‌ها", "طبق آخرین گزارش")
        outputs.forEach { o ->
            GlassCard(Modifier.fillMaxWidth(), corner = 16.dp) {
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
            Spacer(Modifier.height(8.dp))
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
private fun CachedRow(
    emoji: String,
    title: String,
    command: String,
    value: String,
    at: Long,
    onRefresh: () -> Unit
) {
    val c = Tivan
    val stale = RelativeTime.isStale(at)
    GlassCard(Modifier.fillMaxWidth(), onClick = onRefresh) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            IconTile(emoji, size = 42.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = c.text)
                Text(command, style = MaterialTheme.typography.labelSmall, color = c.dim2)
            }
            ValueWithAge(
                value = value,
                age = RelativeTime.ago(at),
                stale = stale,
                align = Alignment.End
            )
        }
    }
}
