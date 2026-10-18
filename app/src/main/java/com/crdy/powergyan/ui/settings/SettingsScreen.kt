package com.crdy.powergyan.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.CircleShape
import com.crdy.powergyan.domain.model.AccentColor
import com.crdy.powergyan.domain.model.ThemeMode
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.crdy.powergyan.data.local.history.BatteryHistoryHelper
import com.crdy.powergyan.data.local.history.toCsv
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.crdy.powergyan.BuildConfig
import com.crdy.powergyan.ui.theme.color
import com.crdy.powergyan.ui.theme.contentOn

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel
) {
    val settings by viewModel.displaySettings.collectAsState()
    val context = LocalContext.current
    var showFaqDialog by remember { mutableStateOf(false) }
    var showLicenseDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Appearance
        Surface(color = MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Appearance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                
                Text("Theme", style = MaterialTheme.typography.labelMedium)
                FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.values().forEach { mode ->
                        FilterChip(
                            selected = settings.themeMode == mode,
                            onClick = { viewModel.setThemeMode(mode) },
                            label = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                Text("Accent color", style = MaterialTheme.typography.labelMedium)
                FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AccentColor.values().forEach { color ->
                        val previewColor = color.color()
                        FilterChip(
                            selected = settings.accentColor == color,
                            onClick = { viewModel.setAccentColor(color) },
                            label = { Text(color.displayName()) },
                            leadingIcon = {
                                Box(
                                    Modifier.size(16.dp).background(previewColor, CircleShape)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = previewColor.copy(alpha = 0.12f),
                                labelColor = previewColor,
                                iconColor = previewColor,
                                selectedContainerColor = previewColor,
                                selectedLabelColor = contentOn(previewColor),
                                selectedLeadingIconColor = contentOn(previewColor)
                            )
                        )
                    }
                }
            }
        }

        DataSection(viewModel)

        // About Section
        Surface(color = MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("About PowerGyan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Version ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                AboutItem("Questions and answers", "How charge control, alarms and root access work", onClick = { showFaqDialog = true })
                AboutItem("Email support", SUPPORT_EMAIL, onClick = {
                    context.openSafely(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$SUPPORT_EMAIL")))
                })
                AboutItem("Privacy policy", "What PowerGyan reads and where it's kept", onClick = {
                    context.openSafely(Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_URL)))
                })
                AboutItem("Terms and conditions", "Root use, warranty and liability", onClick = {
                    context.openSafely(Intent(Intent.ACTION_VIEW, Uri.parse(TERMS_URL)))
                })
                AboutItem("License", "Proprietary software. All rights reserved.", onClick = { showLicenseDialog = true })

                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = {
                    context.openSafely(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${context.packageName}")))
                }, modifier = Modifier.fillMaxWidth()) {
                    Text("Rate on Google Play")
                }
            }
        }
    }

    if (showFaqDialog) {
        AlertDialog(
            onDismissRequest = { showFaqDialog = false },
            title = { Text("Frequently Asked Questions") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    FaqItem("Do I need root?",
                        "Only for Smart Charge and pausing when hot. Battery details, the charge graph and alarms work on any phone.")
                    FaqItem("How does Smart Charge stop charging?",
                        "With root, PowerGyan writes to your phone's charging control file in the kernel, the same switch tools like ACC use. It knows the common files on Qualcomm, Samsung, Pixel and MediaTek phones. Tap Test charge control in Control to confirm it works on yours.")
                    FaqItem("What if the test fails?",
                        "Your phone uses a control file PowerGyan doesn't know yet. Find it in the ACC project's list for your device and enter it under Control > Set control file.")
                    FaqItem("Why does it charge when I plug in below my stop level?",
                        "Plugging in always starts charging up to your stop level, even if you're above the resume level. That way the phone charges when you expect it to.")
                    FaqItem("Why is there always a notification?",
                        "Android only lets apps react to the charger in the background while they show a notification. You can hide it in Android settings without stopping PowerGyan.")
                    FaqItem("What if I uninstall the app while charging is paused?",
                        "PowerGyan turns charging back on when you unplug, turn off Smart Charge, or the monitor stops. If charging ever stays off, restarting the phone always restores it.")
                }
            },
            confirmButton = {
                TextButton(onClick = { showFaqDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showLicenseDialog) {
        AlertDialog(
            onDismissRequest = { showLicenseDialog = false },
            title = { Text("License") },
            text = {
                Text(
                    "Copyright © 2026 Ritik Thakur. All rights reserved.\n\n" +
                        "PowerGyan and its associated documentation are proprietary and closed-source. " +
                        "You may not copy, modify, distribute, sell, lease, or reverse engineer any part " +
                        "of the software without permission."
                )
            },
            confirmButton = {
                TextButton(onClick = { showLicenseDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

private const val SUPPORT_EMAIL = "ritikthakur22in@gmail.com"
// Read-only /view form of the shared Google Docs.
private const val PRIVACY_URL = "https://docs.google.com/document/d/1QGNcFZdpkSznkoxDnnDj6n2Ws_I4q6oQTI017QCWK1A/view"
private const val TERMS_URL = "https://docs.google.com/document/d/1ivVeCogN6e01vrlg085nzoY6cxOPtGOYPNbz6jILQA8/view"

private fun AccentColor.displayName() = when (this) {
    AccentColor.MINT -> "Mint"
    AccentColor.BLUE -> "Cobalt"
    AccentColor.VIOLET -> "Violet"
    AccentColor.ORANGE -> "Amber"
    AccentColor.ROSE -> "Rose"
}

/** Opens [intent] if any app can handle it, instead of crashing on phones without one. */
private fun Context.openSafely(intent: Intent) {
    runCatching { startActivity(intent) }.onFailure {
        android.widget.Toast.makeText(this, "No app found to open this", android.widget.Toast.LENGTH_SHORT).show()
    }
}

/** Export history and back up / restore settings through the system file picker (no storage permission). */
@Composable
private fun DataSection(viewModel: SettingsViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    fun toast(text: String) = android.widget.Toast.makeText(context, text, android.widget.Toast.LENGTH_SHORT).show()
    val today = remember { java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date()) }

    fun write(uri: Uri, text: suspend () -> String, done: String) = scope.launch {
        val ok = withContext(Dispatchers.IO) {
            runCatching { context.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(text().toByteArray()) } }.isSuccess
        }
        toast(if (ok) done else "Couldn't save the file. Try another location.")
    }

    val csvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let { write(it, { BatteryHistoryHelper(context).getHistory().toCsv() }, "History exported") }
    }
    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { write(it, { viewModel.exportSettings() }, "Settings backed up") }
    }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val text = withContext(Dispatchers.IO) {
                // Settings files are tiny; cap the read so a wrong pick can't load something huge.
                runCatching { context.contentResolver.openInputStream(uri)!!.use { String(it.readNBytesCompat(64 * 1024)) } }.getOrNull()
            }
            toast(if (text != null && viewModel.importSettings(text)) "Settings restored" else "That file isn't a PowerGyan backup")
        }
    }

    Surface(color = MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Your data", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            AboutItem("Export battery history", "The last 24 hours as a CSV spreadsheet", onClick = { csvLauncher.launch("powergyan-history-$today.csv") })
            AboutItem("Back up settings", "Save your limits, alarms and theme to a file", onClick = { backupLauncher.launch("powergyan-settings-$today.json") })
            AboutItem("Restore settings", "Load a backup file. Alarm sounds go back to default.", onClick = { restoreLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) })
        }
    }
}

private fun java.io.InputStream.readNBytesCompat(max: Int): ByteArray {
    val out = java.io.ByteArrayOutputStream()
    val buf = ByteArray(8192)
    while (out.size() < max) {
        val n = read(buf, 0, minOf(buf.size, max - out.size()))
        if (n < 0) break
        out.write(buf, 0, n)
    }
    return out.toByteArray()
}

@Composable
private fun FaqItem(question: String, answer: String) {
    Column {
        Text(question, fontWeight = FontWeight.SemiBold)
        Text(answer, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun AboutItem(title: String, desc: String, onClick: (() -> Unit)? = null) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .clickable(enabled = onClick != null, onClick = { onClick?.invoke() })
        .padding(vertical = 8.dp)
    ) {
        Text(title, fontWeight = FontWeight.SemiBold)
        Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
