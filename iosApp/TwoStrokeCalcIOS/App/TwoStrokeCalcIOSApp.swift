import SwiftUI

@main
struct TwoStrokeCalcIOSApp: App {
    @StateObject private var appState = AppState()

    init() {
        SharedKitBridge.initializeIfNeeded()
    }

    var body: some Scene {
        WindowGroup {
            RootView()
                .environmentObject(appState)
                .task {
                    await SharedKitBridge.bootstrap(appState: appState)
                    await StoreKitService.shared.loadProducts(appState: appState)
                }
        }
    }
}
