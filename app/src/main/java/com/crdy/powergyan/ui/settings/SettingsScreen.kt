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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    removeAds: Boolean = false,
    onRemoveAds: () -> Unit = {},
    onRestoreAds: () -> Unit = {}
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
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Appearance
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Appearance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                
                Text("Theme Mode", style = MaterialTheme.typography.labelMedium)
                FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.values().forEach { mode ->
                        FilterChip(
                            selected = settings.themeMode == mode,
                            onClick = { viewModel.setThemeMode(mode) },
                            label = { Text(mode.name) }
                        )
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                Text("Accent Color", style = MaterialTheme.typography.labelMedium)
                FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AccentColor.values().forEach { color ->
                        val previewColor = color.previewColor()
                        FilterChip(
                            selected = settings.accentColor == color,
                            onClick = { viewModel.setAccentColor(color) },
                            label = { Text(color.name) },
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
                                selectedLabelColor = Color.White,
                                selectedLeadingIconColor = Color.White
                            )
                        )
                    }
                }
            }
        }

        // About Section
        // Ad Removal
        if (!removeAds) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Support the Developer", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = onRemoveAds, modifier = Modifier.fillMaxWidth()) {
                        Text("Remove Ads ($1.50)")
                    }
                    TextButton(onClick = onRestoreAds, modifier = Modifier.fillMaxWidth()) {
                        Text("Restore Purchases")
                    }
                }
            }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("About PowerGyan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                
                Text("Version: v1.0", style = MaterialTheme.typography.bodyMedium)
                Text("Package: com.crdy.powergyan", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                
                AboutItem("App Details", "Advanced battery monitoring and smart charge limitation tool.")
                
                AboutItem("FAQs", "Find answers to commonly asked questions.", onClick = { showFaqDialog = true })
                
                AboutItem("Contact Support", "ritikthakur22in@gmail.com", onClick = {
                    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:ritikthakur22in@gmail.com"))
                    context.startActivity(intent)
                })
                
                AboutItem("Developer GitHub", "github.com/ritikthakur22", onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/ritikthakur22"))
                    context.startActivity(intent)
                })

                AboutItem("License", "Proprietary software — All rights reserved", onClick = {
                    showLicenseDialog = true
                })
                
                Spacer(Modifier.height(12.dp))
                Button(onClick = { 
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.crdy.powergyan"))
                    context.startActivity(intent)
                }, modifier = Modifier.fillMaxWidth()) {
                    Text("Rate Us on Play Store")
                }
                
                Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    TextButton(onClick = { 
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com/terms")))
                    }) { Text("Terms & Conditions") }
                    
                    TextButton(onClick = { 
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com/privacy")))
                    }) { Text("Privacy Policy") }
                }
            }
        }
    }

    if (showFaqDialog) {
        AlertDialog(
            onDismissRequest = { showFaqDialog = false },
            title = { Text("Frequently Asked Questions") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text("Q: Do I need Root?", fontWeight = FontWeight.Bold)
                    Text("A: Only for automatic charge limiting (stopping at 80%). The alarms and analytics work without root.")
                    Spacer(Modifier.height(12.dp))
                    
                    Text("Q: How does Smart Charge Control work with Root?", fontWeight = FontWeight.Bold)
                    Text("A: PowerGyan dynamically scans a massive internal database of control files (sysfs) to automatically support almost any rooted device out-of-the-box (Samsung, Pixel, OnePlus, Xiaomi). When your Stop Limit is reached, it executes a silent root command (e.g., su -c \"printf 1 > input_suspend\") to physically sever charging power at the kernel level!")
                    Spacer(Modifier.height(12.dp))

                    Text("Q: Why does it charge when I re-plug the cable below my stop limit?", fontWeight = FontWeight.Bold)
                    Text("A: Smart Re-plug Logic! If your limits are 85% to 90%, and you plug the phone in at 87%, the app assumes you want power. It instantly overrides any previous pauses and forces charging up to 90%.")
                    Spacer(Modifier.height(12.dp))

                    Text("Q: Why does the notification say 'Monitoring' when unplugged?", fontWeight = FontWeight.Bold)
                    Text("A: To bypass strict Android 12+ background execution limits (Doze mode), PowerGyan stays actively asleep in the background using a Foreground Service. This ensures that the exact millisecond you plug your phone in, it instantly enforces limits without you ever having to open the app. If you dislike the unplugged notification, simply hide it in Android Settings!")
                    Spacer(Modifier.height(12.dp))

                    Text("Q: How does Temperature Control work?", fontWeight = FontWeight.Bold)
                    Text("A: It uses software-based thermal protection. PowerGyan continuously monitors the battery temperature. If it exceeds your limit (e.g. 40°C), it applies the exact same sysfs cutoff method as Smart Charge Control to block charging entirely, allowing the device to physically cool down.")
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
                    "Copyright (c) 2026. All Rights Reserved.\n\n" +
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

private fun AccentColor.previewColor() = when (this) {
    AccentColor.MINT -> Color(0xFF00A889)
    AccentColor.BLUE -> Color(0xFF3689E8)
    AccentColor.VIOLET -> Color(0xFF7655D6)
    AccentColor.ORANGE -> Color(0xFFE88A28)
    AccentColor.ROSE -> Color(0xFFD94C72)
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
