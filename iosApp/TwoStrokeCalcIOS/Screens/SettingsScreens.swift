import SwiftUI

struct SettingsScreen: View {
    @EnvironmentObject private var appState: AppState
    @Environment(\.themeColors) private var colors
    @Environment(\.openURL) private var openURL

    @State private var themeDialog = false
    @State private var languageDialog = false
    @State private var navDialog = false
    @State private var generalPage: GeneralContentPage?

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                settingsHeader

                if generalPage == nil {
                    accountSection
                    if !appState.isPro && !appState.isAdmin { proSection }
                    appearanceSection
                    permissionsSection
                    generalSection
                    legalSection
                    settingsFooter
                } else {
                    generalContentDetail(generalPage!)
                }
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 16)
        }
        .background(colors.background)
        .sheet(isPresented: $themeDialog) { themePickerSheet }
        .sheet(isPresented: $languageDialog) { languagePickerSheet }
        .sheet(isPresented: $navDialog) { navPickerSheet }
    }

    private var settingsHeader: some View {
        HStack {
            VStack(alignment: .leading, spacing: 4) {
                Text(S.settingsTitle).font(.title2.bold())
                Text(appState.isPro || appState.isAdmin ? L.t("settings_account_tier_pro") : L.t("settings_account_tier_free"))
                    .font(.caption.weight(.semibold))
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(colors.primaryContainer)
                    .clipShape(Capsule())
            }
            Spacer()
        }
        .padding(.bottom, 4)
    }

    private var accountSection: some View {
        SettingsSectionCard(title: S.settingsAccountSection) {
            SettingsOptionRow(
                icon: "person.circle",
                title: S.accountTitle,
                subtitle: appState.isAuthenticated ? appState.accountEmail : S.settingsAccountSubtitle,
                action: { appState.selectedTab = .account }
            )
        }
    }

    private var proSection: some View {
        SettingsSectionCard(title: S.settingsProSection) {
            VStack(alignment: .leading, spacing: 8) {
                Text(S.settingsProSectionHint)
                    .font(.footnote)
                    .foregroundStyle(colors.onSurfaceVariant)
                    .padding(.horizontal, 16)
                    .padding(.top, 12)
                    .walkthroughAnchor(WalkthroughTargetIds.proSection)
                PrimaryButton(title: S.settingsBuyPro, loading: appState.isPurchasing) {
                    if !appState.isAuthenticated {
                        appState.selectedTab = .account
                    } else {
                        Task { await appState.purchasePro() }
                    }
                }
                .padding(.horizontal, 16)
                Button(S.settingsRestorePro) {
                    Task { await appState.restorePro() }
                }
                .frame(maxWidth: .infinity)
                .padding(.bottom, 12)
            }
        }
    }

    private var appearanceSection: some View {
        SettingsSectionCard(title: S.appearanceSection) {
            SettingsOptionRow(icon: "paintpalette", title: S.themeLabel, subtitle: themeLabel(appState.themeMode), action: { themeDialog = true })
            SettingsOptionRow(icon: "globe", title: S.languageLabel, subtitle: appState.languageMode.label, action: { languageDialog = true })
            SettingsOptionRow(icon: "sidebar.leading", title: S.menuTypeLabel, subtitle: navLabel(appState.navStyle), action: { navDialog = true })
        }
    }

    private var permissionsSection: some View {
        SettingsSectionCard(title: S.settingsPermissionsSection) {
            SettingsOptionRow(
                icon: "bell",
                title: S.settingsNotificationsTitle,
                subtitle: appState.notificationsEnabled ? S.settingsNotificationsGranted : S.settingsNotificationsDenied,
                action: { appState.updateNotifications(!appState.notificationsEnabled) }
            )
        }
    }

    private var generalSection: some View {
        SettingsSectionCard(title: S.settingsGeneralSection) {
            SettingsOptionRow(icon: "questionmark.circle", title: S.settingsHelpTitle, subtitle: S.settingsHelpSubtitle, action: { generalPage = .help })
            SettingsOptionRow(icon: "doc.text", title: S.settingsChangelogTitle, subtitle: S.settingsChangelogSubtitle, action: { generalPage = .changelog })
            SettingsOptionRow(icon: "envelope", title: S.settingsContactTitle, subtitle: S.settingsContactSubtitle, action: { generalPage = .contact })
            SettingsOptionRow(icon: "ant", title: S.settingsBugReportTitle, subtitle: S.settingsBugReportSubtitle, action: { generalPage = .bugReport })
        }
    }

    private var legalSection: some View {
        SettingsSectionCard(title: S.settingsLegalSection) {
            SettingsOptionRow(icon: "hand.raised", title: S.settingsPrivacyTitle, subtitle: "", action: { generalPage = .privacy })
            SettingsOptionRow(icon: "doc.plaintext", title: S.settingsTermsTitle, subtitle: "", action: { generalPage = .terms })
            SettingsOptionRow(icon: "doc.text.magnifyingglass", title: S.settingsDataProcessingTitle, subtitle: "", action: { generalPage = .dataProcessing })
            SettingsOptionRow(icon: "building.2", title: S.settingsImprintTitle, subtitle: "", action: { generalPage = .imprint })
        }
    }

    private var settingsFooter: some View {
        VStack(spacing: 8) {
            Button(S.settingsRestartWalkthrough) {
                appState.walkthroughCompleted = false
                appState.walkthroughStepIndex = 0
                appState.showWalkthrough = true
                SharedKitBridge.persistWalkthroughCompleted(false)
            }
            Text("2-Stroke Calc · v1.0")
                .font(.caption2)
                .foregroundStyle(colors.onSurfaceVariant)
        }
        .frame(maxWidth: .infinity)
        .padding(.top, 8)
    }

    @ViewBuilder
    private func generalContentDetail(_ page: GeneralContentPage) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Button { generalPage = nil } label: {
                Label(S.settingsTitle, systemImage: "chevron.left")
            }
            Text(page.title).font(.title3.bold())
            switch page {
            case .help:
                HelpFaqView()
            case .changelog:
                ChangelogView()
            case .contact:
                ContactFormView(isBugReport: false)
            case .bugReport:
                ContactFormView(isBugReport: true)
            case .privacy:
                InfoSectionsView(intro: SettingsContent.privacyIntro, sections: SettingsContent.privacySections)
            case .terms:
                InfoSectionsView(intro: SettingsContent.termsIntro, sections: SettingsContent.termsSections)
            case .dataProcessing:
                InfoSectionsView(intro: SettingsContent.dataProcessingIntro, sections: SettingsContent.dataProcessingSections)
            case .imprint:
                InfoSectionsView(intro: SettingsContent.imprintIntro, sections: SettingsContent.imprintSections)
            }
        }
    }

    private var themePickerSheet: some View {
        NavigationStack {
            List(ThemeMode.allCases, id: \.self) { mode in
                Button(themeLabel(mode)) { appState.updateTheme(mode); themeDialog = false }
            }
            .navigationTitle(S.themeLabel)
            .toolbar { ToolbarItem(placement: .cancellationAction) { Button(S.cancel) { themeDialog = false } } }
        }
    }

    private var languagePickerSheet: some View {
        NavigationStack {
            List(LanguageMode.allCases) { mode in
                Button {
                    appState.updateLanguage(mode)
                    languageDialog = false
                } label: {
                    HStack {
                        Text(mode.label)
                        Spacer()
                        if appState.languageMode == mode {
                            Image(systemName: "checkmark")
                                .foregroundStyle(Color.accentColor)
                        }
                    }
                }
            }
            .navigationTitle(S.languageLabel)
            .toolbar { ToolbarItem(placement: .cancellationAction) { Button(S.cancel) { languageDialog = false } } }
        }
    }

    private var navPickerSheet: some View {
        NavigationStack {
            List(NavStyle.allCases, id: \.self) { style in
                Button {
                    appState.updateNavStyle(style)
                    navDialog = false
                } label: {
                    HStack {
                        Text(navLabel(style))
                        Spacer()
                        if appState.navStyle == style {
                            Image(systemName: "checkmark")
                                .foregroundStyle(Color.accentColor)
                        }
                    }
                }
            }
            .navigationTitle(S.menuTypeLabel)
            .toolbar { ToolbarItem(placement: .cancellationAction) { Button(S.cancel) { navDialog = false } } }
        }
    }

    private func themeLabel(_ mode: ThemeMode) -> String {
        switch mode {
        case .system: return S.themeSystem
        case .light: return S.themeLight
        case .dark: return S.themeDark
        }
    }

    private func navLabel(_ style: NavStyle) -> String {
        switch style {
        case .bottomBar: return S.navStyleBottom
        case .drawer: return S.navStyleDrawer
        }
    }
}

enum GeneralContentPage {
    case help, changelog, contact, bugReport, privacy, terms, dataProcessing, imprint

    var title: String {
        switch self {
        case .help: return S.settingsHelpTitle
        case .changelog: return S.settingsChangelogTitle
        case .contact: return S.settingsContactTitle
        case .bugReport: return S.settingsBugReportTitle
        case .privacy: return S.settingsPrivacyTitle
        case .terms: return S.settingsTermsTitle
        case .dataProcessing: return S.settingsDataProcessingTitle
        case .imprint: return S.settingsImprintTitle
        }
    }
}

// AccountScreen and AdminPanelScreen remain in this file below

struct AccountScreen: View {
    @EnvironmentObject private var appState: AppState
    @Environment(\.themeColors) private var colors
    var onBack: (() -> Void)?
    @State private var displayName = ""
    @State private var currentPassword = ""
    @State private var newPassword = ""
    @State private var showLogoutConfirm = false

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                if let onBack {
                    Button(action: onBack) { Label(S.settingsTitle, systemImage: "chevron.left") }
                }
                Text(S.accountTitle).font(.title2.bold())
                HStack(spacing: 16) {
                    Image(systemName: "person.circle.fill").font(.system(size: 56)).foregroundStyle(colors.primary)
                    VStack(alignment: .leading) {
                        Text(appState.displayName.isEmpty ? L.t("user_fallback_name") : appState.displayName).font(.headline)
                        Text(appState.accountEmail).foregroundStyle(colors.onSurfaceVariant)
                    }
                }
                if !appState.isAuthenticated {
                    PrimaryButton(title: S.loginButton) { appState.selectedTab = .login }
                } else {
                    AppOutlinedField(label: S.displayNameLabel, text: $displayName)
                    PrimaryButton(title: S.save) { appState.displayName = displayName }
                    AppOutlinedField(label: L.t("current_password"), text: $currentPassword, secure: true)
                    AppOutlinedField(label: L.t("new_password"), text: $newPassword, secure: true)
                    Button(L.t("change_password")) {}
                    if appState.isAdmin {
                        Button(L.t("admin_panel_button")) { appState.selectedTab = .admin }
                    }
                    Button(S.logout, role: .destructive) { showLogoutConfirm = true }
                }
            }
            .padding(20)
        }
        .background(colors.background)
        .onAppear { displayName = appState.displayName }
        .alert(L.t("logout_dialog_title"), isPresented: $showLogoutConfirm) {
            Button(S.logout, role: .destructive) {
                Task { await appState.signOut() }
                onBack?()
            }
            Button(S.cancel, role: .cancel) {}
        }
    }
}

struct AdminPanelScreen: View {
    @EnvironmentObject private var appState: AppState
    @Environment(\.themeColors) private var colors
    @StateObject private var viewModel = AdminPanelViewModel()
    var onBack: (() -> Void)?

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                if let onBack {
                    Button(action: onBack) { Label(S.settingsTitle, systemImage: "chevron.left") }
                }
                Text(S.adminPanelTitle).font(.title2.bold())
                if viewModel.loading { ProgressView() }
                if let error = viewModel.error {
                    Text(error).foregroundStyle(colors.error).font(.footnote)
                }
                if let message = viewModel.message {
                    Text(message).foregroundStyle(colors.primary).font(.footnote)
                }
                Toggle(L.t("admin_demo_vehicles_section_title"), isOn: $viewModel.demoVehiclesEnabled)
                TextField(L.t("admin_search_users_hint"), text: $viewModel.searchQuery)
                    .textFieldStyle(.roundedBorder)
                Text(L.t("admin_calculator_availability_title")).font(.headline)
                ForEach(CalculatorCatalog.displayOrder, id: \.name) { id in
                    Toggle(isOn: Binding(
                        get: { viewModel.calculatorToggles[id.name] ?? true },
                        set: { viewModel.toggleCalculator(id.name, enabled: $0) }
                    )) {
                        Text(S.calculatorTab(id))
                    }
                }
                Text(L.t("admin_pro_modules_section_title")).font(.headline)
                ForEach(Array(viewModel.proModuleToggles.keys.sorted()), id: \.self) { module in
                    Toggle(isOn: Binding(
                        get: { viewModel.proModuleToggles[module] ?? false },
                        set: { viewModel.toggleProModule(module, enabled: $0) }
                    )) {
                        Text(module)
                    }
                }
                PrimaryButton(title: L.t("admin_save_all_settings"), loading: viewModel.loading) {
                    Task { await viewModel.saveSettings(); await appState.applyAdminRefresh() }
                }
                Text(L.t("admin_push_send_all")).font(.headline)
                TextField(L.t("admin_push_title_label"), text: $viewModel.pushTitle).textFieldStyle(.roundedBorder)
                TextField(L.t("admin_push_body_label"), text: $viewModel.pushBody).textFieldStyle(.roundedBorder)
                PrimaryButton(title: L.t("admin_push_send_all"), loading: viewModel.loading) {
                    Task { await viewModel.sendPush() }
                }
                Text(L.tf("admin_users_count", String(viewModel.filteredUsers.count))).font(.headline)
                ForEach(viewModel.filteredUsers, id: \.id) { user in
                    VStack(alignment: .leading, spacing: 4) {
                        Text(user.displayName ?? user.email ?? user.id).font(.subheadline.bold())
                        Text(user.email ?? L.t("gear_result_table_no_jump")).font(.caption).foregroundStyle(colors.onSurfaceVariant)
                        HStack {
                            Button(user.banned == true ? L.t("admin_unban_user_action") : L.t("admin_ban_user_action")) {
                                Task { await viewModel.banUser(user, banned: user.banned != true) }
                            }
                            Button(user.isPro == true ? L.t("admin_revoke_pro_action") : L.t("admin_grant_pro_action")) {
                                Task { await viewModel.setPro(user, isPro: user.isPro != true) }
                            }
                        }
                        .font(.caption)
                    }
                    .padding(12)
                    .background(colors.surface)
                    .clipShape(RoundedRectangle(cornerRadius: AppTheme.containerCornerRadius))
                }
            }
            .padding(20)
        }
        .background(colors.background)
        .task { await viewModel.load() }
    }
}
