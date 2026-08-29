# Battery Visibility App

A lightweight, offline-first Android utility that makes battery level highly readable. Built entirely in Kotlin, Jetpack Compose, and Jetpack Glance.

## Features
- **Large and Clear UI:** Easily read your battery percentage and charging state.
- **Home Screen Widgets:** Highly configurable widgets via Jetpack Glance.
- **Privacy First:** No internet permission, no accounts, no analytics, and no ads.
- **Battery Efficient:** Event-driven updates without continuous background polling.

## Build Requirements
- Android Studio / JDK 17
- Minimum SDK: 29 (Android 10)
- Target SDK: 34 (or latest stable)

## Contributing
Open source contributions are welcome. Please read the architectural and security documents before submitting PRs. Make sure to pass the local tests and lint checks:
`./gradlew test lint`
