package ir.tivan.controller.ui.inputs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ir.tivan.controller.ui.InputUi
import ir.tivan.controller.ui.MainViewModel
import ir.tivan.controller.ui.components.*
import ir.tivan.controller.ui.outputs.RenameDialog
import ir.tivan.controller.ui.theme.Tivan
import ir.tivan.controller.util.RelativeTime

private val MODE_LABELS = listOf("خاموش", "N.O", "N.C")

/** SETx0..SETx9 — what the controller does when this input trips. */
private val RESPONSE_LABELS = listOf(
    "فقط پیامک",
    "فقط تماس",
    "پیامک و تماس",
    "پیامک + خروجی ۱",
    "تماس + خروجی ۱",
    "پیامک، تماس + خروجی ۱",
    "پیامک، تماس + خروجی ۱ (۳ دقیقه)",
    "پیامک، تماس + خروجی ۱ (۵ دقیقه)",
    "پیامک، تماس + خروجی ۱ (۳۰ دقیقه)",
    "پیامک، تماس + خروجی ۱ (۶۰ دقیقه)"
)

@Composable
fun InputsScreen(viewModel: MainViewModel, header: @Composable () -> Unit) {
    val c = Tivan
    val inputs by viewModel.inputs.collectAsState()
    val device by viewModel.selectedDevice.collectAsState()
    var editing by remember { mutableStateOf<Int?>(null) }
    var responseFor by remember { mutableStateOf<Int?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        header()
        SectionHeader("ورودی‌ها", "۴ ورودی تحریک")

        inputs.forEach { input ->
            InputCard(
                state = input,
                responseLabel = RESPONSE_LABELS.getOrElse(
                    device?.inputResponses?.getOrNull(input.index) ?: 0
                ) { RESPONSE_LABELS[0] },
                onModeChange = { viewModel.setInputMode(input.index, it) },
                onEditMessage = { editing = input.index },
                onEditResponse = { responseFor = input.index }
            )
            Spacer(Modifier.height(10.dp))
        }

        Spacer(Modifier.height(14.dp))
        Text(
            "متن هر ورودی همان چیزی است که دستگاه هنگام تحریک برای شما پیامک می‌کند. " +
                "برنامه دقیقاً همین متن را می‌شناسد، پس اگر تغییرش دهید تشخیص خودکار هم به‌روز می‌شود.",
            style = MaterialTheme.typography.labelSmall,
            color = c.dim2,
            textAlign = TextAlign.Justify
        )
        Spacer(Modifier.height(24.dp))
    }

    editing?.let { index ->
        RenameDialog(
            title = "پیام و آیکون ورودی ${RelativeTime.fa(index + 1)}",
            hint = "متن باید انگلیسی و حداکثر ۲۴ کاراکتر باشد — دستگاه همین را هنگام تحریک می‌فرستد",
            initialName = device?.inputMessage(index).orEmpty(),
            initialIcon = device?.inputIcon(index) ?: "📥",
            maxLength = 24,
            onDismiss = { editing = null },
            onConfirm = { m, ic -> viewModel.setInputMessage(index, m, ic); editing = null }
        )
    }

    responseFor?.let { index ->
        ResponseDialog(
            inputNumber = index + 1,
            selected = device?.inputResponses?.getOrNull(index) ?: 0,
            onDismiss = { responseFor = null },
            onSelect = { viewModel.setInputResponse(index, it); responseFor = null }
        )
    }
}

@Composable
private fun InputCard(
    state: InputUi,
    responseLabel: String,
    onModeChange: (Int) -> Unit,
    onEditMessage: () -> Unit,
    onEditResponse: () -> Unit
) {
    val c = Tivan
    val triggered = state.triggered == true
    val accent = when {
        triggered -> c.alarm
        state.mode == 0 -> c.dim2
        else -> c.on
    }

    GlassCard(
        Modifier.fillMaxWidth(),
        tint = if (triggered) c.alarm.copy(alpha = 0.14f) else c.glass,
        borderTint = if (triggered) c.alarm.copy(alpha = 0.4f) else c.stroke
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconTile(
                    state.icon,
                    tint = if (triggered) c.alarm.copy(alpha = 0.18f) else c.glassStrong,
                    borderTint = if (triggered) c.alarm.copy(alpha = 0.4f) else c.stroke
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "ورودی ${RelativeTime.fa(state.index + 1)}",
                        style = MaterialTheme.typography.titleSmall,
                        color = c.text
                    )
                    Text(
                        if (state.triggered == null) "هنوز تحریکی ثبت نشده"
                        else "آخرین تحریک: ${RelativeTime.ago(state.updatedAt)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = c.dim2
                    )
                }
                StatusPill(
                    when {
                        triggered -> "تحریک!"
                        state.mode == 0 -> "غیرفعال"
                        else -> "آماده"
                    },
                    accent
                )
            }

            Spacer(Modifier.height(11.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                MODE_LABELS.forEachIndexed { mode, label ->
                    SegmentButton(
                        text = label,
                        selected = state.mode == mode,
                        modifier = Modifier.weight(1f),
                        onClick = { onModeChange(mode) }
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            // The exact text the controller will SMS on trigger — editable, and
            // the same string the parser matches against.
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(13.dp))
                    .background(if (c.dark) c.glassStrong else c.glass)
                    .border(1.dp, c.stroke, RoundedCornerShape(13.dp))
                    .clickable(onClick = onEditMessage)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    state.message,
                    style = MaterialTheme.typography.labelMedium,
                    color = c.dim,
                    modifier = Modifier.weight(1f),
                    maxLines = 1
                )
                Text("ویرایش", style = MaterialTheme.typography.labelSmall, color = c.primary)
            }

            Spacer(Modifier.height(8.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(13.dp))
                    .clickable(onClick = onEditResponse)
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "واکنش دستگاه:",
                    style = MaterialTheme.typography.labelSmall,
                    color = c.dim2
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    responseLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = c.primary,
                    modifier = Modifier.weight(1f)
                )
                Text("›", style = MaterialTheme.typography.bodyMedium, color = c.dim2)
            }
        }
    }
}

@Composable
fun SegmentButton(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val c = Tivan
    Box(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) c.primary.copy(alpha = 0.22f) else c.glassStrong)
            .border(
                1.dp,
                if (selected) c.primary.copy(alpha = 0.5f) else c.stroke,
                RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) c.text else c.dim2
        )
    }
}

@Composable
private fun ResponseDialog(
    inputNumber: Int,
    selected: Int,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit
) {
    val c = Tivan
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (c.dark) androidx.compose.ui.graphics.Color(0xFF141828)
        else androidx.compose.ui.graphics.Color.White,
        title = {
            Text(
                "واکنش به تحریک ورودی ${RelativeTime.fa(inputNumber)}",
                style = MaterialTheme.typography.titleMedium,
                color = c.text
            )
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "دستور SET${RelativeTime.fa(inputNumber)}x به دستگاه ارسال می‌شود.",
                    style = MaterialTheme.typography.labelSmall,
                    color = c.dim
                )
                Spacer(Modifier.height(10.dp))
                RESPONSE_LABELS.forEachIndexed { level, label ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelect(level) }
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = level == selected, onClick = { onSelect(level) })
                        Spacer(Modifier.width(6.dp))
                        Text(
                            label,
                            style = MaterialTheme.typography.bodySmall,
                            color = c.text,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "SET$inputNumber$level",
                            style = MaterialTheme.typography.labelSmall,
                            color = c.dim2
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("بستن") } }
    )
}
