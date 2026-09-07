# iOS App (SwiftUI)

Die iOS-App für 2-Stroke Lab nutzt **SwiftUI** als native UI und bindet die gemeinsame Kotlin-Logik über das Framework **`sharedKit`** ein (Modul `:shared`).

## Voraussetzungen

- **macOS** mit **Xcode 15+** (iOS-Builds sind auf Windows nicht möglich)
- JDK 17
- Gradle Wrapper im Projektroot (`./gradlew`)

## Projektstruktur

```
iosApp/
├── TwoStrokeCalcIOS.xcodeproj/
└── TwoStrokeCalcIOS/
    ├── App/          – App-Einstieg (@main)
    ├── UI/           – SwiftUI Screens
    ├── Platform/     – StoreKit, Google Sign-In, Push (iOS-only)
    └── Resources/    – GoogleService-Info.plist, Assets
```

## Build (macOS)

1. Repository klonen und in Xcode `iosApp/TwoStrokeCalcIOS.xcodeproj` öffnen
2. Development Team unter Signing & Capabilities setzen
3. `GoogleService-Info.plist` aus der Firebase Console nach `Resources/` kopieren
4. Product → Build (Run Script ruft `./gradlew :shared:embedAndSignAppleFrameworkForXcode` auf)

## macOS-VM Setup (Windows-Entwickler)

Siehe **[MACOS_VM_SETUP.md](MACOS_VM_SETUP.md)** für die vollständige Anleitung (UTM/VMware, Xcode, Signing, Firebase).

| Schritt | Aktion |
|---------|--------|
| VM | UTM oder VMware mit macOS 14+ |
| Tools | Xcode, JDK 17 |
| Repo | Gleicher Clone wie auf Windows |
| Firebase | iOS-App registrieren, `GoogleService-Info.plist` einbinden |
| CI (optional) | GitHub Actions `macos-latest` für iOS-Builds |

## SwiftUI-App (Stand)

Die iOS-App spiegelt die Android-Navigation und Screens strukturell wider:

| Bereich | SwiftUI |
|---------|---------|
| Onboarding | Splash, Welcome, Berechtigungen |
| Navigation | Bottom-Bar + Drawer (Einstellungen) |
| Rechner | Übersicht (Liste/Raster) + **16 Rechner** via `sharedKit` |
| Auth | Login, Register, Passwort vergessen, Auth-Gate |
| Fahrzeuge | Liste, Detail mit Tabs, Tankbuch, Kosten |
| Konto / Einstellungen / Admin | Account, Settings, Admin-Panel |
| Pro | StoreKit 2 + Server-Verifikation (`pro_version`) |
| Walkthrough | Coachmark-Overlay mit Tab-Ankern |
| CI | GitHub Actions `ios-build.yml` (macOS) |

**Hinweis:** Erster Xcode-Build auf macOS erforderlich. `GoogleService-Info.plist` aus Firebase Console mit echter iOS-App-ID ersetzen.

## Shared Framework

In Swift: `import sharedKit`

Beispiel (Smoke-Test):

```swift
import sharedKit

Text(Greeting().greet())
```

Rechner-Beispiel: `CompressionCalculatorView.swift` ruft `CompressionCalculator.shared.calculate(...)` aus dem shared Modul auf.
