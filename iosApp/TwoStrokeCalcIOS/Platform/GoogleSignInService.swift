import Foundation
import UIKit

#if canImport(GoogleSignIn)
import GoogleSignIn
#endif

enum GoogleSignInService {
    @MainActor
    static func signIn(appState: AppState) async {
        #if canImport(GoogleSignIn)
        FirebaseBootstrap.configureGoogleSignInIfNeeded()
        guard GIDSignIn.sharedInstance.configuration != nil else {
            appState.authError = "Google CLIENT_ID nicht in GoogleService-Info.plist gefunden."
            return
        }
        guard let presenter = presentingViewController() else {
            appState.authError = "Kein ViewController für Google Sign-In verfügbar."
            return
        }
        do {
            let result = try await GIDSignIn.sharedInstance.signIn(withPresenting: presenter)
            guard let idToken = result.user.idToken?.tokenString else {
                appState.authError = "Google ID-Token fehlt."
                return
            }
            let accessToken = result.user.accessToken.tokenString
            await appState.signInWithGoogle(idToken: idToken, accessToken: accessToken)
        } catch {
            if (error as NSError).code == GIDSignInError.canceled.rawValue {
                return
            }
            appState.authError = error.localizedDescription
        }
        #else
        appState.authError = "GoogleSignIn SDK nicht verlinkt. Bitte GoogleSignIn-iOS via SPM hinzufügen."
        #endif
    }

    @discardableResult
    static func handle(_ url: URL) -> Bool {
        #if canImport(GoogleSignIn)
        return GIDSignIn.sharedInstance.handle(url)
        #else
        return false
        #endif
    }

    @MainActor
    private static func presentingViewController() -> UIViewController? {
        let scenes = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .sorted { lhs, rhs in
                if lhs.activationState == .foregroundActive { return true }
                if rhs.activationState == .foregroundActive { return false }
                return false
            }

        for scene in scenes {
            let windows = scene.windows.sorted { $0.isKeyWindow && !$1.isKeyWindow }
            for window in windows {
                if let root = window.rootViewController {
                    return topViewController(from: root)
                }
            }
        }
        return nil
    }

    private static func topViewController(from controller: UIViewController) -> UIViewController {
        if let navigation = controller as? UINavigationController,
           let visible = navigation.visibleViewController {
            return topViewController(from: visible)
        }
        if let tab = controller as? UITabBarController,
           let selected = tab.selectedViewController {
            return topViewController(from: selected)
        }
        if let presented = controller.presentedViewController {
            return topViewController(from: presented)
        }
        return controller
    }
}

final class GoogleSignInAppDelegate: NSObject, UIApplicationDelegate {
    func application(
        _ application: UIApplication,
        open url: URL,
        options: [UIApplication.OpenURLOptionsKey: Any] = [:]
    ) -> Bool {
        GoogleSignInService.handle(url)
    }
}
