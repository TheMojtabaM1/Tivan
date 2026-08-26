package ir.tivan.controller.ui.outputs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.tivan.controller.sms.Commands
import ir.tivan.controller.ui.MainViewModel
import ir.tivan.controller.ui.OutputState

@Composable
fun OutputsScreen(viewModel: MainViewModel) {
    val outputs by viewModel.outputs.collectAsState()
    var timerDialogIndex by remember { mutableStateOf<Int?>(null) }
    var renameDialogIndex by remember { mutableStateOf<Int?>(null) }

    Column(Modifier.padding(16.dp)) {
        Text("خروجی‌ها", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "روشن، خاموش یا زمان‌دار کنید",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(14.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.heightIn(max = 480.dp)
        ) {
            items(outputs.size) { index ->
                OutputCard(
                    index = index,
                    state = outputs[index],
                    onToggle = { turnOn ->
                        val cmd = if (turnOn) Commands.outputOn(index + 1) else Commands.outputOff(index + 1)
                        viewModel.toggleOutput(index, turnOn, cmd)
                    },
                    onTimerClick = { timerDialogIndex = index },
                    onRenameClick = { renameDialogIndex = index }
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { (1..4).forEach { viewModel.sendCommand(Commands.outputOn(it)) } },
                modifier = Modifier.weight(1f)
            ) { Text("روشن کردن همه") }
            OutlinedButton(
                onClick = { (1..4).forEach { viewModel.sendCommand(Commands.outputOff(it)) } },
                modifier = Modifier.weight(1f)
            ) { Text("خاموش کردن همه") }
        }

        Spacer(Modifier.height(60.dp))
    }

    timerDialogIndex?.let { idx ->
        TimerDialog(
            outputName = outputs[idx].name,
            onDismiss = { timerDialogIndex = null },
            onConfirmSeconds = { s -> viewModel.sendCommand(Commands.outputTimerSeconds(idx + 1, s)); timerDialogIndex = null },
            onConfirmMinutes = { m -> viewModel.sendCommand(Commands.outputTimerMinutes(idx + 1, m)); timerDialogIndex = null }
        )
    }

    renameDialogIndex?.let { idx ->
        RenameDialog(
            currentName = outputs[idx].name,
            onDismiss = { renameDialogIndex = null },
            onConfirm = { newName ->
                viewModel.renameOutput(idx, newName)
                viewModel.sendCommand(Commands.nameOutput(idx + 1, newName))
                renameDialogIndex = null
            }
        )
    }
}

@Composable
private fun OutputCard(
    index: Int,
    state: OutputState,
    onToggle: (Boolean) -> Unit,
    onTimerClick: () -> Unit,
    onRenameClick: () -> Unit
) {
    val borderColor = if (state.on) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(state.name.ifBlank { "خروجی ${index + 1}" }, fontWeight = FontWeight.Bold)
                    Text("خروجی ${index + 1}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                AssistChip(
                    onClick = {},
                    label = { Text(if (state.on) "روشن" else "خاموش", style = MaterialTheme.typography.labelSmall) }
                )
            }
            Spacer(Modifier.height(10.dp))
            Switch(checked = state.on, onCheckedChange = onToggle)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TextButton(onClick = onTimerClick, contentPadding = PaddingValues(4.dp)) { Text("⏱ تایمر", style = MaterialTheme.typography.labelSmall) }
                TextButton(onClick = onRenameClick, contentPadding = PaddingValues(4.dp)) { Text("✎ نام", style = MaterialTheme.typography.labelSmall) }
            }
        }
    }
}

@Composable
private fun TimerDialog(
    outputName: String,
    onDismiss: () -> Unit,
    onConfirmSeconds: (Int) -> Unit,
    onConfirmMinutes: (Int) -> Unit
) {
    var isMinutes by remember { mutableStateOf(false) }
    var value by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تایمر برای $outputName") },
        text = {
            Column {
                Row {
                    FilterChip(selected = !isMinutes, onClick = { isMinutes = false }, label = { Text("ثانیه (۱-۹۹)") })
                    Spacer(Modifier.width(8.dp))
                    FilterChip(selected = isMinutes, onClick = { isMinutes = true }, label = { Text("دقیقه (۱-۹۹۹)") })
                }
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = value, onValueChange = { value = it.filter(Char::isDigit) },
                    label = { Text("مقدار") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val v = value.toIntOrNull() ?: return@TextButton
                if (isMinutes) onConfirmMinutes(v) else onConfirmSeconds(v)
            }) { Text("ارسال") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}

@Composable
private fun RenameDialog(currentName: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("نام‌گذاری خروجی") },
        text = {
            OutlinedTextField(
                value = text, onValueChange = { if (it.length <= 14) text = it },
                label = { Text("نام انگلیسی، حداکثر ۱۴ کاراکتر") }, singleLine = true, modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = { TextButton(onClick = { if (text.isNotBlank()) onConfirm(text) }) { Text("ذخیره") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}
