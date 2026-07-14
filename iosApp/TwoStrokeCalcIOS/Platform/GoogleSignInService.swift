import Foundation
import UIKit
import FirebaseAuth

#if canImport(GoogleSignIn)
import GoogleSignIn
#endif

enum GoogleSignInService {
    @MainActor
    static func signIn(appState: AppState) async {
        #if canImport(GoogleSignIn)
        FirebaseBootstrap.configureIfNeeded()
        guard GIDSignIn.sharedInstance.configuration != nil else {
            appState.authError = L.t("auth_error_google_client_id_missing")
            return
        }
        guard let presenter = presentingViewController() else {
            appState.authError = L.t("auth_error_google_no_view_controller")
            return
        }
        appState.authLoading = true
        appState.authError = nil
        defer { appState.authLoading = false }

        do {
            let result = try await GIDSignIn.sharedInstance.signIn(withPresenting: presenter)
            guard let idToken = result.user.idToken?.tokenString else {
                appState.authError = L.t("auth_error_google_id_token_missing")
                return
            }
            let accessToken = result.user.accessToken.tokenString
            try await signInToFirebase(idToken: idToken, accessToken: accessToken)
            await appState.completeSuccessfulAuth()
        } catch {
            if (error as NSError).code == GIDSignInError.canceled.rawValue {
                return
            }
            appState.authError = friendlyAuthErrorMessage(error)
        }
        #else
        appState.authError = L.t("auth_error_google_sdk_missing")
        #endif
    }

    @MainActor
    private static func signInToFirebase(idToken: String, accessToken: String) async throws {
        let credential = GoogleAuthProvider.credential(withIDToken: idToken, accessToken: accessToken)
        do {
            _ = try await Auth.auth().signIn(with: credential)
        } catch {
            if isKeychainError(error) {
                FirebaseBootstrap.configureAuthKeychainAccessIfNeeded()
                _ = try await Auth.auth().signIn(with: credential)
            } else {
                throw error
            }
        }
    }

    private static func isKeychainError(_ error: Error) -> Bool {
        let nsError = error as NSError
        if nsError.domain == AuthErrorDomain,
           nsError.code == AuthErrorCode.keychainError.rawValue {
            return true
        }
        let message = nsError.localizedDescription.lowercased()
        return message.contains("keychain")
    }

    private static func friendlyAuthErrorMessage(_ error: Error) -> String {
        let nsError = error as NSError
        if isKeychainError(error) {
            return L.t("auth_error_keychain_access")
        }
        if nsError.domain == AuthErrorDomain,
           nsError.code == AuthErrorCode.invalidCredential.rawValue {
            return L.t("auth_error_google_simulator")
        }
        if nsError.domain == NSURLErrorDomain {
            return L.t("auth_error_network")
        }
        return error.localizedDescription
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
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        FirebaseBootstrap.configureIfNeeded()
        return true
    }

    func application(
        _ application: UIApplication,
        open url: URL,
        options: [UIApplication.OpenURLOptionsKey: Any] = [:]
    ) -> Bool {
        GoogleSignInService.handle(url)
    }
}
