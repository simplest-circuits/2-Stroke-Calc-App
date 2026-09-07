#!/usr/bin/env python3
"""Generate iOS localization from Android strings.xml (de/en/es/pt/sv/da/nb)."""
from __future__ import annotations

import re
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "androidApp" / "src" / "main" / "res"
LOC_DIR = Path(__file__).resolve().parent / "TwoStrokeCalcIOS" / "Localization"

LOCALES = {
    "german": RES / "values" / "strings.xml",
    "english": RES / "values-en" / "strings.xml",
    "spanish": RES / "values-es" / "strings.xml",
    "portuguese": RES / "values-pt" / "strings.xml",
    "swedish": RES / "values-sv" / "strings.xml",
    "danish": RES / "values-da" / "strings.xml",
    "norwegian": RES / "values-nb" / "strings.xml",
}

NORDIC_LOCALES = {"swedish", "danish", "norwegian"}

S_KEYS = {
    "appName": "app_name",
    "splashSlogan": "splash_slogan",
    "navCalculator": "nav_calculator",
    "navTools": "nav_tools",
    "navVehicles": "nav_vehicles",
    "settingsTitle": "settings_title",
    "adminPanelTitle": "admin_panel_title",
    "calculatorOverviewSubtitle": "calculator_overview_subtitle",
    "calculatorBack": "calculator_back",
    "loginTitle": "login_title",
    "loginSubtitle": "login_subtitle",
    "emailLabel": "email_label",
    "passwordLabel": "password_label",
    "loginButton": "login_button",
    "forgotPassword": "forgot_password",
    "signInGoogle": "sign_in_google",
    "noAccountRegister": "no_account_register",
    "registerTitle": "register_title",
    "registerButton": "create_account",
    "displayNameLabel": "profile_display_name_label",
    "forgotPasswordTitle": "forgot_password_title",
    "forgotPasswordButton": "send_link",
    "accountTitle": "profile_title",
    "logout": "logout_label",
    "proReadOnly": "pro_read_only_banner_message",
    "buyPro": "settings_buy_pro",
    "welcomeTitle": "walkthrough_free_welcome_title",
    "welcomeContinue": "walkthrough_next",
    "permissionsTitle": "first_install_notifications_title",
    "permissionsContinue": "first_install_allow",
    "vehiclesEmpty": "vehicles_empty_title",
    "vehiclesAdd": "vehicles_add",
    "save": "save",
    "cancel": "cancel",
    "delete": "delete",
    "calculate": "pt_calculate",
    "resultTitle": "pt_result_title",
    "sectionInput": "pt_section_input",
    "proUpsellTitle": "pro_upsell_title",
    "authGateTitle": "auth_gate_title_default",
    "authGateMessage": "auth_gate_subtitle_default",
    "authGateLogin": "login_button",
    "settingsAccountSection": "settings_account_section",
    "settingsAccountSubtitle": "settings_account_subtitle",
    "settingsProSection": "settings_pro_section",
    "settingsProSectionHint": "settings_pro_section_hint",
    "settingsBuyPro": "settings_buy_pro",
    "settingsRestorePro": "settings_restore_pro",
    "appearanceSection": "appearance_section",
    "themeLabel": "theme_label",
    "languageLabel": "language_label",
    "menuTypeLabel": "menu_type_label",
    "settingsPermissionsSection": "settings_permissions_section",
    "settingsNotificationsTitle": "settings_notifications_title",
    "settingsNotificationsGranted": "settings_notifications_subtitle_granted",
    "settingsNotificationsDenied": "settings_notifications_subtitle_denied",
    "settingsGeneralSection": "settings_general_section",
    "settingsHelpTitle": "settings_help_title",
    "settingsHelpSubtitle": "settings_help_subtitle",
    "settingsHelpBody": "settings_detail_help_intro",
    "settingsChangelogTitle": "settings_changelog_title",
    "settingsChangelogSubtitle": "settings_changelog_subtitle",
    "settingsChangelogBody": "settings_detail_changelog_intro",
    "settingsContactTitle": "settings_contact_title",
    "settingsContactSubtitle": "settings_contact_subtitle",
    "settingsContactBody": "settings_detail_contact_intro",
    "settingsBugReportTitle": "settings_contact_hub_bug_title",
    "settingsBugReportSubtitle": "settings_contact_hub_bug_subtitle",
    "settingsBugReportBody": "settings_bug_report_info",
    "settingsLegalSection": "settings_legal_section",
    "settingsPrivacyTitle": "settings_privacy_title",
    "settingsPrivacyBody": "settings_detail_privacy_intro",
    "settingsTermsTitle": "settings_terms_title",
    "settingsTermsBody": "settings_detail_terms_intro",
    "settingsDataProcessingTitle": "settings_data_processing_title",
    "settingsImprintTitle": "settings_imprint_title",
    "settingsRestartWalkthrough": "settings_restart_walkthrough",
    "walkthroughFreeWelcomeTitle": "walkthrough_free_welcome_title",
    "walkthroughFreeWelcomeBody": "walkthrough_free_welcome_body",
    "walkthroughProWelcomeTitle": "walkthrough_pro_welcome_title",
    "walkthroughProWelcomeBody": "walkthrough_pro_welcome_body",
    "walkthroughCalculatorNavTitle": "walkthrough_calculator_nav_title",
    "walkthroughFreeCalculatorNavBody": "walkthrough_free_calculator_nav_body",
    "walkthroughProCalculatorNavBody": "walkthrough_pro_calculator_nav_body",
    "walkthroughFreeCalculatorTitle": "walkthrough_free_calculator_title",
    "walkthroughFreeCalculatorBody": "walkthrough_free_calculator_body",
    "walkthroughLockedCalculatorTitle": "walkthrough_locked_calculator_title",
    "walkthroughLockedCalculatorBody": "walkthrough_locked_calculator_body",
    "walkthroughProCalculatorListTitle": "walkthrough_pro_calculator_list_title",
    "walkthroughProCalculatorListBody": "walkthrough_pro_calculator_list_body",
    "walkthroughFreeVehiclesNavTitle": "walkthrough_free_vehicles_nav_title",
    "walkthroughFreeVehiclesNavBody": "walkthrough_free_vehicles_nav_body",
    "walkthroughVehiclesNavTitle": "walkthrough_vehicles_nav_title",
    "walkthroughProVehiclesNavBody": "walkthrough_pro_vehicles_nav_body",
    "walkthroughVehiclesAddTitle": "walkthrough_vehicles_add_title",
    "walkthroughProVehiclesAddBody": "walkthrough_pro_vehicles_add_body",
    "walkthroughSettingsNavTitle": "walkthrough_settings_nav_title",
    "walkthroughFreeSettingsNavBody": "walkthrough_free_settings_nav_body",
    "walkthroughProSettingsNavBody": "walkthrough_pro_settings_nav_body",
    "walkthroughSettingsProTitle": "walkthrough_settings_pro_title",
    "walkthroughSettingsProBody": "walkthrough_settings_pro_body",
    "walkthroughSettingsAppearanceTitle": "walkthrough_settings_appearance_title",
    "walkthroughSettingsAppearanceBody": "walkthrough_settings_appearance_body",
    "walkthroughStepCounter": "walkthrough_step_counter",
    "walkthroughSkip": "walkthrough_skip",
    "walkthroughBack": "walkthrough_back",
    "walkthroughNext": "walkthrough_next",
    "walkthroughFinish": "walkthrough_finish",
    "languageSystem": "language_system",
    "languageGerman": "language_german",
    "languageEnglish": "language_english",
    "languageSpanish": "language_spanish",
    "languagePortuguese": "language_portuguese",
    "languageSwedish": "language_swedish",
    "languageDanish": "language_danish",
    "languageNorwegian": "language_norwegian",
    "themeSystem": "theme_system",
    "themeLight": "theme_light",
    "themeDark": "theme_dark",
    "navStyleBottom": "nav_style_bottom",
    "navStyleDrawer": "nav_style_side_panel",
}

MANUAL_OVERRIDES: dict[str, dict[str, str]] = {
    "settingsLegalSection": {
        "german": "Rechtliches",
        "english": "Legal",
        "spanish": "Legal",
        "portuguese": "Jurídico",
    },
    "settingsRestartWalkthrough": {
        "german": "App-Tour erneut starten",
        "english": "Restart app tour",
        "spanish": "Reiniciar tour de la app",
        "portuguese": "Reiniciar tour do app",
    },
}

# iOS uses the App Store (StoreKit), not Google Play Billing.
IOS_STORE_OVERRIDES: dict[str, dict[str, str]] = {
    "pro_upsell_message": {
        "german": "Dieses Modul ist Teil der Pro-Version. Schalte es über den In-App-Kauf im App Store frei.",
        "english": "This module is part of the Pro version. Unlock it with an in-app purchase via the App Store.",
        "spanish": "Este módulo es parte de la versión Pro. Desbloquéalo mediante compra dentro de la app en el App Store.",
        "portuguese": "Este módulo faz parte da versão Pro. Desbloqueie-o por meio de compra no aplicativo na App Store.",
        "swedish": "Denna modul är en del av Pro-versionen. Lås upp den via köp i appen i App Store.",
        "danish": "Dette modul er en del af Pro-versionen. Lås den op via køb i appen i App Store.",
        "norwegian": "Denne modulen er en del av Pro-versjonen. Lås den opp via kjøp i appen i App Store.",
    },
    "settings_pro_billing_unavailable": {
        "german": "App Store-Käufe sind derzeit nicht verfügbar.",
        "english": "App Store purchases are currently unavailable.",
        "spanish": "Las compras del App Store no están disponibles actualmente.",
        "portuguese": "As compras da App Store não estão disponíveis no momento.",
        "swedish": "App Store-köp är för närvarande inte tillgängliga.",
        "danish": "App Store-køb er i øjeblikket ikke tilgængelige.",
        "norwegian": "App Store-kjøp er for øyeblikket utilgjengelige.",
    },
    "walkthrough_settings_pro_body": {
        "german": "Hier kaufst oder stellst du die Pro-Version über den App Store wieder her – für Bearbeiten in Rechnern und Fahrzeugen.",
        "english": "Buy or restore the Pro version via the App Store here – to edit calculators and vehicles.",
        "spanish": "Aquí puedes comprar o restaurar la versión Pro a través del App Store, para editar calculadoras y vehículos.",
        "portuguese": "Aqui você pode comprar ou restaurar a versão Pro via App Store – para edição em calculadoras e veículos.",
        "swedish": "Här kan du köpa eller återställa Pro-versionen via App Store – för redigering i miniräknare och fordon.",
        "danish": "Her kan du købe eller gendanne Pro-versionen via App Store – til redigering i lommeregnere og køretøjer.",
        "norwegian": "Her kan du kjøpe eller gjenopprette Pro-versjonen via App Store – for redigering i kalkulatorer og kjøretøy.",
    },
    "settings_detail_help_faq_answer_5": {
        "german": "Einige Module sind Teil der Pro-Version – standardmäßig Fahrzeuge, Steuerzeiten, Kabelquerschnitt, Flüssigkeitsrechner und Getriebe. Du kannst sie über den App Store kaufen oder per freigeschaltetem Konto nutzen.",
        "english": "Some modules are part of the Pro version – by default vehicles, port timing, wire gauge, liquid calculator and gearbox. You can buy them via the App Store or use them with an unlocked account.",
        "spanish": "Algunos módulos forman parte de la versión Pro: vehículos, sincronización, sección de cable, calculadora de fluidos y caja de cambios. Puedes comprarlos en el App Store o usarlos con una cuenta activada.",
        "portuguese": "Alguns módulos fazem parte da versão Pro – veículos, sincronização, seção de cabo, calculadora de fluidos e caixa de câmbio. Você pode comprá-los na App Store ou usá-los com uma conta ativada.",
        "swedish": "Vissa moduler ingår i Pro-versionen – standardfordon, timing, kabeltvärsnitt, vätskekalkylator och växellåda. Du kan köpa dem via App Store eller använda dem med ett aktiverat konto.",
        "danish": "Nogle moduler er en del af Pro-versionen – standardkøretøjer, timing, kabeltværsnit, væskeberegner og gearkasse. Du kan købe dem via App Store eller bruge dem med en aktiveret konto.",
        "norwegian": "Noen moduler er en del av Pro-versjonen – standard kjøretøy, timing, kabeltverrsnitt, væskekalkulator og girkasse. Du kan kjøpe dem via App Store eller bruke dem med en aktivert konto.",
    },
    "settings_detail_help_faq_answer_6": {
        "german": "Ein Konto ermöglicht die Cloud-Synchronisation deiner Fahrzeuge, den Pro-Kauf über den App Store und die Verwaltung deines Profils. Rechner kannst du auch ohne Konto nutzen.",
        "english": "An account enables cloud sync for your vehicles, Pro purchase via the App Store and profile management. You can use calculators without an account.",
        "spanish": "Una cuenta te permite sincronizar tus vehículos en la nube, comprar Pro a través del App Store y administrar tu perfil. También puedes usar las calculadoras sin una cuenta.",
        "portuguese": "Uma conta permite sincronizar seus veículos na nuvem, comprar o Pro na App Store e gerenciar seu perfil. Você também pode usar as calculadoras sem uma conta.",
        "swedish": "Ett konto låter dig molnsynkronisera dina fordon, köpa Pro via App Store och hantera din profil. Du kan också använda miniräknarna utan konto.",
        "danish": "En konto giver dig mulighed for at skysynkronisere dine køretøjer, købe Pro via App Store og administrere din profil. Du kan også bruge lommeregnere uden en konto.",
        "norwegian": "En konto lar deg skysynkronisere kjøretøyene dine, kjøpe Pro via App Store og administrere profilen din. Du kan også bruke kalkulatorene uten konto.",
    },
    "settings_detail_terms_pro_item_1": {
        "german": "Der Kauf der Pro-Version erfolgt über den App Store. Abwicklung, Rückerstattung und Widerruf richten sich nach den App-Store-Richtlinien.",
        "english": "Pro version purchases are made via the App Store. Processing, refunds and cancellation follow App Store policies.",
        "spanish": "La versión Pro se compra a través del App Store. El procesamiento, los reembolsos y las cancelaciones se basan en las políticas del App Store.",
        "portuguese": "A versão Pro é adquirida na App Store. O processamento, reembolsos e cancelamentos seguem as diretrizes da App Store.",
        "swedish": "Pro-versionen köps via App Store. Behandling, återbetalningar och avbokningar följer App Stores riktlinjer.",
        "danish": "Pro-versionen købes via App Store. Behandling, refusioner og annulleringer følger App Stores retningslinjer.",
        "norwegian": "Pro-versjonen kjøpes via App Store. Behandling, refusjoner og kanselleringer følger App Stores retningslinjer.",
    },
    "settings_detail_terms_pro_item_2": {
        "german": "Welche Module Pro erfordern, kann sich ändern. Freigeschaltete Käufe bleiben mit deiner Apple-ID verknüpft.",
        "english": "Which modules require Pro may change. Unlocked purchases remain linked to your Apple ID.",
        "spanish": "Los módulos que requieren Pro pueden cambiar. Las compras desbloqueadas permanecen vinculadas a tu Apple ID.",
        "portuguese": "Quais módulos exigem Pro podem mudar. Compras desbloqueadas permanecem vinculadas à sua Apple ID.",
        "swedish": "Vilka moduler som kräver Pro kan ändras. Upplåsta köp förblir kopplade till ditt Apple-ID.",
        "danish": "Hvilke moduler der kræver Pro kan ændre sig. Låste køb forbliver knyttet til dit Apple-ID.",
        "norwegian": "Hvilke moduler som krever Pro kan endres. Opplåste kjøp forblir knyttet til Apple-ID-en din.",
    },
    "settings_detail_privacy_purchase_item_1": {
        "german": "Pro-Käufe werden über den App Store abgewickelt. Zahlungsdaten werden von Apple verarbeitet, nicht von uns.",
        "english": "Pro purchases are processed via the App Store. Payment data is handled by Apple, not by us.",
        "spanish": "Las compras Pro se procesan a través del App Store. Los datos de pago son procesados por Apple, no por nosotros.",
        "portuguese": "As compras Pro são processadas na App Store. Os dados de pagamento são processados pela Apple, não por nós.",
        "swedish": "Pro-köp bearbetas via App Store. Betalningsdata hanteras av Apple, inte av oss.",
        "danish": "Pro-køb behandles via App Store. Betalingsdata behandles af Apple, ikke af os.",
        "norwegian": "Pro-kjøp behandles via App Store. Betalingsdata behandles av Apple, ikke av oss.",
    },
    "settings_rate_subtitle": {
        "german": "Bewerte uns im App Store",
        "english": "Rate us on the App Store",
        "spanish": "Califícanos en el App Store",
        "portuguese": "Avalie-nos na App Store",
        "swedish": "Betygsätt oss i App Store",
        "danish": "Bedøm os i App Store",
        "norwegian": "Vurder oss i App Store",
    },
    "auth_error_google_simulator": {
        "german": "Google-Anmeldung ist im Browser-Simulator nicht verfügbar. Bitte E-Mail-Login nutzen oder auf einem echten iPhone testen.",
        "english": "Google sign-in is not available in the browser simulator. Use email sign-in or test on a real iPhone.",
        "spanish": "El inicio de sesión con Google no está disponible en el simulador del navegador. Usa el inicio de sesión por correo o prueba en un iPhone real.",
        "portuguese": "O login com Google não está disponível no simulador do navegador. Use login por e-mail ou teste em um iPhone real.",
        "swedish": "Google-inloggning är inte tillgänglig i webbläsarsimulatorn. Använd e-postinloggning eller testa på en riktig iPhone.",
        "danish": "Google-login er ikke tilgængelig i browser-simulatoren. Brug e-mail-login eller test på en rigtig iPhone.",
        "norwegian": "Google-pålogging er ikke tilgjengelig i nettlesersimulatoren. Bruk e-postpålogging eller test på en ekte iPhone.",
    },
}


def apply_ios_store_overrides(tables: dict[str, dict[str, str]]) -> None:
    for key, per_lang in IOS_STORE_OVERRIDES.items():
        for lang, value in per_lang.items():
            tables[lang][key] = value

CALC_TABS = {
    "timing": ("calculator_tab_timing", "calculator_overview_timing_desc"),
    "portArea": ("calculator_tab_port_area", "calculator_overview_port_area_desc"),
    "ignition": ("calculator_tab_ignition", "calculator_overview_ignition_desc"),
    "dcCable": ("calculator_tab_dc_cable", "calculator_overview_dc_cable_desc"),
    "fluid": ("calculator_tab_fluid", "calculator_overview_fluid_desc"),
    "compression": ("calculator_tab_compression", "calculator_overview_compression_desc"),
    "squishBand": ("calculator_tab_squish_band", "calculator_overview_squish_band_desc"),
    "meanPressure": ("calculator_tab_mean_pressure", "calculator_overview_mean_pressure_desc"),
    "gear": ("calculator_tab_gear", "calculator_overview_gear_desc"),
    "exhaust": ("calculator_tab_exhaust", "calculator_overview_exhaust_desc"),
    "stinger": ("calculator_tab_stinger", "calculator_overview_stinger_desc"),
    "counterweight": ("calculator_tab_counterweight", "calculator_overview_counterweight_desc"),
    "variatorWeight": ("calculator_tab_variator_weight", "calculator_overview_variator_weight_desc"),
    "fuelMix": ("calculator_tab_fuel_mix", "calculator_overview_fuel_mix_desc"),
    "carbJet": ("calculator_tab_carb_jet", "calculator_overview_carb_jet_desc"),
    "dynoInertia": ("calculator_tab_dyno_inertia", "calculator_overview_dyno_inertia_desc"),
    "vehicleDynamics": ("calculator_tab_vehicle_dynamics", "calculator_overview_vehicle_dynamics_desc"),
}


def parse_strings(path: Path) -> dict[str, str]:
    if not path.exists():
        return {}
    tree = ET.parse(path)
    result: dict[str, str] = {}
    for node in tree.getroot().findall("string"):
        name = node.attrib.get("name")
        if not name:
            continue
        text = "".join(node.itertext())
        text = text.replace("\\n", "\n").replace("\\'", "'").replace('\\"', '"')
        text = re.sub(r"%\d+\$[sd]", "%@", text)
        text = text.replace("%d", "%@").replace("%s", "%@")
        result[name] = text
    return result


def lookup(tables: dict[str, dict[str, str]], locale: str, key: str) -> str | None:
    if locale in NORDIC_LOCALES:
        fallback_chain = (locale, "german", "english")
    else:
        fallback_chain = (locale, "english", "german")
    for lang in fallback_chain:
        value = tables.get(lang, {}).get(key)
        if value:
            return value
    return None


def swift_escape(value: str) -> str:
    return value.replace("\\", "\\\\").replace('"', '\\"').replace("\n", "\\n")


def emit_string_dict(name: str, entries: dict[str, str], key_type: str = "StringKey") -> list[str]:
    lines = [f"    private static let {name}: [{key_type}: String] = ["]
    for key, value in entries.items():
        if key_type == "StringKey":
            lines.append(f'        .{key}: "{swift_escape(value)}",')
        else:
            lines.append(f'        "{key}": "{swift_escape(value)}",')
    lines.append("    ]")
    return lines


def emit_android_strings(tables: dict[str, dict[str, str]]) -> None:
    all_keys = sorted(tables["german"].keys())
    lines = [
        "// Generated by iosApp/generate_string_catalog.py — do not edit by hand.",
        "import Foundation",
        "",
        "enum AndroidStrings {",
    ]
    for lang in LOCALES:
        lines.extend(emit_string_dict(lang, {k: tables[lang].get(k, "") for k in all_keys}, "String"))
        lines.append("")

    lines.extend([
        "    static func text(_ key: String, language: AppLanguage) -> String {",
        "        let primary: [String: String]",
        "        let fallbacks: [[String: String]]",
        "        switch language {",
        "        case .german:",
        "            primary = german; fallbacks = [english]",
        "        case .english:",
        "            primary = english; fallbacks = [german]",
        "        case .spanish:",
        "            primary = spanish; fallbacks = [english, german]",
        "        case .portuguese:",
        "            primary = portuguese; fallbacks = [english, german]",
        "        case .swedish:",
        "            primary = swedish; fallbacks = [german, english]",
        "        case .danish:",
        "            primary = danish; fallbacks = [german, english]",
        "        case .norwegian:",
        "            primary = norwegian; fallbacks = [german, english]",
        "        }",
        "        if let value = primary[key], !value.isEmpty { return value }",
        "        for table in fallbacks {",
        "            if let value = table[key], !value.isEmpty { return value }",
        "        }",
        "        return key",
        "    }",
        "}",
        "",
    ])
    out = LOC_DIR / "AndroidStrings.swift"
    out.write_text("\n".join(lines), encoding="utf-8")
    print(f"Wrote {out} ({len(all_keys)} keys)")


def emit_l_swift() -> None:
    body = """// Generated by iosApp/generate_string_catalog.py — do not edit by hand.
import Foundation

/// Localized Android `strings.xml` lookup by resource name.
enum L {
    private static var lang: AppLanguage { AppLocalization.language }

    static func t(_ key: String) -> String {
        AndroidStrings.text(key, language: lang)
    }

    static func tf(_ key: String, _ args: CVarArg...) -> String {
        var template = t(key)
        guard !args.isEmpty else { return template }
        // Android `%d`/`%s` are normalized to `%@`; avoid String(format:) with raw integers.
        template = template.replacingOccurrences(of: "%%", with: "\\u{0001}")
        for arg in args {
            guard let range = template.range(of: "%@") else { break }
            template.replaceSubrange(range, with: stringValue(for: arg))
        }
        return template.replacingOccurrences(of: "\\u{0001}", with: "%")
    }

    private static func stringValue(for value: CVarArg) -> String {
        // CVarArg type-erases integers; cast to Any before switching on concrete types.
        let any: Any = value
        if let string = any as? String { return string }
        if let int = any as? Int { return String(int) }
        if let int32 = any as? Int32 { return String(int32) }
        if let int64 = any as? Int64 { return String(int64) }
        if let uint = any as? UInt { return String(uint) }
        if let uint32 = any as? UInt32 { return String(uint32) }
        if let double = any as? Double { return String(double) }
        if let float = any as? Float { return String(float) }
        if let number = any as? NSNumber { return number.stringValue }
        return String(describing: any)
    }
}
"""
    out = LOC_DIR / "L.swift"
    out.write_text(body, encoding="utf-8")
    print(f"Wrote {out}")


def emit_string_catalog(s_entries: dict, calc_tab_entries: dict, calc_desc_entries: dict) -> None:
    all_s_keys = sorted(s_entries["german"].keys())
    all_calc_keys = sorted(calc_tab_entries["german"].keys())
    out = LOC_DIR / "StringCatalog.swift"

    lines = [
        "// Generated by iosApp/generate_string_catalog.py — do not edit by hand.",
        "import Foundation",
        "",
        "enum AppLanguage: String, CaseIterable {",
        "    case german, english, spanish, portuguese, swedish, danish, norwegian",
        "",
        "    static func fromSystem() -> AppLanguage {",
        "        let code = Locale.current.language.languageCode?.identifier.lowercased() ?? \"en\"",
        "        switch code {",
        "        case \"de\": return .german",
        "        case \"es\": return .spanish",
        "        case \"pt\": return .portuguese",
        "        case \"sv\": return .swedish",
        "        case \"da\": return .danish",
        "        case \"nb\", \"no\": return .norwegian",
        "        case \"en\": return .english",
        "        default: return .english",
        "        }",
        "    }",
        "}",
        "",
        "enum StringKey: String, CaseIterable {",
    ]
    for key in all_s_keys:
        lines.append(f"    case {key}")
    lines.append("}")
    lines.append("")
    lines.append("enum StringCatalog {")
    for lang in LOCALES:
        lines.extend(emit_string_dict(lang, s_entries[lang]))
        lines.append("")
    for lang in LOCALES:
        lines.extend(emit_string_dict(f"calcTab_{lang}", calc_tab_entries[lang], "String"))
        lines.append("")
    for lang in LOCALES:
        lines.extend(emit_string_dict(f"calcDesc_{lang}", calc_desc_entries[lang], "String"))
        lines.append("")

    lines.extend([
        "    static func text(_ key: StringKey, language: AppLanguage) -> String {",
        "        switch language {",
        "        case .german: return german[key] ?? english[key] ?? key.rawValue",
        "        case .english: return english[key] ?? german[key] ?? key.rawValue",
        "        case .spanish: return spanish[key] ?? english[key] ?? german[key] ?? key.rawValue",
        "        case .portuguese: return portuguese[key] ?? english[key] ?? german[key] ?? key.rawValue",
        "        case .swedish: return swedish[key] ?? german[key] ?? english[key] ?? key.rawValue",
        "        case .danish: return danish[key] ?? german[key] ?? english[key] ?? key.rawValue",
        "        case .norwegian: return norwegian[key] ?? german[key] ?? english[key] ?? key.rawValue",
        "        }",
        "    }",
        "",
        "    static func calculatorTab(_ id: String, language: AppLanguage) -> String {",
        "        let table: [String: String]",
        "        switch language {",
        "        case .german: table = calcTab_german",
        "        case .english: table = calcTab_english",
        "        case .spanish: table = calcTab_spanish",
        "        case .portuguese: table = calcTab_portuguese",
        "        case .swedish: table = calcTab_swedish",
        "        case .danish: table = calcTab_danish",
        "        case .norwegian: table = calcTab_norwegian",
        "        }",
        "        return table[id] ?? calcTab_english[id] ?? calcTab_german[id] ?? id",
        "    }",
        "",
        "    static func calculatorDescription(_ id: String, language: AppLanguage) -> String {",
        "        let table: [String: String]",
        "        switch language {",
        "        case .german: table = calcDesc_german",
        "        case .english: table = calcDesc_english",
        "        case .spanish: table = calcDesc_spanish",
        "        case .portuguese: table = calcDesc_portuguese",
        "        case .swedish: table = calcDesc_swedish",
        "        case .danish: table = calcDesc_danish",
        "        case .norwegian: table = calcDesc_norwegian",
        "        }",
        "        return table[id] ?? calcDesc_english[id] ?? calcDesc_german[id] ?? \"\"",
        "    }",
        "}",
        "",
    ])
    out.write_text("\n".join(lines), encoding="utf-8")
    print(f"Wrote {out} ({len(all_s_keys)} UI keys, {len(all_calc_keys)} calculator keys)")
    emit_s_swift(all_s_keys)


def emit_s_swift(keys: list[str]) -> None:
    s_out = LOC_DIR / "S.swift"
    skip = {"walkthroughStepCounter"}
    body = [
        "import Foundation",
        "import sharedKit",
        "",
        "/// Localized UI strings aligned with Android `strings.xml`.",
        "enum S {",
        "    private static var lang: AppLanguage { AppLocalization.language }",
        "    private static func t(_ key: StringKey) -> String { StringCatalog.text(key, language: lang) }",
        "",
    ]
    for key in keys:
        if key in skip:
            continue
        body.append(f"    static var {key}: String {{ t(.{key}) }}")
    body.extend([
        "",
        "    static func walkthroughStepCounter(current: Int, total: Int) -> String {",
        "        let template = t(.walkthroughStepCounter)",
        "        let parts = template.components(separatedBy: \"%@\")",
        "        guard parts.count == 3 else { return template }",
        "        return parts[0] + \"\\(current)\" + parts[1] + \"\\(total)\" + parts[2]",
        "    }",
        "",
        "    static func calculatorTab(_ id: CalculatorId) -> String {",
        "        StringCatalog.calculatorTab(calculatorCatalogKey(id), language: lang)",
        "    }",
        "",
        "    static func calculatorDescription(_ id: CalculatorId) -> String {",
        "        StringCatalog.calculatorDescription(calculatorCatalogKey(id), language: lang)",
        "    }",
        "",
        "    private static func calculatorCatalogKey(_ id: CalculatorId) -> String {",
        "        switch id {",
        "        case .timing: return \"timing\"",
        "        case .portArea: return \"portArea\"",
        "        case .ignition: return \"ignition\"",
        "        case .dcCable: return \"dcCable\"",
        "        case .fluid: return \"fluid\"",
        "        case .compression: return \"compression\"",
        "        case .squishBand: return \"squishBand\"",
        "        case .meanPressure: return \"meanPressure\"",
        "        case .gear: return \"gear\"",
        "        case .exhaust: return \"exhaust\"",
        "        case .stinger: return \"stinger\"",
        "        case .counterweight: return \"counterweight\"",
        "        case .variatorWeight: return \"variatorWeight\"",
        "        case .fuelMix: return \"fuelMix\"",
        "        case .carbJet: return \"carbJet\"",
        "        case .dynoInertia: return \"dynoInertia\"",
        "        case .vehicleDynamics: return \"vehicleDynamics\"",
        "        default: return id.name.lowercased()",
        "        }",
        "    }",
        "}",
        "",
    ])
    s_out.write_text("\n".join(body), encoding="utf-8")
    print(f"Wrote {s_out}")


def main() -> None:
    tables = {lang: parse_strings(path) for lang, path in LOCALES.items()}
    apply_ios_store_overrides(tables)
    LOC_DIR.mkdir(parents=True, exist_ok=True)

    emit_android_strings(tables)
    emit_l_swift()

    s_entries: dict[str, dict[str, str]] = {lang: {} for lang in LOCALES}
    for swift_key, android_key in S_KEYS.items():
        for lang in LOCALES:
            value = lookup(tables, lang, android_key)
            if value:
                s_entries[lang][swift_key] = value
    for swift_key, per_lang in MANUAL_OVERRIDES.items():
        for lang, value in per_lang.items():
            s_entries[lang][swift_key] = value

    calc_tab_entries: dict[str, dict[str, str]] = {lang: {} for lang in LOCALES}
    calc_desc_entries: dict[str, dict[str, str]] = {lang: {} for lang in LOCALES}
    for calc_key, (tab_res, desc_res) in CALC_TABS.items():
        for lang in LOCALES:
            tab = lookup(tables, lang, tab_res)
            desc = lookup(tables, lang, desc_res)
            if tab:
                calc_tab_entries[lang][calc_key] = tab
            if desc:
                calc_desc_entries[lang][calc_key] = desc

    emit_string_catalog(s_entries, calc_tab_entries, calc_desc_entries)


if __name__ == "__main__":
    main()
