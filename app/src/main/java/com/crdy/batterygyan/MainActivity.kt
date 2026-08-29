package com.crdy.batterygyan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.crdy.batterygyan.data.local.SettingsDataStore
import com.crdy.batterygyan.domain.model.ThemeMode
import com.crdy.batterygyan.domain.model.AccentColor
import com.crdy.batterygyan.domain.model.AlertPolicy
import com.crdy.batterygyan.platform.battery.AndroidBatteryDataSource
import com.crdy.batterygyan.platform.access.CapabilityDetector
import com.crdy.batterygyan.platform.access.GenericSysfsChargeControlProvider
import com.crdy.batterygyan.platform.alerts.BatteryAlertController
import com.crdy.batterygyan.monetization.BillingManager
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.crdy.batterygyan.ui.home.HomeScreen
import com.crdy.batterygyan.ui.home.HomeViewModel
import com.crdy.batterygyan.ui.settings.SettingsScreen
import com.crdy.batterygyan.ui.settings.SettingsViewModel
import com.crdy.batterygyan.ui.theme.BatteryGyanTheme

class MainActivity : ComponentActivity() {
    private var currentAlertPolicy = AlertPolicy()
    
    // For MVP, manual DI is sufficient.
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
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(billingManager)
        requestAdConsent()
        setContent {
            val settings by settingsViewModel.displaySettings.collectAsState()
            currentAlertPolicy = settings.alertPolicy
            val removeAds by billingManager.removeAds.collectAsState()
            
            val darkTheme = when (settings.themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            val accent = when (settings.accentColor) {
                AccentColor.MINT -> Color(0xFF00A889)
                AccentColor.BLUE -> Color(0xFF3689E8)
                AccentColor.VIOLET -> Color(0xFF7655D6)
                AccentColor.ORANGE -> Color(0xFFE88A28)
                AccentColor.ROSE -> Color(0xFFD94C72)
            }

            BatteryGyanTheme(darkTheme = darkTheme, accentColor = accent) {
                var showSettings by remember { mutableStateOf(false) }

                Scaffold { innerPadding ->
                    Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                        NavigationRail {
                            NavigationRailItem(
                                selected = !showSettings,
                                onClick = { showSettings = false },
                                icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
                                label = { androidx.compose.material3.Text("Home") }
                            )
                            NavigationRailItem(
                                selected = showSettings,
                                onClick = { showSettings = true },
                                icon = { Icon(Icons.Filled.Settings, contentDescription = "Customize") },
                                label = { androidx.compose.material3.Text("Customize") }
                            )
                        }
                        Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                            androidx.compose.animation.AnimatedContent(
                                targetState = showSettings,
                                label = "screen transition"
                            ) { settingsOpen ->
                                if (settingsOpen) SettingsScreen(viewModel = settingsViewModel, removeAds = removeAds, onRemoveAds = { billingManager.launchRemoveAds(this@MainActivity) }, onRestoreAds = billingManager::restorePurchases)
                                else HomeScreen(viewModel = homeViewModel, settings = settings, showAds = !removeAds)
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
