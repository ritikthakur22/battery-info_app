<div align="center">

<img src="https://capsule-render.vercel.app/api?type=waving&color=6355B5&height=190&section=header&text=Battery%20Gyan&fontSize=54&fontColor=ffffff&animation=fadeIn&fontAlignY=38&desc=See%20your%20power.%20Understand%20your%20battery.&descAlignY=62&descSize=16" width="100%" alt="Battery Gyan banner" />

<p><strong>A beautiful, readable battery dashboard for Android — with standard, Root and Shizuku capability paths.</strong></p>

<img src="https://img.shields.io/badge/Android-10%2B-00A889?style=flat-square&logo=android&logoColor=white" alt="Android 10+" />
<img src="https://img.shields.io/badge/Kotlin-1.9.22-7F52FF?style=flat-square&logo=kotlin&logoColor=white" alt="Kotlin" />
<img src="https://img.shields.io/badge/Compose-Material%203-6355B5?style=flat-square&logo=jetpackcompose&logoColor=white" alt="Compose" />
<img src="https://img.shields.io/badge/API-29%2B-00A889?style=flat-square&logo=android" alt="API 29+" />

<br /><br />
<img src="https://readme-typing-svg.demolab.com?font=JetBrains+Mono&size=16&duration=2600&pause=700&color=6355B5&center=true&vCenter=true&width=500&lines=Live+battery+telemetry;Readable+by+design;Root+%2B+Shizuku+capabilities;Ads+you+can+remove+forever." alt="Animated feature list" />

</div>

## ✨ The quick story

Android’s default battery indicator is tiny and shallow. Battery Gyan turns it into a friendly, glanceable dashboard with oversized type, a lively progress ring, charging guidance, and the details power users actually want.

<table>
<tr><td>⚡ <b>Live telemetry</b><br/>Level, state, plug type, temperature, voltage, current, energy, health and technology.</td><td>🎨 <b>Expressive UI</b><br/>Animated charging pulse, semantic color states, Material 3 surfaces and adaptive iconography.</td></tr>
<tr><td>♿ <b>Accessibility first</b><br/>Adjustable type/icon scale, clear labels, strong hierarchy and TalkBack-friendly native controls.</td><td>🛡️ <b>Core stays local</b><br/>Battery data stays on-device; Internet is used only for ads and optional informational JSON.</td></tr>
</table>



## 🧪 Advanced access

Standard battery information works without Root, Shizuku, or Internet. When available, Battery Gyan verifies a real `su -c id` root probe or Shizuku binder/permission state. Charge limiting uses only fixed known sysfs interfaces, validates ranges, writes, reads back, and reports failure when verification is impossible. There is no universal OEM promise.

## 💳 Monetization

The app uses Google Mobile Ads with official test units in development and a one-time non-consumable Play Billing product named `remove_ads_lifetime`. The production price is configured in Play Console; the code does not fake purchases or entitlement state. Restore queries Google Play to recover the ad-free entitlement.

## 🌐 Remote information

Optional announcements and compatibility information are fetched as size-limited, schema-checked HTTPS JSON and cached locally. They never deliver executable code, shell commands, or charge-control behavior. If the network is unavailable, the battery dashboard remains usable.

## 🛠️ Build it

Requires Android Studio with JDK 17 and Android SDK 34.

```bash
git clone https://github.com/ritikthakur22/battery-info_app.git
cd battery-info_app
./gradlew assembleDebug
./gradlew lint
```

Release signing is intentionally local/secret-only. Put values in an ignored `keystore.properties` file when you need a signed build; the repository contains no signing credentials. AdMob test IDs must be replaced with production IDs before publishing.

## 🔐 Privacy & license

Battery Gyan respects your privacy. It requests Internet access only for optional informational updates and Google services (Ads/Billing). Battery readings, local settings, Root checks, and charge-control data remain strictly on your device. 

**License:** All Rights Reserved. This project is closed-source and proprietary. Unauthorized distribution, modification, or commercial use is strictly prohibited.

<div align="center">
<br />
<img src="https://capsule-render.vercel.app/api?type=waving&color=211846&height=100&section=footer&animation=twinkling" width="100%" alt="Animated footer" />
</div>
