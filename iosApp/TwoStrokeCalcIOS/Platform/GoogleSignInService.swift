import Foundation
import UIKit

#if canImport(GoogleSignIn)
import GoogleSignIn
#endif

enum GoogleSignInService {
    @MainActor
    static func signIn(appState: AppState) async {
        #if canImport(GoogleSignIn)
        guard let clientID = FirebaseBootstrap.googleClientID else {
            appState.authError = "Google CLIENT_ID nicht in GoogleService-Info.plist gefunden."
            return
        }
        guard let root = UIApplication.shared.connectedScenes
            .compactMap({ $0 as? UIWindowScene })
            .flatMap(\.windows)
            .first(where: \.isKeyWindow)?.rootViewController else {
            appState.authError = "Kein Root-ViewController für Google Sign-In."
            return
        }
        let config = GIDConfiguration(clientID: clientID)
        GIDSignIn.sharedInstance.configuration = config
        do {
            let result = try await GIDSignIn.sharedInstance.signIn(withPresenting: root)
            guard let idToken = result.user.idToken?.tokenString else {
                appState.authError = "Google ID-Token fehlt."
                return
            }
            await appState.signInWithGoogle(idToken: idToken)
        } catch {
            appState.authError = error.localizedDescription
        }
        #else
        appState.authError = "GoogleSignIn SDK nicht verlinkt. Bitte GoogleSignIn-iOS via SPM hinzufügen."
        #endif
    }
}
