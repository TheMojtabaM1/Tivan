package ir.tivan.controller.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ir.tivan.controller.ui.MainViewModel
import ir.tivan.controller.ui.components.*
import ir.tivan.controller.ui.inputs.SegmentButton
import ir.tivan.controller.ui.security.EmptyHint
import ir.tivan.controller.ui.theme.AppTheme
import ir.tivan.controller.ui.theme.Tivan
import ir.tivan.controller.ui.theme.TivanLayout
import ir.tivan.controller.util.AppPreferences
import ir.tivan.controller.util.RelativeTime
import ir.tivan.controller.util.UiMode

private enum class SettingsTab(val label: String, val emoji: String) {
    Appearance("نما", "🎨"),
    Numbers("شماره‌ها", "👤"),
    Reports("گزارش", "🔔"),
    Outputs("خروجی", "⏱"),
    Thermostat("ترموستات", "🌡"),
    Remote("ریموت", "📻"),
    Advanced("پیشرفته", "🛠")
}

@Composable
fun SettingsScreen(viewModel: MainViewModel, prefs: AppPreferences, header: @Composable () -> Unit) {
    val c = Tivan
    var tab by remember { mutableStateOf(SettingsTab.Numbers) }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            header()
            SectionHeader("تنظیمات", "پیکربندی کامل دستگاه")
        }

        // Horizontal tab strip — six sections is too many for a bottom bar but
        // fits comfortably here, and keeps each page short enough to scan.
        // Shape branches by theme like every other screen: Obsidian drops the
        // pill background for a plain underline, Instrument uses square
        // bordered chips, Linen keeps the rounded pill.
        val layout = TivanLayout
        Row(
            Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(if (layout == AppTheme.OBSIDIAN) 18.dp else 7.dp)
        ) {
            SettingsTab.entries.forEach { t ->
                val sel = t == tab
                when (layout) {
                    AppTheme.OBSIDIAN ->
                        Column(
                            Modifier.clickable { tab = t },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                t.label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (sel) c.text else c.dim2
                            )
                            Spacer(Modifier.height(6.dp))
                            Box(
                                Modifier
                                    .width(22.dp)
                                    .height(2.dp)
                                    .background(if (sel) c.primary else Color.Transparent)
                            )
                        }

                    else -> {
                        val shape = if (layout == AppTheme.INSTRUMENT) RoundedCornerShape(4.dp) else RoundedCornerShape(13.dp)
                        Row(
                            Modifier
                                .clip(shape)
                                .background(if (sel) c.primary.copy(alpha = 0.22f) else c.glassStrong)
                                .border(1.dp, if (sel) c.primary.copy(alpha = 0.5f) else c.stroke, shape)
                                .clickable { tab = t }
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(t.emoji, style = MaterialTheme.typography.labelMedium)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                t.label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (sel) c.text else c.dim2
                            )
                        }
                    }
                }
            }
        }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 14.dp)
        ) {
            when (tab) {
                SettingsTab.Appearance -> AppearanceTab(prefs)
                SettingsTab.Numbers -> NumbersTab(viewModel)
                SettingsTab.Reports -> ReportsTab(viewModel)
                SettingsTab.Outputs -> OutputsTab(viewModel)
                SettingsTab.Thermostat -> ThermostatTab(viewModel)
                SettingsTab.Remote -> RemoteTab(viewModel)
                SettingsTab.Advanced -> AdvancedTab(viewModel)
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

// -------------------------------------------------------------- appearance ---
@Composable
private fun AppearanceTab(prefs: AppPreferences) {
    val c = Tivan
    val theme by prefs.theme.collectAsState()
    val uiMode by prefs.uiMode.collectAsState()

    SettingsGroup("طرح ظاهری") {
        Text(
            "هر سه طرح روی همه‌ی صفحه‌ها اعمال می‌شود و بلافاصله تغییر می‌کند.",
            style = MaterialTheme.typography.labelSmall,
            color = c.dim
        )
        Spacer(Modifier.height(11.dp))
        AppTheme.entries.forEach { t ->
            ThemeRow(theme = t, selected = t == theme, onClick = { prefs.setTheme(t) })
            Spacer(Modifier.height(8.dp))
        }
    }

    Spacer(Modifier.height(10.dp))
    SettingsGroup("حالت نمایش") {
        Text(
            "حالت ساده فقط کنترل خروجی‌ها و دزدگیر را جلوی چشم می‌گذارد؛ بقیه‌ی " +
                "تنظیمات همچنان از همین‌جا در دسترس‌اند. حالت پیشرفته همه‌ی تب‌ها را نشان می‌دهد.",
            style = MaterialTheme.typography.labelSmall,
            color = c.dim
        )
        Spacer(Modifier.height(11.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SegmentButton(
                text = "ساده",
                selected = uiMode == UiMode.SIMPLE,
                modifier = Modifier.weight(1f),
                onClick = { prefs.setUiMode(UiMode.SIMPLE) }
            )
            SegmentButton(
                text = "پیشرفته",
                selected = uiMode == UiMode.ADVANCED,
                modifier = Modifier.weight(1f),
                onClick = { prefs.setUiMode(UiMode.ADVANCED) }
            )
        }
    }
}

@Composable
private fun ThemeRow(theme: AppTheme, selected: Boolean, onClick: () -> Unit) {
    val c = Tivan
    val tokens = ir.tivan.controller.ui.theme.tokensFor(theme)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) c.primary.copy(alpha = 0.14f) else c.glassStrong)
            .border(
                1.dp,
                if (selected) c.primary.copy(alpha = 0.5f) else c.stroke,
                RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // A tiny live swatch of the theme's own palette, so the picker shows
        // what it means rather than just naming it.
        Box(
            Modifier
                .size(36.dp)
                .clip(RoundedCornerShape((tokens.cardCorner.value / 1.6f).dp.coerceIn(3.dp, 16.dp)))
                .background(tokens.bg)
                .border(1.dp, tokens.stroke, RoundedCornerShape((tokens.cardCorner.value / 1.6f).dp.coerceIn(3.dp, 16.dp)))
        ) {
            Box(
                Modifier
                    .padding(6.dp)
                    .size(10.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(tokens.primary)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(theme.label, style = MaterialTheme.typography.titleSmall, color = c.text)
            Text(theme.description, style = MaterialTheme.typography.labelSmall, color = c.dim2)
        }
        if (selected) StatusPill("فعال", c.primary)
    }
}

// ---------------------------------------------------------------- numbers ---
@Composable
private fun NumbersTab(viewModel: MainViewModel) {
    val c = Tivan
    val status by viewModel.status.collectAsState()
    var slot by remember { mutableStateOf("01") }
    var number by remember { mutableStateOf("") }

    SettingsGroup("مدیران فرعی و کاربران") {
        Text(
            "حافظه ۰۱ تا ۰۴ مدیر فرعی و ۰۵ تا ۴۰ کاربر عادی است. مدیران فرعی از تحریک " +
                "ورودی‌ها هم باخبر می‌شوند، کاربران عادی فقط خروجی‌ها را کنترل می‌کنند.",
            style = MaterialTheme.typography.labelSmall,
            color = c.dim
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Box(Modifier.width(84.dp)) {
                LabeledField("حافظه", slot, {
                    slot = it.filter { ch -> ch.isDigit() }.take(2)
                }, "01", KeyboardType.Number)
            }
            Box(Modifier.weight(1f)) {
                LabeledField("شماره موبایل", number, {
                    number = it.filter { ch -> ch.isDigit() }.take(11)
                }, "09xxxxxxxxx", KeyboardType.Phone)
            }
        }
        Spacer(Modifier.height(11.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    viewModel.sendCommand("TEL${slot.padStart(2, '0')}$number")
                    number = ""
                },
                enabled = number.length >= 10 && slot.isNotBlank(),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp)
            ) { Text("ذخیره") }
            OutlinedButton(
                onClick = { viewModel.sendCommand("TEST${slot.padStart(2, '0')}") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp)
            ) { Text("استعلام") }
            OutlinedButton(
                onClick = { viewModel.sendCommand("DELTEL${slot.padStart(2, '0')}") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp)
            ) { Text("حذف") }
        }
    }

    Spacer(Modifier.height(10.dp))
    SettingsGroup("شماره‌های خوانده‌شده") {
        val admins = status?.adminNumbers.orEmpty()
        if (admins.isEmpty()) {
            Text(
                "هنوز پاسخ استعلامی ذخیره نشده. با دکمه «استعلام» شماره هر حافظه را بخواهید.",
                style = MaterialTheme.typography.labelSmall,
                color = c.dim2
            )
        } else {
            admins.forEach {
                Text(it, style = MaterialTheme.typography.bodySmall, color = c.text)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                RelativeTime.ago(status?.adminsAt ?: 0L),
                style = MaterialTheme.typography.labelSmall,
                color = c.dim2
            )
        }
    }

    Spacer(Modifier.height(10.dp))
    DangerRow(
        "پاک کردن همه شماره‌ها",
        "DELALLTEL — شماره مدیر اصلی هم پاک می‌شود و باید دوباره تعریف شود"
    ) { viewModel.sendCommand("DELALLTEL") }
}

// ---------------------------------------------------------------- reports ---
@Composable
private fun ReportsTab(viewModel: MainViewModel) {
    val c = Tivan
    val device by viewModel.selectedDevice.collectAsState()
    val mode = device?.autoReportMode ?: 1

    SettingsGroup("گزارش خودکار") {
        Text(
            "تعیین می‌کند بعد از هر روشن یا خاموش شدن خروجی، گزارش برای چه کسی برود.",
            style = MaterialTheme.typography.labelSmall,
            color = c.dim
        )
        Spacer(Modifier.height(11.dp))
        listOf(
            0 to "بدون گزارش",
            1 to "فقط به کاربر",
            2 to "به کاربر و مدیر اصلی",
            3 to "فقط به مدیر اصلی"
        ).forEach { (value, label) ->
            RadioRow(
                label = label,
                command = "REP$value",
                selected = mode == value,
                onClick = { viewModel.setAutoReport(value) }
            )
        }
    }

    Spacer(Modifier.height(10.dp))
    SettingsGroup("هشدارها") {
        ToggleRow(
            title = "بیزر دستگاه",
            subtitle = "BUZZER1 / BUZZER0 — صدای بوق هنگام فرمان",
            checked = device?.buzzer ?: true,
            onChange = { viewModel.setBuzzer(it) }
        )
        ToggleRow(
            title = "حافظه خروجی بعد از برق",
            subtitle = "MEM1 / MEM0 — حفظ وضعیت خروجی‌ها پس از قطع و وصل برق",
            checked = device?.outputMemory ?: false,
            onChange = { viewModel.setOutputMemory(it) }
        )
    }

    Spacer(Modifier.height(10.dp))
    SettingsGroup("شنود محیط") {
        ActionButton("شروع شنود صدای محیط", "SHONOOD") { viewModel.sendCommand("SHONOOD") }
    }
}

// ---------------------------------------------------------------- outputs ---
@Composable
private fun OutputsTab(viewModel: MainViewModel) {
    val c = Tivan
    val device by viewModel.selectedDevice.collectAsState()
    val channelCount = device?.channelCount ?: 4
    var output by remember { mutableStateOf(1) }
    var minutes by remember { mutableStateOf("10") }

    SettingsGroup("تاخیر در وصل خروجی") {
        Text(
            "ONDT — خروجی پس از مدت تعیین‌شده روشن می‌شود و روشن می‌ماند.\n" +
                "PONDT — خروجی پس از آن مدت فقط یک پالس یک‌ثانیه‌ای می‌زند.",
            style = MaterialTheme.typography.labelSmall,
            color = c.dim
        )
        Spacer(Modifier.height(12.dp))
        Text("خروجی", style = MaterialTheme.typography.labelSmall, color = c.dim)
        Spacer(Modifier.height(7.dp))
        // Wrapped into rows of 4 so 8-channel devices don't squeeze the buttons
        // down to an unreadable width.
        (1..channelCount).chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                row.forEach { n ->
                    SegmentButton(
                        text = RelativeTime.fa(n),
                        selected = output == n,
                        modifier = Modifier.weight(1f),
                        onClick = { output = n }
                    )
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
            Spacer(Modifier.height(7.dp))
        }
        Spacer(Modifier.height(12.dp))
        LabeledField(
            "مدت (۱ تا ۹۹۹ دقیقه)", minutes,
            { minutes = it.filter { ch -> ch.isDigit() }.take(3) },
            "10", KeyboardType.Number
        )
        Spacer(Modifier.height(11.dp))
        val m = minutes.toIntOrNull() ?: 0
        val padded = m.toString().padStart(3, '0')
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { viewModel.sendCommand("ONDT$output$padded") },
                enabled = m in 1..999,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp)
            ) { Text("وصل دائم") }
            OutlinedButton(
                onClick = { viewModel.sendCommand("PONDT$output$padded") },
                enabled = m in 1..999,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp)
            ) { Text("وصل پالسی") }
        }
    }

    Spacer(Modifier.height(10.dp))
    SettingsGroup("کنترل با تک‌زنگ") {
        Text(
            "MISSED — تعیین می‌کند با هر تماس بی‌پاسخ چه دستوری اجرا شود. " +
                "خالی گذاشتن یعنی تماس فقط رد شود.",
            style = MaterialTheme.typography.labelSmall,
            color = c.dim
        )
        Spacer(Modifier.height(11.dp))
        var missed by remember { mutableStateOf("") }
        LabeledField("دستور", missed, { missed = it.uppercase().take(8) }, "41")
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { viewModel.sendCommand("MISSED:$missed") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp)
            ) { Text("ذخیره") }
            OutlinedButton(
                onClick = { viewModel.sendCommand("MISSED:") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp)
            ) { Text("غیرفعال") }
        }
    }
}

// ------------------------------------------------------------- thermostat ---
@Composable
private fun ThermostatTab(viewModel: MainViewModel) {
    val c = Tivan
    SettingsGroup("سنسور دما") {
        ActionButton("خواندن دمای فعلی", "?temp") { viewModel.sendCommand("?temp") }
    }
    Spacer(Modifier.height(10.dp))
    ThermostatBlock(
        title = "ترموستات A — خروجی ۳",
        highCmd = "TEMPHIA",
        lowCmd = "TEMPLOA",
        setCmd = "TEMPSETA",
        viewModel = viewModel
    )
    Spacer(Modifier.height(10.dp))
    ThermostatBlock(
        title = "ترموستات B — خروجی ۴",
        highCmd = "TEMPHIB",
        lowCmd = "TEMPLOB",
        setCmd = "TEMPSETB",
        viewModel = viewModel
    )
}

@Composable
private fun ThermostatBlock(
    title: String,
    highCmd: String,
    lowCmd: String,
    setCmd: String,
    viewModel: MainViewModel
) {
    val c = Tivan
    var high by remember { mutableStateOf("28") }
    var low by remember { mutableStateOf("18") }
    var enabled by remember { mutableStateOf(true) }
    var heating by remember { mutableStateOf(true) }
    var auto by remember { mutableStateOf(true) }
    var sms by remember { mutableStateOf(false) }
    var call by remember { mutableStateOf(false) }

    SettingsGroup(title) {
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Box(Modifier.weight(1f)) {
                LabeledField("حد بالا °C", high, {
                    high = it.filter { ch -> ch.isDigit() || ch == '-' }.take(4)
                }, "28", KeyboardType.Number)
            }
            Box(Modifier.weight(1f)) {
                LabeledField("حد پایین °C", low, {
                    low = it.filter { ch -> ch.isDigit() || ch == '-' }.take(4)
                }, "18", KeyboardType.Number)
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { viewModel.sendCommand("$highCmd$high") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp)
            ) { Text("ارسال حد بالا") }
            OutlinedButton(
                onClick = { viewModel.sendCommand("$lowCmd$low") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp)
            ) { Text("ارسال حد پایین") }
        }

        Spacer(Modifier.height(14.dp))
        ToggleRow("فعال بودن ترموستات", "رقم اول", enabled) { enabled = it }
        ToggleRow(
            if (heating) "حالت گرمایش" else "حالت سرمایش",
            "رقم دوم — ۱ گرمایش، ۰ سرمایش",
            heating
        ) { heating = it }
        ToggleRow("کنترل خودکار خروجی", "رقم سوم", auto) { auto = it }
        ToggleRow("ارسال پیامک در حد دما", "رقم چهارم", sms) { sms = it }
        ToggleRow("تماس در حد دما", "رقم پنجم", call) { call = it }

        val bits = buildString {
            append(if (enabled) 1 else 0)
            append(if (heating) 1 else 0)
            append(if (auto) 1 else 0)
            append(if (sms) 1 else 0)
            append(if (call) 1 else 0)
        }
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = { viewModel.sendCommand("$setCmd$bits") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        ) { Text("ارسال $setCmd$bits") }
    }
}

// ----------------------------------------------------------------- remote ---
@Composable
private fun RemoteTab(viewModel: MainViewModel) {
    val device by viewModel.selectedDevice.collectAsState()
    SettingsGroup("ریموت کنترل ۴۳۳ مگاهرتز") {
        ToggleRow(
            title = "حالت لچ (فلیپ‌فلاپ)",
            subtitle = "REMOTEFF — هر فشار وضعیت را برعکس می‌کند. خاموش یعنی پالسی (REMOTEPL).",
            checked = device?.remoteLatch ?: true,
            onChange = { viewModel.setRemoteLatch(it) }
        )
        ToggleRow(
            title = "مد دزدگیر برای ریموت",
            subtitle = "RMODESE — کلید A فعال و B غیرفعال می‌کند. خاموش یعنی حالت عادی (RMODENO).",
            checked = device?.remoteSecurityMode ?: false,
            onChange = { viewModel.setRemoteSecurityMode(it) }
        )
    }
    Spacer(Modifier.height(10.dp))
    SettingsGroup("راهنمای لرن") {
        InfoLine("لرن ریموت", "کلید LRN را ۳ ثانیه نگه دارید تا BUSY کند بزند، سپس کلید ریموت را بزنید.")
        InfoLine("پاک کردن یک ریموت", "LRN را ۶ ثانیه نگه دارید، سپس کلید همان ریموت را بزنید.")
        InfoLine("پاک کردن همه", "LRN را ۹ ثانیه نگه دارید تا بوق طولانی بزند.")
        InfoLine("مهلت", "اگر ۶۰ ثانیه کلیدی زده نشود، عملیات خودکار لغو می‌شود.")
    }
}

// --------------------------------------------------------------- advanced ---
@Composable
private fun AdvancedTab(viewModel: MainViewModel) {
    val c = Tivan
    var raw by remember { mutableStateOf("") }

    SettingsGroup("استعلام شارژ و کد USSD") {
        Text(
            "این کدها را باید از گوشی خودتان بگیرید یا برای دستگاه پیامک کنید.",
            style = MaterialTheme.typography.labelSmall,
            color = c.dim
        )
        Spacer(Modifier.height(10.dp))
        listOf(
            "همراه اول" to "*140*11#",
            "ایرانسل" to "*141*1#",
            "رایتل" to "*140#"
        ).forEach { (op, code) ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { viewModel.sendCommand(code) }
                    .padding(vertical = 9.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(op, style = MaterialTheme.typography.bodySmall, color = c.text, modifier = Modifier.weight(1f))
                Text(code, style = MaterialTheme.typography.labelMedium, color = c.primary)
            }
        }
    }

    Spacer(Modifier.height(10.dp))
    SettingsGroup("ارسال دستور دلخواه") {
        Text(
            "هر دستور دفترچه را مستقیم بفرستید. دستورات انگلیسی و بدون فاصله‌اند.",
            style = MaterialTheme.typography.labelSmall,
            color = c.dim
        )
        Spacer(Modifier.height(11.dp))
        LabeledField("دستور", raw, { raw = it }, "REPORT")
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = { viewModel.sendCommand(raw.trim()); raw = "" },
            enabled = raw.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        ) { Text("ارسال") }
    }

    Spacer(Modifier.height(10.dp))
    SettingsGroup("سیم‌کارت") {
        InfoLine("زبان سیم‌کارت", "برای کارکرد صحیح باید انگلیسی باشد: همراه اول *198*2# ، ایرانسل *555*4*3*2# ، رایتل *720*7*1*3#")
        InfoLine("بازگشت به تنظیمات کارخانه", "دستگاه را خاموش کنید، LRN را نگه دارید و برق را وصل کنید.")
    }
}

// --------------------------------------------------------------- building ---
@Composable
private fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    val c = Tivan
    GlassCard(Modifier.fillMaxWidth()) {
        Column {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 15.dp, vertical = 13.dp)
            ) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = c.text)
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(c.stroke))
            Column(Modifier.padding(15.dp), content = content)
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    val c = Tivan
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodySmall, color = c.text)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = c.dim2)
        }
        Spacer(Modifier.width(10.dp))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = androidx.compose.ui.graphics.Color.White,
                checkedTrackColor = c.on
            )
        )
    }
}

@Composable
private fun RadioRow(label: String, command: String, selected: Boolean, onClick: () -> Unit) {
    val c = Tivan
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 7.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = c.text, modifier = Modifier.weight(1f))
        Text(command, style = MaterialTheme.typography.labelSmall, color = c.dim2)
    }
}

@Composable
private fun ActionButton(label: String, command: String, onClick: () -> Unit) {
    val c = Tivan
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = c.text, modifier = Modifier.weight(1f))
        StatusPill(command, c.primary)
    }
}

@Composable
private fun DangerRow(title: String, subtitle: String, onClick: () -> Unit) {
    val c = Tivan
    var confirm by remember { mutableStateOf(false) }
    GlassCard(
        Modifier.fillMaxWidth(),
        tint = c.alarm.copy(alpha = 0.1f),
        borderTint = c.alarm.copy(alpha = 0.35f),
        onClick = { confirm = true }
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = c.alarm)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = c.dim2)
            }
        }
    }
    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            containerColor = if (c.dark) androidx.compose.ui.graphics.Color(0xFF141828)
            else androidx.compose.ui.graphics.Color.White,
            title = { Text(title, color = c.text) },
            text = { Text(subtitle, color = c.dim) },
            confirmButton = {
                TextButton(onClick = { onClick(); confirm = false }) {
                    Text("بله، انجام بده", color = c.alarm)
                }
            },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("انصراف") } }
        )
    }
}

@Composable
private fun InfoLine(title: String, body: String) {
    val c = Tivan
    Column(Modifier.padding(vertical = 7.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = c.text)
        Text(body, style = MaterialTheme.typography.labelSmall, color = c.dim2)
    }
}
