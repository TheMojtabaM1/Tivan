package ir.tivan.controller.ui.security

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.tivan.controller.data.LogDirection
import ir.tivan.controller.sms.Commands
import ir.tivan.controller.ui.MainViewModel

@Composable
fun SecurityScreen(viewModel: MainViewModel) {
    val armed by viewModel.securityArmed.collectAsState()
    val logs by viewModel.logs.collectAsState()
    var zones by remember { mutableStateOf(Commands.SecurityZones.TWO_ZONE) }

    Column(Modifier.padding(16.dp)) {
        Text("حالت دزدگیر", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "مدیریت زون‌ها و وضعیت آلارم",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(14.dp))

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                Modifier.padding(20.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val ringColor = if (armed) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
                Surface(
                    shape = CircleShape,
                    color = ringColor.copy(alpha = 0.12f),
                    border = BorderStroke(2.dp, ringColor),
                    modifier = Modifier.size(76.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text(if (armed) "🛡" else "🔓", style = MaterialTheme.typography.headlineMedium)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    if (armed) "فعال" else "غیرفعال",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        val next = !armed
                        viewModel.setSecurityArmed(next, if (next) Commands.securityOn() else Commands.securityOff())
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (armed) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (armed) "غیرفعال کردن دزدگیر" else "فعال کردن دزدگیر")
                }

                Spacer(Modifier.height(16.dp))
                Text("تعداد زون فعال", style = MaterialTheme.typography.labelMedium, modifier = Modifier.align(Alignment.Start))
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        Commands.SecurityZones.OFF to "غیرفعال",
                        Commands.SecurityZones.ONE_ZONE to "تک زون",
                        Commands.SecurityZones.TWO_ZONE to "دو زون"
                    ).forEach { (z, label) ->
                        FilterChip(
                            selected = zones == z,
                            onClick = { zones = z; viewModel.sendCommand(Commands.securityZones(z)) },
                            label = { Text(label) }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Text("رویدادهای اخیر", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        logs.filter { it.direction == LogDirection.IN }.take(15).forEach { log ->
            ListItem(
                headlineContent = { Text(log.body, maxLines = 2) },
                supportingContent = { Text("دریافتی") },
                leadingContent = { Text("📩") }
            )
            Divider()
        }
        Spacer(Modifier.height(60.dp))
    }
}
