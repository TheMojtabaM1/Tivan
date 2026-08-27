package ir.tivan.controller

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import ir.tivan.controller.ui.MainViewModel
import ir.tivan.controller.ui.components.*
import ir.tivan.controller.ui.inputs.InputsScreen
import ir.tivan.controller.ui.outputs.OutputsScreen
import ir.tivan.controller.ui.security.SecurityScreen
import ir.tivan.controller.ui.settings.SettingsScreen
import ir.tivan.controller.ui.status.StatusScreen
import ir.tivan.controller.ui.theme.Tivan
import ir.tivan.controller.ui.theme.TivanTheme
import ir.tivan.controller.util.RelativeTime
import ir.tivan.controller.util.SmsPermissions
import kotlinx.coroutines.delay

private enum class Tab(val label: String, val emoji: String) {
    Outputs("خروجی", "⚡"),
    Inputs("ورودی", "📥"),
    Security("دزدگیر", "🛡"),
    Status("وضعیت", "📊"),
    Settings("تنظیمات", "⚙")
}

class MainActivity : ComponentActivity() {

    private val permissionState = mutableStateOf(SmsPermissions.State.Askable)

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            SmsPermissions.markAsked(this)
            refreshPermissionState()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Draw behind the system bars so the gradient reaches the edges; every
        // interactive surface then re-applies its own inset padding.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        refreshPermissionState()
        if (!SmsPermissions.hasAsked(this)) requestSmsPermissions()

        setContent {
            TivanTheme {
                RootScreen(
                    permissionState = permissionState.value,
                    onRequestPermissions = ::requestSmsPermissions
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // The user may have granted it in Settings while we were backgrounded.
        refreshPermissionState()
    }

    private fun refreshPermissionState() {
        permissionState.value = SmsPermissions.state(this)
    }

    private fun requestSmsPermissions() {
        SmsPermissions.markAsked(this)
        val wanted = buildList {
            addAll(SmsPermissions.ALL)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        val missing = wanted.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) refreshPermissionState()
        else permissionLauncher.launch(missing.toTypedArray())
    }
}

@Composable
private fun RootScreen(
    permissionState: SmsPermissions.State,
    onRequestPermissions: () -> Unit,
    viewModel: MainViewModel = viewModel()
) {
    val c = Tivan
    val context = LocalContext.current
    var tab by rememberSaveable { mutableStateOf(Tab.Outputs) }
    var sheetOpen by rememberSaveable { mutableStateOf(false) }

    // Tabs the user visited, so the system back gesture walks back through them
    // instead of dropping straight out of the app.
    val history = rememberSaveable(
        saver = listSaver<SnapshotStateList<Tab>, String>(
            save = { list -> list.map { it.name } },
            restore = { saved -> saved.map { Tab.valueOf(it) }.toMutableStateList() }
        )
    ) { mutableStateListOf(Tab.Outputs) }

    val devices by viewModel.devices.collectAsState()
    val device by viewModel.selectedDevice.collectAsState()
    val status by viewModel.status.collectAsState()
    val outputs by viewModel.outputs.collectAsState()
    val pendingSecurity by viewModel.pendingSecurity.collectAsState()
    val alarm by viewModel.alarm.collectAsState()

    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        viewModel.toast.collect { snackbar.showSnackbar(it.text) }
    }
    // Let a red hero settle back down on its own so it can't stick forever.
    LaunchedEffect(alarm) {
        if (alarm != null) {
            delay(30_000)
            viewModel.clearAlarm()
        }
    }

    fun goTo(next: Tab) {
        if (next == tab) return
        history.add(next)
        tab = next
    }

    // Back closes the sheet first, then unwinds the tab history, and only falls
    // through to the system (leaving the app) once we're back on the first tab.
    BackHandler(enabled = sheetOpen || history.size > 1) {
        when {
            sheetOpen -> sheetOpen = false
            history.size > 1 -> {
                history.removeAt(history.lastIndex)
                tab = history.last()
            }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(c.bg, c.bg2, c.bg)
                )
            )
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = {
                SnackbarHost(snackbar) { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = if (c.dark) Color(0xFF1B2036) else Color(0xFF2A3050),
                        contentColor = Color.White,
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            },
            bottomBar = {
                BottomBar(current = tab, onSelect = ::goTo)
            }
        ) { padding ->
            Box(
                Modifier
                    .fillMaxSize()
                    // Consume only the top/bottom the Scaffold reports; each
                    // screen scrolls freely inside this box.
                    .padding(top = padding.calculateTopPadding())
                    .padding(bottom = padding.calculateBottomPadding())
            ) {
                if (devices.isEmpty()) {
                    EmptyState(onAdd = { sheetOpen = true })
                } else {
                    val header: @Composable () -> Unit = {
                        Column {
                            Spacer(Modifier.height(6.dp))
                            if (permissionState != SmsPermissions.State.Granted) {
                                PermissionBanner(
                                    blocked = permissionState == SmsPermissions.State.Blocked,
                                    canSend = SmsPermissions.canSendDirectly(context),
                                    onRequest = onRequestPermissions
                                )
                                Spacer(Modifier.height(11.dp))
                            }
                            DeviceBar(
                                device = device,
                                antenna = status?.antenna,
                                onOpenSwitcher = { sheetOpen = true }
                            )
                            Spacer(Modifier.height(13.dp))
                            HeroFor(
                                tab = tab,
                                alarmInput = alarm,
                                pendingOutputs = outputs.filter { it.pending },
                                pendingSecurity = pendingSecurity,
                                securityArmed = status?.securityArmed,
                                onCount = outputs.count { it.on == true },
                                antenna = status?.antenna,
                                antennaAt = status?.antennaAt ?: 0L,
                                temperature = status?.temperature,
                                temperatureAt = status?.temperatureAt ?: 0L,
                                lastContactAt = status?.lastContactAt ?: 0L
                            )
                        }
                    }

                    when (tab) {
                        Tab.Outputs -> OutputsScreen(viewModel, header)
                        Tab.Inputs -> InputsScreen(viewModel, header)
                        Tab.Security -> SecurityScreen(viewModel, header)
                        Tab.Status -> StatusScreen(viewModel, header)
                        Tab.Settings -> SettingsScreen(viewModel, header)
                    }
                }
            }
        }
    }

    if (sheetOpen) {
        DeviceSwitcherSheet(
            devices = devices,
            selectedId = device?.id,
            onSelect = { viewModel.selectDevice(it) },
            onAdd = { n, p, i -> viewModel.addDevice(n, p, i) },
            onDelete = { viewModel.deleteDevice(it) },
            onDismiss = { sheetOpen = false }
        )
    }
}

@Composable
private fun HeroFor(
    tab: Tab,
    alarmInput: Int?,
    pendingOutputs: List<ir.tivan.controller.ui.OutputUi>,
    pendingSecurity: Boolean?,
    securityArmed: Boolean?,
    onCount: Int,
    antenna: String?,
    antennaAt: Long,
    temperature: String?,
    temperatureAt: Long,
    lastContactAt: Long
) {
    val mood: HeroMood
    val emoji: String
    val title: String
    val subtitle: String

    when {
        alarmInput != null -> {
            mood = HeroMood.Alarm
            emoji = "🚨"
            title = "آژیر فعال شد!"
            subtitle = "تحریک ورودی ${RelativeTime.fa(alarmInput + 1)}"
        }
        pendingSecurity != null -> {
            mood = HeroMood.Pending
            emoji = "⏳"
            title = "منتظر تأیید دزدگیر"
            subtitle = "دستور ارسال شد — تا رسیدن پاسخ دستگاه قطعی نیست"
        }
        pendingOutputs.isNotEmpty() -> {
            mood = HeroMood.Pending
            emoji = "⏳"
            title = "منتظر تأیید ${pendingOutputs.first().name}"
            subtitle = "دستور ارسال شد — وضعیت تا رسیدن پیامک دستگاه قطعی نیست"
        }
        securityArmed == true -> {
            mood = HeroMood.Normal
            emoji = "🔒"
            title = "دزدگیر فعال است"
            subtitle = "آخرین تماس با دستگاه: ${RelativeTime.ago(lastContactAt)}"
        }
        else -> {
            mood = HeroMood.Normal
            emoji = "🛡"
            title = "همه چیز عادی است"
            subtitle = "آخرین تماس با دستگاه: ${RelativeTime.ago(lastContactAt)}"
        }
    }

    AdaptiveHero(
        mood = mood,
        emoji = emoji,
        title = title,
        subtitle = subtitle,
        stats = listOf(
            HeroStat("خروجی فعال", "${RelativeTime.fa(onCount)} از ۴"),
            HeroStat(
                "دزدگیر",
                when (securityArmed) {
                    true -> "فعال"
                    false -> "غیرفعال"
                    null -> "—"
                }
            ),
            HeroStat("آنتن", antenna ?: "—", RelativeTime.ago(antennaAt).takeIf { antennaAt > 0 }),
            HeroStat(
                "دما",
                temperature?.let { "$it°" } ?: "—",
                RelativeTime.ago(temperatureAt).takeIf { temperatureAt > 0 }
            )
        )
    )
}

@Composable
private fun BottomBar(current: Tab, onSelect: (Tab) -> Unit) {
    val c = Tivan
    Column(
        Modifier
            .fillMaxWidth()
            .background(if (c.dark) Color(0xE60A0C16) else Color(0xF2FFFFFF))
    ) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(c.stroke))
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Tab.entries.forEach { t ->
                val sel = t == current
                Column(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (sel) c.primary.copy(alpha = 0.16f) else Color.Transparent)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onSelect(t) }
                        .padding(vertical = 7.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(t.emoji, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(3.dp))
                    Text(
                        t.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (sel) c.text else c.dim2,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        // Keeps the bar clear of the gesture pill / 3-button nav.
        Spacer(
            Modifier
                .fillMaxWidth()
                .windowInsetsBottomHeight(WindowInsets.navigationBars)
        )
    }
}

@Composable
private fun EmptyState(onAdd: () -> Unit) {
    val c = Tivan
    Column(
        Modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("📡", style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.height(14.dp))
        Text(
            "هنوز دستگاهی اضافه نشده",
            style = MaterialTheme.typography.headlineSmall,
            color = c.text
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "برای شروع، کنترلر TIVAN خود را با شماره سیم‌کارتش اضافه کنید",
            style = MaterialTheme.typography.bodySmall,
            color = c.dim,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        Button(onClick = onAdd, shape = RoundedCornerShape(16.dp)) {
            Text("افزودن دستگاه")
        }
    }
}
