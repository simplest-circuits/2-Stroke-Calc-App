import SwiftUI

struct HelpFaqView: View {
    @Environment(\.themeColors) private var colors
    @State private var expanded: Set<UUID> = []

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text(SettingsContent.helpIntro)
                .foregroundStyle(colors.onSurfaceVariant)
            Text("FAQ").font(.headline)
            ForEach(SettingsContent.faqItems) { item in
                DisclosureGroup(
                    isExpanded: Binding(
                        get: { expanded.contains(item.id) },
                        set: { isOpen in
                            if isOpen { expanded.insert(item.id) } else { expanded.remove(item.id) }
                        }
                    ),
                    content: {
                        Text(item.answer)
                            .font(.subheadline)
                            .foregroundStyle(colors.onSurfaceVariant)
                            .padding(.top, 4)
                    },
                    label: {
                        Text(item.question).font(.subheadline.weight(.semibold))
                    }
                )
                .padding(12)
                .background(colors.surface)
                .clipShape(RoundedRectangle(cornerRadius: AppTheme.containerCornerRadius))
            }
            Text("Tipps").font(.headline).padding(.top, 8)
            ForEach(SettingsContent.helpTips, id: \.self) { tip in
                HStack(alignment: .top, spacing: 8) {
                    Text("•")
                    Text(tip).font(.subheadline)
                }
                .foregroundStyle(colors.onSurfaceVariant)
            }
        }
    }
}

struct ChangelogView: View {
    @Environment(\.themeColors) private var colors
    @State private var pageIndex = 0

    private var sections: [SettingsContent.InfoSection] { SettingsContent.changelogSections }
    private var current: SettingsContent.InfoSection {
        sections[min(pageIndex, sections.count - 1)]
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(current.title).font(.headline)
            ForEach(current.points, id: \.self) { point in
                HStack(alignment: .top, spacing: 8) {
                    Text("•")
                    Text(point).font(.subheadline)
                }
            }
            HStack {
                Button("Zurück") { if pageIndex > 0 { pageIndex -= 1 } }
                    .disabled(pageIndex == 0)
                Spacer()
                Text("\(pageIndex + 1) / \(sections.count)").font(.caption)
                Spacer()
                Button("Weiter") { if pageIndex < sections.count - 1 { pageIndex += 1 } }
                    .disabled(pageIndex >= sections.count - 1)
            }
            .padding(.top, 8)
        }
        .foregroundStyle(colors.onSurfaceVariant)
    }
}

struct InfoSectionsView: View {
    @Environment(\.themeColors) private var colors
    let intro: String
    let sections: [SettingsContent.InfoSection]

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text(intro).foregroundStyle(colors.onSurfaceVariant)
            ForEach(sections) { section in
                VStack(alignment: .leading, spacing: 8) {
                    Text(section.title).font(.headline)
                    ForEach(section.points, id: \.self) { point in
                        HStack(alignment: .top, spacing: 8) {
                            Text("•")
                            Text(point).font(.subheadline)
                        }
                    }
                }
                .foregroundStyle(colors.onSurfaceVariant)
            }
        }
    }
}

struct ContactFormView: View {
    @Environment(\.themeColors) private var colors
    @Environment(\.openURL) private var openURL
    let isBugReport: Bool
    @State private var name = ""
    @State private var email = ""
    @State private var message = ""
    @State private var validationError: String?

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(isBugReport ? SettingsContent.bugIntro : SettingsContent.contactIntro)
                .foregroundStyle(colors.onSurfaceVariant)
            AppOutlinedField(label: "Name", text: $name)
            AppOutlinedField(label: "E-Mail", text: $email)
            AppOutlinedField(label: "Nachricht", text: $message)
            if let validationError {
                Text(validationError).foregroundStyle(colors.error).font(.footnote)
            }
            PrimaryButton(title: "Senden") { sendMail() }
        }
    }

    private func sendMail() {
        guard !message.trimmingCharacters(in: .whitespaces).isEmpty else {
            validationError = "Bitte eine Nachricht eingeben."
            return
        }
        validationError = nil
        let subject = isBugReport ? "Bug-Report 2-Stroke Calc iOS" : "Kontakt 2-Stroke Calc iOS"
        let body = """
        Name: \(name)
        E-Mail: \(email)

        \(message)
        """
        let encodedSubject = subject.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? subject
        let encodedBody = body.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? body
        let urlString = "mailto:\(SettingsContent.supportEmail)?subject=\(encodedSubject)&body=\(encodedBody)"
        if let url = URL(string: urlString) {
            openURL(url)
        }
    }
}
