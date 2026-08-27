package ir.tivan.controller.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import ir.tivan.controller.ui.theme.Tivan
import ir.tivan.controller.util.SmsPermissions

/**
 * Shown when the SMS permissions are missing.
 *
 * The wording differs by cause: if the system will still prompt, the button
 * asks again; if the permission is blocked — which on a sideloaded build means
 * Android's hard restriction, not a user refusal — asking again would do
 * nothing, so it explains the two routes that actually work instead.
 */
@Composable
fun PermissionBanner(
    blocked: Boolean,
    canSend: Boolean,
    onRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = Tivan
    val context = LocalContext.current
    var showHelp by remember { mutableStateOf(false) }
    val accent = if (blocked) c.alarm else c.pending

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        tint = accent.copy(alpha = 0.13f),
        borderTint = accent.copy(alpha = 0.4f)
    ) {
        Column(Modifier.padding(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconTile(
                    if (blocked) "🚫" else "🔑",
                    size = 40.dp,
                    corner = 13.dp,
                    tint = accent.copy(alpha = 0.18f),
                    borderTint = accent.copy(alpha = 0.4f)
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (blocked) "دسترسی پیامک مسدود است" else "دسترسی پیامک لازم است",
                        style = MaterialTheme.typography.titleSmall,
                        color = c.text
                    )
                    Text(
                        when {
                            blocked && canSend ->
                                "دریافت پیامک اجازه ندارد، پس تأیید خودکار وضعیت کار نمی‌کند."
                            blocked ->
                                "اندروید این دسترسی را برای نصب بیرون از فروشگاه محدود کرده است."
                            else ->
                                "برای ارسال دستور و خواندن پاسخ دستگاه لازم است."
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = c.dim
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (blocked) {
                    Button(
                        onClick = { showHelp = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(13.dp)
                    ) { Text("راه حل") }
                    OutlinedButton(
                        onClick = { SmsPermissions.openAppSettings(context) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(13.dp)
                    ) { Text("تنظیمات") }
                } else {
                    Button(
                        onClick = onRequest,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(13.dp)
                    ) { Text("اجازه بده") }
                }
            }

            if (blocked) {
                Spacer(Modifier.height(9.dp))
                Text(
                    "بدون این دسترسی هم می‌توانید دستور بفرستید — برنامه پیامک گوشی باز " +
                        "می‌شود و خودتان ارسال را می‌زنید.",
                    style = MaterialTheme.typography.labelSmall,
                    color = c.dim2
                )
            }
        }
    }

    if (showHelp) {
        val clipboard = LocalClipboardManager.current
        val adb = SmsPermissions.adbCommand(context.packageName)
        AlertDialog(
            onDismissRequest = { showHelp = false },
            containerColor = if (c.dark) Color(0xFF141828) else Color.White,
            title = {
                Text(
                    "چرا «Allow» خاکستری است؟",
                    style = MaterialTheme.typography.titleMedium,
                    color = c.text
                )
            },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        "از اندروید ۱۰ به بعد، دسترسی پیامک جزو دسترسی‌های «محدودشده» است. " +
                            "وقتی برنامه از بیرون فروشگاه نصب شود، سیستم اجازه‌ی فعال کردن آن " +
                            "را نمی‌دهد و گزینه Allow خاکستری می‌ماند. این ایراد برنامه نیست و " +
                            "از داخل خود برنامه هم قابل رفع نیست.",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.dim
                    )
                    Spacer(Modifier.height(14.dp))
                    Text("راه اول — با کامپیوتر", style = MaterialTheme.typography.titleSmall, color = c.text)
                    Spacer(Modifier.height(5.dp))
                    Text(
                        "گوشی را با USB به کامپیوتر وصل کنید، USB debugging را روشن کنید و " +
                            "این دستورها را اجرا کنید:",
                        style = MaterialTheme.typography.labelSmall,
                        color = c.dim
                    )
                    Spacer(Modifier.height(7.dp))
                    Surface(
                        shape = RoundedCornerShape(11.dp),
                        color = c.glassStrong,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            adb,
                            style = MaterialTheme.typography.labelSmall,
                            color = c.text,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    TextButton(onClick = { clipboard.setText(AnnotatedString(adb)) }) {
                        Text("کپی دستورها")
                    }

                    Spacer(Modifier.height(10.dp))
                    Text("راه دوم — بدون کامپیوتر", style = MaterialTheme.typography.titleSmall, color = c.text)
                    Spacer(Modifier.height(5.dp))
                    Text(
                        "از همین برنامه استفاده کنید بدون دسترسی: هنگام زدن هر دکمه، برنامه " +
                            "پیامک گوشی با متن آماده باز می‌شود و فقط کافی است ارسال را بزنید. " +
                            "در این حالت وضعیت خودکار به‌روز نمی‌شود و باید گزارش دستگاه را " +
                            "خودتان ببینید.",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.dim
                    )
                }
            },
            confirmButton = { TextButton(onClick = { showHelp = false }) { Text("متوجه شدم") } }
        )
    }
}
