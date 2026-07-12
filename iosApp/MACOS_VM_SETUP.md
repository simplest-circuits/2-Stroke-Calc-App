# macOS VM Setup für iOS-Entwicklung

> **Ohne Mac und ohne iPhone testen?** Siehe **[IOS_TESTING_WITHOUT_MAC.md](IOS_TESTING_WITHOUT_MAC.md)** – empfohlen: GitHub Actions + Appetize.io (Browser-Simulator).

Diese Anleitung richtet sich an Windows-Entwickler, die die iOS-App (`iosApp/`) **lokal auf einem Mac/VM** bauen und testen möchten.

## Empfohlene VM-Lösungen

| Tool | Hinweis |
|------|---------|
| **UTM** (kostenlos) | Gut für Apple Silicon und Intel Mac-Images |
| **VMware Fusion** | Kommerziell, stabil |
| **Parallels** | Schnell, kostenpflichtig |

## Systemanforderungen

- macOS 14 Sonoma oder neuer (VM)
- Xcode 15+ aus dem Mac App Store
- JDK 17 (`brew install openjdk@17`)
- Git

## Schritte

### 1. Repository klonen

```bash
git clone <repo-url>
cd "2StrokeCalc App"
```

### 2. Gradle prüfen

```bash
./gradlew :androidApp:assembleDebug
./gradlew :shared:testAndroidHostTest
```

### 3. Xcode-Projekt öffnen

```bash
open iosApp/TwoStrokeCalcIOS.xcodeproj
```

### 4. Signing konfigurieren

1. Target **TwoStrokeCalcIOS** auswählen
2. **Signing & Capabilities** → Development Team setzen
3. Bundle ID: `com.simplestsoft.twostrokecalc` (muss mit Firebase iOS-App und `GoogleService-Info.plist` übereinstimmen)

### 5. Firebase iOS

1. [Firebase Console](https://console.firebase.google.com/) → Projekt `strokecalc-app`
2. iOS-App hinzufügen
3. `GoogleService-Info.plist` nach `iosApp/TwoStrokeCalcIOS/Resources/` kopieren
4. In Xcode zum Target hinzufügen

### 6. Build

- **Product → Build** (Cmd+B)
- Der Run Script **Compile Kotlin Framework** ruft `./gradlew :shared:embedAndSignAppleFrameworkForXcode` auf
- **User Script Sandboxing** ist im Projekt deaktiviert (`ENABLE_USER_SCRIPT_SANDBOXING = NO`)

### 7. Simulator starten

- **Product → Run** (Cmd+R)
- iOS Simulator wählen

## CI-Alternative (ohne lokale VM)

GitHub Actions Workflow auf `macos-latest` (`.github/workflows/ios-build.yml`):

```yaml
- run: ./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
- run: mkdir -p shared/build/xcode-frameworks/Debug/iphonesimulator && cp -R shared/build/bin/iosSimulatorArm64/debugFramework/sharedKit.framework shared/build/xcode-frameworks/Debug/iphonesimulator/
- run: xcodebuild -project iosApp/TwoStrokeCalcIOS.xcodeproj -scheme TwoStrokeCalcIOS -destination 'platform=iOS Simulator,name=iPhone 16' CODE_SIGNING_ALLOWED=NO build
```

## Bekannte Einschränkungen

- iOS-Framework-Builds funktionieren **nicht** auf Windows nativ
- `:shared`-Code in `commonMain`/`androidMain` kann auf Windows entwickelt und getestet werden
- Erstes Xcode-Build kann mehrere Minuten dauern (Gradle + Kotlin/Native)

## TestFlight & App Store

### Checkliste vor TestFlight

- [ ] Auth: E-Mail Login, Registrierung, Passwort-Reset
- [ ] Google Sign-In (GoogleSignIn-iOS SPM + URL Scheme in Projekt)
- [ ] Pro-Kauf & Restore (`pro_version` in App Store Connect)
- [ ] Fahrzeug anlegen (Katalog + manuell), Tankbuch, Kosten
- [ ] Cloud-Sync mit Firebase (eingeloggt)
- [ ] Walkthrough mit Spotlight auf Tab-Bar
- [ ] Admin-Panel (nur Admin-Konto)
- [ ] Push-Berechtigung im Onboarding

### Schritte

1. App Store Connect → neue iOS-App `com.simplestsoft.twostrokecalc`
2. In-App-Kauf **`pro_version`** (Non-Consumable) anlegen – gleiche Product-ID wie Android
3. Firebase Console → iOS-App registrieren → echte `GoogleService-Info.plist` ersetzen (GOOGLE_APP_ID aus Console)
4. Archive in Xcode: **Product → Archive** → **Distribute App → TestFlight**
5. CI-Alternative: GitHub Actions Workflow `.github/workflows/ios-build.yml` (Simulator-Build); Archive erfordert Signing-Secrets

## Privacy

- `PrivacyInfo.xcprivacy` liegt unter `TwoStrokeCalcIOS/Resources/`
- Push-Benachrichtigungen: lokale Erinnerungen via `NotificationService.swift`

## Hilfe

Siehe auch [iosApp/README.md](README.md) und den [Kotlin KMP Integrations-Leitfaden](https://kotlinlang.org/docs/multiplatform/multiplatform-integrate-in-existing-app.html).
