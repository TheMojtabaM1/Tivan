package ir.tivan.controller.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ir.tivan.controller.ui.components.GlassCard
import ir.tivan.controller.ui.components.IconTile
import ir.tivan.controller.ui.theme.Tivan
import ir.tivan.controller.util.UiMode

/**
 * Asked once, the very first time the app is opened. The choice only decides
 * how much shows up front — every setting stays reachable from Settings
 * either way, and this can be changed there later.
 */
@Composable
fun OnboardingScreen(onChoose: (UiMode) -> Unit) {
    val c = Tivan
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(c.bg, c.bg2, c.bg)))
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "چه‌جور شروع کنیم؟",
                style = MaterialTheme.typography.headlineMedium,
                color = c.text
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "هر وقت خواستید از تنظیمات می‌توانید این را عوض کنید.",
                style = MaterialTheme.typography.bodySmall,
                color = c.dim
            )
            Spacer(Modifier.height(28.dp))

            ModeCard(
                emoji = "🏠",
                title = "ساده",
                desc = "فقط کنترل خروجی‌ها و دزدگیر. بقیه‌ی تنظیمات هم زیر همان یک تب تنظیمات می‌مانند.",
                onClick = { onChoose(UiMode.SIMPLE) }
            )
            Spacer(Modifier.height(14.dp))
            ModeCard(
                emoji = "🛠",
                title = "پیشرفته",
                desc = "همه‌چیز از اول باز است: ورودی‌ها، وضعیت زنده‌ی دستگاه و تنظیمات کامل.",
                onClick = { onChoose(UiMode.ADVANCED) }
            )
        }
    }
}

@Composable
private fun ModeCard(emoji: String, title: String, desc: String, onClick: () -> Unit) {
    val c = Tivan
    GlassCard(Modifier.fillMaxWidth(), corner = 22.dp, onClick = onClick) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            IconTile(emoji, size = 48.dp, corner = 16.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge, color = c.text)
                Spacer(Modifier.height(4.dp))
                Text(
                    desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = c.dim,
                    textAlign = TextAlign.Start
                )
            }
        }
    }
}
