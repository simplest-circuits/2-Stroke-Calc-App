# 2-Stroke Calc

Android-App (Kotlin + Jetpack Compose) für Berechnungen rund um 2-Takt-Motoren.

## Struktur

- **Rechner** – Platzhalter für die eigentlichen Berechnungen
- **Einstellungen** – Layout und Screens angelehnt an die Schwarzes-Brett-App (Design, Sprache, Menütyp, Hilfe, Kontakt, Rechtliches)
- **Navigation** – Bottom-Menü oder Seitenpanel (Drawer), umschaltbar in den Einstellungen

## Tech Stack

- Kotlin, Jetpack Compose, Material 3
- Hilt, DataStore, Navigation Compose
- Min SDK 26, Target SDK 35

## Struktur

- **shared/** – Kotlin Multiplatform (gemeinsame Rechner-Logik und Backend)
- **androidApp/** – Android-UI (Compose, Hilt, Firebase-Integration)
- **iosApp/** – iOS-UI (SwiftUI, `sharedKit`-Framework) – Build nur auf macOS

## Build

```bash
./gradlew :androidApp:assembleDebug
```

## Paket

`com.simplestsoft.twostrokecalc`
