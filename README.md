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

## 🧭 How it works

```mermaid
flowchart LR
    A[Android battery broadcast] --> B[AndroidBatteryDataSource]
    B --> C[BatteryRepository]
    C --> D[HomeViewModel]
    D --> E[Animated Compose dashboard]
    B --> F[Battery widget]
    G[Settings] --> H[(Local DataStore)]
    H --> E
    style A fill:#211846,color:#fff,stroke:#00C9A7
    style E fill:#6355B5,color:#fff,stroke:#FFB52E
    style H fill:#EDE9FF,color:#211846,stroke:#6355B5
```

The app is deliberately event-driven: the foreground flow registers only while the screen is observed, and the widget reads the platform battery manager when it renders. That means no service, wake lock, timer, or cloud account is needed.

## 🎬 UI motion, without motion sickness

| Moment | Motion | Meaning |
|---|---|---|
| Battery value changes | Ring eases to the new percentage | The reading is fresh |
| Device is charging | Accent ring breathes gently | Power is flowing |
| Low battery | Accent shifts to warm amber | A clear, non-verbal cue |
| Screen opens | Progress indicator resolves | Data is being read |

## 🧱 Architecture

```mermaid
sequenceDiagram
    participant OS as Android OS
    participant Data as Battery data source
    participant VM as HomeViewModel
    participant UI as Compose UI
    OS->>Data: ACTION_BATTERY_CHANGED
    Data->>VM: BatterySnapshot
    VM->>UI: StateFlow update
    UI->>UI: Animate ring + redraw cards
```

## 🛠️ Build it

Requires Android Studio with JDK 17 and Android SDK 34.

```bash
git clone https://github.com/ritikthakur22/battery-info_app.git
cd battery-info_app
./gradlew assembleDebug
./gradlew lint
```

Release signing is intentionally local/secret-only. Put values in an ignored `keystore.properties` file when you need a signed build; the repository contains no signing credentials.

## 📦 Project map

```text
app/src/main/java/com/crdy/batterygyan/
├── data/       repositories + local DataStore
├── domain/     battery and display models
├── platform/   Android battery event adapter
├── ui/         animated Compose home + settings
└── widget/     responsive Jetpack Glance widget
```

## 🔐 Privacy & license

Battery Gyan requests zero permissions and has no `INTERNET` declaration. Battery readings never leave the device. The project is available under [GPL-3.0](LICENSE).

<div align="center">
<br />
<img src="https://capsule-render.vercel.app/api?type=waving&color=211846&height=100&section=footer&animation=twinkling" width="100%" alt="Animated footer" />
</div>
