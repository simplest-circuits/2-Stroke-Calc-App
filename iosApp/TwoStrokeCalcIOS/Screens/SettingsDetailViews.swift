import SwiftUI

struct HelpFaqView: View {
    @Environment(\.themeColors) private var colors
    @State private var expanded: Set<UUID> = []

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text(SettingsContent.helpIntro)
                .foregroundStyle(colors.onSurfaceVariant)
            Text(L.t("settings_detail_help_faq_title")).font(.headline)
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
            Text(L.t("settings_detail_help_tips_title")).font(.headline).padding(.top, 8)
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
                Button(L.t("walkthrough_back")) { if pageIndex > 0 { pageIndex -= 1 } }
                    .disabled(pageIndex == 0)
                Spacer()
                Text("\(pageIndex + 1) / \(sections.count)").font(.caption)
                Spacer()
                Button(L.t("walkthrough_next")) { if pageIndex < sections.count - 1 { pageIndex += 1 } }
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
            AppOutlinedField(label: L.t("settings_contact_name_label"), text: $name)
            AppOutlinedField(label: L.t("email_label"), text: $email)
            AppOutlinedField(label: L.t("settings_contact_message_label"), text: $message)
            if let validationError {
                Text(validationError).foregroundStyle(colors.error).font(.footnote)
            }
            PrimaryButton(title: L.t("settings_contact_send")) { sendMail() }
        }
    }

    private func sendMail() {
        guard !message.trimmingCharacters(in: .whitespaces).isEmpty else {
            validationError = L.t("settings_contact_message_required")
            return
        }
        validationError = nil
        let subject = isBugReport ? L.t("settings_bug_report_email_subject") : L.t("settings_detail_contact_form_title")
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
