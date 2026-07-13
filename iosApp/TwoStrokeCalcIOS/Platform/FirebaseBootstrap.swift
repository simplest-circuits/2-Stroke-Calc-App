import FirebaseCore
import FirebaseAuth
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
        configureAuthKeychainAccessIfNeeded()
        configureGoogleSignInIfNeeded()
    }

    /// Firebase Auth stores sessions in the Keychain. Simulator/unsigned builds need an explicit access group.
    static func configureAuthKeychainAccessIfNeeded() {
        guard let bundleId = Bundle.main.bundleIdentifier else { return }

        var candidates: [String] = []
        if let groups = Bundle.main.object(forInfoDictionaryKey: "keychain-access-groups") as? [String] {
            candidates.append(contentsOf: groups)
        }
        candidates.append(bundleId)

        for group in candidates {
            do {
                try Auth.auth().useUserAccessGroup(group)
                return
            } catch {
                continue
            }
        }
    }

    static func configureGoogleSignInIfNeeded() {
        #if canImport(GoogleSignIn)
        guard let clientID = googleClientID else { return }
        GIDSignIn.sharedInstance.configuration = GIDConfiguration(clientID: clientID)
        #endif
    }
}
