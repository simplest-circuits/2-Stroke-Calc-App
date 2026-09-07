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

    static var helpIntro: String { L.t("settings_detail_help_intro") }
    static var contactIntro: String { L.t("settings_detail_contact_intro") }
    static var bugIntro: String { L.t("settings_bug_report_info") }
    static var termsIntro: String { L.t("settings_detail_terms_intro") }
    static var privacyIntro: String { L.t("settings_detail_privacy_intro") }
    static var dataProcessingIntro: String { L.t("settings_detail_processing_intro") }
    static var imprintIntro: String { L.t("settings_detail_imprint_intro") }
    static var supportEmail: String { L.t("settings_contact_support_email") }

    static var faqItems: [FaqItem] {
        [
            FaqItem(question: L.t("settings_detail_help_faq_question_1"), answer: L.t("settings_detail_help_faq_answer_1")),
            FaqItem(question: L.t("settings_detail_help_faq_question_2"), answer: L.t("settings_detail_help_faq_answer_2")),
            FaqItem(question: L.t("settings_detail_help_faq_question_3"), answer: L.t("settings_detail_help_faq_answer_3")),
            FaqItem(question: L.t("settings_detail_help_faq_question_4"), answer: L.t("settings_detail_help_faq_answer_4")),
            FaqItem(question: L.t("settings_detail_help_faq_question_5"), answer: L.t("settings_detail_help_faq_answer_5")),
            FaqItem(question: L.t("settings_detail_help_faq_question_6"), answer: L.t("settings_detail_help_faq_answer_6")),
            FaqItem(question: L.t("settings_detail_help_faq_question_7"), answer: L.t("settings_detail_help_faq_answer_7")),
        ]
    }

    static var helpTips: [String] {
        [
            L.t("settings_detail_help_tip_1"),
            L.t("settings_detail_help_tip_2"),
            L.t("settings_detail_help_tip_3"),
            L.t("settings_detail_help_tip_4"),
        ]
    }

    static var changelogSections: [InfoSection] {
        [
            InfoSection(title: L.t("settings_changelog_v16_title"), points: [
                L.t("settings_changelog_v16_item_1"),
                L.t("settings_changelog_v16_item_2"),
            ]),
            InfoSection(title: L.t("settings_changelog_v15_title"), points: [
                L.t("settings_changelog_v15_item_1"),
            ]),
            InfoSection(title: L.t("settings_changelog_v14_title"), points: [
                L.t("settings_changelog_v14_item_1"),
                L.t("settings_changelog_v14_item_2"),
            ]),
            InfoSection(title: L.t("settings_changelog_v13_title"), points: [
                L.t("settings_changelog_v13_item_1"),
                L.t("settings_changelog_v13_item_2"),
            ]),
            InfoSection(title: L.t("settings_changelog_v12_title"), points: [
                L.t("settings_changelog_v12_item_1"),
                L.t("settings_changelog_v12_item_2"),
                L.t("settings_changelog_v12_item_3"),
            ]),
            InfoSection(title: L.t("settings_changelog_v11_title"), points: [
                L.t("settings_changelog_v11_item_1"),
                L.t("settings_changelog_v11_item_2"),
                L.t("settings_changelog_v11_item_3"),
                L.t("settings_changelog_v11_item_4"),
            ]),
            InfoSection(title: L.t("settings_changelog_v10_title"), points: [
                L.t("settings_changelog_v10_item_1"),
            ]),
            InfoSection(title: L.t("settings_changelog_v9_title"), points: [
                L.t("settings_changelog_v9_item_1"),
                L.t("settings_changelog_v9_item_2"),
            ]),
            InfoSection(title: L.t("settings_changelog_v8_title"), points: [
                L.t("settings_changelog_v8_item_1"),
                L.t("settings_changelog_v8_item_4"),
            ]),
            InfoSection(title: L.t("settings_changelog_v7_title"), points: [
                L.t("settings_changelog_v7_item_1"),
                L.t("settings_changelog_v7_item_3"),
                L.t("settings_changelog_v7_item_4"),
            ]),
            InfoSection(title: L.t("settings_changelog_v6_title"), points: [
                L.t("settings_changelog_v6_item_1"),
                L.t("settings_changelog_v6_item_2"),
                L.t("settings_changelog_v6_item_3"),
            ]),
            InfoSection(title: L.t("settings_changelog_v2_title"), points: [
                L.t("settings_changelog_v2_item_1"),
                L.t("settings_changelog_v2_item_2"),
            ]),
            InfoSection(title: L.t("settings_changelog_v1_title"), points: [L.t("settings_changelog_v1_item_1")]),
        ]
    }

    static var termsSections: [InfoSection] {
        [
            InfoSection(title: L.t("settings_detail_terms_usage_title"), points: [
                L.t("settings_detail_terms_usage_item_1"),
                L.t("settings_detail_terms_usage_item_2"),
            ]),
            InfoSection(title: L.t("settings_pro_section"), points: [
                L.t("settings_detail_terms_pro_item_1"),
                L.t("settings_detail_terms_pro_item_2"),
            ]),
            InfoSection(title: L.t("nav_account"), points: [
                L.t("settings_detail_terms_account_item_1"),
                L.t("settings_detail_terms_account_item_2"),
            ]),
        ]
    }

    static var privacySections: [InfoSection] {
        [
            InfoSection(title: L.t("settings_detail_privacy_local_title"), points: [
                L.t("settings_detail_privacy_local_item_1"),
                L.t("settings_detail_privacy_local_item_2"),
            ]),
            InfoSection(title: L.t("settings_detail_privacy_cloud_title"), points: [
                L.t("settings_detail_privacy_cloud_item_1"),
                L.t("settings_detail_privacy_cloud_item_2"),
            ]),
            InfoSection(title: L.t("settings_detail_privacy_purchase_title"), points: [
                L.t("settings_detail_privacy_purchase_item_1"),
            ]),
            InfoSection(title: L.t("settings_detail_privacy_notifications_title"), points: [
                L.t("settings_detail_privacy_notifications_item_1"),
            ]),
        ]
    }

    static var dataProcessingSections: [InfoSection] {
        [
            InfoSection(title: L.t("settings_detail_processing_basis_title"), points: [
                L.t("settings_detail_processing_basis_item_1"),
                L.t("settings_detail_processing_basis_item_2"),
            ]),
            InfoSection(title: L.t("settings_detail_processing_account_title"), points: [
                L.t("settings_detail_processing_account_item_1"),
                L.t("settings_detail_processing_account_item_2"),
            ]),
            InfoSection(title: L.t("settings_detail_processing_contact_title"), points: [
                L.t("settings_detail_processing_contact_item_1"),
                L.t("settings_detail_processing_contact_item_2"),
            ]),
            InfoSection(title: L.t("settings_detail_processing_local_title"), points: [
                L.t("settings_detail_processing_local_item_1"),
            ]),
        ]
    }

    static var imprintSections: [InfoSection] {
        [
            InfoSection(title: L.t("settings_detail_imprint_provider_title"), points: [
                L.t("settings_detail_imprint_provider_item_1"),
                supportEmail,
            ]),
            InfoSection(title: L.t("settings_detail_imprint_responsible_title"), points: [
                L.t("settings_detail_imprint_responsible_item_1"),
            ]),
        ]
    }
}
