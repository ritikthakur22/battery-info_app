package com.crdy.powergyan

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.crdy.powergyan.data.local.SettingsDataStore
import com.crdy.powergyan.domain.model.AccentColor
import com.crdy.powergyan.domain.model.AlertPolicy
import com.crdy.powergyan.domain.model.ThemeMode
import com.crdy.powergyan.monetization.BillingManager
import com.crdy.powergyan.platform.access.CapabilityDetector
import com.crdy.powergyan.platform.access.GenericSysfsChargeControlProvider
import com.crdy.powergyan.platform.alerts.BatteryAlertController
import com.crdy.powergyan.platform.battery.AndroidBatteryDataSource
import com.crdy.powergyan.service.BatteryMonitorService
import com.crdy.powergyan.ui.home.BatteryDetailsScreen
import com.crdy.powergyan.ui.home.HomeScreen
import com.crdy.powergyan.ui.home.HomeViewModel
import com.crdy.powergyan.ui.settings.CustomizationScreen
import com.crdy.powergyan.ui.settings.SettingsScreen
import com.crdy.powergyan.ui.settings.SettingsViewModel
import com.crdy.powergyan.ui.theme.PowerGyanTheme
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var currentAlertPolicy = AlertPolicy()
    
    private val batteryAlertController by lazy { BatteryAlertController(applicationContext) }
    private val batteryRepository by lazy { AndroidBatteryDataSource(applicationContext, batteryAlertController) { currentAlertPolicy } }
    private val settingsRepository by lazy { SettingsDataStore(applicationContext) }
    private val capabilityDetector by lazy { CapabilityDetector(applicationContext) }
    private val chargeControlProvider by lazy { GenericSysfsChargeControlProvider() }
    private val billingManager by lazy { BillingManager(applicationContext) }
    
    private val homeViewModel: HomeViewModel by viewModels { 
        HomeViewModel.Factory(batteryRepository) 
    }
    
    private val settingsViewModel: SettingsViewModel by viewModels {
        SettingsViewModel.Factory(settingsRepository, capabilityDetector, chargeControlProvider)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        // Force PhoneWindow to install its content decor before Compose asks for it.
        window.decorView
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(billingManager)
        requestAdConsent()
        setContent {
            val settings by settingsViewModel.displaySettings.collectAsState()
            currentAlertPolicy = settings.alertPolicy
            val removeAds by billingManager.removeAds.collectAsState()
            
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

            val accent = when (settings.accentColor) {
                AccentColor.MINT -> Color(0xFF00A889)
                AccentColor.BLUE -> Color(0xFF3689E8)
                AccentColor.VIOLET -> Color(0xFF7655D6)
                AccentColor.ORANGE -> Color(0xFFE88A28)
                AccentColor.ROSE -> Color(0xFFD94C72)
            }

            PowerGyanTheme(darkTheme = darkTheme, accentColor = accent) {
                var currentScreen by remember { mutableStateOf(0) } // 0=Home, 1=Battery Details, 2=Customization, 3=Settings
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ModalDrawerSheet {
                            Text("PowerGyan", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.headlineMedium)
                            HorizontalDivider()
                            NavigationDrawerItem(
                                label = { Text("Home") },
                                selected = currentScreen == 0,
                                onClick = { currentScreen = 0; scope.launch { drawerState.close() } },
                                icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                            )
                            NavigationDrawerItem(
                                label = { Text("Battery Details") },
                                selected = currentScreen == 1,
                                onClick = { currentScreen = 1; scope.launch { drawerState.close() } },
                                icon = { Icon(Icons.Filled.Info, contentDescription = "Battery Details") },
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                            )
                            NavigationDrawerItem(
                                label = { Text("Charge Limiter") },
                                selected = currentScreen == 2,
                                onClick = { currentScreen = 2; scope.launch { drawerState.close() } },
                                icon = { Icon(Icons.Filled.Build, contentDescription = "Charge Limiter") },
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                            )
                            NavigationDrawerItem(
                                label = { Text("Settings") },
                                selected = currentScreen == 3,
                                onClick = { currentScreen = 3; scope.launch { drawerState.close() } },
                                icon = { Icon(Icons.Filled.Settings, contentDescription = "Settings") },
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                            )
                        }
                    }
                ) {
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = { 
                                    Text(
                                        when (currentScreen) {
                                            0 -> "PowerGyan"
                                            1 -> "Battery Details"
                                            2 -> "Charge Limiter"
                                            else -> "Settings"
                                        }
                                    ) 
                                },
                                navigationIcon = {
                                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                        Icon(Icons.Filled.Menu, contentDescription = "Menu")
                                    }
                                }
                            )
                        }
                    ) { innerPadding ->
                        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                            AnimatedContent(targetState = currentScreen, label = "screen transition") { screen ->
                                when (screen) {
                                    0 -> HomeScreen(viewModel = homeViewModel, settings = settings, showAds = !removeAds)
                                    1 -> BatteryDetailsScreen(showAds = !removeAds, homeViewModel = homeViewModel)
                                    2 -> CustomizationScreen(viewModel = settingsViewModel, settings = settings)
                                    3 -> SettingsScreen(viewModel = settingsViewModel, removeAds = removeAds, onRemoveAds = { billingManager.launchRemoveAds(this@MainActivity) }, onRestoreAds = billingManager::restorePurchases)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun requestAdConsent() {
        val consentInformation = UserMessagingPlatform.getConsentInformation(this)
        val params = ConsentRequestParameters.Builder().build()
        consentInformation.requestConsentInfoUpdate(
            this,
            params,
            { UserMessagingPlatform.loadAndShowConsentFormIfRequired(this) { MobileAds.initialize(this) } },
            { MobileAds.initialize(this) }
        )
    }
}
