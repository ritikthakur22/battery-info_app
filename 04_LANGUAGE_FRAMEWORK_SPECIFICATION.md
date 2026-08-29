# Language, Framework and Engineering Specification

## 1. Final Technology Choice
**Kotlin + Jetpack Compose + Material 3 + Jetpack Glance**

This is the recommended implementation stack for this product.

### Why Kotlin
- Native Android language.
- Direct access to Android battery APIs.
- Direct access to AppWidget infrastructure.
- First-class Jetpack support.
- Compose is Kotlin-based. citeturn499963search2

### Why Compose
- Modern UI.
- Excellent support for dynamic sizing/themes.
- Strong accessibility semantics.
- Less XML boilerplate.
- Fits the requested premium/minimal design.

### Why Glance
Glance is Android's Compose-style framework for app widgets. It is specifically built for widget surfaces and supports responsive sizing. citeturn499963search0turn499963search1

Important: Glance is not the same UI runtime as normal Compose. Widget composables are constrained by AppWidget/RemoteViews capabilities. citeturn499963search0

## 2. UI Specification
Design language:
- Premium minimalist.
- Material 3 foundation.
- Large typography.
- Soft visual hierarchy.
- Strong contrast.
- No unnecessary cards everywhere.
- No excessive animations.

Main visual hierarchy:
```text
           BATTERY

             73%
          [battery]

       Charging / Normal

   Temperature      Health
```

The percentage is the primary visual element.

## 3. Settings Specification
Sections:

### Appearance
- Text size slider.
- Icon size slider.
- Alignment.
- Theme: System / Light / Dark.
- Text color.
- Icon color.
- Background style.

### Information
- Charging status.
- Temperature.
- Health.
- Voltage/current where available.

### Widget
- Widget preview.
- Preferred style.
- Reset widget appearance.

### Accessibility
- Large text preset.
- High contrast.
- Reduced animation.

### About
- App version.
- Privacy statement.
- Open-source licenses if applicable.

## 4. Widget Design
Provide three layouts:

### Small
```text
┌───────────┐
│   73%     │
│  battery  │
└───────────┘
```

### Medium
```text
┌──────────────────┐
│  ████     73%    │
│  Charging        │
└──────────────────┘
```

### Large
```text
┌─────────────────────────┐
│                         │
│          73%            │
│       █████████         │
│        Charging         │
│     12°C   Good Health  │
│                         │
└─────────────────────────┘
```

Use responsive layouts rather than one fixed layout for every widget size. Glance's `SizeMode.Responsive` is intended for this type of adaptation. citeturn499963search7

## 5. Architecture Rules for Antigravity CLI
The AI coding agent must follow:

1. Read `PROJECT_SPEC.md` and `ARCHITECTURE.md` before modifying architecture.
2. Never add a dependency without explaining its purpose in the PR/commit.
3. Never add INTERNET permission.
4. Never create a persistent service for a UI convenience feature.
5. Never poll battery level continuously.
6. Never use hidden Android APIs.
7. Keep battery reads event-driven.
8. Preserve minSdk 29.
9. Keep widgets simple.
10. Write tests for domain logic.
11. Run `./gradlew test` after logic changes.
12. Run Android instrumented/Compose tests where available.
13. Run a release build before release work.
14. Update documentation when behavior changes.

## 6. Recommended Development Phases

### Phase 1 — Skeleton
- Kotlin project.
- Compose + Material 3.
- Navigation/basic screen structure.
- DataStore.
- Theme system.

### Phase 2 — Battery Core
- Battery data source.
- BatterySnapshot.
- Charging-state interpretation.
- Main screen.

### Phase 3 — Accessibility
- Large text.
- Large icons.
- Contrast.
- TalkBack descriptions.
- Font scaling.

### Phase 4 — Widget
- Glance widget.
- Responsive sizes.
- Widget preview.
- Tap-to-open behavior.
- Battery update mechanism.

### Phase 5 — Performance
- Remove unnecessary dependencies.
- Profile memory.
- Profile CPU.
- Validate background behavior.
- Validate battery impact.

### Phase 6 — Compatibility
Test at least:
- Pixel/AOSP Android 10+.
- Samsung One UI.
- Xiaomi/POCO/Redmi.
- OnePlus/Oppo/Realme.
- Motorola.

### Phase 7 — Release
- R8.
- Release signing.
- Play metadata.
- Privacy documentation.
- Accessibility verification.
- Battery-use verification.
- Crash-free smoke testing.

## 7. Definition of Done
A feature is not done until:
- code compiles;
- tests pass;
- UI is accessible;
- battery/resource impact is reviewed;
- no unnecessary permission was introduced;
- documentation is updated;
- behavior works after process death;
- behavior survives reboot where relevant.

## 8. Important Platform Constraint
Do not advertise:
“Works as a lock-screen widget on every Android phone.”

Advertise:
“Large home-screen battery widget, with lock-screen availability on supported Android devices.”

Android widget hosts determine many presentation details, and current Android documentation describes host/category constraints rather than universal lock-screen availability. citeturn616797search4turn499963search0