import Foundation

struct WalkthroughStepModel {
    let id: String
    let title: String
    let body: String
    let targetTab: AppRoute?
}

enum WalkthroughBuilder {
    @MainActor
    static func steps(for appState: AppState) -> [WalkthroughStepModel] {
        if appState.isPro || appState.isAdmin {
            return proSteps()
        }
        return freeSteps()
    }

    private static func freeSteps() -> [WalkthroughStepModel] {
        [
            WalkthroughStepModel(
                id: "welcome",
                title: S.walkthroughFreeWelcomeTitle,
                body: S.walkthroughFreeWelcomeBody,
                targetTab: nil
            ),
            WalkthroughStepModel(
                id: "calc_nav",
                title: S.walkthroughCalculatorNavTitle,
                body: S.walkthroughFreeCalculatorNavBody,
                targetTab: .calculator
            ),
            WalkthroughStepModel(
                id: "calc_free",
                title: S.walkthroughFreeCalculatorTitle,
                body: S.walkthroughFreeCalculatorBody,
                targetTab: .calculator
            ),
            WalkthroughStepModel(
                id: "calc_locked",
                title: S.walkthroughLockedCalculatorTitle,
                body: S.walkthroughLockedCalculatorBody,
                targetTab: .calculator
            ),
            WalkthroughStepModel(
                id: "vehicles",
                title: S.walkthroughFreeVehiclesNavTitle,
                body: S.walkthroughFreeVehiclesNavBody,
                targetTab: .vehicles
            ),
            WalkthroughStepModel(
                id: "settings",
                title: S.walkthroughSettingsNavTitle,
                body: S.walkthroughFreeSettingsNavBody,
                targetTab: .settings
            ),
            WalkthroughStepModel(
                id: "pro",
                title: S.walkthroughSettingsProTitle,
                body: S.walkthroughSettingsProBody,
                targetTab: .settings
            ),
            WalkthroughStepModel(
                id: "appearance",
                title: S.walkthroughSettingsAppearanceTitle,
                body: S.walkthroughSettingsAppearanceBody,
                targetTab: .settings
            ),
        ]
    }

    private static func proSteps() -> [WalkthroughStepModel] {
        [
            WalkthroughStepModel(
                id: "welcome",
                title: S.walkthroughProWelcomeTitle,
                body: S.walkthroughProWelcomeBody,
                targetTab: nil
            ),
            WalkthroughStepModel(
                id: "calc_nav",
                title: S.walkthroughCalculatorNavTitle,
                body: S.walkthroughProCalculatorNavBody,
                targetTab: .calculator
            ),
            WalkthroughStepModel(
                id: "calc_list",
                title: S.walkthroughProCalculatorListTitle,
                body: S.walkthroughProCalculatorListBody,
                targetTab: .calculator
            ),
            WalkthroughStepModel(
                id: "vehicles",
                title: S.walkthroughVehiclesNavTitle,
                body: S.walkthroughProVehiclesNavBody,
                targetTab: .vehicles
            ),
            WalkthroughStepModel(
                id: "vehicles_add",
                title: S.walkthroughVehiclesAddTitle,
                body: S.walkthroughProVehiclesAddBody,
                targetTab: .vehicles
            ),
            WalkthroughStepModel(
                id: "settings",
                title: S.walkthroughSettingsNavTitle,
                body: S.walkthroughProSettingsNavBody,
                targetTab: .settings
            ),
            WalkthroughStepModel(
                id: "appearance",
                title: S.walkthroughSettingsAppearanceTitle,
                body: S.walkthroughSettingsAppearanceBody,
                targetTab: .settings
            ),
        ]
    }
}
