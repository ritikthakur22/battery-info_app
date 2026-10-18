package com.crdy.powergyan

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons

import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.crdy.powergyan.data.local.SettingsDataStore
import com.crdy.powergyan.domain.model.ThemeMode
import com.crdy.powergyan.platform.access.CapabilityDetector
import com.crdy.powergyan.platform.battery.AndroidBatteryDataSource
import com.crdy.powergyan.service.BatteryMonitorService
import com.crdy.powergyan.ui.home.BatteryDetailsScreen
import com.crdy.powergyan.ui.home.HomeScreen
import com.crdy.powergyan.ui.home.HomeViewModel
import com.crdy.powergyan.ui.settings.CustomizationScreen
import com.crdy.powergyan.ui.settings.SettingsScreen
import com.crdy.powergyan.ui.settings.SettingsViewModel
import com.crdy.powergyan.ui.theme.PowerGyanTheme
import com.crdy.powergyan.ui.theme.color
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    
    // Alerts are owned by BatteryMonitorService; a second controller here would ring twice.
    private val batteryRepository by lazy { AndroidBatteryDataSource(applicationContext) }
    private val settingsRepository by lazy { SettingsDataStore(applicationContext) }
    private val capabilityDetector by lazy { CapabilityDetector() }
    
    private val homeViewModel: HomeViewModel by viewModels { 
        HomeViewModel.Factory(batteryRepository) 
    }
    
    private val settingsViewModel: SettingsViewModel by viewModels {
        SettingsViewModel.Factory(settingsRepository, capabilityDetector, batteryRepository)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        // Force PhoneWindow to install its content decor before Compose asks for it.
        window.decorView
        super.onCreate(savedInstanceState)
        setContent {
            val settings by settingsViewModel.displaySettings.collectAsState()
            var notificationPermissionRequested by rememberSaveable { mutableStateOf(false) }
            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { }

            LaunchedEffect(settings.alertPolicy.enabled, settings.smartChargeConfig.enabled) {
                val monitoringEnabled = settings.alertPolicy.enabled || settings.smartChargeConfig.enabled
                if (
                    monitoringEnabled &&
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED &&
                    !notificationPermissionRequested
                ) {
                    notificationPermissionRequested = true
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            
            LaunchedEffect(settings.alertPolicy.enabled, settings.smartChargeConfig.enabled) {
                val intent = Intent(this@MainActivity, BatteryMonitorService::class.java)
                if (settings.alertPolicy.enabled || settings.smartChargeConfig.enabled) {
                    runCatching {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent)
                        else startService(intent)
                    }
                } else {
                    stopService(intent)
                }
            }
            
            val darkTheme = when (settings.themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            PowerGyanTheme(darkTheme = darkTheme, accentColor = settings.accentColor.color()) {
                var currentScreen by rememberSaveable { mutableStateOf(0) } // 0=Home, 1=Analytics, 2=Control, 3=Settings

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { 
                                Text(
                                    when (currentScreen) {
                                        0 -> "PowerGyan"
                                        1 -> "Battery Analytics"
                                        2 -> "Charge Control"
                                        else -> "Preferences"
                                    },
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                ) 
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.background,
                                titleContentColor = MaterialTheme.colorScheme.onBackground
                            )
                        )
                    },
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                selected = currentScreen == 0,
                                onClick = { currentScreen = 0 },
                                icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
                                label = { Text("Home") }
                            )
                            NavigationBarItem(
                                selected = currentScreen == 1,
                                onClick = { currentScreen = 1 },
                                icon = { Icon(androidx.compose.material.icons.Icons.Filled.Info, contentDescription = "Analytics") },
                                label = { Text("Analytics") }
                            )
                            NavigationBarItem(
                                selected = currentScreen == 2,
                                onClick = { currentScreen = 2 },
                                icon = { Icon(androidx.compose.material.icons.Icons.Filled.Build, contentDescription = "Control") },
                                label = { Text("Control") }
                            )
                            NavigationBarItem(
                                selected = currentScreen == 3,
                                onClick = { currentScreen = 3 },
                                icon = { Icon(Icons.Filled.Settings, contentDescription = "Settings") },
                                label = { Text("Settings") }
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                        AnimatedContent(targetState = currentScreen, label = "screen transition") { screen ->
                            when (screen) {
                                0 -> HomeScreen(viewModel = homeViewModel, settings = settings)
                                1 -> BatteryDetailsScreen(homeViewModel = homeViewModel)
                                2 -> CustomizationScreen(viewModel = settingsViewModel, settings = settings)
                                3 -> SettingsScreen(viewModel = settingsViewModel)
                            }
                        }
                    }
                }
            }
        }
    }
}
