package ir.tivan.controller.ui.components

import android.content.Intent
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ir.tivan.controller.data.Device
import ir.tivan.controller.ui.theme.Tivan
import ir.tivan.controller.util.PhoneNumber

/** Compact bar showing which controller the screen is acting on. */
@Composable
fun DeviceBar(
    device: Device?,
    antenna: String?,
    onOpenSwitcher: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = Tivan
    GlassCard(modifier = modifier.fillMaxWidth(), onClick = onOpenSwitcher) {
        Row(
            Modifier.padding(11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconTile(device?.icon ?: "➕", size = 42.dp)
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    device?.name ?: "دستگاهی اضافه نشده",
                    style = MaterialTheme.typography.titleMedium,
                    color = c.text
                )
                Text(
                    device?.phoneNumber ?: "برای افزودن لمس کنید",
                    style = MaterialTheme.typography.labelSmall,
                    color = c.dim
                )
            }
            if (antenna != null) {
                StatusPill(antenna, c.on)
                Spacer(Modifier.width(8.dp))
            }
            Text("▾", style = MaterialTheme.typography.bodyMedium, color = c.dim2)
        }
    }
}

/**
 * Device picker plus the add form.
 *
 * The action row sits outside the scrolling area and carries
 * `navigationBarsPadding()` + `imePadding()`, which is what keeps the buttons
 * clear of the Android gesture bar and the keyboard — the old sheet let them
 * slide underneath both.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceSwitcherSheet(
    devices: List<Device>,
    selectedId: Long?,
    onSelect: (Long) -> Unit,
    onAdd: (name: String, phone: String, icon: String, channelCount: Int) -> Unit,
    onDelete: (Device) -> Unit,
    onDismiss: () -> Unit
) {
    val c = Tivan
    var adding by remember { mutableStateOf(devices.isEmpty()) }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var icon by remember { mutableStateOf("🏠") }
    var channelCount by remember { mutableStateOf(4) }

    val context = LocalContext.current
    val contactLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri = result.data?.data ?: return@rememberLauncherForActivityResult
        context.contentResolver.query(
            uri,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
            ),
            null, null, null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val numberIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                if (numberIdx >= 0) phone = PhoneNumber.normalizeIran(cursor.getString(numberIdx) ?: "")
                if (nameIdx >= 0) cursor.getString(nameIdx)?.let { name = it }
            }
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = if (c.dark) Color(0xFF141828) else Color.White,
        contentColor = c.text,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        // The sheet takes no inset of its own so the footer below can own the
        // bottom edge and apply navigationBarsPadding itself.
        windowInsets = WindowInsets(0)
    ) {
        Column(Modifier.fillMaxWidth()) {
            Column(
                Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                Text(
                    if (adding) "افزودن دستگاه" else "انتخاب دستگاه",
                    style = MaterialTheme.typography.headlineSmall,
                    color = c.text
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    if (adding) "شماره سیم‌کارت داخل کنترلر TIVAN را وارد کنید"
                    else "دستگاه فعال را انتخاب یا دستگاه تازه‌ای اضافه کنید",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.dim
                )
                Spacer(Modifier.height(18.dp))

                if (!adding) {
                    LazyColumn(
                        Modifier.heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(devices, key = { it.id }) { d ->
                            DeviceRow(
                                device = d,
                                selected = d.id == selectedId,
                                onClick = { onSelect(d.id); onDismiss() },
                                onDelete = { onDelete(d) }
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                } else {
                    LabeledField("نام دستگاه", name, { name = it }, "مثلاً ویلا شمال")
                    Spacer(Modifier.height(13.dp))
                    LabeledField(
                        "شماره سیم‌کارت", phone, { phone = it.filter { ch -> ch.isDigit() } },
                        "09xxxxxxxxx", KeyboardType.Phone
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            contactLauncher.launch(
                                Intent(
                                    Intent.ACTION_PICK,
                                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(15.dp)
                    ) { Text("انتخاب از مخاطبین") }
                    Spacer(Modifier.height(13.dp))
                    Text("تعداد کانال دستگاه", style = MaterialTheme.typography.labelSmall, color = c.dim)
                    Spacer(Modifier.height(7.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Device.CHANNEL_OPTIONS.forEach { n ->
                            ChannelOption(
                                count = n,
                                selected = channelCount == n,
                                modifier = Modifier.weight(1f),
                                onClick = { channelCount = n }
                            )
                        }
                    }
                    Spacer(Modifier.height(13.dp))
                    Text("آیکون", style = MaterialTheme.typography.labelSmall, color = c.dim)
                    Spacer(Modifier.height(7.dp))
                    EmojiPicker(
                        options = DEVICE_ICONS,
                        selected = icon,
                        onSelect = { icon = it }
                    )
                    Spacer(Modifier.height(10.dp))
                }
            }

            // Footer: never scrolls, always above the system bars and the keyboard.
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(if (c.dark) Color(0xFF141828) else Color.White)
                    .padding(horizontal = 20.dp)
                    .padding(top = 14.dp)
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(bottom = 14.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (devices.isNotEmpty()) {
                        OutlinedButton(
                            onClick = { adding = !adding },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp)
                        ) { Text(if (adding) "انصراف" else "دستگاه جدید") }
                    }
                    if (adding) {
                        Button(
                            onClick = {
                                if (phone.isNotBlank()) {
                                    onAdd(name, phone, icon, channelCount)
                                    onDismiss()
                                }
                            },
                            enabled = phone.length >= 10,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp)
                        ) { Text("ذخیره") }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceRow(
    device: Device,
    selected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val c = Tivan
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        corner = 18.dp,
        tint = if (selected) c.primary.copy(alpha = 0.18f) else c.glass,
        borderTint = if (selected) c.primary.copy(alpha = 0.45f) else c.stroke,
        onClick = onClick
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconTile(device.icon, size = 40.dp)
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(device.name, style = MaterialTheme.typography.titleSmall, color = c.text)
                Text(
                    device.phoneNumber,
                    style = MaterialTheme.typography.labelSmall,
                    color = c.dim2
                )
            }
            if (selected) StatusPill("فعال", c.primary) else {
                TextButton(onClick = onDelete) {
                    Text("حذف", style = MaterialTheme.typography.labelSmall, color = c.alarm)
                }
            }
        }
    }
}

@Composable
private fun ChannelOption(
    count: Int,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val c = Tivan
    Box(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) c.primary.copy(alpha = 0.22f) else c.glassStrong)
            .border(
                1.dp,
                if (selected) c.primary.copy(alpha = 0.5f) else c.stroke,
                RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "${ir.tivan.controller.util.RelativeTime.fa(count)} کانال",
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) c.text else c.dim2
        )
    }
}

@Composable
fun LabeledField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    val c = Tivan
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = c.dim)
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            placeholder = { Text(placeholder, color = c.dim2) },
            singleLine = true,
            shape = RoundedCornerShape(15.dp),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = c.primary,
                unfocusedBorderColor = c.stroke,
                focusedTextColor = c.text,
                unfocusedTextColor = c.text
            )
        )
    }
}

/** Grid of emoji choices used by the device sheet and the rename dialogs. */
@Composable
fun EmojiPicker(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    perRow: Int = 6
) {
    val c = Tivan
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.chunked(perRow).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { e ->
                    val isSel = e == selected
                    Box(
                        Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(15.dp))
                            .background(if (isSel) c.primary.copy(alpha = 0.24f) else c.glassStrong)
                            .border(
                                1.dp,
                                if (isSel) c.primary.copy(alpha = 0.55f) else c.stroke,
                                RoundedCornerShape(15.dp)
                            )
                            .clickable { onSelect(e) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(e, style = MaterialTheme.typography.titleMedium)
                    }
                }
                repeat(perRow - row.size) { Spacer(Modifier.size(46.dp)) }
            }
        }
    }
}

val DEVICE_ICONS = listOf(
    "🏠", "🏡", "🏭", "🏪", "🏢", "🚜",
    "💧", "🌱", "🅿️", "🔌", "🛖", "⛺"
)
