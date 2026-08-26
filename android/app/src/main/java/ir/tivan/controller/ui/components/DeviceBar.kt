package ir.tivan.controller.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.tivan.controller.data.Device

@Composable
fun DeviceTopBar(
    device: Device?,
    onOpenSwitcher: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp, 8.dp)
            .clickable(onClick = onOpenSwitcher)
    ) {
        Row(
            modifier = Modifier.padding(14.dp, 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "${device?.icon ?: "📵"} ${device?.name ?: "دستگاهی انتخاب نشده"}",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = device?.phoneNumber ?: "برای شروع یک دستگاه اضافه کنید",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(Icons.Default.KeyboardArrowLeft, contentDescription = null)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceSwitcherSheet(
    devices: List<Device>,
    selectedId: Long?,
    onSelect: (Long) -> Unit,
    onAdd: (name: String, phone: String) -> Unit,
    onDelete: (Device) -> Unit,
    onDismiss: () -> Unit
) {
    var showAdd by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(16.dp)) {
            Text("انتخاب دستگاه", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                items(devices, key = { it.id }) { device ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .background(
                                if (device.id == selectedId) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                else MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(14.dp)
                            )
                            .clickable { onSelect(device.id); onDismiss() }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("${device.icon} ${device.name}", fontWeight = FontWeight.SemiBold)
                            Text(device.phoneNumber, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (device.id == selectedId) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { onDelete(device) }) {
                                Icon(Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            if (!showAdd) {
                OutlinedButton(onClick = { showAdd = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("افزودن دستگاه جدید")
                }
            } else {
                AddDeviceForm(onAdd = { n, p -> onAdd(n, p); showAdd = false }, onCancel = { showAdd = false })
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun AddDeviceForm(onAdd: (String, String) -> Unit, onCancel: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    Column {
        OutlinedTextField(
            value = name, onValueChange = { name = it },
            label = { Text("نام دستگاه (مثلا: خانه)") }, modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = phone, onValueChange = { phone = it },
            label = { Text("شماره سیم‌کارت دستگاه") }, modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        Row {
            TextButton(onClick = onCancel) { Text("انصراف") }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = { if (name.isNotBlank() && phone.isNotBlank()) onAdd(name, phone) },
                modifier = Modifier.weight(1f)
            ) { Text("افزودن") }
        }
    }
}
