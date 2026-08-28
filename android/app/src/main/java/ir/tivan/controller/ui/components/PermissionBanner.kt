package ir.tivan.controller.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import ir.tivan.controller.ui.theme.Tivan
import ir.tivan.controller.util.NotificationAccess
import ir.tivan.controller.util.SmsPermissions

/**
 * Shown when the app cannot read controller replies on its own.
 *
 * If the system will still prompt for SMS access, the banner just asks. If it
 * will not — the hard restriction on a sideloaded install, where Settings shows
 * "Allow" permanently greyed out — asking again achieves nothing, so it offers
 * notification access instead: a special permission the user can actually turn
 * on, which lets the app read replies out of the SMS app's notifications.
 */
@Composable
fun PermissionBanner(
    blocked: Boolean,
    notificationAccess: Boolean,
    onRequest: () -> Unit,
    onEnableNotificationAccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = Tivan
    val context = LocalContext.current
    var showHelp by remember { mutableStateOf(false) }

    // With notification access working, replies are being read; all that is
    // left is the extra tap when sending, which is a nudge, not a warning.
    val severity = if (blocked && !notificationAccess) c.alarm else c.pending

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        tint = severity.copy(alpha = 0.13f),
        borderTint = severity.copy(alpha = 0.4f)
    ) {
        Column(Modifier.padding(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconTile(
                    when {
                        !blocked -> "🔑"
                        notificationAccess -> "🔔"
                        else -> "🚫"
                    },
                    size = 40.dp,
                    corner = 13.dp,
                    tint = severity.copy(alpha = 0.18f),
                    borderTint = severity.copy(alpha = 0.4f)
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        when {
                            !blocked -> "دسترسی پیامک لازم است"
                            notificationAccess -> "در حالت خواندن از نوتیفیکیشن"
                            else -> "اندروید دسترسی پیامک را قفل کرده"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        color = c.text
                    )
                    Text(
                        when {
                            !blocked -> "برای ارسال دستور و خواندن پاسخ دستگاه لازم است."
                            notificationAccess ->
                                "پاسخ دستگاه از نوتیفیکیشن برنامه پیامک خوانده می‌شود. " +
                                    "برای ارسال، برنامه پیامک با متن آماده باز می‌شود."
                            else ->
                                "این قفل از داخل Settings باز نمی‌شود. به‌جایش می‌توانید " +
                                    "اجازه دهید برنامه پاسخ‌ها را از نوتیفیکیشن بخواند."
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = c.dim
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            when {
                !blocked -> Button(
                    onClick = onRequest,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(13.dp)
                ) { Text("اجازه بده") }

                !notificationAccess -> {
                    Button(
                        onClick = { showHelp = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(13.dp)
                    ) { Text("چطور فعال کنم؟") }
                    Spacer(Modifier.height(7.dp))
                    OutlinedButton(
                        onClick = { SmsPermissions.openAppSettings(context) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(13.dp)
                    ) { Text("باز کردن تنظیمات برنامه") }
                }

                else -> OutlinedButton(
                    onClick = { showHelp = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(13.dp)
                ) { Text("توضیح و راه‌های دیگر") }
            }

            if (blocked && notificationAccess) {
                Spacer(Modifier.height(9.dp))
                Text(
                    "اگر نوتیفیکیشن آن شماره را بی‌صدا یا پاک کرده باشید، وضعیت به‌روز نمی‌شود.",
                    style = MaterialTheme.typography.labelSmall,
                    color = c.dim2
                )
            }
        }
    }

    if (showHelp) {
        val clipboard = LocalClipboardManager.current
        val install = "adb install -g -r TIVAN-Controller.apk"
        AlertDialog(
            onDismissRequest = { showHelp = false },
            containerColor = if (c.dark) Color(0xFF141828) else Color.White,
            title = {
                Text(
                    "چرا Allow خاکستری است؟",
                    style = MaterialTheme.typography.titleMedium,
                    color = c.text
                )
            },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        "از اندروید ۱۵، دسترسی پیامک برای برنامه‌هایی که از بیرون Google Play " +
                            "نصب شده‌اند قفل می‌شود و گزینه Allow خاکستری می‌ماند. ایراد از " +
                            "برنامه نیست. چهار راه زیر را به ترتیب امتحان کنید — معمولاً " +
                            "همان راه اول کافی است.",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.dim
                    )

                    Spacer(Modifier.height(16.dp))
                    Text(
                        "راه اول — باز کردن قفل در تنظیمات (۳۰ ثانیه)",
                        style = MaterialTheme.typography.titleSmall,
                        color = c.text
                    )
                    Spacer(Modifier.height(8.dp))
                    Step("۱", "روی آیکون برنامه نگه دارید و «App info» را بزنید — یا Settings ← Apps ← TIVAN Controller.")
                    Step("۲", "منوی سه‌نقطه (⋮) بالای صفحه را بزنید.")
                    Step("۳", "گزینه «Allow restricted settings» را بزنید و اگر رمز خواست تأیید کنید.")
                    Step("۴", "به Permissions ← SMS بروید و «Allow» را انتخاب کنید.")
                    Spacer(Modifier.height(4.dp))
                    Text("اگر منو یا گزینه را پیدا نکردید:", style = MaterialTheme.typography.labelMedium, color = c.text)
                    Spacer(Modifier.height(5.dp))
                    Hint("سامسونگ", "به‌جای ⋮ ممکن است «More» نوشته باشد.")
                    Hint("شیائومی و ردمی", "اول Settings ← Privacy protection ← Special permissions ← Install unknown apps را برای این برنامه روشن کنید. در بعضی مدل‌ها این گزینه اصلاً وجود ندارد.")
                    Hint("موتورولا", "اگر ⋮ در صفحه اصلی App info نبود، اول Permissions را باز کنید.")
                    Hint("وان‌پلاس و اوپو", "از مسیر App info ← App details ← Permissions دنبال منو بگردید.")

                    Spacer(Modifier.height(16.dp))
                    Text(
                        "راه دوم — خواندن از نوتیفیکیشن (اگر راه اول جواب نداد)",
                        style = MaterialTheme.typography.titleSmall,
                        color = c.text
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "روی بعضی گوشی‌ها قفل پیامک با راه اول باز نمی‌شود. در این حالت " +
                            "برنامه می‌تواند پاسخ دستگاه را از نوتیفیکیشن برنامه پیامک شما " +
                            "بخواند. دسترسی نوتیفیکیشن این قفل را ندارد و همیشه قابل فعال است.",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.dim
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { showHelp = false; onEnableNotificationAccess() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(13.dp)
                    ) { Text("روشن کردن خواندن از نوتیفیکیشن") }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "محدودیتش: فقط پیام‌هایی را می‌بیند که برنامه پیامک نوتیفیکیشن می‌دهد. " +
                            "اگر آن گفتگو را بی‌صدا کرده باشید، چیزی خوانده نمی‌شود.",
                        style = MaterialTheme.typography.labelSmall,
                        color = c.dim2
                    )

                    Spacer(Modifier.height(16.dp))
                    Text(
                        "راه سوم — یک‌بار نصب با کامپیوتر (مطمئن‌ترین)",
                        style = MaterialTheme.typography.titleSmall,
                        color = c.text
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "با پرچم ‎-g‎ نصب کنید. این پرچم هنگام نصب برنامه را در فهرست مجاز " +
                            "قرار می‌دهد و بعد از آن همه چیز کاملاً خودکار کار می‌کند. " +
                            "USB debugging باید روشن باشد:",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.dim
                    )
                    Spacer(Modifier.height(7.dp))
                    Surface(
                        shape = RoundedCornerShape(11.dp),
                        color = c.glassStrong,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            install,
                            style = MaterialTheme.typography.labelSmall,
                            color = c.text,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    TextButton(onClick = { clipboard.setText(AnnotatedString(install)) }) {
                        Text("کپی دستور")
                    }

                    Spacer(Modifier.height(10.dp))
                    Text("راه چهارم — Shizuku", style = MaterialTheme.typography.titleSmall, color = c.text)
                    Spacer(Modifier.height(5.dp))
                    Text(
                        "اگر کامپیوتر ندارید ولی اندروید ۱۱ یا بالاتر دارید، با اپ Shizuku و " +
                            "Wireless debugging می‌توانید همان دستور بالا را روی خود گوشی اجرا کنید.",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.dim
                    )

                    if (NotificationAccess.isEnabled(context)) {
                        Spacer(Modifier.height(14.dp))
                        TextButton(onClick = { NotificationAccess.openSettings(context) }) {
                            Text("مدیریت دسترسی نوتیفیکیشن")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showHelp = false
                    SmsPermissions.openAppSettings(context)
                }) { Text("تنظیمات برنامه") }
            },
            dismissButton = {
                TextButton(onClick = { showHelp = false }) { Text("بستن") }
            }
        )
    }
}

@Composable
private fun Hint(brand: String, text: String) {
    val c = Tivan
    Column(Modifier.padding(bottom = 7.dp)) {
        Text(brand, style = MaterialTheme.typography.labelMedium, color = c.text)
        Text(text, style = MaterialTheme.typography.labelSmall, color = c.dim2)
    }
}

@Composable
private fun Step(number: String, text: String) {
    val c = Tivan
    Row(Modifier.padding(bottom = 8.dp)) {
        Box(
            Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(c.primary.copy(alpha = 0.22f)),
            contentAlignment = Alignment.Center
        ) {
            Text(number, style = MaterialTheme.typography.labelSmall, color = c.primary)
        }
        Spacer(Modifier.width(9.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = c.dim,
            modifier = Modifier.weight(1f)
        )
    }
}
