# Battery Gyan v2.0 — Production Specification & Architecture Guide
**Status**: Ready for Development (Antigravity CLI + Codex CLI)  
**Target Release**: Q3 2026 Google Play Store  
**Version**: 2.0.0  
**Package**: `com.crdy.batterygyan`

---

## 📋 Executive Summary

**Battery Gyan** is a premium, open-source Android battery charge limiter supporting **three root methods** (Magisk → Shizuku → non-root fallback) with real-time analytics. All features are **free to all users**; the **$1.50 lifetime purchase removes ads only**.

- ✅ Production-grade charge control (slider-based, safe, verified)
- ✅ Multi-root support (Magisk + Shizuku + non-root graceful degradation)
- ✅ Full theme customization (Material 3 + Material You)
- ✅ Battery analytics dashboard (24h graph, health estimation, degradation trend)
- ✅ Offline-first, zero tracking, GDPR-compliant
- ✅ Optimized for battery efficiency & UI performance
- ✅ Play Store premium standards (Material 3 UI, adaptive icons, accessibility)

---

## 🎯 Core Features (All Tiers)

### 1. **Charge Control Engine**

#### Root (Magisk)
```
User sets: STOP% = 99, RESUME% = 75

On device boot → Service starts
When battery ≥ STOP% → Write "0" to /sys/class/power_supply/battery/input_current_limit
                         Read-back verify for confirmation
When battery ≤ RESUME% → Write "1" to /sys/class/power_supply/battery/input_current_limit
                          Resume charging
```

**File Targets** (auto-detect + prioritize):
- `/sys/class/power_supply/battery/input_current_limit` (generic)
- `/sys/class/power_supply/battery/charging_enabled` (some OEMs)
- `/sys/devices/platform/gpio-charger/power_supply/battery/status` (legacy)
- Fallback: Generic BatteryManager poll (no actual control, warnings only)

**Safety**:
- Read-back verification: After write, read value → confirm match
- Immutable thresholds: RESUME always ≤ STOP (UI prevents invalid states)
- Reset button: Clear all charge control files (red button, root-only)
- Log all writes: Store in Room DB for debugging

#### Shizuku (Fallback Root)
- Same slider UI + capability detection
- Use Shizuku binder to execute `su -c` commands (if no native Magisk)
- Graceful degradation if permission denied

#### Non-Root (Degraded Mode)
- Same slider UI (visual parity)
- **No actual charge control** (device controls charging)
- **Local alarm at threshold**: When battery reaches STOP%, play sound + notification
- Sound options: Pre-installed (~8 ringtones) OR custom audio import
- Notification: "Battery at 99% — Unplug to prevent degradation"

---

### 2. **Sound/Alarm System (Non-Root)**

**Pre-installed Ringtones** (~8):
1. Beep (single tone, 500ms)
2. Alarm (rising tone, 2s)
3. Siren (warning sound, 3s)
4. Notification (soft chime)
5. Alert (loud bell)
6. Whistle (pitched warning)
7. Buzzer (electronic buzz)
8. System default (device alarm tone)

**Custom Import**:
- File picker: `.mp3`, `.wav`, `.ogg` (max 10MB)
- User-selectable in Customize tab
- Stored in app's files directory (private)
- Playback via MediaPlayer (repeats until dismissed)

**Threshold Triggers**:
- On STOP% reached: Play selected sound + show notification
- Dismissable: Swipe notification OR tap "OK" button
- Repeat: Every 5 minutes if threshold still active (optional toggle)

---

### 3. **Battery Analytics Dashboard**

**Display Metrics**:
1. **Current Status**: Percentage, Health %, Temperature, Voltage
2. **24h Graph**: Charge/discharge curve (time vs %)
3. **Drain Rate**: mA/hour calculation
4. **Health Estimation**: Capacity loss % (degradation trend)
5. **Cycle Count**: (if device exposes via BatteryManager)
6. **Predicted Lifespan**: Degradation curve projection

**Data Collection** (Room SQLite):
```sql
CREATE TABLE battery_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    timestamp LONG NOT NULL,
    battery_pct INTEGER,
    health_pct INTEGER,
    temp_c REAL,
    voltage_mv INTEGER,
    is_charging BOOLEAN,
    charge_current_ma INTEGER
);

CREATE TABLE health_trend (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    date TEXT UNIQUE,
    health_pct INTEGER,
    capacity_degradation_pct REAL
);
```

**Graph Rendering**:
- Library: Compose Canvas OR Vico (modern Compose charting)
- Auto-scale X (24h) and Y (0-100%)
- Interactive: Tap point to show timestamp + details
- Refresh: Every 30 seconds (user background)

---

### 4. **Theme Customization**

**Color Picker System**:
- Material 3 native: Accent + Primary + Secondary + Tertiary
- Default: System Material You (Android 12+)
- Picker UI: 12-color palette grid + custom hex input
- Live preview: Show accent in UI elements real-time
- Persist: DataStore JSON

**Theme Modes**:
- System (follows device settings)
- Light (always light)
- Dark (always dark)

**UI Components Affected**:
- Sliders: Accent color thumb
- Buttons: Primary color
- Cards: Secondary tint
- Text highlights: Tertiary

---

### 5. **Boot-Persistent Auto-Start**

**Mechanism**:
```kotlin
// AndroidManifest.xml
<receiver android:name=".BootReceiver"
          android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.BOOT_COMPLETED" />
    </intent-filter>
</receiver>

// BootReceiver.kt
override fun onReceive(context: Context, intent: Intent) {
    if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
        // Restore last saved thresholds from DataStore
        val stop = dataStore.getInt("charge_stop_pct", 80)
        val resume = dataStore.getInt("charge_resume_pct", 75)
        
        // Start charge control service
        context.startService(Intent(context, ChargeControlService::class.java))
    }
}
```

**Root Detection** (On Boot):
1. Check for `/system/bin/magisk` OR `/data/magisk` (Magisk)
2. Check for Shizuku service availability
3. Try `/sys/class/power_supply/battery/input_current_limit` write-test
4. If all fail: Fall back to non-root mode (alarms only)

**Service**:
- Foreground service (Android 8+): Persistent notification
- WorkManager fallback: 15-min periodic task for reliability
- Stop criteria: User disables in settings OR uninstall

---

### 6. **Widget & Quick Settings Tile**

**Home Screen Widget**:
- Size: Small (2x2) + Medium (4x2)
- Data: Current %, Health %, Temp, Charge status
- Action: Tap to open app
- Update: Every 5 minutes OR on BatteryChanged broadcast

**Quick Settings Tile** (Android 7+, optional):
- Label: "Charge Control On/Off"
- Action: Tap to toggle charge limiter
- State indicator: Green (active) / Gray (inactive)

---

## 🏗️ Architecture

### Directory Structure
```
battery_gyan/
├── app/src/main/
│   ├── java/com/crdy/batterygyan/
│   │   ├── ui/
│   │   │   ├── screens/
│   │   │   │   ├── HomeScreen.kt
│   │   │   │   └── CustomizeScreen.kt
│   │   │   ├── components/
│   │   │   │   ├── DualSlider.kt
│   │   │   │   ├── BatteryGauge.kt
│   │   │   │   ├── ColorPicker.kt
│   │   │   │   └── SettingsSidebar.kt
│   │   │   └── theme/
│   │   │       ├── Theme.kt
│   │   │       ├── Color.kt
│   │   │       └── Typography.kt
│   │   ├── domain/
│   │   │   ├── usecase/
│   │   │   │   ├── GetBatteryStatusUseCase.kt
│   │   │   │   ├── SetChargeControlUseCase.kt
│   │   │   │   ├── GetBatteryHistoryUseCase.kt
│   │   │   │   └── ApplyThemeUseCase.kt
│   │   │   └── model/
│   │   │       ├── BatteryStatus.kt
│   │   │       ├── ChargeConfig.kt
│   │   │       └── ThemeConfig.kt
│   │   ├── data/
│   │   │   ├── local/
│   │   │   │   ├── db/
│   │   │   │   │   ├── BatteryDatabase.kt
│   │   │   │   │   ├── BatteryLogDao.kt
│   │   │   │   │   └── HealthTrendDao.kt
│   │   │   │   └── datastore/
│   │   │   │       ├── SettingsDataStore.kt
│   │   │   │       └── settingsPreferences.proto
│   │   │   └── source/
│   │   │       ├── BatteryDataSource.kt
│   │   │       ├── ChargeControlSource.kt
│   │   │       ├── RootDetectionSource.kt
│   │   │       └── ShizukuSource.kt
│   │   ├── service/
│   │   │   ├── ChargeControlService.kt
│   │   │   ├── BatteryMonitorService.kt
│   │   │   ├── AlarmNotificationService.kt
│   │   │   └── BootReceiver.kt
│   │   ├── viewmodel/
│   │   │   ├── HomeViewModel.kt
│   │   │   ├── CustomizeViewModel.kt
│   │   │   └── AnalyticsViewModel.kt
│   │   ├── util/
│   │   │   ├── RootCommandExecutor.kt
│   │   │   ├── ShizukuHelper.kt
│   │   │   ├── BatteryCalculations.kt
│   │   │   ├── LoggingUtil.kt
│   │   │   └── Constants.kt
│   │   └── MainActivity.kt
│   ├── res/
│   │   ├── drawable/ (adaptive icons, vector assets)
│   │   ├── values/ (colors, strings, dimens)
│   │   ├── values-night/ (dark theme)
│   │   └── raw/ (ringtones: beep.wav, alarm.wav, etc.)
│   └── AndroidManifest.xml
├── build.gradle.kts
├── README.md
└── fastlane/metadata/

```

### State Management (MVVM + Flow)

```kotlin
// HomeViewModel.kt
data class HomeUiState(
    val batteryPercent: Int = 0,
    val healthPercent: Int = 100,
    val temperature: Float = 25f,
    val voltage: Int = 4200,
    val isCharging: Boolean = false,
    val chargeStopPct: Int = 80,
    val chargeResumePct: Int = 75,
    val rootCapability: RootCapability = RootCapability.NONE,
    val isLoading: Boolean = false,
    val error: String? = null
)

class HomeViewModel(
    private val getBatteryStatus: GetBatteryStatusUseCase,
    private val chargeControl: SetChargeControlUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getBatteryStatus().collect { status ->
                _uiState.update { it.copy(
                    batteryPercent = status.percent,
                    healthPercent = status.health,
                    temperature = status.temperature,
                    voltage = status.voltage,
                    isCharging = status.isCharging
                )}
            }
        }
    }

    fun setChargeThresholds(stop: Int, resume: Int) {
        viewModelScope.launch {
            chargeControl(stop, resume)
        }
    }
}
```

### Service Architecture

```kotlin
// ChargeControlService.kt (Foreground Service)
class ChargeControlService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createForegroundNotification("Charge control active")
        startForeground(NOTIFICATION_ID, notification)
        
        // Fetch current thresholds from DataStore
        lifecycleScope.launch {
            settingsDataStore.chargeStop.collect { stopPct ->
                settingsDataStore.chargeResume.collect { resumePct ->
                    monitorBattery(stopPct, resumePct)
                }
            }
        }
        return START_STICKY // Restart if killed
    }

    private fun monitorBattery(stop: Int, resume: Int) {
        val batteryFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        registerReceiver(
            object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    val level = intent?.getIntExtra("level", 0) ?: return
                    when {
                        level >= stop -> dischargeCharge() // Stop
                        level <= resume -> enableCharge()  // Resume
                    }
                }
            }, batteryFilter
        )
    }

    private fun dischargeCharge() {
        when (rootCapability) {
            RootCapability.MAGISK -> {
                val cmd = "echo 0 > /sys/class/power_supply/battery/input_current_limit"
                RootCommandExecutor.execute(cmd)
            }
            RootCapability.SHIZUKU -> {
                ShizukuHelper.execute(cmd)
            }
            RootCapability.NONE -> {
                showAlarmNotification("Battery at threshold - unplug to save health")
            }
        }
    }
}
```

---

## 📱 UI/UX Design System

### Navigation
**Primary**: Left Sidebar (hamburger icon)
- Home
- Customize
- Settings
- About
- GitHub
- Rate App
- Changelog

**Gesture**: Swipe left-to-right opens sidebar

**Screens**:

#### Home Screen
```
┌─────────────────────────────────┐
│ ☰  Battery Gyan          ⚙️     │  <- Hamburger + settings icon
├─────────────────────────────────┤
│                                 │
│         ╭─────────╮             │
│         │   83%   │  Charging   │  <- Large animated gauge
│         ╰─────────╯             │
│                                 │
│  Health: 95%  |  Temp: 38°C    │  <- Quick stats
│  Voltage: 4200mV  |  +250mA    │
│                                 │
│  ┌─────────────────────────────┐│
│  │  📊 Analytics               ││  <- Tap to view graph
│  └─────────────────────────────┘│
│  ┌─────────────────────────────┐│
│  │  ⚡ Charge Control: ACTIVE  ││  <- Status badge
│  │  Stop: 80%  |  Resume: 75%  ││
│  └─────────────────────────────┘│
│                                 │
│  [  Open Customize  ]           │
│                                 │
└─────────────────────────────────┘
```

#### Customize Screen
```
┌─────────────────────────────────┐
│ ☰  Customize              ✓     │
├─────────────────────────────────┤
│                                 │
│  🔌 Charge Control              │
│  Root Mode: ✓ Magisk            │
│                                 │
│  Stop Charging At:              │
│  [=====●════] 80%               │  <- Dual slider
│                                 │
│  Resume Charging At:            │
│  [===●═══════] 75%              │
│                                 │
│  Status: Write verified ✓       │  <- Real-time feedback
│                                 │
│  [  Reset Charge Control  ] (red)│
│                                 │
│  ─────────────────────────────  │
│                                 │
│  🔊 Alarm Sound (Non-root mode) │
│  Selected: Alert                │
│  [▼ Dropdown]                   │
│  [+ Import Custom Audio]        │
│                                 │
│  ─────────────────────────────  │
│                                 │
│  🎨 Theme Customization         │
│  Mode: [System ▼]               │
│                                 │
│  Primary Color:                 │
│  [■] [■] [■] [■] [■]           │  <- Color palette
│  [■] [■] [■] [■] [■]           │
│  [  Custom Hex: #1F51BA  ]      │
│                                 │
│  (Secondary, Tertiary, Accent   │
│   color pickers similar)        │
│                                 │
│  ─────────────────────────────  │
│  Preview: [Accent example text] │
│                                 │
└─────────────────────────────────┘
```

#### Analytics Screen
```
┌─────────────────────────────────┐
│ ☰  Analytics              📊    │
├─────────────────────────────────┤
│                                 │
│  ┌──────────────────────────┐  │
│  │ 100% ┌────────────────── │  │  <- 24h charge graph
│  │      │     ╱╲╱╲ ╱╲      │  │
│  │ 50%  │╱╲╱╲╱  ╲╱  ╲ ╱   │  │
│  │      │                  │  │
│  │ 0%   └──────────────────│  │
│  │       00:00      12:00  │  │
│  └──────────────────────────┘  │
│                                 │
│  📈 Health Estimation           │
│  Current Health: 95%            │
│  Degradation: -5% (6 months)    │
│  Predicted Lifespan: 4.2 years  │
│                                 │
│  Cycle Count: 247               │
│  Drain Rate: -180 mA/hour       │
│                                 │
│  ─────────────────────────────  │
│                                 │
│  [  Export Data (JSON)  ]       │
│                                 │
└─────────────────────────────────┘
```

#### Settings/About Sidebar
```
┌─────────────────────┐
│ ⬅️  Settings        │
├─────────────────────┤
│ Home                │
│ Customize           │
│ Analytics           │
├─────────────────────┤
│ About               │
│ Version: 2.0.0      │
│ Changelog           │
│ GitHub Repo         │
├─────────────────────┤
│ Rate on Play Store  │
│ ❤️ Donate/Support   │
├─────────────────────┤
│ Privacy Policy      │
│ Open Source Licenses│
└─────────────────────┘
```

---

## 🔐 Permissions & Safety

### Requested Permissions
```xml
<!-- AndroidManifest.xml -->
<uses-permission android:name="android.permission.BATTERY_STATS" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.READ_LOGS" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
```

### Permission Justification

| Permission | Purpose | Risk |
|-----------|---------|------|
| `BATTERY_STATS` | Read battery status, health, temp, voltage | Low (read-only) |
| `INTERNET` | Download AdMob ads (play.google.com safe list) | Low (sandbox) |
| `READ_LOGS` | Detect `/system/bin/magisk` presence + charge files | Low (read-only sysfs) |
| `RECEIVE_BOOT_COMPLETED` | Auto-start charge control on device boot | Medium (persistent) |
| `FOREGROUND_SERVICE` | Persistent notification for background service | Medium (user-visible) |

### Security Checklist
- ✅ No hardcoded secrets (passwords, API keys)
- ✅ Keystore NOT tracked in Git (use GitHub Secrets for CI/CD)
- ✅ All root commands: Static, verified, immutable
- ✅ No remote code execution (no shell commands from network)
- ✅ No executable downloads
- ✅ Read-back verification: After sysfs write, confirm value
- ✅ Safe slider bounds: UI enforces RESUME ≤ STOP ≤ 99%
- ✅ Offline-first: No cloud dependencies (except AdMob)
- ✅ GDPR-compliant: No personal data collection

---

## 📊 Database Schema

### DataStore (Proto Buf / JSON)
```protobuf
syntax = "proto3";

message SettingsPreferences {
    string theme_mode = 1; // "system", "light", "dark"
    uint32 primary_color = 2; // Hex RGB
    uint32 secondary_color = 3;
    uint32 tertiary_color = 4;
    uint32 accent_color = 5;
    
    uint32 charge_stop_pct = 6;    // 1-99
    uint32 charge_resume_pct = 7;  // 1-stop_pct
    string selected_alarm_sound = 8; // "default" or filename
    bool is_premium = 9;
    int64 last_charge_control_write = 10; // timestamp
}
```

### Room Database

```kotlin
@Entity(tableName = "battery_log")
data class BatteryLogEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long,
    val batteryPct: Int,
    val healthPct: Int,
    val tempCelsius: Float,
    val voltageMv: Int,
    val isCharging: Boolean,
    val chargeCurrentMa: Int
)

@Entity(tableName = "health_trend")
data class HealthTrendEntry(
    @PrimaryKey val date: String, // "YYYY-MM-DD"
    val healthPct: Int,
    val capacityDegradationPct: Double
)
```

---

## 🚀 Development Roadmap

### Phase 1: MVP (Week 1-2)
- [ ] Core charge control (Magisk read/write)
- [ ] Home screen + battery gauge
- [ ] Customize sliders (stop/resume)
- [ ] Foreground service + boot receiver
- [ ] Basic theme (Material 3 defaults)

### Phase 2: Full Feature (Week 2-3)
- [ ] Shizuku integration
- [ ] Non-root alarm + sound system
- [ ] Full theme customization (color picker)
- [ ] Analytics + Room database
- [ ] Left sidebar navigation
- [ ] Widget

### Phase 3: Polish (Week 3-4)
- [ ] Play Store optimization (screenshots, description)
- [ ] AdMob integration (test IDs → production)
- [ ] Billing (Play Billing Library v7+)
- [ ] Unit tests (charge control logic)
- [ ] Lint + R8 minification
- [ ] CI/CD validation

### Phase 4: Launch (Week 4)
- [ ] Internal testing (physical devices: Magisk, Shizuku, non-root)
- [ ] Beta release (Google Play internal test track)
- [ ] Production release
- [ ] Post-launch monitoring

---

## 🛠️ Build Configuration

### Gradle Dependencies
```kotlin
// build.gradle.kts
dependencies {
    // Jetpack
    implementation("androidx.compose.ui:ui:1.6.0")
    implementation("androidx.compose.material3:material3:1.2.0")
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    implementation("androidx.datastore:datastore-core:1.0.0")
    implementation("androidx.protobuf:protobuf-lite:3.21.4")
    implementation("androidx.room:room-runtime:2.6.0")
    implementation("androidx.room:room-ktx:2.6.0")
    implementation("androidx.work:work-runtime-ktx:2.8.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.0")
    
    // Hilt (Dependency Injection)
    implementation("com.google.dagger:hilt-android:2.48")
    kapt("com.google.dagger:hilt-compiler:2.48")
    
    // Shizuku
    implementation("dev.rikka.shizuku:api:13.1.0")
    implementation("dev.rikka.shizuku:provider:13.1.0")
    
    // AdMob
    implementation("com.google.android.gms:play-services-ads:22.6.0")
    
    // Play Billing
    implementation("com.android.billingclient:billing-ktx:7.1.1")
    
    // Charting (for analytics graph)
    implementation("com.patrykandpatrick.vico:compose:1.14.0")
    
    // Logging
    implementation("com.jakewharton.timber:timber:5.0.1")
    
    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.0")
    testImplementation("androidx.room:room-testing:2.6.0")
    
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}

plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
    kotlin("plugin.serialization")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.crdy.batterygyan"
    compileSdk = 35
    
    defaultConfig {
        applicationId = "com.crdy.batterygyan"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "2.0.0"
    }
    
    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }
    
    buildFeatures {
        compose = true
    }
    
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.10"
    }
}
```

---

## 🎮 Testing Strategy

### Unit Tests
```kotlin
// ChargeControlLogicTest.kt
class ChargeControlLogicTest {
    @Test
    fun `slider prevents resume gt stop`() {
        val stop = 80
        val resume = 85
        val isValid = resume <= stop
        assertFalse(isValid)
    }

    @Test
    fun `read-back verification confirms write`() {
        val written = "0"
        val readBack = "/sys/class/power_supply/battery/input_current_limit".readFile()
        assertEquals(written, readBack)
    }

    @Test
    fun `battery threshold triggers charge control`() {
        val battery = 99
        val stop = 80
        val shouldStop = battery >= stop
        assertTrue(shouldStop)
    }
}
```

### Functional Testing (Physical Devices)
1. **Magisk Device**: Verify charge control writes to sysfs
2. **Shizuku Device**: Verify binder communication
3. **Non-Root Device**: Verify alarm + sound playback
4. **Boot Test**: Restart device, verify auto-start
5. **Threshold Test**: Charge to STOP%, verify stop; drain to RESUME%, verify resume
6. **UI Test**: Verify slider bounds, theme changes, sidebar navigation

---

## 📦 Play Store Optimization

### App Title
```
Battery Gyan - Charge Limiter & Analytics
```

### Short Description
```
Charge limiter with battery analytics. Works on Magisk, Shizuku, or non-root (alarm mode). 
All features free. $1.50 lifetime: ad-free. Full theme customization, 24h graph, health estimation.
```

### Full Description
```
Battery Gyan is a premium Android battery charge limiter & analytics tool.

🔌 CHARGE CONTROL
• Magisk: Direct sysfs control (safest, fastest)
• Shizuku: Binder-based control (no root)
• Non-Root: Local alarm at threshold (notification-based)
• Dual slider: Set STOP% (1-99) and RESUME% (1-STOP%)
• Safety: Read-back verification after every write
• Boot-persistent: Auto-starts on device reboot

📊 ANALYTICS
• 24-hour charge/discharge graph
• Battery health estimation (capacity degradation %)
• Cycle count (if device exposes)
• Temperature, voltage, drain rate monitoring
• Predicted lifespan degradation curve

🎨 CUSTOMIZATION
• Full Material 3 theme engine
• Primary + Secondary + Tertiary + Accent color pickers
• Light / Dark / System theme modes
• Custom alarm sounds (non-root) + built-in ringtones
• Responsive design (phones, tablets, foldables)

🛡️ PRIVACY & SAFETY
• Offline-first: No cloud sync, no tracking
• GDPR-compliant: No personal data collection
• Open-source (GPL-3.0)
• Safe commands: Immutable, verified, graceful fallback

💰 PRICING
$1.50 lifetime: Removes ads only
ALL FEATURES available to all users (free + premium identical)

⚙️ REQUIREMENTS
• Android 10+ (minSdk 29)
• For Magisk: Magisk installed
• For Shizuku: Shizuku app + ADB grant
• Non-Root: Works on all devices (degraded mode)

🔗 LINKS
GitHub: github.com/crdy/battery-gyan
Report Issues: github.com/crdy/battery-gyan/issues
```

### Screenshots (8 recommended)
1. Home screen + battery gauge
2. Customize sliders (Magisk mode)
3. Theme customization (color picker)
4. Analytics dashboard (24h graph)
5. Non-root alarm mode + sound selection
6. Settings sidebar + menu
7. Widget preview
8. Premium badge (ad-free)

### Metadata
- **Category**: Tools
- **Content Rating**: 3+ (no mature content)
- **Target Audience**: Power users, battery enthusiasts, root community
- **Keywords**: battery limiter, charge control, Magisk, analytics, battery health
- **Support Email**: contact@ritikthakur.com.np
- **Privacy Policy URL**: (link to Privacy.md in GitHub repo)

---

## 🔄 CI/CD Pipeline (GitHub Actions)

### Build Workflow
```yaml
name: Build & Release

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
      
      - name: Run Tests
        run: ./gradlew test
      
      - name: Run Lint
        run: ./gradlew lint
      
      - name: Build Release AAB
        run: ./gradlew bundleRelease
      
      - name: Upload to GitHub Releases
        uses: softprops/action-gh-release@v1
        with:
          files: |
            app/build/outputs/apk/debug/app-debug.apk
            app/build/outputs/bundle/release/app-release.aab
```

---

## ✅ Launch Checklist

- [ ] Code review + testing (all 3 root methods)
- [ ] Play Store screenshots + description
- [ ] AdMob production IDs (replace test IDs)
- [ ] Play Billing configuration (remove_ads_lifetime product)
- [ ] GitHub release artifacts signed + uploaded
- [ ] Privacy Policy published
- [ ] README updated with v2.0 features
- [ ] Changelog drafted for Play Store
- [ ] Beta release (internal test track)
- [ ] Monitor crash reports + user feedback
- [ ] Production release

---

## 📞 Questions Before Implementation?

1. **Custom notification sounds import**: File browser or audio picker?
2. **Analytics export**: JSON only, or CSV + Charts?
3. **Automation integration**: Tasker broadcast intent broadcast later?
4. **OEM-specific providers**: Add Xiaomi/Samsung/OnePlus providers post-launch?
5. **Premium features roadmap**: Multiple profiles, scheduling post-launch?
6. **Localization**: English only, or multi-language (Hindi, Nepali)?
7. **CI/CD keys**: Use GitHub Secrets for signing config?

---

**Ready to build with Antigravity CLI + Codex CLI? Let's ship Battery Gyan to Play Store! 🚀**
