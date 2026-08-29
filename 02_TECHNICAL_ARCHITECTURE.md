# Technical Architecture

## 1. Architecture Decision
Use **native Android with Kotlin + Jetpack Compose + Material 3**.

Reasoning:
- Kotlin is the native Android-first language and is required by current Jetpack Compose setup guidance. citeturn499963search2
- Compose is appropriate for a modern configurable UI.
- Native Android APIs provide the lowest-overhead access to battery state and widgets.
- Avoid Flutter for this utility because the product's critical work is native Android battery/widget integration, and Flutter would add runtime/framework overhead without improving the core capability.

## 2. Recommended Stack
Language:
- Kotlin

UI:
- Jetpack Compose
- Material 3
- Material icons where suitable

Widgets:
- Jetpack Glance for widget UI, backed by AppWidget infrastructure.
- Glance is specifically designed for Android app widgets and uses Compose-style Kotlin APIs. citeturn499963search1

State:
- DataStore Preferences for small persistent settings.

Concurrency:
- Kotlin Coroutines.
- Flow only where it adds real value.

Lifecycle:
- AndroidX Lifecycle/ViewModel.

Testing:
- JUnit.
- AndroidX Test.
- Compose UI testing.
- Widget/instrumentation tests.

Build:
- Gradle Kotlin DSL.
- Version Catalog (`libs.versions.toml`).
- Gradle Wrapper committed to Git.

## 3. Layered Structure
```text
UI Layer
 ├─ Compose screens
 ├─ Material 3 theme
 ├─ Accessibility semantics
 └─ Settings controls

Presentation Layer
 ├─ BatteryViewModel
 ├─ UI state
 └─ user actions

Domain Layer
 ├─ BatterySnapshot
 ├─ BatteryState interpretation
 └─ display/configuration rules

Data / Platform Layer
 ├─ BatteryRepository
 ├─ BatteryStateSource
 ├─ WidgetRepository
 └─ SettingsRepository

Android Integration
 ├─ BroadcastReceiver(s)
 ├─ AppWidget/Glance provider
 └─ optional boot handling only when needed
```

## 4. Battery State Acquisition
Use public Android battery APIs.

`Intent.ACTION_BATTERY_CHANGED` is a protected sticky broadcast that carries battery level, charging state, and related information; Android documents it as something to receive through `Context.registerReceiver()` rather than a manifest-declared receiver. citeturn616797search7

Recommended pattern:
1. Read the initial battery snapshot when the app/widget becomes active.
2. Register an in-process receiver while the app UI is visible.
3. Update UI reactively.
4. Let widgets respond to system/widget lifecycle events.
5. Avoid a permanent one-second/minute polling loop.

## 5. Widget Architecture
Use `GlanceAppWidget` + `GlanceAppWidgetReceiver`.

Recommended widget types:
- Small: icon + large percentage.
- Medium: percentage + icon + charging state.
- Large: percentage + icon + selected secondary information.

Use responsive widget layouts. Glance supports responsive sizing modes and Android's widget metadata supports resize limits. citeturn499963search0turn499963search7

Do not design around a permanently updating minute-by-minute timer. Current Android guidance explicitly warns that frequent widget updates can drain battery. citeturn499963search3

## 6. State Model
```text
BatterySnapshot
  percentage: Int
  status: Charging | Discharging | Full | Unknown
  plugged: None | AC | USB | Wireless | Unknown
  temperatureC: Float?
  voltageMv: Int?
  currentUa: Int?
  health: BatteryHealth?
  timestamp: Long
```

```text
DisplaySettings
  textScale: Float
  iconScale: Float
  alignment: Start | Center | End
  themeMode: System | Light | Dark
  textColorMode: Theme | Custom
  iconColorMode: Theme | Custom
  backgroundStyle: Solid | Transparent | Surface
  secondaryInfoEnabled: Boolean
```

Keep these models platform-independent wherever possible.

## 7. Process and Battery Strategy
The app should normally have no long-running process.

### No widget
- No background service.
- UI-driven battery observation only.

### Widget present
- Widget provider receives lifecycle/system update requests.
- Battery updates should be event-driven when practical.
- Conservative scheduled refresh can be used as fallback.
- Do not use WorkManager for every battery percentage change.

### Overlay
Not part of MVP. A floating overlay would require additional permission and a long-running mechanism, conflicting with the strongest product requirement: minimal battery usage.

## 8. Boot Handling
Do not start background work merely because the device booted.

Instead:
- detect whether widgets exist when required by the widget subsystem;
- restore settings automatically through local persistence;
- only register a boot receiver if a concrete supported feature requires it.

User requirement: when widgets are used, automatic availability after reboot should happen without asking the user to reopen the app.

## 9. Lock-Screen Strategy
Do not promise universal lock-screen widget support.

Android's current widget documentation indicates modern Android widget placement behavior is host/OS dependent; Glance's current documentation notes that the `widgetCategory` metadata can describe home-screen/keyguard categories, while also documenting that on Android 5.0+ only `home_screen` is valid for normal app-widget declarations. Therefore, lock-screen behavior must be treated as device/OS/host dependent rather than guaranteed. citeturn499963search0

Product wording:
“Home-screen widget with lock-screen availability on supported devices.”

## 10. Project Structure
```text
app/
  src/main/java/<package>/
    MainActivity.kt

    data/
      BatteryRepository.kt
      SettingsRepository.kt
      local/
        SettingsDataStore.kt

    domain/
      model/
        BatterySnapshot.kt
        DisplaySettings.kt
      BatteryUseCases.kt

    platform/
      battery/
        AndroidBatteryDataSource.kt
      widget/
        BatteryWidgetReceiver.kt

    ui/
      home/
        HomeScreen.kt
        HomeViewModel.kt
      settings/
        SettingsScreen.kt
        SettingsViewModel.kt
      components/
      theme/

    widget/
      BatteryGlanceWidget.kt
      WidgetContent.kt

    util/

  src/main/res/
    xml/
    drawable/
    mipmap/
    values/
```

## 11. Performance Rules
- No periodic coroutine loop just to read battery percentage.
- No database unless battery history becomes a feature.
- DataStore only for settings.
- Do not load remote images/fonts.
- Minimize dependencies.
- Avoid large third-party UI libraries.
- Use static/vector assets.
- Avoid unnecessary recomposition.
- Keep widget content simple.
- Profile release builds on low-end hardware.

## 12. Build Configuration
Recommended:
```text
minSdk = 29
targetSdk = current Play requirement at release
compileSdk = latest stable installed/approved for the project
kotlin = current stable compatible with chosen Android/Compose toolchain
compose BOM = current stable
glance = current stable
```

At the time of this specification, Jetpack Glance 1.2.0 is the stable release line; 1.3.0-alpha02 exists, so production should use the stable line unless a required feature exists only in alpha. citeturn499963search4

## 13. Antigravity CLI Development Strategy
The project should be easy for an AI coding CLI to navigate.

Repository root should contain:
```text
README.md
AGENTS.md
PROJECT_SPEC.md
ARCHITECTURE.md
SECURITY.md
TEST_PLAN.md
TODO.md
app/
gradle/
gradlew
gradlew.bat
settings.gradle.kts
build.gradle.kts
gradle/libs.versions.toml
```

`AGENTS.md` should state:
- never add INTERNET permission;
- no third-party analytics;
- no ads;
- no long-running service without explicit architecture approval;
- use public Android APIs only;
- preserve minSdk 29;
- update docs when architecture changes;
- run unit + instrumentation/Compose tests before marking work complete.