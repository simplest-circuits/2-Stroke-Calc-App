import SwiftUI

struct CalculatorScaffold<Content: View>: View {
    @Environment(\.themeColors) private var colors
    let content: Content

    init(@ViewBuilder content: () -> Content) {
        self.content = content()
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                content
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 16)
        }
        .background(colors.background)
    }
}

struct CalculatorHeader: View {
    @Environment(\.themeColors) private var colors
    let title: String
    let subtitle: String?

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(title)
                .font(.title2.bold())
                .foregroundStyle(colors.onBackground)
            if let subtitle, !subtitle.isEmpty {
                Text(subtitle)
                    .font(.subheadline)
                    .foregroundStyle(colors.onSurfaceVariant)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

struct CalculatorSection<Content: View>: View {
    @Environment(\.themeColors) private var colors
    let title: String
    let content: Content

    init(_ title: String, @ViewBuilder content: () -> Content) {
        self.title = title
        self.content = content()
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(title)
                .font(.headline)
                .foregroundStyle(colors.primary)
            content
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(colors.surface)
        .clipShape(RoundedRectangle(cornerRadius: AppTheme.containerCornerRadius))
        .overlay(
            RoundedRectangle(cornerRadius: AppTheme.containerCornerRadius)
                .stroke(colors.outline.opacity(0.35), lineWidth: 1)
        )
    }
}

struct DecimalField: View {
    @Environment(\.themeColors) private var colors
    let label: String
    @Binding var text: String
    var suffix: String = ""
    var hint: String?
    var enabled: Bool = true

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label)
                .font(.caption)
                .foregroundStyle(colors.onSurfaceVariant)
            HStack {
                TextField("0", text: $text)
                    .keyboardType(.decimalPad)
                    .disabled(!enabled)
                if !suffix.isEmpty {
                    Text(suffix)
                        .foregroundStyle(colors.onSurfaceVariant)
                }
            }
            .padding(12)
            .background(colors.surfaceVariant.opacity(0.5))
            .clipShape(RoundedRectangle(cornerRadius: 10))
            if let hint {
                Text(hint)
                    .font(.caption2)
                    .foregroundStyle(colors.onSurfaceVariant)
            }
        }
    }
}

struct CalculatorFilterChip: View {
    @Environment(\.themeColors) private var colors
    let label: String
    let selected: Bool
    var enabled: Bool = true
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(label)
                .font(.subheadline.weight(.medium))
                .padding(.horizontal, 14)
                .padding(.vertical, 8)
                .background(selected ? colors.primaryContainer : colors.surfaceVariant.opacity(0.45))
                .foregroundStyle(selected ? colors.primary : colors.onSurface)
                .clipShape(Capsule())
        }
        .buttonStyle(.plain)
        .disabled(!enabled)
    }
}

struct CalculatorInputModeSwitch: View {
    let optionALabel: String
    let optionBLabel: String
    let useOptionA: Bool
    var enabled: Bool = true
    let onUseOptionAChange: (Bool) -> Void

    var body: some View {
        HStack(spacing: 8) {
            CalculatorFilterChip(label: optionALabel, selected: useOptionA, enabled: enabled) {
                onUseOptionAChange(true)
            }
            CalculatorFilterChip(label: optionBLabel, selected: !useOptionA, enabled: enabled) {
                onUseOptionAChange(false)
            }
        }
    }
}

struct CalculatorBidirectionalField: View {
    let optionALabel: String
    let optionBLabel: String
    let useOptionA: Bool
    let onUseOptionAChange: (Bool) -> Void
    @Binding var text: String
    let fieldLabelA: String
    let fieldLabelB: String
    let suffixA: String
    let suffixB: String
    var enabled: Bool = true

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            CalculatorInputModeSwitch(
                optionALabel: optionALabel,
                optionBLabel: optionBLabel,
                useOptionA: useOptionA,
                enabled: enabled,
                onUseOptionAChange: onUseOptionAChange
            )
            DecimalField(
                label: useOptionA ? fieldLabelA : fieldLabelB,
                text: $text,
                suffix: useOptionA ? suffixA : suffixB,
                enabled: enabled
            )
        }
    }
}

struct PrimaryResultText: View {
    @Environment(\.themeColors) private var colors
    let text: String

    var body: some View {
        Text(text)
            .font(.title3.weight(.semibold))
            .foregroundStyle(colors.primary)
            .frame(maxWidth: .infinity, alignment: .leading)
    }
}

struct ResultCard: View {
    @Environment(\.themeColors) private var colors
    let title: String
    let lines: [String]

    var body: some View {
        CalculatorSection(title) {
            if lines.isEmpty {
                Text("Werte eingeben…")
                    .foregroundStyle(colors.onSurfaceVariant)
            } else {
                ForEach(lines, id: \.self) { line in
                    Text(line)
                        .font(.body.monospacedDigit())
                }
            }
        }
    }
}

struct ProReadOnlyBanner: View {
    @Environment(\.themeColors) private var colors
    let onBuyPro: () -> Void

    var body: some View {
        HStack {
            Text(S.proReadOnly)
                .font(.footnote)
                .foregroundStyle(colors.onSurfaceVariant)
            Spacer()
            Button(S.buyPro, action: onBuyPro)
                .font(.footnote.bold())
        }
        .padding(12)
        .background(colors.primaryContainer.opacity(0.6))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }
}

struct PrimaryButton: View {
    @Environment(\.themeColors) private var colors
    let title: String
    var loading: Bool = false
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Group {
                if loading {
                    ProgressView().tint(colors.onPrimary)
                } else {
                    Text(title).fontWeight(.semibold)
                }
            }
            .frame(maxWidth: .infinity, minHeight: 48)
        }
        .buttonStyle(.borderedProminent)
        .tint(colors.primary)
    }
}

struct AppOutlinedField: View {
    @Environment(\.themeColors) private var colors
    let label: String
    @Binding var text: String
    var secure: Bool = false

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label).font(.caption).foregroundStyle(colors.onSurfaceVariant)
            Group {
                if secure {
                    SecureField("", text: $text)
                } else {
                    TextField("", text: $text)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                }
            }
            .padding(12)
            .background(colors.surfaceVariant.opacity(0.35))
            .clipShape(RoundedRectangle(cornerRadius: 10))
        }
    }
}

func parseDouble(_ text: String) -> Double? {
    let normalized = text.replacingOccurrences(of: ",", with: ".")
    return Double(normalized)
}

func fmt(_ value: Double, decimals: Int = 2) -> String {
    String(format: "%.\(decimals)f", value)
}

func formatIgnitionDegrees(_ value: Double, round: Bool = true) -> String {
    if round {
        return "\(Int(value.rounded()))°"
    }
    return String(format: "%.2f°", value)
}

func formatIgnitionMillimeters(_ value: Double) -> String {
    String(format: "%.2f mm", value)
}
