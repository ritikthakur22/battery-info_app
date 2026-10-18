<div align="center">

<img src="docs/images/powergyan-feature-graphic.png" width="100%" alt="PowerGyan charge limiter and battery analytics banner" />

<p><strong>PowerGyan - Charge Limiter &amp; Battery Analytics [Root]</strong></p>

<img src="https://img.shields.io/badge/Android-10%2B-3689E8?style=flat-square&logo=android&logoColor=white" alt="Android 10+" />
<img src="https://img.shields.io/badge/Kotlin-1.9.22-7F52FF?style=flat-square&logo=kotlin&logoColor=white" alt="Kotlin" />
<img src="https://img.shields.io/badge/Jetpack%20Compose-Material%203-3689E8?style=flat-square&logo=jetpackcompose&logoColor=white" alt="Jetpack Compose Material 3" />
<img src="https://img.shields.io/badge/API-29%2B-00A889?style=flat-square&logo=android" alt="API 29+" />

<br /><br />
<img src="https://readme-typing-svg.demolab.com?font=JetBrains+Mono&size=16&duration=2600&pause=700&color=3689E8&center=true&vCenter=true&width=540&lines=Live+battery+telemetry;Readable+by+design;Root+charge+control+when+verified;Widgets+and+local+alerts." alt="Animated feature list" />

</div>

PowerGyan is an Android battery dashboard for users who want clear battery information, local alerts, widgets, and optional capability-based charge control. Standard battery information works without root or internet access.

## App preview

This is the interface included in the `v1.0.5` APK. The generated feature image summarizes the product direction; the screenshots are captured from the running app.

Play Console icon: [powergyan-app-icon.png](docs/images/powergyan-app-icon.png), a 512×512 PNG exported from the adaptive launcher icon.

Play Console feature graphic: [powergyan-feature-graphic.png](docs/images/powergyan-feature-graphic.png) — 1024×500 RGB PNG.

<div align="center">
<img src="docs/images/powergyan-feature-hero.png" alt="PowerGyan battery analytics and charge control feature overview" width="860" />
</div>

<table>
<tr>
<td align="center"><img src="docs/screenshots/home-dashboard.png" alt="PowerGyan home battery dashboard" width="220" /><br /><sub>Home dashboard</sub></td>
<td align="center"><img src="docs/screenshots/navigation-drawer.png" alt="PowerGyan navigation drawer" width="220" /><br /><sub>Navigation drawer</sub></td>
<td align="center"><img src="docs/screenshots/battery-details.png" alt="PowerGyan battery details and curve" width="220" /><br /><sub>Battery details</sub></td>
</tr>
<tr>
<td align="center"><img src="docs/screenshots/settings-appearance.png" alt="PowerGyan appearance settings" width="220" /><br /><sub>Appearance settings</sub></td>
<td align="center"><img src="docs/screenshots/charge-limiter-alarms.png" alt="PowerGyan charge limiter and alarms" width="220" /><br /><sub>Charge limiter and alarms</sub></td>
<td align="center">⚡<br /><sub>Root and non-root capability paths</sub></td>
</tr>
</table>

## Features

**Works on any phone**
- Live battery percentage, status, power (W), temperature, voltage, current and charge cycles where the phone reports them.
- 24-hour battery graph, charging-speed chart (power at each battery level) and time-to-full estimate.
- Battery wear: estimated capacity compared with the design capacity.
- Screen-on vs screen-off drain rate since you last unplugged.
- Charge alarm at a chosen level and at 100%, low-battery warning, and plug/unplug sounds (system sounds or your own files).
- Home-screen widget with level, status and a 24-hour line.
- Export battery history as CSV; back up and restore settings to a file.
- Light, dark and system themes with five accent colors.

**With root (Magisk, KernelSU or APatch)**
- Smart Charge: stop charging at a level and start again when it drops.
- Lower stop level on wireless chargers.
- Full by a set time: hold at your limit overnight, then top up to 100% before you wake.
- Pause charging when the battery is hot, with 2°C hysteresis.
- Built-in control files for common Qualcomm, Samsung, Pixel and MediaTek kernels, a manual override, and a **Test charge control** button that checks it works on your phone.
- Quick Settings tile to turn Smart Charge on and off.

## How to use

### Smart Charge (root)
1. Grant PowerGyan root access in your root manager.
2. Open **Control**, turn on **Smart Charge** and set the stop and start-again levels.
3. While charging, tap **Test charge control** to confirm your phone's control file works.
4. Plugging in between the two levels always charges up to the stop level.

### Alarms (no root)
1. Open **Control** and turn on **Battery alarms**.
2. Pick the level to ring at, an optional low-battery warning, and sounds.
3. The charge alarm shows full screen, even on the lock screen, until you tap **Stop alarm**.

## Charge control safety

Charge control depends on your phone's kernel and isn't universal. Values are validated before they reach `su`. PowerGyan turns charging back on when you unplug, turn off Smart Charge, or the monitor stops. If charging ever stays off (for example after a force stop), restarting the phone restores it.

## Privacy and permissions

PowerGyan has no internet permission, no ads, no analytics and no accounts. Battery history and settings stay on the phone. It uses notification, boot, foreground-service and full-screen-alarm permissions for monitoring and alarms, and optional root only to switch charging. It doesn't request location, contacts, camera, microphone or accessibility access.

## Build and test

Requires Android Studio, JDK 17, and Android SDK 36 (Android 16).

```bash
git clone https://github.com/ritikthakur22/battery-info_app.git
cd battery-info_app
./gradlew assembleDebug
./gradlew test
./gradlew lint
```

Application ID: `com.crdy.powergyan`  
Version: `1.0.5` (Version Code: 5)  
Minimum Android version: API 29

Release signing is local/secret-only. Use an ignored `keystore.properties` file for a signed build. Never commit keystores, passwords, local properties, or production credentials.

## Project structure (Source-Available)

This repository operates on a **Source-Available** model. To protect the intellectual property and core algorithms of PowerGyan, this repository is not fully open-sourced, nor is it fully closed-source. 

Certain structural files, UI components, and build scripts are visible for transparency and personal inspection. However, proprietary source codes, backend logic, and critical algorithmic files within the `app/src/` directory are deliberately un-tracked and omitted from this public repository.

## Release

The `v1.0.5` release contains the signed release APK and Play Console AAB:

[Download PowerGyan v1.0.5](https://github.com/ritikthakur22/battery-info_app/releases/tag/v1.0.5)

## License

**Source-Available Proprietary License — Personal Inspection Only / No Unauthorized Reuse or Commercial Use**

This software is strictly proprietary. By accessing this repository, you are granted permission to download and inspect the available source files for personal, educational, or auditing purposes. You may **NOT** modify, reuse, distribute, decompile, or commercially exploit any part of this software, its source code, or its compiled binaries. Please read the `LICENSE` file for the full legal terms.

## Support

- Email: [ritikthakur22in@gmail.com](mailto:ritikthakur22in@gmail.com)
- GitHub: [github.com/ritikthakur22](https://github.com/ritikthakur22)

<div align="center">
<img src="https://capsule-render.vercel.app/api?type=waving&color=0B1A2A&height=100&section=footer&animation=twinkling" width="100%" alt="Animated footer" />
</div>
