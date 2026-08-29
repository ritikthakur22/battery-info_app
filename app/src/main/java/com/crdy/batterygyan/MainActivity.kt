package com.crdy.batterygyan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.crdy.batterygyan.data.local.SettingsDataStore
import com.crdy.batterygyan.domain.model.ThemeMode
import com.crdy.batterygyan.platform.battery.AndroidBatteryDataSource
import com.crdy.batterygyan.ui.home.HomeScreen
import com.crdy.batterygyan.ui.home.HomeViewModel
import com.crdy.batterygyan.ui.settings.SettingsScreen
import com.crdy.batterygyan.ui.settings.SettingsViewModel
import com.crdy.batterygyan.ui.theme.BatteryGyanTheme

class MainActivity : ComponentActivity() {
    
    // For MVP, manual DI is sufficient.
    private val batteryRepository by lazy { AndroidBatteryDataSource(applicationContext) }
    private val settingsRepository by lazy { SettingsDataStore(applicationContext) }
    
    private val homeViewModel: HomeViewModel by viewModels { 
        HomeViewModel.Factory(batteryRepository) 
    }
    
    private val settingsViewModel: SettingsViewModel by viewModels {
        SettingsViewModel.Factory(settingsRepository)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val settings by settingsViewModel.displaySettings.collectAsState()
            
            val darkTheme = when (settings.themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            BatteryGyanTheme(darkTheme = darkTheme) {
                var showSettings by remember { mutableStateOf(false) }

                Scaffold(
                    floatingActionButton = {
                        FloatingActionButton(onClick = { showSettings = !showSettings }) {
                            Icon(Icons.Filled.Settings, contentDescription = "Settings")
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        if (showSettings) {
                            SettingsScreen(viewModel = settingsViewModel)
                        } else {
                            HomeScreen(viewModel = homeViewModel, settings = settings)
                        }
                    }
                }
            }
        }
    }
}
