import FirebaseCore

enum FirebaseBootstrap {
    static var googleClientID: String? {
        guard let path = Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist"),
              let dict = NSDictionary(contentsOfFile: path) as? [String: Any] else { return nil }
        return dict["CLIENT_ID"] as? String
    }

    static func configureIfNeeded() {
        if FirebaseApp.app() == nil {
            FirebaseApp.configure()
        }
    }
}
