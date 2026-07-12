import SwiftUI

@main
struct TwoStrokeCalcIOSApp: App {
    @StateObject private var appState = AppState()
    @UIApplicationDelegateAdaptor(GoogleSignInAppDelegate.self) private var googleSignInDelegate

    init() {
        SharedKitBridge.initializeIfNeeded()
    }

    var body: some Scene {
        WindowGroup {
            RootView()
                .environmentObject(appState)
                .onOpenURL { url in
                    _ = GoogleSignInService.handle(url)
                }
                .task {
                    await SharedKitBridge.bootstrap(appState: appState)
                    await StoreKitService.shared.loadProducts(appState: appState)
                }
        }
    }
}
