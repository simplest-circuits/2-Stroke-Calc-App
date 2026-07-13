import Foundation

@MainActor
enum AppLocalization {
    static var language: AppLanguage = AppLanguage.fromSystem()

    static func apply(languageMode: LanguageMode) {
        language = languageMode.resolveAppLanguage()
    }
}

extension LanguageMode {
    func resolveAppLanguage() -> AppLanguage {
        switch self {
        case .system: return AppLanguage.fromSystem()
        case .german: return .german
        case .english: return .english
        case .spanish: return .spanish
        case .portuguese: return .portuguese
        case .swedish: return .swedish
        case .danish: return .danish
        case .norwegian: return .norwegian
        }
    }

    var label: String {
        switch self {
        case .system: return S.languageSystem
        case .german: return S.languageGerman
        case .english: return S.languageEnglish
        case .spanish: return S.languageSpanish
        case .portuguese: return S.languagePortuguese
        case .swedish: return S.languageSwedish
        case .danish: return S.languageDanish
        case .norwegian: return S.languageNorwegian
        }
    }
}
