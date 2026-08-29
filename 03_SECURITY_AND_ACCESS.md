# Security, Privacy and Permission Specification

## 1. Security Philosophy
This app handles local battery/device-state information only. It should have an extremely small attack surface.

Principles:
- offline by default;
- least privilege;
- no account;
- no server;
- no network dependency;
- public Android APIs only;
- no hidden/non-SDK APIs.

Android restricts use of non-SDK interfaces; the app must avoid them entirely. citeturn616797search6

## 2. Permission Policy
### Required permissions
Preferably none.

Do not request:
- INTERNET
- READ_PHONE_STATE
- location
- contacts
- storage
- microphone
- camera
- accessibility service

### Optional permissions
Only introduce a permission when a feature cannot work without it and the feature is explicitly part of the product.

For example, a future floating overlay feature would require Android's overlay permission. It is not required for the MVP and should not be included simply “in case”.

## 3. Network Security
Manifest should not include:
```xml
<uses-permission android:name="android.permission.INTERNET"/>
```

No:
- Firebase
- Crash reporting SDK
- analytics SDK
- advertising SDK
- remote configuration
- WebView
- cloud API

This provides a strong privacy claim: core operation does not require network access.

## 4. Local Data
Store only user preferences:
- text/icon scale;
- theme;
- colors;
- alignment;
- selected display options;
- widget-related configuration.

Use DataStore Preferences.

Do not store:
- personally identifying information;
- battery history unless explicitly added later;
- installed-app inventory;
- device identifiers;
- advertising IDs.

## 5. Exported Components
Every Android component must explicitly declare exported behavior.

Rules:
- MainActivity: exported because it is the launcher activity.
- Widget receiver: exported as required by Android AppWidget infrastructure, with the proper widget update intent filter and provider metadata.
- Internal receivers/services: exported=false unless Android requires otherwise.

Never expose unnecessary activities, providers, services, or receivers.

## 6. PendingIntent Security
Where widget interactions use PendingIntent:
- use immutable PendingIntents unless mutability is genuinely required;
- use explicit intents;
- do not pass sensitive data through intent extras.

## 7. Backup
Settings backup behavior should be deliberate.

Safe settings can usually be backed up. Avoid storing secrets because the app should have none.

Widget restoration must tolerate changed widget IDs. Android provides AppWidget restoration callbacks for providers that need to remap old and new widget IDs. citeturn616797search0

## 8. Input Validation
Validate persisted settings:
- text scale within safe minimum/maximum;
- icon scale within safe range;
- color values valid;
- enum values have safe defaults;
- corrupted settings fall back to defaults.

Never allow a malformed stored value to crash the main UI.

## 9. Privacy UX
Settings/About page should clearly state:
“Works offline. No account. No ads. No analytics. Your settings stay on your device.”

Do not make exaggerated security claims such as “military-grade security” because the product does not need them.

## 10. Release Security
For release builds:
- enable R8/minification where stable;
- remove debug logs;
- do not ship debug certificates;
- protect signing keys outside the repository;
- use Play App Signing if distributing through Google Play;
- run dependency vulnerability checks;
- keep dependencies current.

Never commit:
- keystore files;
- passwords;
- signing credentials;
- API keys;
- personal tokens.

## 11. Threat Model
### Threat: Malicious network dependency
Mitigation: no INTERNET permission and no network stack.

### Threat: Excessive permissions
Mitigation: permission budget = none for MVP.

### Threat: Widget intent abuse
Mitigation: explicit immutable PendingIntents and minimal exported surface.

### Threat: Corrupted local state
Mitigation: schema validation and safe defaults.

### Threat: Dependency supply-chain risk
Mitigation: minimal dependencies, version catalog, reproducible Gradle builds where practical, regular updates.

## 12. Security Acceptance Criteria
- No unnecessary dangerous permission.
- Prefer no permissions at all.
- No INTERNET permission.
- No network requests in core functionality.
- No secrets in source control.
- No non-SDK API.
- All exported components reviewed.
- Release build has no accidental debug logging.