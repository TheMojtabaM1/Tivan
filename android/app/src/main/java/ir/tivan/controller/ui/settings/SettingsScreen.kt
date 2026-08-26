package ir.tivan.controller.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.tivan.controller.ui.MainViewModel

private enum class SettingsSection(val title: String, val emoji: String) {
    ROOT("تنظیمات پیشرفته", ""),
    USERS("مدیران فرعی و کاربران", "👤"),
    TEMPERATURE("سنسور دما و ترموستات A/B", "🌡"),
    REMOTE("ریموت کنترل", "📡"),
    INPUTS("ورودی‌ها و مدهای تحریک", "🔌"),
    NAMING("پیام تحریک ورودی‌ها", "✎"),
    GENERAL("تنظیمات عمومی", "⚙️"),
    MANUAL("ارسال دستور دستی", "📟"),
    RESET("بازیابی تنظیمات کارخانه", "♻️")
}

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    var section by remember { mutableStateOf(SettingsSection.ROOT) }

    Column(Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (section != SettingsSection.ROOT) {
                IconButton(onClick = { section = SettingsSection.ROOT }) {
                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = "بازگشت")
                }
            }
            Column {
                Text(
                    if (section == SettingsSection.ROOT) "تنظیمات پیشرفته" else "${section.emoji} ${section.title}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                if (section == SettingsSection.ROOT) {
                    Text(
                        "مدیریت کامل دستگاه TIVAN S44T",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Spacer(Modifier.height(14.dp))

        when (section) {
            SettingsSection.ROOT -> RootList(onNavigate = { section = it })
            SettingsSection.USERS -> UsersSection(viewModel)
            SettingsSection.TEMPERATURE -> TemperatureSection(viewModel)
            SettingsSection.REMOTE -> RemoteSection(viewModel)
            SettingsSection.INPUTS -> InputsSection(viewModel)
            SettingsSection.NAMING -> NamingSection(viewModel)
            SettingsSection.GENERAL -> GeneralSection(viewModel)
            SettingsSection.MANUAL -> ManualSection(viewModel)
            SettingsSection.RESET -> ResetSection(viewModel)
        }
        Spacer(Modifier.height(60.dp))
    }
}

@Composable
private fun RootList(onNavigate: (SettingsSection) -> Unit) {
    Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
        Column {
            SettingsSection.values().filter { it != SettingsSection.ROOT }.forEach { s ->
                ListItem(
                    headlineContent = { Text(s.title, fontWeight = FontWeight.SemiBold) },
                    leadingContent = { Text(s.emoji, style = MaterialTheme.typography.titleLarge) },
                    trailingContent = { Icon(Icons.Default.KeyboardArrowLeft, contentDescription = null) },
                    modifier = Modifier.clickable { onNavigate(s) }
                )
                Divider()
            }
        }
    }
}
