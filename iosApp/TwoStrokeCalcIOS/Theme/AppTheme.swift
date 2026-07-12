import SwiftUI

enum ThemeMode: String, CaseIterable, Identifiable {
    case system, light, dark
    var id: String { rawValue }
}

struct AppTheme {
    static let gold = Color(red: 0.831, green: 0.686, blue: 0.216)
    static let brightGold = Color(red: 0.910, green: 0.773, blue: 0.278)
    static let containerCornerRadius: CGFloat = 16

    static func colorScheme(for mode: ThemeMode) -> ColorScheme? {
        switch mode {
        case .system: return nil
        case .light: return .light
        case .dark: return .dark
        }
    }

    static func lightColors() -> ThemeColors {
        ThemeColors(
            primary: gold,
            onPrimary: Color(red: 0.122, green: 0.090, blue: 0.031),
            primaryContainer: Color(red: 1.0, green: 0.953, blue: 0.839),
            background: Color(red: 0.980, green: 0.969, blue: 0.949),
            surface: Color(red: 1.0, green: 0.992, blue: 0.976),
            surfaceVariant: Color(red: 0.941, green: 0.922, blue: 0.890),
            onBackground: Color(red: 0.122, green: 0.106, blue: 0.086),
            onSurfaceVariant: Color(red: 0.361, green: 0.333, blue: 0.282),
            outline: Color(red: 0.769, green: 0.722, blue: 0.659),
            error: .red
        )
    }

    static func darkColors() -> ThemeColors {
        ThemeColors(
            primary: brightGold,
            onPrimary: Color(red: 0.102, green: 0.082, blue: 0.031),
            primaryContainer: Color(red: 0.310, green: 0.239, blue: 0.102),
            background: Color(red: 0.059, green: 0.051, blue: 0.039),
            surface: Color(red: 0.122, green: 0.114, blue: 0.098),
            surfaceVariant: Color(red: 0.184, green: 0.169, blue: 0.145),
            onBackground: Color(red: 0.961, green: 0.941, blue: 0.910),
            onSurfaceVariant: Color(red: 0.722, green: 0.686, blue: 0.627),
            outline: Color(red: 0.361, green: 0.333, blue: 0.282),
            error: Color(red: 1.0, green: 0.4, blue: 0.4)
        )
    }
}

struct ThemeColors {
    let primary: Color
    let onPrimary: Color
    let primaryContainer: Color
    let background: Color
    let surface: Color
    let surfaceVariant: Color
    let onBackground: Color
    let onSurfaceVariant: Color
    let outline: Color
    let error: Color

    var isDark: Bool { background == AppTheme.darkColors().background }
}

struct ThemeEnvironmentKey: EnvironmentKey {
    static let defaultValue = AppTheme.lightColors()
}

extension EnvironmentValues {
    var themeColors: ThemeColors {
        get { self[ThemeEnvironmentKey.self] }
        set { self[ThemeEnvironmentKey.self] = newValue }
    }
}

struct ThemedRoot<Content: View>: View {
    @ObservedObject var appState: AppState
    @ViewBuilder let content: () -> Content

    var body: some View {
        let colors = appState.isDarkTheme ? AppTheme.darkColors() : AppTheme.lightColors()
        content()
            .environment(\.themeColors, colors)
            .preferredColorScheme(AppTheme.colorScheme(for: appState.themeMode))
            .tint(colors.primary)
    }
}
