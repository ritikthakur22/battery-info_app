# Battery Gyan v2.0 — Antigravity CLI + Codex CLI Quick Reference

## 📋 Prerequisites

```bash
# Install Antigravity CLI (GitHub management)
curl -sSL https://antigravity.dev/install.sh | bash

# Install Codex CLI (code generation + scaffolding)
npm install -g codex-cli

# Verify Android SDK & Gradle
android --version
./gradlew --version
```

---

## 🚀 Project Setup (Day 1)

### 1. Initialize Git Repo with Antigravity

```bash
cd /home/crdy/testing/app/battery_app/

# Initialize Antigravity (GitHub issue tracking + PRs)
antigravity init --name "battery-gyan" --repo "crdy/battery-gyan"

# Create GitHub remote
git remote add origin https://github.com/crdy/battery-gyan.git
git branch -M main
git push -u origin main
```

### 2. Create Development Branch

```bash
# Feature branch for v2.0 refactor
antigravity branch create --type feature --name "v2-production-release"

# Switch branch
git checkout v2-production-release
```

### 3. Setup Codex Project Structure

```bash
# Generate Android project skeleton (Compose + MVVM + Room)
codex scaffold android \
  --package "com.crdy.batterygyan" \
  --name "Battery Gyan" \
  --target-sdk 35 \
  --min-sdk 29 \
  --architecture "mvvm" \
  --ui "compose" \
  --database "room" \
  --di "hilt"
```

---

## 🏗️ Phase 1: Core Architecture (Week 1-2)

### Task 1.1: Setup Data Layer

```bash
# Use Codex to scaffold Room database
codex generate room-entities \
  --output "app/src/main/java/com/crdy/batterygyan/data/local/db" \
  --entities "BatteryLogEntry,HealthTrendEntry,ChargeConfigEntry"

# Generate DAOs
codex generate room-daos \
  --entities "BatteryLogEntry,HealthTrendEntry" \
  --output "app/src/main/java/com/crdy/batterygyan/data/local/db"

# Generate Database class
codex generate room-database \
  --output "app/src/main/java/com/crdy/batterygyan/data/local/db" \
  --daos "BatteryLogDao,HealthTrendDao"
```

### Task 1.2: Setup DataStore (Preferences)

```bash
# Generate Proto schema for settings
codex generate proto-datastore \
  --name "SettingsPreferences" \
  --fields "theme_mode:string,primary_color:uint32,charge_stop_pct:uint32,charge_resume_pct:uint32" \
  --output "app/src/main/java/com/crdy/batterygyan/data/local/datastore"

# Generate DataStore wrapper
codex generate datastore-preferences \
  --output "app/src/main/java/com/crdy/batterygyan/data/local/datastore"
```

### Task 1.3: Setup Battery Data Source

```bash
# Create data source interface + implementation for battery status
cat > app/src/main/java/com/crdy/batterygyan/data/source/BatteryDataSource.kt << 'EOF'
package com.crdy.batterygyan.data.source

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class BatteryStatus(
    val percent: Int,
    val health: Int,
    val temperature: Float,
    val voltage: Int,
    val isCharging: Boolean,
    val technology: String
)

class AndroidBatteryDataSource(private val context: Context) {
    fun getBatteryStatus(): Flow<BatteryStatus> = callbackFlow {
        val batteryIntentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = context.registerReceiver(null, batteryIntentFilter)
        
        batteryStatus?.let {
            val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val health = it.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)
            val temp = it.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) / 10f
            val voltage = it.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)
            val status = it.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val technology = it.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "Unknown"
            
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING
            
            trySend(BatteryStatus(level, health, temp, voltage, isCharging, technology))
        }
        
        awaitClose { /* cleanup */ }
    }
}
EOF

echo "✅ BatteryDataSource.kt created"
```

### Task 1.4: Setup Root Detection

```bash
# Create root detection utility
cat > app/src/main/java/com/crdy/batterygyan/util/RootDetectionSource.kt << 'EOF'
package com.crdy.batterygyan.util

import android.content.Context
import java.io.File

enum class RootCapability {
    MAGISK, SHIZUKU, NONE
}

object RootDetectionSource {
    fun detectRootCapability(context: Context): RootCapability {
        // Check for Magisk
        if (File("/system/bin/magisk").exists() || File("/data/magisk").exists()) {
            return RootCapability.MAGISK
        }
        
        // Check for Shizuku
        try {
            context.packageManager.getPackageInfo("moe.shizuku.privileged.api", 0)
            return RootCapability.SHIZUKU
        } catch (e: Exception) {
            // Shizuku not installed
        }
        
        return RootCapability.NONE
    }
    
    fun testChargeControlFile(): Boolean {
        val chargeFile = "/sys/class/power_supply/battery/input_current_limit"
        return File(chargeFile).exists() && File(chargeFile).canWrite()
    }
}
EOF

echo "✅ RootDetectionSource.kt created"
```

### Task 1.5: Commit Phase 1

```bash
git add .
antigravity commit --message "Phase 1: Core data layer (Room, DataStore, Battery source)"
```

---

## 🎨 Phase 2: UI Layer (Week 2-3)

### Task 2.1: Generate Compose Screens

```bash
# Generate Home screen scaffold
codex generate compose-screen \
  --name "HomeScreen" \
  --package "com.crdy.batterygyan.ui.screens" \
  --viewmodel "HomeViewModel" \
  --output "app/src/main/java/com/crdy/batterygyan/ui/screens/HomeScreen.kt"

# Generate Customize screen
codex generate compose-screen \
  --name "CustomizeScreen" \
  --package "com.crdy.batterygyan.ui.screens" \
  --viewmodel "CustomizeViewModel" \
  --output "app/src/main/java/com/crdy/batterygyan/ui/screens/CustomizeScreen.kt"
```

### Task 2.2: Generate Custom Compose Components

```bash
# Dual slider component for charge control
cat > app/src/main/java/com/crdy/batterygyan/ui/components/DualSlider.kt << 'EOF'
package com.crdy.batterygyan.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

@Composable
fun DualSlider(
    value1: Float,
    onValue1Change: (Float) -> Unit,
    value2: Float,
    onValue2Change: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..100f,
    modifier: Modifier = Modifier
) {
    // First slider: Stop percentage
    Slider(
        value = value1,
        onValueChange = { newVal ->
            // Ensure value2 (resume) never exceeds value1 (stop)
            if (newVal >= value2) {
                onValue1Change(newVal)
            }
        },
        valueRange = valueRange,
        modifier = modifier
    )
    
    // Second slider: Resume percentage
    Slider(
        value = value2,
        onValueChange = { newVal ->
            if (newVal <= value1) {
                onValue2Change(newVal)
            }
        },
        valueRange = valueRange,
        modifier = modifier
    )
}
EOF

echo "✅ DualSlider.kt created"
```

### Task 2.3: Battery Gauge Component

```bash
cat > app/src/main/java/com/crdy/batterygyan/ui/components/BatteryGauge.kt << 'EOF'
package com.crdy.batterygyan.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun BatteryGauge(
    percentage: Int,
    modifier: Modifier = Modifier
) {
    val color = when {
        percentage >= 80 -> Color.Green
        percentage >= 50 -> Color.Yellow
        else -> Color.Red
    }
    
    Canvas(modifier = modifier.size(200.dp)) {
        val radius = size.minDimension / 2
        val center = size.center
        
        // Draw background circle
        drawCircle(color = Color.LightGray, radius = radius * 0.95f, center = center)
        
        // Draw progress arc
        val sweepAngle = (percentage / 100f) * 360f
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = sweepAngle,
            useCenter = true,
            topLeft = center - radius * 0.95f,
            size = size * 0.95f,
            style = Stroke(width = 8f)
        )
    }
}
EOF

echo "✅ BatteryGauge.kt created"
```

### Task 2.4: Theme System

```bash
# Generate Material 3 theme configuration
codex generate compose-theme \
  --name "BatteryGyanTheme" \
  --package "com.crdy.batterygyan.ui.theme" \
  --colors "primary,secondary,tertiary,accent" \
  --output "app/src/main/java/com/crdy/batterygyan/ui/theme"
```

### Task 2.5: Navigation Sidebar

```bash
cat > app/src/main/java/com/crdy/batterygyan/ui/components/SettingsSidebar.kt << 'EOF'
package com.crdy.batterygyan.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun SettingsSidebar(
    drawerState: DrawerState,
    onHomeClick: () -> Unit,
    onCustomizeClick: () -> Unit,
    onAnalyticsClick: () -> Unit,
    content: @Composable () -> Unit
) {
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            Column {
                NavigationDrawerItem(
                    label = { Text("Home") },
                    selected = false,
                    onClick = onHomeClick
                )
                NavigationDrawerItem(
                    label = { Text("Customize") },
                    selected = false,
                    onClick = onCustomizeClick
                )
                NavigationDrawerItem(
                    label = { Text("Analytics") },
                    selected = false,
                    onClick = onAnalyticsClick
                )
            }
        },
        content = content
    )
}
EOF

echo "✅ SettingsSidebar.kt created"
```

### Task 2.6: Commit Phase 2

```bash
git add app/src/main/java/com/crdy/batterygyan/ui/
antigravity commit --message "Phase 2: UI layer (Screens, Components, Theme)"
```

---

## ⚡ Phase 3: Service & Business Logic (Week 3)

### Task 3.1: Charge Control Service

```bash
cat > app/src/main/java/com/crdy/batterygyan/service/ChargeControlService.kt << 'EOF'
package com.crdy.batterygyan.service

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class ChargeControlService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Read thresholds from DataStore
        GlobalScope.launch {
            // Monitor battery and apply charge control
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            registerReceiver(BatteryMonitorReceiver(this@ChargeControlService), filter)
        }
        
        return START_STICKY // Restart if killed
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
}

class BatteryMonitorReceiver(private val context: Context) : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val level = intent?.getIntExtra("level", 0) ?: return
        
        // Apply charge control logic here
        // if (level >= stopPct) disableCharge()
        // if (level <= resumePct) enableCharge()
    }
}
EOF

echo "✅ ChargeControlService.kt created"
```

### Task 3.2: Boot Receiver

```bash
cat > app/src/main/java/com/crdy/batterygyan/service/BootReceiver.kt << 'EOF'
package com.crdy.batterygyan.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Start ChargeControlService
            val serviceIntent = Intent(context, ChargeControlService::class.java)
            context.startService(serviceIntent)
        }
    }
}
EOF

# Update AndroidManifest.xml
xmllint --shell app/src/main/AndroidManifest.xml << 'XML'
register add /manifest/application/receiver[@android:name=".service.BootReceiver"]
EOF

echo "✅ BootReceiver.kt created & manifest updated"
```

### Task 3.3: Use Cases (Domain Layer)

```bash
codex generate usecase \
  --name "GetBatteryStatusUseCase" \
  --package "com.crdy.batterygyan.domain.usecase" \
  --dependencies "BatteryDataSource" \
  --output "app/src/main/java/com/crdy/batterygyan/domain/usecase/GetBatteryStatusUseCase.kt"

codex generate usecase \
  --name "SetChargeControlUseCase" \
  --package "com.crdy.batterygyan.domain.usecase" \
  --dependencies "ChargeControlSource" \
  --output "app/src/main/java/com/crdy/batterygyan/domain/usecase/SetChargeControlUseCase.kt"
```

### Task 3.4: ViewModels

```bash
codex generate viewmodel \
  --name "HomeViewModel" \
  --package "com.crdy.batterygyan.viewmodel" \
  --usecases "GetBatteryStatusUseCase,GetBatteryHistoryUseCase" \
  --output "app/src/main/java/com/crdy/batterygyan/viewmodel/HomeViewModel.kt"

codex generate viewmodel \
  --name "CustomizeViewModel" \
  --package "com.crdy.batterygyan.viewmodel" \
  --usecases "SetChargeControlUseCase,ApplyThemeUseCase" \
  --output "app/src/main/java/com/crdy/batterygyan/viewmodel/CustomizeViewModel.kt"
```

### Task 3.5: Commit Phase 3

```bash
git add .
antigravity commit --message "Phase 3: Services, use cases, ViewModels"
```

---

## 📊 Phase 4: Analytics & Persistence (Week 3)

### Task 4.1: Battery Analytics Logic

```bash
cat > app/src/main/java/com/crdy/batterygyan/util/BatteryCalculations.kt << 'EOF'
package com.crdy.batterygyan.util

object BatteryCalculations {
    fun estimateHealthDegradation(
        currentCapacityPercent: Int,
        daysSinceFirstCharge: Int
    ): Double {
        // Simple degradation model: 1% per 90 days
        return (100 - currentCapacityPercent).toDouble() / daysSinceFirstCharge * 90
    }
    
    fun estimateLifespanYears(
        healthPercent: Int,
        degradationRate: Double
    ): Double {
        val yearsUntilDegraded = (100 - healthPercent) / degradationRate / 365
        return yearsUntilDegraded
    }
    
    fun calculateDrainRate(
        startPercent: Int,
        endPercent: Int,
        elapsedMinutes: Int
    ): Float {
        return ((startPercent - endPercent).toFloat() / elapsedMinutes.toFloat()) * 60 // mA/hour estimate
    }
}
EOF

echo "✅ BatteryCalculations.kt created"
```

### Task 4.2: Database Operations

```bash
# Generate repository for battery data access
codex generate repository \
  --name "BatteryRepository" \
  --package "com.crdy.batterygyan.data" \
  --dao "BatteryLogDao,HealthTrendDao" \
  --output "app/src/main/java/com/crdy/batterygyan/data/BatteryRepository.kt"
```

### Task 4.3: Commit Phase 4

```bash
git add .
antigravity commit --message "Phase 4: Analytics calculations & database operations"
```

---

## 🧪 Phase 5: Testing & Optimization (Week 4)

### Task 5.1: Unit Tests

```bash
# Generate test files
codex generate test \
  --name "ChargeControlLogicTest" \
  --package "com.crdy.batterygyan.logic" \
  --output "app/src/test/java/com/crdy/batterygyan/logic/ChargeControlLogicTest.kt"

# Run tests
./gradlew test
```

### Task 5.2: Lint & Code Quality

```bash
# Run lint
./gradlew lint

# Generate lint report
./gradlew lintReport

# Apply R8/ProGuard minification rules
# Edit: app/proguard-rules.pro
```

### Task 5.3: Build Release APK & AAB

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK (unsigned, add keystore config)
./gradlew assembleRelease

# Build App Bundle (for Play Store)
./gradlew bundleRelease

# Sign with keystore (use GitHub Secrets for passwords)
jarsigner -verbose -sigalg SHA1withRSA -digestalg SHA1 \
  -keystore /path/to/release.jks \
  app/build/outputs/apk/release/app-release-unsigned.apk \
  release
```

### Task 5.4: Create GitHub Release

```bash
# Create release tag
git tag -a v2.0.0 -m "Battery Gyan v2.0.0 - Production Release"

# Push tag
git push origin v2.0.0

# Create release via Antigravity
antigravity release create \
  --tag v2.0.0 \
  --name "Battery Gyan v2.0.0" \
  --description "Production release with charge control, analytics, theme customization" \
  --artifacts app/build/outputs/apk/release/app-release.apk \
              app/build/outputs/bundle/release/app-release.aab
```

---

## 🚀 Phase 6: Play Store Submission

### Task 6.1: Prepare Play Store Assets

```bash
# Generate Play Store screenshots (use fastlane)
fastlane screenshots

# Prepare metadata
cat > fastlane/metadata/android/en-US/title.txt << 'EOF'
Battery Gyan - Charge Limiter & Analytics
EOF

cat > fastlane/metadata/android/en-US/short_description.txt << 'EOF'
Smart charge limiter supporting Magisk, Shizuku, and non-root with battery analytics.
EOF

cat > fastlane/metadata/android/en-US/full_description.txt << 'EOF'
[Insert full description from spec]
EOF
```

### Task 6.2: Upload to Google Play

```bash
# Authenticate with Google Play
fastlane supply init

# Upload APK to internal test track
fastlane supply --apk app/build/outputs/apk/release/app-release.apk --track internal

# Monitor beta feedback
fastlane supply --track beta

# Promote to production
fastlane supply --promote_to production
```

---

## 🔄 Continuous Integration (GitHub Actions)

### Workflow File

```bash
# Create GitHub Actions workflow
mkdir -p .github/workflows

cat > .github/workflows/build.yml << 'EOF'
name: Build & Test

on: [push, pull_request]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: 17
          distribution: temurin
      
      - name: Build Debug APK
        run: ./gradlew assembleDebug
      
      - name: Run Unit Tests
        run: ./gradlew test
      
      - name: Run Lint
        run: ./gradlew lint
      
      - name: Build Release Bundle
        run: ./gradlew bundleRelease
        env:
          KEYSTORE_PASSWORD: ${{ secrets.KEYSTORE_PASSWORD }}
          KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}
      
      - name: Upload Artifacts
        uses: actions/upload-artifact@v3
        with:
          name: build-artifacts
          path: |
            app/build/outputs/apk/debug/
            app/build/outputs/bundle/release/
EOF

git add .github/
antigravity commit --message "CI/CD: GitHub Actions workflow"
```

---

## 🎯 Daily Development Workflow

### Morning Standup
```bash
# Check PRs and issues
antigravity issues list --status open

# View sprint progress
antigravity board view

# Start working on a task
antigravity issue checkout --id "BATTERY-12"
git checkout feature/BATTERY-12
```

### Committing Changes
```bash
# Make changes, test, then commit with Antigravity
git add .
antigravity commit --message "feat: Add dual slider component for charge control"

# Link to issue
antigravity issue link --id "BATTERY-12" --commit $(git rev-parse HEAD)
```

### Code Review
```bash
# Create pull request
antigravity pr create \
  --title "Feature: Charge control sliders" \
  --base main \
  --issue BATTERY-12

# Monitor review feedback
antigravity pr comments --id <PR_NUMBER>
```

### Merge to Main
```bash
# After approval
antigravity pr merge --id <PR_NUMBER>

# Auto-closes linked issues
git pull origin main
```

---

## 📦 Useful Codex Commands

```bash
# Generate entire feature module
codex scaffold feature --name "analytics" --output "app/src/main/java/..."

# Generate unit test for a class
codex generate test-class --name "BatteryRepositoryTest" --target "BatteryRepository"

# Generate API/networking layer (if needed later)
codex generate retrofit-client --name "BatteryApi" --url "https://api.example.com"

# Generate bottom sheet dialog
codex generate compose-bottom-sheet --name "ChargeControlBottomSheet"

# Generate LazyColumn with pagination
codex generate compose-lazy-list --name "BatteryHistoryList" --paginated true
```

---

## ✅ Quick Checklist

- [ ] Git + Antigravity initialized
- [ ] Codex scaffolding complete
- [ ] Room + DataStore configured
- [ ] Battery data source working
- [ ] Root detection implemented
- [ ] UI screens built
- [ ] Services running
- [ ] Tests passing
- [ ] APK/AAB signed
- [ ] Play Store metadata ready
- [ ] Beta release published
- [ ] Production release live

---

**Build & ship with confidence! 🚀**

