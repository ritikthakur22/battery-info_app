<div align="center">

<img src="https://capsule-render.vercel.app/api?type=waving&color=6355B5&height=190&section=header&text=Battery%20Gyan&fontSize=54&fontColor=ffffff&animation=fadeIn&fontAlignY=38&desc=See%20your%20power.%20Understand%20your%20battery.&descAlignY=62&descSize=16" width="100%" alt="Battery Gyan banner" />

<p><strong>A beautiful, readable, offline-first battery dashboard for Android.</strong></p>

<img src="https://img.shields.io/badge/Android-10%2B-00A889?style=flat-square&logo=android&logoColor=white" alt="Android 10+" />
<img src="https://img.shields.io/badge/Kotlin-1.9.22-7F52FF?style=flat-square&logo=kotlin&logoColor=white" alt="Kotlin" />
<img src="https://img.shields.io/badge/Compose-Material%203-6355B5?style=flat-square&logo=jetpackcompose&logoColor=white" alt="Compose" />
<img src="https://img.shields.io/badge/License-GPL--3.0-E79A17?style=flat-square" alt="GPL-3.0" />

<br /><br />
<img src="https://readme-typing-svg.demolab.com?font=JetBrains+Mono&size=16&duration=2600&pause=700&color=6355B5&center=true&vCenter=true&width=500&lines=Live+battery+telemetry;Readable+by+design;Zero+trackers.+Zero+internet.;A+widget+that+stays+out+of+your+way." alt="Animated feature list" />

</div>

## ✨ The quick story

Android’s default battery indicator is tiny and shallow. Battery Gyan turns it into a friendly, glanceable dashboard with oversized type, a lively progress ring, charging guidance, and the details power users actually want.

<table>
<tr><td>⚡ <b>Live telemetry</b><br/>Level, state, plug type, temperature, voltage, current, energy, health and technology.</td><td>🎨 <b>Expressive UI</b><br/>Animated charging pulse, semantic color states, Material 3 surfaces and adaptive iconography.</td></tr>
<tr><td>♿ <b>Accessibility first</b><br/>Adjustable 0.8×–2× type scale, clear labels, strong hierarchy and TalkBack-friendly content.</td><td>🛡️ <b>Private by default</b><br/>No account, ads, analytics, network access or background polling. Ever.</td></tr>
</table>



## 🛠️ Build it

Requires Android Studio with JDK 17 and Android SDK 34.

```bash
git clone https://github.com/ritikthakur22/battery-info_app.git
cd battery-info_app
./gradlew assembleDebug
./gradlew lint
```

Release signing is intentionally local/secret-only. Put values in an ignored `keystore.properties` file when you need a signed build; the repository contains no signing credentials.

## 📦 Project File Tree

```text
app/src/main/java/com/crdy/batterygyan
├── data
│   ├── BatteryRepository.kt
│   ├── local
│   │   └── SettingsDataStore.kt
│   └── SettingsRepository.kt
├── domain
│   └── model
│       ├── BatterySnapshot.kt
│       └── DisplaySettings.kt
├── MainActivity.kt
├── platform
│   ├── battery
│   │   └── AndroidBatteryDataSource.kt
│   └── widget
├── ui
│   ├── components
│   ├── home
│   │   ├── HomeScreen.kt
│   │   └── HomeViewModel.kt
│   ├── settings
│   │   ├── SettingsScreen.kt
│   │   └── SettingsViewModel.kt
│   └── theme
│       ├── Color.kt
│       ├── Theme.kt
│       └── Type.kt
├── util
└── widget
    ├── BatteryGlanceWidget.kt
    └── BatteryWidgetReceiver.kt
```

## 🔐 Privacy & license

Battery Gyan requests zero permissions and has no `INTERNET` declaration. Battery readings never leave the device. The project is available under [GPL-3.0](LICENSE).

<div align="center">
<br />
<img src="https://capsule-render.vercel.app/api?type=waving&color=211846&height=100&section=footer&animation=twinkling" width="100%" alt="Animated footer" />
</div>
