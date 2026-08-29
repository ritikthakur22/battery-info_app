package com.crdy.batterygyan.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.crdy.batterygyan.domain.model.ThemeMode
import com.crdy.batterygyan.domain.model.AccentColor
import com.crdy.batterygyan.monetization.TestNativeAdCard

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    removeAds: Boolean = false,
    onRemoveAds: () -> Unit = {},
    onRestoreAds: () -> Unit = {}
) {
    val settings by viewModel.displaySettings.collectAsState()
    val capabilities by viewModel.capabilities.collectAsState()
    val chargeResult by viewModel.chargeLimitResult.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Customize",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text("Display", style = MaterialTheme.typography.titleMedium)
            Text("Text size (${settings.textScale}x)", style = MaterialTheme.typography.bodyLarge)
            Slider(
                value = settings.textScale,
                onValueChange = { viewModel.setTextScale(it) },
                valueRange = 0.8f..2.0f,
                steps = 4,
                modifier = Modifier.fillMaxWidth()
            )

            Text("Icon size (${settings.iconScale}x)", style = MaterialTheme.typography.bodyLarge)
            Slider(
                value = settings.iconScale,
                onValueChange = { viewModel.setIconScale(it) },
                valueRange = 0.8f..1.6f,
                steps = 3,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(18.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(18.dp))

            Text("Accent color", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
            ) {
                AccentColor.values().forEach { color ->
                    FilterChip(
                        selected = settings.accentColor == color,
                        onClick = { viewModel.setAccentColor(color) },
                        label = { Text(color.name.lowercase().replaceFirstChar { it.uppercase() }) },
                        modifier = Modifier.size(width = 78.dp, height = 40.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            HorizontalDivider()
            Spacer(modifier = Modifier.height(18.dp))
            Text("Privacy & Ads", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Text(
                if (removeAds) "Premium unlocked • ads are disabled" else "Support development with a one-time purchase",
                style = MaterialTheme.typography.bodyMedium
            )
            if (!removeAds) {
                Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                    androidx.compose.material3.Button(onClick = onRemoveAds) { Text("Remove Ads") }
                    androidx.compose.material3.TextButton(onClick = onRestoreAds) { Text("Restore") }
                }
                Text("Battery Gyan — Remove Ads • one-time purchase", style = MaterialTheme.typography.labelSmall)
                TestNativeAdCard(Modifier.fillMaxWidth().padding(top = 8.dp))
            }

            Text("Theme", style = MaterialTheme.typography.titleMedium)
            ThemeMode.values().forEach { mode ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RadioButton(
                        selected = settings.themeMode == mode,
                        onClick = { viewModel.setThemeMode(mode) }
                    )
                    Text(text = mode.name, style = MaterialTheme.typography.bodyLarge)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Show Secondary Info (Health/Temp)", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Switch(
                    checked = settings.secondaryInfoEnabled,
                    onCheckedChange = { viewModel.toggleSecondaryInfo(it) }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(18.dp))

            Text("Advanced Access", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Text("Capabilities are detected on this device; unsupported controls stay disabled.", style = MaterialTheme.typography.bodyMedium)
            AccessRow("Root", if (capabilities.rootAvailable) "Verified" else "Not available")
            AccessRow("Shizuku", when {
                !capabilities.shizukuAvailable -> "Unavailable"
                capabilities.shizukuPermissionGranted -> "Permission granted"
                else -> "Needs permission"
            })
            if (capabilities.shizukuAvailable && !capabilities.shizukuPermissionGranted) {
                androidx.compose.material3.TextButton(onClick = viewModel::requestShizukuPermission) { Text("Grant Shizuku permission") }
            }
            if (capabilities.chargeLimit) {
                Text("Charge limit", style = MaterialTheme.typography.titleSmall)
                Text("Stop at 80% • resume at 75%", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                    androidx.compose.material3.Button(onClick = { viewModel.setChargeLimit(80, 75) }) { Text("Enable") }
                    androidx.compose.material3.OutlinedButton(onClick = viewModel::resetChargeLimit) { Text("Reset") }
                }
                chargeResult?.let { result ->
                    Text(result.message, color = if (result.success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            } else {
                Text("Charge limit: Not supported on this device", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(18.dp))
            Text("Sounds & Alerts", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Low-battery sound", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                Switch(checked = settings.alertPolicy.enabled, onCheckedChange = viewModel::setAlertsEnabled)
            }
            Text("Alert threshold: ${settings.alertPolicy.safeThreshold}%", style = MaterialTheme.typography.bodyMedium)
            Slider(value = settings.alertPolicy.safeThreshold.toFloat(), onValueChange = viewModel::setAlertThreshold, valueRange = 5f..50f, steps = 8)

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Terms and Conditions",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(8.dp)
                    // TODO: Add actual UriHandler clickable link here when docs are ready
                )
                Text(
                    text = "Privacy Policy",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(8.dp)
                    // TODO: Add actual UriHandler clickable link here when docs are ready
                )
            }
        }
    }
}

@Composable
private fun AccessRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Text(value, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    }
}
