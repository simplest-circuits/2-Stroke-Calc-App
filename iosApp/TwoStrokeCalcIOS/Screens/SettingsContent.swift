import Foundation

enum SettingsContent {
    struct FaqItem: Identifiable {
        let id = UUID()
        let question: String
        let answer: String
    }

    struct InfoSection: Identifiable {
        let id = UUID()
        let title: String
        let points: [String]
    }

    static let helpIntro = "Hier findest du Antworten auf häufige Fragen und Tipps zur Nutzung von 2-Stroke Calc."
    static let contactIntro = "Du erreichst uns per E-Mail. Wir melden uns so schnell wie möglich."
    static let bugIntro = "Beschreibe den Fehler möglichst genau – Gerät, App-Version und Schritte zur Reproduktion helfen uns."
    static let termsIntro = "Mit der Nutzung von 2-Stroke Calc akzeptierst du die folgenden Bedingungen."
    static let privacyIntro = "Wir nehmen den Schutz deiner Daten ernst. Nachfolgend findest du eine Übersicht, welche Daten verarbeitet werden."
    static let dataProcessingIntro = "Diese Übersicht beschreibt, welche personenbezogenen Daten verarbeitet werden und zu welchem Zweck."
    static let imprintIntro = "Anbieterkennzeichnung gemäß § 5 TMG."
    static let supportEmail = "support@simplestsoft.com"

    static let faqItems: [FaqItem] = [
        FaqItem(question: "Was berechnet 2-Stroke Calc?", answer: "Die App bietet verschiedene Rechner für typische Berechnungen rund um 2-Takt-Motoren. Alle verfügbaren Module findest du in der Rechner-Übersicht."),
        FaqItem(question: "Werden meine Eingaben gespeichert?", answer: "Rechner-Eingaben werden lokal verarbeitet und nicht dauerhaft gespeichert. Mit einem Konto werden Fahrzeugdaten in der Cloud synchronisiert."),
        FaqItem(question: "Welche Maßeinheiten nutzt die App?", answer: "Die Rechner verwenden feste Einheiten je nach Berechnung – überwiegend metrisch (mm, ccm, °, km/h usw.)."),
        FaqItem(question: "Wie wechsle ich das Menü-Layout?", answer: "Unter Einstellungen → Darstellung → Menütyp kannst du zwischen Bottom-Menü und Seitenpanel wechseln."),
        FaqItem(question: "Was ist die Pro-Version?", answer: "Einige Module sind Teil der Pro-Version. Du kannst sie über den App Store kaufen oder per freigeschaltetem Konto nutzen."),
        FaqItem(question: "Wofür brauche ich ein Konto?", answer: "Ein Konto ermöglicht Cloud-Synchronisation, Pro-Kauf und Profilverwaltung. Rechner kannst du auch ohne Konto nutzen."),
        FaqItem(question: "Wofür sind Push-Benachrichtigungen?", answer: "Sie erinnern dich an fällige Wartungen, TÜV-Termine und ablaufende Versicherungen deiner Fahrzeuge."),
    ]

    static let helpTips: [String] = [
        "Tippe auf einen Rechner in der Übersicht, um die Detailberechnung zu öffnen.",
        "In den Einstellungen kannst du Design, Sprache und Menütyp anpassen.",
        "Mit Pro kannst du Fahrzeuge anlegen, bearbeiten und in der Cloud synchronisieren.",
        "Der Walkthrough in den Einstellungen führt dich durch die wichtigsten Funktionen.",
    ]

    static let changelogSections: [InfoSection] = [
        InfoSection(title: "Version 1.2.6", points: [
            "Vollbild-Landschaftsansicht stabilisiert",
            "Vollbild-Dialoge optimiert",
        ]),
        InfoSection(title: "Version 1.2.5", points: [
            "Neue Sprachen: Spanisch, Italienisch und Portugiesisch",
            "Fahrzeug-Navigation stabilisiert",
        ]),
        InfoSection(title: "Version 1.2.4", points: [
            "Stabilerer App-Start",
            "Walkthrough-Overlay überarbeitet",
            "Fahrzeug-Avatar optimiert",
        ]),
        InfoSection(title: "Version 1.2.3", points: [
            "Fahrzeugdatenbank mit 4000+ Modellen",
            "Neuer Fahrdynamik-Rechner",
            "Neuer Schwungrad-Rechner",
        ]),
        InfoSection(title: "Version 1.1.0", points: [
            "Freemium-Modell mit Demo-Fahrzeug",
            "Pro-Dialog und Walkthrough",
        ]),
        InfoSection(title: "Version 1.0.0", points: ["Erste Veröffentlichung"]),
    ]

    static let termsSections: [InfoSection] = [
        InfoSection(title: "Nutzung", points: [
            "Die App dient als Hilfsmittel für technische Berechnungen. Ergebnisse ersetzen keine fachliche Prüfung.",
            "Die Nutzung erfolgt auf eigene Verantwortung.",
        ]),
        InfoSection(title: "Pro-Version", points: [
            "Der Kauf erfolgt über den App Store. Abwicklung richtet sich nach den Apple-Richtlinien.",
            "Freigeschaltete Käufe bleiben mit deinem Apple-Konto verknüpft.",
        ]),
        InfoSection(title: "Konto", points: [
            "Für Cloud-Synchronisation und Pro-Kauf ist ein Konto erforderlich.",
            "Du kannst dein Konto jederzeit löschen.",
        ]),
    ]

    static let privacySections: [InfoSection] = [
        InfoSection(title: "Lokale Daten", points: [
            "Rechner-Eingaben werden auf dem Gerät verarbeitet.",
            "App-Einstellungen werden lokal gespeichert.",
        ]),
        InfoSection(title: "Cloud-Daten", points: [
            "Mit Konto werden Fahrzeugdaten über Firebase synchronisiert.",
            "Dazu gehören Stammdaten, Wartung und Tankbuch.",
        ]),
        InfoSection(title: "Käufe", points: [
            "Pro-Käufe werden über den App Store abgewickelt.",
        ]),
        InfoSection(title: "Benachrichtigungen", points: [
            "Wartungserinnerungen werden lokal auf dem Gerät geplant.",
        ]),
    ]

    static let dataProcessingSections: [InfoSection] = [
        InfoSection(title: "Kontodaten", points: [
            "E-Mail, Anzeigename und Rolleninformationen für Auth und Sync.",
        ]),
        InfoSection(title: "Fahrzeugdaten", points: [
            "Stammdaten, Wartung, Tankbuch und Kosten bei Cloud-Sync.",
        ]),
        InfoSection(title: "Kontakt", points: [
            "Bei Kontaktanfragen verarbeiten wir die von dir eingegebenen Daten zur Bearbeitung.",
        ]),
    ]

    static let imprintSections: [InfoSection] = [
        InfoSection(title: "Anbieter", points: ["SimplestSoft", SettingsContent.supportEmail]),
        InfoSection(title: "Verantwortlich für den Inhalt", points: [
            "SimplestSoft (Anschrift auf Anfrage über support@simplestsoft.com)",
        ]),
    ]
}
