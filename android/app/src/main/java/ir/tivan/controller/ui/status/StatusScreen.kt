package ir.tivan.controller.ui.status

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.tivan.controller.data.LogDirection
import ir.tivan.controller.sms.Commands
import ir.tivan.controller.ui.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun StatusScreen(viewModel: MainViewModel) {
    val lastReport by viewModel.lastReportText.collectAsState()
    val logs by viewModel.logs.collectAsState()
    val sdf = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Column(Modifier.padding(16.dp)) {
        Text("وضعیت و گزارش", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "آخرین گزارش‌گیری از دستگاه",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(14.dp))

        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("آخرین متن دریافتی از دستگاه", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Text(lastReport, style = MaterialTheme.typography.bodyMedium)
            }
        }

        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { viewModel.sendCommand(Commands.report()) }, modifier = Modifier.weight(1f)) {
                Text("🔄 گزارش ورودی/خروجی")
            }
            OutlinedButton(onClick = { viewModel.sendCommand(Commands.antenna()) }, modifier = Modifier.weight(1f)) {
                Text("📶 آنتن‌دهی")
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { viewModel.sendCommand(Commands.readTemperature()) }, modifier = Modifier.weight(1f)) {
                Text("🌡 دمای محیط")
            }
            OutlinedButton(onClick = { viewModel.sendCommand(Commands.chargeUssdCodes["ایرانسل"] ?: "*141*1#") }, modifier = Modifier.weight(1f)) {
                Text("💳 استعلام شارژ")
            }
        }

        Spacer(Modifier.height(20.dp))
        Text("تاریخچه پیامک‌ها", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(6.dp))
        if (logs.isEmpty()) {
            Text("هنوز پیامکی رد و بدل نشده", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 12.dp))
        }
        logs.forEach { log ->
            ListItem(
                headlineContent = { Text(log.body, maxLines = 3) },
                supportingContent = { Text(if (log.direction == LogDirection.IN) "دریافتی · ${sdf.format(Date(log.timestamp))}" else "ارسالی · ${sdf.format(Date(log.timestamp))}") },
                leadingContent = { Text(if (log.direction == LogDirection.IN) "📩" else "📤") }
            )
            Divider()
        }
        Spacer(Modifier.height(60.dp))
    }
}
