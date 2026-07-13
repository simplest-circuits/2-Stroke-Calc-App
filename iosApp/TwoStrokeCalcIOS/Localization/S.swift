import Foundation
import sharedKit

/// Localized UI strings aligned with Android `strings.xml`.
enum S {
    private static var lang: AppLanguage { AppLocalization.language }
    private static func t(_ key: StringKey) -> String { StringCatalog.text(key, language: lang) }

    static var accountTitle: String { t(.accountTitle) }
    static var adminPanelTitle: String { t(.adminPanelTitle) }
    static var appName: String { t(.appName) }
    static var appearanceSection: String { t(.appearanceSection) }
    static var authGateLogin: String { t(.authGateLogin) }
    static var authGateMessage: String { t(.authGateMessage) }
    static var authGateTitle: String { t(.authGateTitle) }
    static var buyPro: String { t(.buyPro) }
    static var calculate: String { t(.calculate) }
    static var calculatorBack: String { t(.calculatorBack) }
    static var calculatorOverviewSubtitle: String { t(.calculatorOverviewSubtitle) }
    static var cancel: String { t(.cancel) }
    static var delete: String { t(.delete) }
    static var displayNameLabel: String { t(.displayNameLabel) }
    static var emailLabel: String { t(.emailLabel) }
    static var forgotPassword: String { t(.forgotPassword) }
    static var forgotPasswordButton: String { t(.forgotPasswordButton) }
    static var forgotPasswordTitle: String { t(.forgotPasswordTitle) }
    static var languageDanish: String { t(.languageDanish) }
    static var languageEnglish: String { t(.languageEnglish) }
    static var languageGerman: String { t(.languageGerman) }
    static var languageLabel: String { t(.languageLabel) }
    static var languageNorwegian: String { t(.languageNorwegian) }
    static var languagePortuguese: String { t(.languagePortuguese) }
    static var languageSpanish: String { t(.languageSpanish) }
    static var languageSwedish: String { t(.languageSwedish) }
    static var languageSystem: String { t(.languageSystem) }
    static var loginButton: String { t(.loginButton) }
    static var loginSubtitle: String { t(.loginSubtitle) }
    static var loginTitle: String { t(.loginTitle) }
    static var logout: String { t(.logout) }
    static var menuTypeLabel: String { t(.menuTypeLabel) }
    static var navCalculator: String { t(.navCalculator) }
    static var navStyleBottom: String { t(.navStyleBottom) }
    static var navStyleDrawer: String { t(.navStyleDrawer) }
    static var navVehicles: String { t(.navVehicles) }
    static var noAccountRegister: String { t(.noAccountRegister) }
    static var passwordLabel: String { t(.passwordLabel) }
    static var permissionsContinue: String { t(.permissionsContinue) }
    static var permissionsTitle: String { t(.permissionsTitle) }
    static var proReadOnly: String { t(.proReadOnly) }
    static var proUpsellTitle: String { t(.proUpsellTitle) }
    static var registerButton: String { t(.registerButton) }
    static var registerTitle: String { t(.registerTitle) }
    static var resultTitle: String { t(.resultTitle) }
    static var save: String { t(.save) }
    static var sectionInput: String { t(.sectionInput) }
    static var settingsAccountSection: String { t(.settingsAccountSection) }
    static var settingsAccountSubtitle: String { t(.settingsAccountSubtitle) }
    static var settingsBugReportBody: String { t(.settingsBugReportBody) }
    static var settingsBugReportSubtitle: String { t(.settingsBugReportSubtitle) }
    static var settingsBugReportTitle: String { t(.settingsBugReportTitle) }
    static var settingsBuyPro: String { t(.settingsBuyPro) }
    static var settingsChangelogBody: String { t(.settingsChangelogBody) }
    static var settingsChangelogSubtitle: String { t(.settingsChangelogSubtitle) }
    static var settingsChangelogTitle: String { t(.settingsChangelogTitle) }
    static var settingsContactBody: String { t(.settingsContactBody) }
    static var settingsContactSubtitle: String { t(.settingsContactSubtitle) }
    static var settingsContactTitle: String { t(.settingsContactTitle) }
    static var settingsDataProcessingTitle: String { t(.settingsDataProcessingTitle) }
    static var settingsGeneralSection: String { t(.settingsGeneralSection) }
    static var settingsHelpBody: String { t(.settingsHelpBody) }
    static var settingsHelpSubtitle: String { t(.settingsHelpSubtitle) }
    static var settingsHelpTitle: String { t(.settingsHelpTitle) }
    static var settingsImprintTitle: String { t(.settingsImprintTitle) }
    static var settingsLegalSection: String { t(.settingsLegalSection) }
    static var settingsNotificationsDenied: String { t(.settingsNotificationsDenied) }
    static var settingsNotificationsGranted: String { t(.settingsNotificationsGranted) }
    static var settingsNotificationsTitle: String { t(.settingsNotificationsTitle) }
    static var settingsPermissionsSection: String { t(.settingsPermissionsSection) }
    static var settingsPrivacyBody: String { t(.settingsPrivacyBody) }
    static var settingsPrivacyTitle: String { t(.settingsPrivacyTitle) }
    static var settingsProSection: String { t(.settingsProSection) }
    static var settingsProSectionHint: String { t(.settingsProSectionHint) }
    static var settingsRestartWalkthrough: String { t(.settingsRestartWalkthrough) }
    static var settingsRestorePro: String { t(.settingsRestorePro) }
    static var settingsTermsBody: String { t(.settingsTermsBody) }
    static var settingsTermsTitle: String { t(.settingsTermsTitle) }
    static var settingsTitle: String { t(.settingsTitle) }
    static var signInGoogle: String { t(.signInGoogle) }
    static var splashSlogan: String { t(.splashSlogan) }
    static var themeDark: String { t(.themeDark) }
    static var themeLabel: String { t(.themeLabel) }
    static var themeLight: String { t(.themeLight) }
    static var themeSystem: String { t(.themeSystem) }
    static var vehiclesAdd: String { t(.vehiclesAdd) }
    static var vehiclesEmpty: String { t(.vehiclesEmpty) }
    static var walkthroughBack: String { t(.walkthroughBack) }
    static var walkthroughCalculatorNavTitle: String { t(.walkthroughCalculatorNavTitle) }
    static var walkthroughFinish: String { t(.walkthroughFinish) }
    static var walkthroughFreeCalculatorBody: String { t(.walkthroughFreeCalculatorBody) }
    static var walkthroughFreeCalculatorNavBody: String { t(.walkthroughFreeCalculatorNavBody) }
    static var walkthroughFreeCalculatorTitle: String { t(.walkthroughFreeCalculatorTitle) }
    static var walkthroughFreeSettingsNavBody: String { t(.walkthroughFreeSettingsNavBody) }
    static var walkthroughFreeVehiclesNavBody: String { t(.walkthroughFreeVehiclesNavBody) }
    static var walkthroughFreeVehiclesNavTitle: String { t(.walkthroughFreeVehiclesNavTitle) }
    static var walkthroughFreeWelcomeBody: String { t(.walkthroughFreeWelcomeBody) }
    static var walkthroughFreeWelcomeTitle: String { t(.walkthroughFreeWelcomeTitle) }
    static var walkthroughLockedCalculatorBody: String { t(.walkthroughLockedCalculatorBody) }
    static var walkthroughLockedCalculatorTitle: String { t(.walkthroughLockedCalculatorTitle) }
    static var walkthroughNext: String { t(.walkthroughNext) }
    static var walkthroughProCalculatorListBody: String { t(.walkthroughProCalculatorListBody) }
    static var walkthroughProCalculatorListTitle: String { t(.walkthroughProCalculatorListTitle) }
    static var walkthroughProCalculatorNavBody: String { t(.walkthroughProCalculatorNavBody) }
    static var walkthroughProSettingsNavBody: String { t(.walkthroughProSettingsNavBody) }
    static var walkthroughProVehiclesAddBody: String { t(.walkthroughProVehiclesAddBody) }
    static var walkthroughProVehiclesNavBody: String { t(.walkthroughProVehiclesNavBody) }
    static var walkthroughProWelcomeBody: String { t(.walkthroughProWelcomeBody) }
    static var walkthroughProWelcomeTitle: String { t(.walkthroughProWelcomeTitle) }
    static var walkthroughSettingsAppearanceBody: String { t(.walkthroughSettingsAppearanceBody) }
    static var walkthroughSettingsAppearanceTitle: String { t(.walkthroughSettingsAppearanceTitle) }
    static var walkthroughSettingsNavTitle: String { t(.walkthroughSettingsNavTitle) }
    static var walkthroughSettingsProBody: String { t(.walkthroughSettingsProBody) }
    static var walkthroughSettingsProTitle: String { t(.walkthroughSettingsProTitle) }
    static var walkthroughSkip: String { t(.walkthroughSkip) }
    static var walkthroughVehiclesAddTitle: String { t(.walkthroughVehiclesAddTitle) }
    static var walkthroughVehiclesNavTitle: String { t(.walkthroughVehiclesNavTitle) }
    static var welcomeContinue: String { t(.welcomeContinue) }
    static var welcomeTitle: String { t(.welcomeTitle) }

    static func walkthroughStepCounter(current: Int, total: Int) -> String {
        let template = t(.walkthroughStepCounter)
        let parts = template.components(separatedBy: "%@")
        guard parts.count == 3 else { return template }
        return parts[0] + "\(current)" + parts[1] + "\(total)" + parts[2]
    }

    static func calculatorTab(_ id: CalculatorId) -> String {
        StringCatalog.calculatorTab(calculatorCatalogKey(id), language: lang)
    }

    static func calculatorDescription(_ id: CalculatorId) -> String {
        StringCatalog.calculatorDescription(calculatorCatalogKey(id), language: lang)
    }

    private static func calculatorCatalogKey(_ id: CalculatorId) -> String {
        switch id {
        case .timing: return "timing"
        case .portArea: return "portArea"
        case .ignition: return "ignition"
        case .dcCable: return "dcCable"
        case .fluid: return "fluid"
        case .compression: return "compression"
        case .squishBand: return "squishBand"
        case .meanPressure: return "meanPressure"
        case .gear: return "gear"
        case .exhaust: return "exhaust"
        case .counterweight: return "counterweight"
        case .variatorWeight: return "variatorWeight"
        case .fuelMix: return "fuelMix"
        case .carbJet: return "carbJet"
        case .dynoInertia: return "dynoInertia"
        case .vehicleDynamics: return "vehicleDynamics"
        default: return id.name.lowercased()
        }
    }
}
