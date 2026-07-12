import FirebaseCore
#if canImport(GoogleSignIn)
import GoogleSignIn
#endif

enum FirebaseBootstrap {
    static var googleClientID: String? {
        if let firebaseClientID = FirebaseApp.app()?.options.clientID, !firebaseClientID.isEmpty {
            return firebaseClientID
        }
        guard let path = Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist"),
              let dict = NSDictionary(contentsOfFile: path) as? [String: Any] else { return nil }
        return dict["CLIENT_ID"] as? String
    }

    static func configureIfNeeded() {
        if FirebaseApp.app() == nil {
            FirebaseApp.configure()
        }
        configureGoogleSignInIfNeeded()
    }

    static func configureGoogleSignInIfNeeded() {
        #if canImport(GoogleSignIn)
        guard let clientID = googleClientID else { return }
        GIDSignIn.sharedInstance.configuration = GIDConfiguration(clientID: clientID)
        #endif
    }
}
