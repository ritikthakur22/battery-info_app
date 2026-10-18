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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.crdy.powergyan.domain.model.DisplaySettings
import com.crdy.powergyan.platform.alerts.BatteryAlertController
import com.crdy.powergyan.service.formatMinuteOfDay

private val CTRL_PATH = Regex("/sys/[A-Za-z0-9_./,:-]+")
private val CTRL_VALUE = Regex("[A-Za-z0-9_.-]+")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomizationScreen(viewModel: SettingsViewModel, settings: DisplaySettings) {
    val context = LocalContext.current
    val capabilities by viewModel.capabilities.collectAsState()
    val rooted = capabilities.rootAvailable
    val config = settings.smartChargeConfig
    var showRebootConfirmation by remember { mutableStateOf(false) }
    var showResetConfirmation by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

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
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (!rooted) {
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.large) {
                Column(Modifier.padding(16.dp)) {
                    Text("Smart Charge needs root", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Grant PowerGyan root access in Magisk, KernelSU or APatch, then come back to this screen. Alarms below work without root.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // Smart Charge
        Section {
            SwitchRow(
                title = "Smart Charge",
                subtitle = "Stop charging at a set level and start again when it drops.",
                checked = config.enabled,
                // Always allow turning it off, even if root was lost.
                enabled = rooted || config.enabled,
                onCheckedChange = { enabled ->
                    viewModel.updateSmartChargeConfig(config.copy(enabled = enabled))
                    // The service restores the file it actually used; this covers the case where it isn't running.
                    if (!enabled) viewModel.resetChargeLimit()
                }
            )

            if (rooted) {
                Spacer(Modifier.height(16.dp))
                LabeledSlider(
                    label = "Stop charging at",
                    value = "${config.stopLimit}%",
                    current = config.stopLimit.toFloat(),
                    range = 50f..100f,
                    onChange = { newStop ->
                        val stop = newStop.toInt()
                        viewModel.updateSmartChargeConfig(config.copy(stopLimit = stop, resumeLimit = minOf(config.resumeLimit, stop - 1).coerceAtLeast(20)))
                    }
                )
                LabeledSlider(
                    label = "Start charging again at",
                    value = "${config.resumeLimit}%",
                    current = config.resumeLimit.toFloat(),
                    range = 20f..(config.stopLimit - 1f),
                    onChange = { viewModel.updateSmartChargeConfig(config.copy(resumeLimit = it.toInt())) }
                )

                HorizontalDivider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline)
                SwitchRow(
                    title = "Lower limit on wireless",
                    subtitle = "Wireless chargers run warmer, so stop earlier on them.",
                    checked = config.wirelessStopLimit > 0,
                    onCheckedChange = { viewModel.updateSmartChargeConfig(config.copy(wirelessStopLimit = if (it) minOf(config.stopLimit, 70) else 0)) }
                )
                if (config.wirelessStopLimit > 0) {
                    LabeledSlider(
                        label = "Stop wireless charging at",
                        value = "${config.wirelessStopLimit}%",
                        current = config.wirelessStopLimit.toFloat(),
                        range = 50f..100f,
                        onChange = { viewModel.updateSmartChargeConfig(config.copy(wirelessStopLimit = it.toInt())) }
                    )
                }
                Spacer(Modifier.height(8.dp))
                SwitchRow(
                    title = "Full by a set time",
                    subtitle = if (config.fullByMinute >= 0) "Tops up to 100% in the 2 hours before ${formatMinuteOfDay(config.fullByMinute)}."
                        else "Hold at your limit overnight, then charge to 100% before you wake up.",
                    checked = config.fullByMinute >= 0,
                    onCheckedChange = { if (it) showTimePicker = true else viewModel.updateSmartChargeConfig(config.copy(fullByMinute = -1)) }
                )
                if (config.fullByMinute >= 0) {
                    TextButton(onClick = { showTimePicker = true }, contentPadding = PaddingValues(0.dp)) {
                        Text("Change time")
                    }
                }

                val chargeResult by viewModel.chargeLimitResult.collectAsState()
                chargeResult?.let {
                    Text(
                        it.message,
                        color = if (it.success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                var showAdvanced by remember { mutableStateOf(false) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = viewModel::testChargeControl) { Text("Test charge control") }
                    TextButton(onClick = { showAdvanced = !showAdvanced }) {
                        Text(if (showAdvanced) "Hide control file" else "Set control file")
                    }
                }
                if (showAdvanced) ControlFileEditor(settings, viewModel)
            }
        }

        // Pause when hot
        Section {
            SwitchRow(
                title = "Pause when hot",
                subtitle = if (config.enabled) "Charging resumes after the battery cools 2°C below the limit."
                    else "Turn on Smart Charge first. This works together with it.",
                checked = config.tempControlEnabled,
                enabled = rooted && config.enabled,
                onCheckedChange = { viewModel.updateSmartChargeConfig(config.copy(tempControlEnabled = it)) }
            )
            if (rooted && config.enabled && config.tempControlEnabled) {
                Spacer(Modifier.height(16.dp))
                LabeledSlider(
                    label = "Pause charging above",
                    value = "${config.maxTempC}°C",
                    current = config.maxTempC.toFloat(),
                    range = 35f..50f,
                    onChange = { viewModel.updateSmartChargeConfig(config.copy(maxTempC = it.toInt())) }
                )
            }
        }

        // Alarms (no root needed)
        Section {
            SwitchRow(
                title = "Battery alarms",
                subtitle = "Ring at a charge level and at 100%, and warn when the battery runs low.",
                checked = settings.alertPolicy.enabled,
                onCheckedChange = viewModel::setAlertsEnabled
            )
            if (settings.alertPolicy.enabled) {
                Spacer(Modifier.height(16.dp))
                LabeledSlider(
                    label = "Ring at",
                    value = "${settings.alertPolicy.safeThreshold}%",
                    current = settings.alertPolicy.safeThreshold.toFloat(),
                    range = 20f..100f,
                    onChange = viewModel::setAlertThreshold
                )
                HorizontalDivider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline)
                LabeledSlider(
                    label = "Warn when battery drops to",
                    value = if (settings.alertPolicy.lowThreshold == 0) "Off" else "${settings.alertPolicy.lowThreshold}%",
                    current = settings.alertPolicy.lowThreshold.toFloat(),
                    range = 0f..50f,
                    steps = 9,
                    onChange = { viewModel.setLowThreshold(it.toInt()) }
                )
                HorizontalDivider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline)
                SoundRow("Alarm sound", settings.alertPolicy.customSoundUri, RingtoneManager.TYPE_ALARM, viewModel::setCustomSoundUri)
                SoundRow("Plug-in sound", settings.alertPolicy.plugSoundUri, RingtoneManager.TYPE_NOTIFICATION, viewModel::setPlugSoundUri)
                SoundRow("Unplug sound", settings.alertPolicy.unplugSoundUri, RingtoneManager.TYPE_NOTIFICATION, viewModel::setUnplugSoundUri)
            }
        }

        // Device
        Section {
            Text("Device", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = { showResetConfirmation = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Reset all settings")
            }
            if (rooted) {
                OutlinedButton(onClick = { showRebootConfirmation = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Restart phone")
                }
            }
        }
    }

    if (showRebootConfirmation) {
        AlertDialog(
            onDismissRequest = { showRebootConfirmation = false },
            title = { Text("Restart phone?") },
            text = { Text("Your phone restarts right away. Save anything open first.") },
            confirmButton = {
                TextButton(onClick = {
                    showRebootConfirmation = false
                    runCatching { Runtime.getRuntime().exec(arrayOf("su", "-c", "reboot")) }
                }) { Text("Restart") }
            },
            dismissButton = { TextButton(onClick = { showRebootConfirmation = false }) { Text("Cancel") } }
        )
    }

    if (showTimePicker) {
        val initial = config.fullByMinute.takeIf { it >= 0 } ?: (7 * 60)
        val state = rememberTimePickerState(initialHour = initial / 60, initialMinute = initial % 60)
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Be fully charged by") },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(onClick = {
                    showTimePicker = false
                    viewModel.updateSmartChargeConfig(config.copy(fullByMinute = state.hour * 60 + state.minute))
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancel") } }
        )
    }

    if (showResetConfirmation) {
        AlertDialog(
            onDismissRequest = { showResetConfirmation = false },
            title = { Text("Reset all settings?") },
            text = { Text("Theme, alarms, sounds and Smart Charge go back to their defaults. Charging is turned back on.") },
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
private fun Section(content: @Composable ColumnScope.() -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, enabled: Boolean = true, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

@Composable
private fun LabeledSlider(label: String, value: String, current: Float, range: ClosedFloatingPointRange<Float>, steps: Int = 0, onChange: (Float) -> Unit) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    }
    Slider(value = current.coerceIn(range), onValueChange = onChange, valueRange = range, steps = steps)
}

/** One alert sound: shows the current choice and lets the user pick a system sound, pick a file, or preview it. */
@Composable
private fun SoundRow(title: String, uri: String?, ringtoneType: Int, onPicked: (String?) -> Unit) {
    val context = LocalContext.current
    val name = remember(uri) {
        uri?.let { runCatching { RingtoneManager.getRingtone(context, Uri.parse(it))?.getTitle(context) }.getOrNull() ?: "Custom sound" }
            ?: "Default"
    }
    val ringtoneLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            @Suppress("DEPRECATION")
            val picked = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            onPicked(picked?.toString())
        }
    }
    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { picked ->
        if (picked != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(picked, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            onPicked(picked.toString())
        }
    }
    var preview by remember { mutableStateOf(BatteryAlertController.PreviewState.STOPPED) }

    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        Text(name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TextButton(onClick = {
                ringtoneLauncher.launch(Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                    putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, ringtoneType)
                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                })
            }) { Text("Change") }
            TextButton(onClick = { fileLauncher.launch(arrayOf("audio/*")) }) { Text("Use a file") }
            TextButton(onClick = { preview = BatteryAlertController.playPreview(context, uri) }) {
                Text(if (preview == BatteryAlertController.PreviewState.PLAYING) "Pause" else "Play")
            }
        }
    }
}

@Composable
private fun ControlFileEditor(settings: DisplaySettings, viewModel: SettingsViewModel) {
    val config = settings.smartChargeConfig
    var ctrlPath by remember { mutableStateOf(config.ctrlPath) }
    var ctrlEnable by remember { mutableStateOf(config.ctrlEnable) }
    var ctrlDisable by remember { mutableStateOf(config.ctrlDisable) }
    // Same rules the service enforces; these values end up inside an su command.
    val pathOk = ctrlPath.matches(CTRL_PATH)
    val valuesOk = ctrlEnable.matches(CTRL_VALUE) && ctrlDisable.matches(CTRL_VALUE)

    Text(
        "Only change this if Smart Charge doesn't work on your phone. Find the right file for your device in the ACC project's list.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = ctrlPath,
        onValueChange = { ctrlPath = it.trim() },
        label = { Text("Control file") },
        isError = !pathOk,
        supportingText = { if (!pathOk) Text("Must be a path under /sys") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(value = ctrlEnable, onValueChange = { ctrlEnable = it.trim() }, label = { Text("Charging on") }, singleLine = true, isError = !valuesOk, modifier = Modifier.weight(1f))
        OutlinedTextField(value = ctrlDisable, onValueChange = { ctrlDisable = it.trim() }, label = { Text("Charging off") }, singleLine = true, isError = !valuesOk, modifier = Modifier.weight(1f))
    }
    Spacer(Modifier.height(8.dp))
    Button(
        enabled = pathOk && valuesOk,
        onClick = { viewModel.updateSmartChargeConfig(config.copy(ctrlPath = ctrlPath, ctrlEnable = ctrlEnable, ctrlDisable = ctrlDisable)) },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Save control file")
    }
}
