import SwiftUI

enum LanguageMode: String, CaseIterable, Identifiable {
    case system, german, english
    var id: String { rawValue }

    var label: String {
        switch self {
        case .system: return "System"
        case .german: return "Deutsch"
        case .english: return "English"
        }
    }
}

struct SettingsSectionCard<Content: View>: View {
    @Environment(\.themeColors) private var colors
    let title: String
    @ViewBuilder let content: Content

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title)
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(colors.primary)
            VStack(spacing: 0) { content }
                .background(colors.surface)
                .clipShape(RoundedRectangle(cornerRadius: AppTheme.containerCornerRadius))
                .overlay(
                    RoundedRectangle(cornerRadius: AppTheme.containerCornerRadius)
                        .stroke(colors.outline.opacity(0.25), lineWidth: 1)
                )
        }
    }
}

struct SettingsOptionRow: View {
    @Environment(\.themeColors) private var colors
    let icon: String
    let title: String
    let subtitle: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 12) {
                Image(systemName: icon)
                    .frame(width: 28)
                    .foregroundStyle(colors.primary)
                VStack(alignment: .leading, spacing: 2) {
                    Text(title).font(.body.weight(.medium))
                    Text(subtitle).font(.caption).foregroundStyle(colors.onSurfaceVariant)
                }
                Spacer()
                Image(systemName: "chevron.right")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(colors.onSurfaceVariant)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
        }
        .buttonStyle(.plain)
    }
}

struct SelectionDialog<Item: Hashable & Identifiable>: View where Item: RawRepresentable, Item.RawValue == String {
    let title: String
    let items: [Item]
    let label: (Item) -> String
    @Binding var selection: Item
    @Binding var isPresented: Bool

    var body: some View {
        NavigationStack {
            List {
                ForEach(items, id: \.id) { item in
                    Button {
                        selection = item
                        isPresented = false
                    } label: {
                        HStack {
                            Text(label(item))
                            Spacer()
                            if item.id == selection.id {
                                Image(systemName: "checkmark").foregroundStyle(.accent)
                            }
                        }
                    }
                }
            }
            .navigationTitle(title)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(S.cancel) { isPresented = false }
                }
            }
        }
    }
}

extension ThemeMode: RawRepresentable {}
extension NavStyle: RawRepresentable {}
extension LanguageMode: RawRepresentable {}
