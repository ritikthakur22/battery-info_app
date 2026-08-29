# Battery Gyan — Product Requirements Document (PRD)

## 1. Product Summary
A lightweight, fully offline Android utility that makes battery level highly readable for users who struggle to see the small system battery percentage. The app provides:
- A large battery percentage and battery icon inside the app.
- A large configurable home-screen widget.
- Optional lock-screen/widget availability where the OS/launcher supports it.
- Large, adjustable text and icon sizes.
- Modern, premium, minimalist Material 3 UI.
- No accounts, backend, analytics, ads, or network dependency.

Primary product principle: **maximum readability with minimum battery/resource cost**.

## 2. Target Users
Primary audience: everyone who benefits from larger battery information, including glasses users, older users, and users who prefer highly visible UI.

Accessibility-first defaults:
- Large typography.
- Large touch targets.
- High legibility.
- Clear status labels.
- Minimal visual clutter.

## 3. Goals
### Must Have
1. Show battery percentage prominently.
2. Show a clear battery icon.
3. Provide a large resizable home-screen widget.
4. Let the user configure text/icon size.
5. Let the user configure position/alignment where technically possible.
6. Support light/dark/system themes.
7. Support Material-style text/icon colors.
8. Show useful battery data without making the main display cluttered.
9. Work fully offline.
10. Minimize battery usage.
11. Support Android 10+ (API 29+) as the baseline.
12. Use modern Android APIs and avoid hidden/non-SDK APIs.

### Nice to Have
- Charging state/speed presentation.
- Temperature and battery-health information when the platform exposes reliable values.
- Widget variants/presets.
- Quick settings or notification shortcut only if they add value without persistent background cost.

## 4. Non-Goals
- No cloud synchronization.
- No user account.
- No social features.
- No advertising.
- No battery-booster/cleaner claims.
- No continuous polling loop.
- No unnecessary foreground service.
- No dependence on manufacturer-private APIs.

## 5. Core UX
### Main screen
Hero area:
- Large battery percentage.
- Large battery icon.
- Charging/discharging indicator.
- Optional secondary information.

Configuration:
- Text size.
- Icon size.
- Alignment/position where applicable.
- Theme.
- Material text/icon color.
- Optional secondary battery metrics.

The default screen should communicate the battery level in under one second.

### Widget
Primary widget:
- Large percentage.
- Battery icon.
- Charging state.
- Resizable.
- Responsive layout for multiple widget sizes.
- Opens the app settings/main screen on tap.

Widgets should be passive and event-driven where practical. Android recommends minimizing widget update frequency because updates consume resources. citeturn616797search1turn499963search3

## 6. Battery Information
Recommended default:
- Percentage: always visible.
- Battery state: charging / discharging / full.
- Optional estimated time remaining only when the OS provides a sufficiently reliable value.

Optional advanced information:
- Temperature.
- Voltage.
- Current.
- Health.
- Battery capacity.

Platform limitations must be respected; unavailable information should be hidden rather than fabricated.

## 7. Operating Modes
### Mode A — App Only
User opens the app when needed.
No persistent service.

### Mode B — Widget Enabled
When one or more widgets exist:
- Widget is maintained using Android widget mechanisms.
- App does not start a continuous polling service merely because a widget exists.
- Battery updates should react to relevant system broadcasts where available and use conservative scheduled updates as fallback.

Android's AppWidgetProvider receives system widget-update events, and Android's current guidance emphasizes infrequent widget updates for battery efficiency. citeturn616797search0turn499963search3

## 8. Compatibility
Baseline:
- minSdk = 29 (Android 10).

Target:
- Current Play-distribution target SDK available at release time.
Google Play requires current target API levels, so targetSdk should be kept current independently of minSdk. citeturn616797search5

Device families:
- Pixel/AOSP.
- Samsung.
- Xiaomi/POCO/Redmi.
- OnePlus/Oppo/Realme.
- Vivo.
- Motorola.
- Other Android 10+ devices.

Manufacturer differences must be isolated behind compatibility checks.

## 9. Accessibility Requirements
- Large default typography.
- User-adjustable text size.
- User-adjustable icon size.
- Minimum touch target: 48dp where interactive.
- Content descriptions for icons.
- Do not communicate battery level through color alone.
- Respect system font scale where practical.
- Support TalkBack semantics.
- Avoid unnecessary animations.
- High-contrast option.

## 10. Privacy
The app is designed to require:
- No account.
- No cloud.
- No telemetry.
- No analytics.
- No ads.
- No network functionality.

Preferred manifest policy:
- Do not declare INTERNET permission.

All preferences remain local to the device.

## 11. Acceptance Criteria
The MVP is acceptable when:
- Battery percentage is readable at a glance.
- Widget can be added and resized on supported launchers.
- Widget reflects charging/percentage changes without continuous polling.
- App consumes negligible/background-minimal resources.
- Core functionality works with no network connection.
- Settings survive process death and reboot.
- No sensitive permission is requested.
- Main UI works correctly at large system font scales.
- No hidden/private Android API is required.

## 12. Future Expansion
Potential later features:
- Multiple widget styles.
- Battery history stored locally.
- Configurable low/high battery alerts.
- More advanced accessibility presets.
- Wear OS companion only if separately justified.