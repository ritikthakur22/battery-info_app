package com.crdy.powergyan.ui.settings

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.crdy.powergyan.domain.model.DisplaySettings
import com.crdy.powergyan.domain.model.SmartChargeConfig
import com.crdy.powergyan.platform.alerts.BatteryAlertController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomizationScreen(viewModel: SettingsViewModel, settings: DisplaySettings) {
    val context = LocalContext.current
    val capabilities by viewModel.capabilities.collectAsState()
    val scrollState = rememberScrollState()
    var showRebootConfirmation by remember { mutableStateOf(false) }
    val advancedAccessReady = capabilities.rootAvailable
    var showResetConfirmation by remember { mutableStateOf(false) }
    var soundPickerTarget by remember { mutableStateOf<String?>(null) }

    val lifecycleOwner = context as? LifecycleOwner
    DisposableEffect(lifecycleOwner) {
        if (lifecycleOwner == null) return@DisposableEffect onDispose { }
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshCapabilities()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Smart Charge Control Section
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("Smart Charge Control", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Switch(
                        checked = settings.smartChargeConfig.enabled,
                        onCheckedChange = { enabled -> 
                            viewModel.updateSmartChargeConfig(settings.smartChargeConfig.copy(enabled = enabled))
                            if (enabled) {
                            } else {
                                val config = settings.smartChargeConfig
                                val path = config.ctrlPath
                                val enableVal = config.ctrlEnable
                                runCatching {
                                    Runtime.getRuntime().exec(arrayOf("su", "-c", "printf '$enableVal' > '$path'"))
                                    if (path.endsWith("battery_charging_enabled")) {
                                        Runtime.getRuntime().exec(arrayOf("su", "-c", "printf '0' > '/sys/class/power_supply/battery/input_suspend'"))
                                    }
                                }
                                viewModel.resetChargeLimit()
                            }
                        }
                    )
                }
                
                
                if (!advancedAccessReady) {
                    Text("Requires verified Root for charge control", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    Text("Root was not verified. Shizuku detection alone cannot control this sysfs interface yet.", color = MaterialTheme.colorScheme.error)
                } else {
                    val stopAt = settings.smartChargeConfig.stopLimit.toFloat()
                    val resumeAt = settings.smartChargeConfig.resumeLimit.toFloat()

                    Text("Stop Charging At: ${stopAt.toInt()}%")
                    Slider(
                        value = stopAt,
                        onValueChange = { newStop ->
                            val newResume = minOf(resumeAt.toInt(), newStop.toInt() - 1)
                            viewModel.updateSmartChargeConfig(
                                settings.smartChargeConfig.copy(
                                    stopLimit = newStop.toInt(),
                                    resumeLimit = newResume.coerceAtLeast(20)
                                )
                            )
                        },
                        valueRange = 50f..100f
                    )
                    
                    Text("Resume Charging At: ${resumeAt.toInt()}%")
                    Slider(
                        value = resumeAt,
                        onValueChange = { newResume ->
                            if (newResume <= stopAt - 1) {
                                viewModel.updateSmartChargeConfig(
                                    settings.smartChargeConfig.copy(resumeLimit = newResume.toInt())
                                )
                            }
                        },
                        valueRange = 20f..minOf(99f, stopAt - 1f)
                    )

                    val chargeResult by viewModel.chargeLimitResult.collectAsState()
                    if (chargeResult != null) {
                        Text(
                            chargeResult!!.message, 
                            color = if (chargeResult!!.success) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))
                    
                    var showAdvanced by remember { mutableStateOf(false) }
                    TextButton(onClick = { showAdvanced = !showAdvanced }) {
                        Text(if (showAdvanced) "Hide Control File Configuration" else "Advanced: Configure Control File")
                    }
                    
                    if (showAdvanced) {
                        var ctrlPath by remember { mutableStateOf(settings.smartChargeConfig.ctrlPath) }
                        var ctrlEnable by remember { mutableStateOf(settings.smartChargeConfig.ctrlEnable) }
                        var ctrlDisable by remember { mutableStateOf(settings.smartChargeConfig.ctrlDisable) }
                        
                        OutlinedTextField(
                            value = ctrlPath,
                            onValueChange = { ctrlPath = it },
                            label = { Text("Path to Desired File") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = ctrlEnable,
                                onValueChange = { ctrlEnable = it },
                                label = { Text("Enable Value") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = ctrlDisable,
                                onValueChange = { ctrlDisable = it },
                                label = { Text("Disable Value") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { 
                                viewModel.updateSmartChargeConfig(settings.smartChargeConfig.copy(
                                    ctrlPath = ctrlPath,
                                    ctrlEnable = ctrlEnable,
                                    ctrlDisable = ctrlDisable
                                ))
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("UPDATE CTRL FILE DATA")
                        }
                    }
                }
            }
        }

        // Non-Root Alarm Section
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.padding(16.dp)) {
                Text("Battery Alarms", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("Enable Alarms", modifier = Modifier.weight(1f))
                    Switch(checked = settings.alertPolicy.enabled, onCheckedChange = viewModel::setAlertsEnabled)
                }
                
                Text("Ring when reaches: ${settings.alertPolicy.safeThreshold}%")
                Slider(value = settings.alertPolicy.safeThreshold.toFloat(), onValueChange = viewModel::setAlertThreshold, valueRange = 20f..100f)
                
                Spacer(Modifier.height(8.dp))
                
                val ringtoneLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                    if (result.resultCode == Activity.RESULT_OK) {
                        val uri = result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI) as? Uri
                        viewModel.setCustomSoundUri(uri?.toString())
                    }
                }
                val documentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                    if (uri != null) {
                        runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                        when (soundPickerTarget) {
                            "alarm" -> viewModel.setCustomSoundUri(uri.toString())
                            "plug" -> viewModel.setPlugSoundUri(uri.toString())
                            "unplug" -> viewModel.setUnplugSoundUri(uri.toString())
                        }
                    }
                    soundPickerTarget = null
                }
                
                fun getRingtoneName(uriStr: String?): String {
                    if (uriStr == null) return "Default Sound"
                    return try {
                        val ringtone = RingtoneManager.getRingtone(context, Uri.parse(uriStr))
                        ringtone?.getTitle(context) ?: "Custom Sound"
                    } catch (e: Exception) {
                        "Custom Sound"
                    }
                }
                
                Text("Current Alarm: ${getRingtoneName(settings.alertPolicy.customSoundUri)}", style = MaterialTheme.typography.bodySmall)
                Button(
                    onClick = {
                        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM or RingtoneManager.TYPE_NOTIFICATION)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                        }
                        ringtoneLauncher.launch(intent)
                    }, 
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Choose Alarm Sound")
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { soundPickerTarget = "alarm"; documentLauncher.launch(arrayOf("audio/*")) }, modifier = Modifier.weight(1f)) { Text("Local file") }
                    var alarmPreviewState by remember { mutableStateOf(BatteryAlertController.PreviewState.STOPPED) }
                    OutlinedButton(onClick = { alarmPreviewState = BatteryAlertController.playPreview(context, settings.alertPolicy.customSoundUri) }, modifier = Modifier.weight(1f)) { Text(if (alarmPreviewState == BatteryAlertController.PreviewState.PLAYING) "Pause" else if (alarmPreviewState == BatteryAlertController.PreviewState.PAUSED) "Resume" else "Play demo") }
                }
                
                Spacer(Modifier.height(8.dp))
                
                val plugLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                    if (result.resultCode == Activity.RESULT_OK) {
                        val uri = result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI) as? Uri
                        viewModel.setPlugSoundUri(uri?.toString())
                    }
                }
                
                Text("Current Plug Sound: ${getRingtoneName(settings.alertPolicy.plugSoundUri)}", style = MaterialTheme.typography.bodySmall)
                Button(onClick = { 
                    val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                        putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                    }
                    plugLauncher.launch(intent)
                }, modifier = Modifier.fillMaxWidth()) {
                    Text("Choose Plug Sound")
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { soundPickerTarget = "plug"; documentLauncher.launch(arrayOf("audio/*")) }, modifier = Modifier.weight(1f)) { Text("Local file") }
                    var plugPreviewState by remember { mutableStateOf(BatteryAlertController.PreviewState.STOPPED) }
                    OutlinedButton(onClick = { plugPreviewState = BatteryAlertController.playPreview(context, settings.alertPolicy.plugSoundUri) }, modifier = Modifier.weight(1f)) { Text(if (plugPreviewState == BatteryAlertController.PreviewState.PLAYING) "Pause" else if (plugPreviewState == BatteryAlertController.PreviewState.PAUSED) "Resume" else "Play demo") }
                }
                
                Spacer(Modifier.height(8.dp))
                
                val unplugLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                    if (result.resultCode == Activity.RESULT_OK) {
                        val uri = result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI) as? Uri
                        viewModel.setUnplugSoundUri(uri?.toString())
                    }
                }
                
                Text("Current Unplug Sound: ${getRingtoneName(settings.alertPolicy.unplugSoundUri)}", style = MaterialTheme.typography.bodySmall)
                Button(onClick = { 
                    val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                        putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                    }
                    unplugLauncher.launch(intent)
                }, modifier = Modifier.fillMaxWidth()) {
                    Text("Choose Unplug Sound")
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { soundPickerTarget = "unplug"; documentLauncher.launch(arrayOf("audio/*")) }, modifier = Modifier.weight(1f)) { Text("Local file") }
                    var unplugPreviewState by remember { mutableStateOf(BatteryAlertController.PreviewState.STOPPED) }
                    OutlinedButton(onClick = { unplugPreviewState = BatteryAlertController.playPreview(context, settings.alertPolicy.unplugSoundUri) }, modifier = Modifier.weight(1f)) { Text(if (unplugPreviewState == BatteryAlertController.PreviewState.PLAYING) "Pause" else if (unplugPreviewState == BatteryAlertController.PreviewState.PAUSED) "Resume" else "Play demo") }
                }
            }
        }

        // Temperature Control Section (third)
        TemperatureControlSection(viewModel = viewModel, settings = settings, isRooted = advancedAccessReady)

        // Safety Section
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(8.dp))
                    Text("Advanced", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                }
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { showRebootConfirmation = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Reboot Device (Requires Root)")
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { showResetConfirmation = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Reset All Settings to Default")
                }
            }
        }
    }

    if (showRebootConfirmation) {
        AlertDialog(
            onDismissRequest = { showRebootConfirmation = false },
            title = { Text("Reboot device?") },
            text = { Text("This immediately restarts the device and requires verified Root access. Continue?") },
            confirmButton = {
                TextButton(onClick = {
                    showRebootConfirmation = false
                    runCatching { Runtime.getRuntime().exec(arrayOf("su", "-c", "reboot")) }
                }) { Text("Reboot") }
            },
            dismissButton = { TextButton(onClick = { showRebootConfirmation = false }) { Text("Cancel") } }
        )
    }

    if (showResetConfirmation) {
        AlertDialog(
            onDismissRequest = { showResetConfirmation = false },
            title = { Text("Reset all settings?") },
            text = { Text("This clears themes, colors, alarms, sounds, and charge-control settings. The change cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showResetConfirmation = false
                    viewModel.resetAllSettings()
                }) { Text("Reset") }
            },
            dismissButton = { TextButton(onClick = { showResetConfirmation = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun TemperatureControlSection(viewModel: SettingsViewModel, settings: DisplaySettings, isRooted: Boolean) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Temperature Control (Experimental)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Switch(
                    checked = settings.smartChargeConfig.tempControlEnabled,
                    onCheckedChange = { enabled ->
                        if (isRooted) {
                            viewModel.updateSmartChargeConfig(settings.smartChargeConfig.copy(tempControlEnabled = enabled))
                        }
                    },
                    enabled = isRooted
                )
            }

            if (!isRooted) {
                Text("Requires verified Root for temperature control", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
            } else {
                Text("Enforce slow charging to cool device.", style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(16.dp))

            val maxTemp = settings.smartChargeConfig.maxTempC.toFloat()
            Text(
                "Throttle charge if above: ${maxTemp.toInt()}°C",
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold
            )
            Slider(
                value = maxTemp,
                onValueChange = { newTemp ->
                    viewModel.updateSmartChargeConfig(
                        settings.smartChargeConfig.copy(maxTempC = newTemp.toInt())
                    )
                },
                valueRange = 35f..50f
            )
            Text(
                "Warning: Aggressive throttling may significantly increase charging time. We strictly limit maximum allowable bounds for device safety.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}
