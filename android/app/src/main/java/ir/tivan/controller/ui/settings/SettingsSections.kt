package ir.tivan.controller.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.tivan.controller.sms.Commands
import ir.tivan.controller.ui.MainViewModel

@Composable
private fun SectionCard(title: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            if (title != null) {
                Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(10.dp))
            }
            content()
        }
    }
}

@Composable
private fun NumField(label: String, value: String, onChange: (String) -> Unit, allowMinus: Boolean = false) {
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(it.filter { c -> c.isDigit() || (allowMinus && c == '-') }) },
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

// ---------------- Sub-admins / users ----------------
@Composable
fun UsersSection(viewModel: MainViewModel) {
    var slot by remember { mutableStateOf("1") }
    var phone by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionCard("افزودن مدیر فرعی (۱ تا ۴) یا کاربر عادی (۵ تا ۴۰)") {
            NumField("شماره حافظه (۱-۴۰)", slot, { if (it.length <= 2) slot = it })
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = phone, onValueChange = { phone = it.filter(Char::isDigit) },
                label = { Text("شماره موبایل با صفر (مثلا 09123456789)") },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = {
                    val s = slot.toIntOrNull() ?: return@Button
                    if (phone.isNotBlank()) viewModel.sendCommand(Commands.saveNumber(s, phone))
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("ذخیره شماره") }
        }

        SectionCard("خواندن یا پاک کردن حافظه شماره") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = {
                    val s = slot.toIntOrNull() ?: return@OutlinedButton
                    viewModel.sendCommand(Commands.readSlot(s))
                }, modifier = Modifier.weight(1f)) { Text("استعلام") }
                OutlinedButton(onClick = {
                    val s = slot.toIntOrNull() ?: return@OutlinedButton
                    viewModel.sendCommand(Commands.deleteSlot(s))
                }, modifier = Modifier.weight(1f)) { Text("پاک کردن") }
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { viewModel.sendCommand(Commands.deleteAllNumbers()) }) {
                Text("پاک کردن همه شماره‌ها (حتی مدیر اصلی)", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

// ---------------- Temperature / thermostats ----------------
@Composable
fun TemperatureSection(viewModel: MainViewModel) {
    var hiA by remember { mutableStateOf("") }
    var loA by remember { mutableStateOf("") }
    var hiB by remember { mutableStateOf("") }
    var loB by remember { mutableStateOf("") }
    var enabledA by remember { mutableStateOf(true) }
    var heatingA by remember { mutableStateOf(true) }
    var autoA by remember { mutableStateOf(true) }
    var smsA by remember { mutableStateOf(true) }
    var callA by remember { mutableStateOf(false) }
    var enabledB by remember { mutableStateOf(true) }
    var heatingB by remember { mutableStateOf(false) }
    var autoB by remember { mutableStateOf(true) }
    var smsB by remember { mutableStateOf(true) }
    var callB by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionCard("دمای لحظه‌ای") {
            Button(onClick = { viewModel.sendCommand(Commands.readTemperature()) }, modifier = Modifier.fillMaxWidth()) {
                Text("دریافت دمای فعلی محیط (?temp)")
            }
        }

        SectionCard("ترموستات A - خروجی ۳") {
            NumField("حد دمای بالا", hiA, { hiA = it }, allowMinus = true)
            Spacer(Modifier.height(8.dp))
            NumField("حد دمای پایین", loA, { loA = it }, allowMinus = true)
            Spacer(Modifier.height(8.dp))
            Row { OutlinedButton(onClick = { hiA.toIntOrNull()?.let { viewModel.sendCommand(Commands.tempHighA(it)) } }, modifier = Modifier.weight(1f)) { Text("ارسال حد بالا") }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = { loA.toIntOrNull()?.let { viewModel.sendCommand(Commands.tempLowA(it)) } }, modifier = Modifier.weight(1f)) { Text("ارسال حد پایین") } }
            Spacer(Modifier.height(10.dp))
            ThermostatToggles(enabledA, { enabledA = it }, heatingA, { heatingA = it }, autoA, { autoA = it }, smsA, { smsA = it }, callA, { callA = it })
            Spacer(Modifier.height(10.dp))
            Button(onClick = {
                viewModel.sendCommand(Commands.thermostatA(Commands.ThermostatConfig(enabledA, heatingA, autoA, smsA, callA)))
            }, modifier = Modifier.fillMaxWidth()) { Text("اعمال تنظیمات ترموستات A") }
        }

        SectionCard("ترموستات B - خروجی ۴") {
            NumField("حد دمای بالا", hiB, { hiB = it }, allowMinus = true)
            Spacer(Modifier.height(8.dp))
            NumField("حد دمای پایین", loB, { loB = it }, allowMinus = true)
            Spacer(Modifier.height(8.dp))
            Row { OutlinedButton(onClick = { hiB.toIntOrNull()?.let { viewModel.sendCommand(Commands.tempHighB(it)) } }, modifier = Modifier.weight(1f)) { Text("ارسال حد بالا") }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = { loB.toIntOrNull()?.let { viewModel.sendCommand(Commands.tempLowB(it)) } }, modifier = Modifier.weight(1f)) { Text("ارسال حد پایین") } }
            Spacer(Modifier.height(10.dp))
            ThermostatToggles(enabledB, { enabledB = it }, heatingB, { heatingB = it }, autoB, { autoB = it }, smsB, { smsB = it }, callB, { callB = it })
            Spacer(Modifier.height(10.dp))
            Button(onClick = {
                viewModel.sendCommand(Commands.thermostatB(Commands.ThermostatConfig(enabledB, heatingB, autoB, smsB, callB)))
            }, modifier = Modifier.fillMaxWidth()) { Text("اعمال تنظیمات ترموستات B") }
        }
    }
}

@Composable
private fun ThermostatToggles(
    enabled: Boolean, onEnabled: (Boolean) -> Unit,
    heating: Boolean, onHeating: (Boolean) -> Unit,
    auto: Boolean, onAuto: (Boolean) -> Unit,
    sms: Boolean, onSms: (Boolean) -> Unit,
    call: Boolean, onCall: (Boolean) -> Unit
) {
    LabeledSwitch("فعال بودن ترموستات", enabled, onEnabled)
    LabeledSwitch("حالت گرمایش (خاموش=سرمایش)", heating, onHeating)
    LabeledSwitch("حالت اتوماتیک خروجی", auto, onAuto)
    LabeledSwitch("ارسال پیامک در آستانه دما", sms, onSms)
    LabeledSwitch("تماس در آستانه دما", call, onCall)
}

@Composable
private fun LabeledSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

// ---------------- Remote ----------------
@Composable
fun RemoteSection(viewModel: MainViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionCard("حالت عملکرد کلیدها") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { viewModel.sendCommand(Commands.remoteMode(true)) }, modifier = Modifier.weight(1f)) { Text("لچ / فلیپ‌فلاپ") }
                OutlinedButton(onClick = { viewModel.sendCommand(Commands.remoteMode(false)) }, modifier = Modifier.weight(1f)) { Text("لحظه‌ای (پالسی)") }
            }
        }
        SectionCard("مد کارکرد ریموت") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { viewModel.sendCommand(Commands.remoteFunction(false)) }, modifier = Modifier.weight(1f)) { Text("نرمال") }
                OutlinedButton(onClick = { viewModel.sendCommand(Commands.remoteFunction(true)) }, modifier = Modifier.weight(1f)) { Text("دزدگیر (A/B)") }
            }
        }
        SectionCard("لرن / پاک کردن ریموت") {
            Text(
                "برای آموزش یا پاک کردن ریموت باید دکمه LRN روی خود دستگاه فشرده شود (این عملیات از راه دور و با پیامک ممکن نیست).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ---------------- Inputs ----------------
@Composable
fun InputsSection(viewModel: MainViewModel) {
    var input by remember { mutableStateOf(1) }
    var response by remember { mutableStateOf(0) }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionCard("انتخاب ورودی") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (1..4).forEach { i ->
                    FilterChip(selected = input == i, onClick = { input = i }, label = { Text("ورودی $i") })
                }
            }
        }
        SectionCard("مد تحریک ورودی $input") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { viewModel.sendCommand(Commands.inputMode(input, Commands.InputMode.OFF)) }, modifier = Modifier.weight(1f)) { Text("خاموش") }
                OutlinedButton(onClick = { viewModel.sendCommand(Commands.inputMode(input, Commands.InputMode.NO)) }, modifier = Modifier.weight(1f)) { Text("N.O") }
                OutlinedButton(onClick = { viewModel.sendCommand(Commands.inputMode(input, Commands.InputMode.NC)) }, modifier = Modifier.weight(1f)) { Text("N.C") }
            }
        }
        SectionCard("پاسخ به تحریک ورودی $input") {
            val labels = listOf(
                "پیامک", "تماس", "پیامک و تماس",
                "پیامک و روشن خروجی متناظر", "تماس و روشن خروجی متناظر", "پیامک و تماس و روشن خروجی",
                "پیامک و تماس و خروجی ۳ دقیقه", "پیامک و تماس و خروجی ۵ دقیقه",
                "پیامک و تماس و خروجی ۳۰ دقیقه", "پیامک و تماس و خروجی ۶۰ دقیقه"
            )
            labels.forEachIndexed { level, label ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable {
                        response = level
                        viewModel.sendCommand(Commands.inputResponse(input, level))
                    }.padding(vertical = 8.dp)
                ) {
                    RadioButton(selected = response == level, onClick = {
                        response = level
                        viewModel.sendCommand(Commands.inputResponse(input, level))
                    })
                    Spacer(Modifier.width(6.dp))
                    Text(label, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        SectionCard("شنود صدای محیط") {
            Button(onClick = { viewModel.sendCommand(Commands.listen()) }, modifier = Modifier.fillMaxWidth()) {
                Text("درخواست شنود (تماس ۱ دقیقه‌ای)")
            }
        }
    }
}

// ---------------- Naming / trigger messages ----------------
@Composable
fun NamingSection(viewModel: MainViewModel) {
    var input by remember { mutableStateOf(1) }
    var message by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionCard("پیام دلخواه هنگام تحریک ورودی‌ها") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (1..4).forEach { i -> FilterChip(selected = input == i, onClick = { input = i }, label = { Text("ورودی $i") }) }
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = message, onValueChange = { if (it.length <= 24) message = it },
                label = { Text("متن انگلیسی، حداکثر ۲۴ کاراکتر") },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = { if (message.isNotBlank()) viewModel.sendCommand(Commands.triggerMessage(input, message)) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("ذخیره پیام") }
        }
        Text(
            "برای نام‌گذاری خروجی‌ها از صفحه «خروجی‌ها» و دکمه ✎ نام استفاده کنید.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ---------------- General settings ----------------
@Composable
fun GeneralSection(viewModel: MainViewModel) {
    var missedOutput by remember { mutableStateOf("") }
    var delayOutput by remember { mutableStateOf(1) }
    var delayMinutes by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionCard("بیزر دستگاه") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { viewModel.sendCommand(Commands.buzzer(true)) }, modifier = Modifier.weight(1f)) { Text("فعال") }
                OutlinedButton(onClick = { viewModel.sendCommand(Commands.buzzer(false)) }, modifier = Modifier.weight(1f)) { Text("غیرفعال (سایلنت)") }
            }
        }
        SectionCard("حافظه خروجی بعد از قطع برق") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { viewModel.sendCommand(Commands.outputMemory(true)) }, modifier = Modifier.weight(1f)) { Text("حافظه‌دار") }
                OutlinedButton(onClick = { viewModel.sendCommand(Commands.outputMemory(false)) }, modifier = Modifier.weight(1f)) { Text("عادی") }
            }
        }
        SectionCard("حالت ارسال گزارش خودکار") {
            Column {
                listOf(
                    Commands.ReportMode.NONE to "عدم ارسال گزارش",
                    Commands.ReportMode.USER_ONLY to "فقط به کاربر",
                    Commands.ReportMode.USER_AND_ADMIN to "کاربر و مدیر اصلی",
                    Commands.ReportMode.ADMIN_ONLY to "فقط مدیر اصلی"
                ).forEach { (mode, label) ->
                    OutlinedButton(onClick = { viewModel.sendCommand(Commands.autoReport(mode)) }, modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                        Text(label)
                    }
                }
            }
        }
        SectionCard("دستور کنترل با تک‌زنگ (Missed Call)") {
            OutlinedTextField(
                value = missedOutput, onValueChange = { missedOutput = it.filter(Char::isDigit) },
                label = { Text("کد دستور، مثلا 41 یا 401 - خالی=فقط رد تماس") },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = { viewModel.sendCommand(Commands.missedCallSet(missedOutput)) }, modifier = Modifier.fillMaxWidth()) {
                Text("اعمال دستور تک‌زنگ")
            }
        }
        SectionCard("تاخیر در وصل خروجی‌ها") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (1..4).forEach { i -> FilterChip(selected = delayOutput == i, onClick = { delayOutput = i }, label = { Text("خروجی $i") }) }
            }
            Spacer(Modifier.height(8.dp))
            NumField("زمان تاخیر (دقیقه، ۱-۹۹۹)", delayMinutes, { delayMinutes = it })
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    delayMinutes.toIntOrNull()?.let { viewModel.sendCommand(Commands.outputDelayOnMinutes(delayOutput, it)) }
                }, modifier = Modifier.weight(1f)) { Text("تاخیر در وصل دائم") }
                OutlinedButton(onClick = {
                    delayMinutes.toIntOrNull()?.let { viewModel.sendCommand(Commands.outputDelayOnPulseMinutes(delayOutput, it)) }
                }, modifier = Modifier.weight(1f)) { Text("تاخیر در وصل پالسی") }
            }
        }
        SectionCard("راهنمای زبان سیم‌کارت (باید انگلیسی باشد)") {
            Commands.simLangCodes.forEach { (op, code) ->
                Text("$op: $code", style = MaterialTheme.typography.bodySmall)
            }
            Text(
                "این کدها باید از طریق دایلر گوشی و روی خود سیم‌کارت (قبل از نصب در دستگاه) شماره‌گیری شوند.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

// ---------------- Manual raw command ----------------
@Composable
fun ManualSection(viewModel: MainViewModel) {
    var command by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionCard("ارسال هر دستور پیامکی خام") {
            OutlinedTextField(
                value = command, onValueChange = { command = it },
                label = { Text("مثلا REPORT یا TEMPSETA11100") },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = { if (command.isNotBlank()) { viewModel.sendCommand(command.trim()); command = "" } },
                modifier = Modifier.fillMaxWidth()
            ) { Text("ارسال پیامک") }
        }
        Text(
            "برای پیشرفته‌ها: هر دستور دقیقا طبق راهنمای دستگاه TIVAN S44T ارسال می‌شود. فاصله بین دو دستور باید حداقل ۲۰ ثانیه باشد.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ---------------- Factory reset ----------------
@Composable
fun ResetSection(viewModel: MainViewModel) {
    var confirmed by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionCard("بازیابی تنظیمات کارخانه") {
            Text(
                "این عملیات فقط از روی خود دستگاه انجام می‌شود: در حالی که دستگاه خاموش است دکمه LRN را نگه دارید و تغذیه را وصل کنید. تمام تنظیمات از جمله شماره مدیر اصلی پاک خواهد شد.",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(12.dp))
            LabeledSwitch("متوجه شدم و می‌دانم دستگاه ریست کامل می‌شود", confirmed, { confirmed = it })
            Spacer(Modifier.height(10.dp))
            Button(
                enabled = confirmed,
                onClick = { viewModel.sendCommand(Commands.deleteAllNumbers()) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) { Text("پاک کردن همه شماره‌ها از طریق پیامک") }
        }
    }
}
