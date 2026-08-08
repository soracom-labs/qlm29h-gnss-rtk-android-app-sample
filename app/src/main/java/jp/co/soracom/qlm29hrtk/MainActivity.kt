package jp.co.soracom.qlm29hrtk

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbManager
import android.hardware.usb.UsbDevice
import android.os.Bundle
import android.view.WindowManager
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import jp.co.soracom.qlm29hrtk.usb.UsbPermissionManager
import jp.co.soracom.qlm29hrtk.ui.map.TrackMapCard
import jp.co.soracom.qlm29hrtk.ui.map.MapCacheControls
import jp.co.soracom.qlm29hrtk.ui.map.MapScreen
import jp.co.soracom.qlm29hrtk.ui.map.MapUiState
import jp.co.soracom.qlm29hrtk.ui.map.MapActions
import jp.co.soracom.qlm29hrtk.ui.console.NmeaConsoleCard
import jp.co.soracom.qlm29hrtk.ui.settings.DiagnosticsCard
import jp.co.soracom.qlm29hrtk.ui.settings.SettingsScreen
import jp.co.soracom.qlm29hrtk.ui.settings.RtkSettingsActions
import jp.co.soracom.qlm29hrtk.ui.settings.SettingsUiState
import jp.co.soracom.qlm29hrtk.ui.theme.Qlm29hTheme
import jp.co.soracom.qlm29hrtk.storage.StorageInspector
import java.time.Instant
import jp.co.soracom.qlm29hrtk.soracom.SoracomQualityPolicy

class MainActivity : ComponentActivity() {
    private lateinit var runtime: RtkRuntime
    private lateinit var permissionManager: UsbPermissionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        runtime = (application as RtkApplication).runtime
        permissionManager = UsbPermissionManager(applicationContext)
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 2901)
        }
        setContent {
            val state by runtime.state.collectAsState()
            val locationPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions(),
            ) { permissions ->
                val precise = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                runtime.setSmartphonePermissionGranted(precise)
                if (precise) runtime.updateSmartphoneGnssEnabled(true)
            }
            LaunchedEffect(Unit) {
                runtime.setSmartphonePermissionGranted(
                    ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED,
                )
            }
            LaunchedEffect(state.display.keepScreenOn) {
                if (state.display.keepScreenOn) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }
            LaunchedEffect(Unit) { runtime.handleUsbAttached() }
            DisposableEffect(Unit) {
                val receiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context?, intent: Intent?) {
                        when (intent?.action) {
                            UsbPermissionManager.ACTION_USB_PERMISSION -> {
                                val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                                val id = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)?.deviceId
                                if (granted && id != null) {
                                    runtime.connect(id)
                                } else {
                                    runtime.showError("USB permission was denied")
                                    runtime.refreshDevices()
                                }
                            }
                            UsbManager.ACTION_USB_DEVICE_ATTACHED -> runtime.handleUsbAttached()
                            UsbManager.ACTION_USB_DEVICE_DETACHED -> runtime.onDeviceDetached()
                        }
                    }
                }
                val filter = IntentFilter().apply {
                    addAction(UsbPermissionManager.ACTION_USB_PERMISSION)
                    addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
                    addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
                }
                registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
                onDispose { unregisterReceiver(receiver) }
            }
            Qlm29hTheme(state.display.darkTheme) {
                MainScreen(
                    state = state,
                    mapActions = MapActions(
                        onSmartphoneVisibleChange = runtime::updateSmartphoneTrackVisible,
                        onFollowChange = runtime::setMapFollow,
                        onReturnToLive = { runtime.selectMapSession(null) },
                    ),
                    settingsActions = RtkSettingsActions(runtime),
                    permissionManager = permissionManager,
                    onClearConsole = runtime::clearConsole,
                    onSmartphoneGnssToggle = { enabled ->
                        if (!enabled) {
                            runtime.updateSmartphoneGnssEnabled(false)
                        } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            runtime.setSmartphonePermissionGranted(true)
                            runtime.updateSmartphoneGnssEnabled(true)
                        } else {
                            locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                        }
                    },
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (::runtime.isInitialized) runtime.setAppInForeground(true)
    }

    override fun onStop() {
        if (::runtime.isInitialized) runtime.setAppInForeground(false)
        super.onStop()
    }
}

private enum class AppTab(val label: String, val iconRes: Int) {
    MAP("Map", R.drawable.ic_tab_map),
    CONSOLE("Console", R.drawable.ic_tab_console),
    SETTINGS("Settings", R.drawable.ic_tab_settings),
}

@androidx.compose.runtime.Composable
private fun MainScreen(
    state: AppState,
    mapActions: MapActions,
    settingsActions: jp.co.soracom.qlm29hrtk.ui.settings.SettingsActions,
    permissionManager: UsbPermissionManager,
    onClearConsole: () -> Unit,
    onSmartphoneGnssToggle: (Boolean) -> Unit,
) {
    var selected by remember { mutableStateOf(AppTab.MAP) }
    var sessionsNavigationRequest by remember { mutableIntStateOf(0) }
    Scaffold(
        bottomBar = {
            AppBottomTabBar(selected = selected, onSelected = { selected = it })
        },
    ) { padding ->
        Surface(Modifier.fillMaxSize().padding(padding)) {
            when (selected) {
                AppTab.MAP -> MapScreen(
                    MapUiState.from(state),
                    mapActions,
                )
                AppTab.CONSOLE -> LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
                    item {
                        NmeaConsoleCard(
                            state.diagnostics.console,
                            state.diagnostics.checksumErrors,
                            onClearConsole,
                            onShareHistorical = {
                                sessionsNavigationRequest += 1
                                selected = AppTab.SETTINGS
                            },
                        )
                    }
                }
                AppTab.SETTINGS -> SettingsScreen(
                    SettingsUiState.from(state),
                    settingsActions,
                    permissionManager,
                    onSmartphoneGnssToggle,
                    sessionsNavigationRequest,
                )
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun AppBottomTabBar(selected: AppTab, onSelected: (AppTab) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(color = colors.surface, shadowElevation = 8.dp) {
        Column(Modifier.navigationBarsPadding()) {
            HorizontalDivider(color = colors.outlineVariant)
            Row(Modifier.fillMaxWidth().height(68.dp)) {
                AppTab.entries.forEach { tab ->
                    val isSelected = selected == tab
                    val contentColor = colors.onSurfaceVariant

                    // DISPLAY-03: one continuous tab strip keeps navigation calm; only the
                    // active destination receives Celeste in its background and underline.
                    // Labels and icons remain neutral, matching the SORACOM User Console.
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(if (isSelected) colors.primary.copy(alpha = 0.09f) else Color.Transparent)
                            .selectable(
                                selected = isSelected,
                                role = Role.Tab,
                                onClick = { onSelected(tab) },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Icon(
                                painter = painterResource(tab.iconRes),
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                                tint = contentColor,
                            )
                            Text(
                                text = tab.label,
                                color = contentColor,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        Box(
                            Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .height(4.dp)
                                .background(if (isSelected) colors.primary else Color.Transparent),
                        )
                    }
                }
            }
        }
    }
}
