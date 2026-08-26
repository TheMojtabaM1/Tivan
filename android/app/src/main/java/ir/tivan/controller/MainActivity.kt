package ir.tivan.controller

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Power
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import ir.tivan.controller.ui.MainViewModel
import ir.tivan.controller.ui.components.DeviceSwitcherSheet
import ir.tivan.controller.ui.components.DeviceTopBar
import ir.tivan.controller.ui.outputs.OutputsScreen
import ir.tivan.controller.ui.security.SecurityScreen
import ir.tivan.controller.ui.settings.SettingsScreen
import ir.tivan.controller.ui.status.StatusScreen
import ir.tivan.controller.ui.theme.TivanTheme

private enum class Tab(val label: String) { OUTPUTS("خروجی‌ها"), SECURITY("دزدگیر"), STATUS("وضعیت"), SETTINGS("تنظیمات") }

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestSmsPermissionsIfNeeded()

        setContent {
            TivanTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    RootScreen()
                }
            }
        }
    }

    private fun requestSmsPermissionsIfNeeded() {
        val needed = listOf(Manifest.permission.SEND_SMS, Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS)
            .filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        if (needed.isNotEmpty()) permissionLauncher.launch(needed.toTypedArray())
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RootScreen(viewModel: MainViewModel = viewModel()) {
    var tab by remember { mutableStateOf(Tab.OUTPUTS) }
    var showSheet by remember { mutableStateOf(false) }

    val devices by viewModel.devices.collectAsState()
    val selectedId by viewModel.selectedDeviceId.collectAsState()
    val selectedDevice by viewModel.selectedDevice.collectAsState()

    Scaffold(
        topBar = { DeviceTopBar(device = selectedDevice, onOpenSwitcher = { showSheet = true }) },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == Tab.OUTPUTS, onClick = { tab = Tab.OUTPUTS },
                    icon = { Icon(Icons.Default.Power, contentDescription = null) }, label = { Text(Tab.OUTPUTS.label) }
                )
                NavigationBarItem(
                    selected = tab == Tab.SECURITY, onClick = { tab = Tab.SECURITY },
                    icon = { Icon(Icons.Default.Security, contentDescription = null) }, label = { Text(Tab.SECURITY.label) }
                )
                NavigationBarItem(
                    selected = tab == Tab.STATUS, onClick = { tab = Tab.STATUS },
                    icon = { Icon(Icons.Default.Report, contentDescription = null) }, label = { Text(Tab.STATUS.label) }
                )
                NavigationBarItem(
                    selected = tab == Tab.SETTINGS, onClick = { tab = Tab.SETTINGS },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) }, label = { Text(Tab.SETTINGS.label) }
                )
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            if (devices.isEmpty()) {
                EmptyState(onAdd = { showSheet = true })
            } else {
                when (tab) {
                    Tab.OUTPUTS -> OutputsScreen(viewModel)
                    Tab.SECURITY -> SecurityScreen(viewModel)
                    Tab.STATUS -> StatusScreen(viewModel)
                    Tab.SETTINGS -> SettingsScreen(viewModel)
                }
            }
        }
    }

    if (showSheet) {
        DeviceSwitcherSheet(
            devices = devices,
            selectedId = selectedId,
            onSelect = { viewModel.selectDevice(it) },
            onAdd = { name, phone -> viewModel.addDevice(name, phone) },
            onDelete = { viewModel.deleteDevice(it) },
            onDismiss = { showSheet = false }
        )
    }
}

@Composable
private fun EmptyState(onAdd: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("هنوز هیچ دستگاهی اضافه نشده", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "برای شروع، یک دستگاه TIVAN با شماره سیم‌کارتش اضافه کنید",
            style = MaterialTheme.typography.bodySmall,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onAdd) { Text("افزودن دستگاه") }
    }
}
