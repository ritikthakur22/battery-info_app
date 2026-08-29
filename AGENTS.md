# AGENTS.md — Battery Visibility Android App

## Mission
Build a very lightweight, offline-first Android battery visibility/accessibility utility.

## Hard Constraints
- Kotlin only.
- Jetpack Compose + Material 3 for app UI.
- Jetpack Glance for widgets.
- minSdk 29.
- No INTERNET permission.
- No ads.
- No analytics.
- No account/backend.
- No continuous battery polling.
- No unnecessary foreground service.
- No hidden/non-SDK APIs.
- Prefer zero runtime permissions for MVP.
- Battery usage is a first-class acceptance criterion.

## Architecture
Keep dependencies flowing:
UI -> Presentation -> Domain -> Data/Platform.

Use DataStore for preferences. Do not introduce Room unless a real persistent-history feature is approved.

## Battery
Use public BatteryManager/Intent battery APIs. Observe battery while UI is active and use Android widget lifecycle/broadcast mechanisms for widgets.

## Widgets
Use Glance and responsive layouts.
Do not update every minute simply because a widget exists.
Keep widget content simple.

## Security
- All non-required components exported=false.
- Widget receiver exported as required by AppWidget.
- Explicit immutable PendingIntents.
- No secrets in repository.
- No INTERNET permission.

## Coding
- Prefer small classes.
- Avoid premature abstractions.
- Use immutable UI state.
- Validate persisted settings.
- Provide safe defaults.
- Add tests for non-UI logic.
- Avoid unnecessary recomposition.

## Before Completion
Run:
./gradlew test
./gradlew assembleDebug

For release work also run:
./gradlew assembleRelease

Verify manifest permissions and inspect APK/dependency contents before release.