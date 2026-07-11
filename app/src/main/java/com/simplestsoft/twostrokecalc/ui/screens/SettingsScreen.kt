package com.simplestsoft.twostrokecalc.ui.screens

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ViewSidebar
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.pm.PackageInfoCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.simplestsoft.twostrokecalc.R
import com.simplestsoft.twostrokecalc.domain.model.LanguageMode
import com.simplestsoft.twostrokecalc.domain.model.NavStyle
import com.simplestsoft.twostrokecalc.domain.model.ThemeMode
import com.simplestsoft.twostrokecalc.review.PlayStoreReview
import com.simplestsoft.twostrokecalc.ui.components.AppColors
import com.simplestsoft.twostrokecalc.ui.components.AppOutlinedTextField
import com.simplestsoft.twostrokecalc.ui.components.FormFieldDefaults
import com.simplestsoft.twostrokecalc.ui.billing.BillingViewModel
import com.simplestsoft.twostrokecalc.ui.util.findComponentActivity
import com.simplestsoft.twostrokecalc.ui.util.keyboardAwareScroll
import com.simplestsoft.twostrokecalc.ui.util.screenSystemBarPadding
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class GeneralContentPage {
    HELP,
    CONTACT,
    TERMS,
    PRIVACY,
    DATA_PROCESSING,
    IMPRINT,
    CHANGELOG,
}

private enum class ContactDetailSubPage {
    GENERAL_MESSAGE,
    BUG_REPORT,
}

private const val VISIBLE_CHANGELOG_VERSION_COUNT = 5

private enum class BugReportCategory(val labelRes: Int) {
    CRASH(R.string.settings_bug_report_cat_crash),
    FEATURE(R.string.settings_bug_report_cat_feature),
    UI(R.string.settings_bug_report_cat_ui),
    CALCULATION(R.string.settings_bug_report_cat_calculation),
    OTHER(R.string.settings_bug_report_cat_other),
}

private data class SettingsContentText(
    val helpSectionTitle: String,
    val generalSectionTitle: String,
    val helpBack: String,
    val generalBack: String,
    val pages: Map<GeneralContentPage, SettingsPageText>,
)

private data class SettingsPageText(
    val title: String,
    val subtitle: String,
    val intro: String,
    val faqTitle: String = "",
    val faqs: List<SettingsFaqText> = emptyList(),
    val tipsTitle: String = "",
    val tips: List<String> = emptyList(),
    val infoSections: List<SettingsInfoSectionText> = emptyList(),
    val contactForm: SettingsContactFormText? = null,
)

private data class SettingsFaqText(val question: String, val answer: String)

private data class SettingsInfoSectionText(val title: String, val points: List<String>)

private data class SettingsContactFormText(
    val title: String,
    val nameLabel: String,
    val emailLabel: String,
    val messageLabel: String,
    val sendLabel: String,
    val missingMessage: String,
    val noEmailAppMessage: String,
    val chooserTitle: String,
    val emailSubject: String,
    val supportEmail: String,
)

private data class SettingsMailCooldownState(
    val remainingMs: Long,
    val refresh: suspend () -> Unit,
)

@Composable
fun SettingsScreen(
    themeMode: ThemeMode,
    languageMode: LanguageMode,
    navStyle: NavStyle,
    isProUser: Boolean = false,
    isAdmin: Boolean = false,
    isAuthenticated: Boolean = false,
    onTheme: (ThemeMode) -> Unit,
    onLanguageMode: (LanguageMode) -> Unit,
    onNavStyle: (NavStyle) -> Unit,
    onAccount: () -> Unit = {},
    getSettingsMailCooldownRemainingMs: suspend () -> Long = { 0L },
    recordSettingsMailSubmit: suspend () -> Unit = {},
    appearanceHighlightModifier: Modifier = Modifier,
    proSectionHighlightModifier: Modifier = Modifier,
    billingViewModel: BillingViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val activity = context.findComponentActivity()
    val billingState by billingViewModel.state.collectAsStateWithLifecycle()
    var pushNotificationsActive by remember {
        mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled())
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        pushNotificationsActive = granted || NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
    LifecycleResumeEffect(Unit) {
        pushNotificationsActive = NotificationManagerCompat.from(context).areNotificationsEnabled()
        onPauseOrDispose { }
    }

    val settingsContent = defaultSettingsContentText()
    var themeDialog by remember { mutableStateOf(false) }
    var langDialog by remember { mutableStateOf(false) }
    var navDialog by remember { mutableStateOf(false) }
    var openedGeneralPage by rememberSaveable { mutableStateOf<GeneralContentPage?>(null) }
    var contactDetailSubPage by rememberSaveable { mutableStateOf<ContactDetailSubPage?>(null) }
    LaunchedEffect(openedGeneralPage) {
        if (openedGeneralPage != GeneralContentPage.CONTACT) {
            contactDetailSubPage = null
        }
    }

    BackHandler(
        enabled = themeDialog || langDialog || navDialog ||
            contactDetailSubPage != null || openedGeneralPage != null,
    ) {
        when {
            themeDialog -> themeDialog = false
            langDialog -> langDialog = false
            navDialog -> navDialog = false
            contactDetailSubPage != null -> contactDetailSubPage = null
            openedGeneralPage != null -> openedGeneralPage = null
        }
    }

    LaunchedEffect(billingState.errorMessage) {
        val errorKey = billingState.errorMessage ?: return@LaunchedEffect
        val message = when (errorKey) {
            "sign_in_required" -> context.getString(R.string.settings_pro_sign_in_required)
            "billing_unavailable" -> context.getString(R.string.settings_pro_billing_unavailable)
            "product_unavailable" -> context.getString(R.string.settings_pro_product_unavailable)
            "verification_failed" -> context.getString(R.string.settings_pro_verification_failed)
            else -> errorKey
        }
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        billingViewModel.clearError()
    }

    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .screenSystemBarPadding()
            .background(AppColors.background())
            .keyboardAwareScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val selectedGeneralPage = openedGeneralPage
        SettingsHeader(isProUser = isProUser || isAdmin)

        if (selectedGeneralPage == null) {
            SettingsSectionCard(
                title = stringResource(R.string.settings_account_section),
                options = {
                    SettingsOptionRow(
                        icon = Icons.Default.AccountCircle,
                        title = stringResource(R.string.nav_account),
                        subtitle = stringResource(R.string.settings_account_subtitle),
                        onClick = onAccount,
                    )
                },
            )

            if (!isProUser && !isAdmin) {
                SettingsSectionCard(
                    title = stringResource(R.string.settings_pro_section),
                    modifier = proSectionHighlightModifier,
                    options = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.settings_pro_section_hint),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Button(
                                onClick = {
                                    if (!isAuthenticated) {
                                        onAccount()
                                    } else {
                                        activity?.let { billingViewModel.launchProPurchase(it) }
                                    }
                                },
                                enabled = !billingState.isPurchasing,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                if (billingState.isPurchasing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                    )
                                } else {
                                    val price = billingState.priceFormatted
                                    Text(
                                        if (price != null) {
                                            stringResource(R.string.settings_buy_pro_with_price, price)
                                        } else {
                                            stringResource(R.string.settings_buy_pro)
                                        },
                                    )
                                }
                            }
                            TextButton(
                                onClick = {
                                    if (!isAuthenticated) {
                                        onAccount()
                                    } else {
                                        billingViewModel.restorePurchases()
                                    }
                                },
                                enabled = !billingState.isPurchasing,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(stringResource(R.string.settings_restore_pro))
                            }
                        }
                    },
                )
            }

            SettingsSectionCard(
                title = stringResource(R.string.appearance_section),
                modifier = appearanceHighlightModifier,
                options = {
                    SettingsOptionRow(
                        icon = Icons.Default.Palette,
                        title = stringResource(R.string.theme_label),
                        subtitle = themeLabel(themeMode),
                        onClick = { themeDialog = true },
                    )
                    SettingsOptionRow(
                        icon = Icons.Default.Translate,
                        title = stringResource(R.string.language_label),
                        subtitle = languageModeLabel(languageMode),
                        onClick = { langDialog = true },
                    )
                    SettingsOptionRow(
                        icon = Icons.AutoMirrored.Filled.ViewSidebar,
                        title = stringResource(R.string.menu_type_label),
                        subtitle = navStyleLabel(navStyle),
                        onClick = { navDialog = true },
                    )
                },
            )

            SettingsSectionCard(
                title = stringResource(R.string.settings_permissions_section),
                options = {
                    SettingsOptionRow(
                        icon = Icons.Default.Notifications,
                        title = stringResource(R.string.settings_notifications_title),
                        subtitle = if (pushNotificationsActive) {
                            stringResource(R.string.settings_notifications_subtitle_granted)
                        } else {
                            stringResource(R.string.settings_notifications_subtitle_denied)
                        },
                        onClick = {
                            if (pushNotificationsActive) {
                                activity?.startActivity(
                                    Intent(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.fromParts("package", activity.packageName, null),
                                    ),
                                )
                            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionLauncher.launch(
                                    android.Manifest.permission.POST_NOTIFICATIONS,
                                )
                            } else {
                                activity?.startActivity(
                                    Intent(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.fromParts("package", activity?.packageName, null),
                                    ),
                                )
                            }
                        },
                    )
                },
            )

            SettingsSectionCard(
                title = settingsContent.helpSectionTitle,
                options = {
                    val helpText = settingsContent.page(GeneralContentPage.HELP)
                    SettingsOptionRow(
                        icon = Icons.AutoMirrored.Filled.Help,
                        title = helpText.title,
                        subtitle = helpText.subtitle,
                        onClick = { openedGeneralPage = GeneralContentPage.HELP },
                    )
                },
            )

            SettingsSectionCard(
                title = stringResource(R.string.settings_app_section),
                options = {
                    SettingsOptionRow(
                        icon = Icons.Default.Star,
                        title = stringResource(R.string.settings_rate_title),
                        subtitle = stringResource(R.string.settings_rate_subtitle),
                        onClick = { PlayStoreReview.openStoreListing(context) },
                    )
                },
            )

            SettingsSectionCard(
                title = settingsContent.generalSectionTitle,
                options = {
                    SettingsOptionRow(
                        icon = Icons.Default.Email,
                        title = settingsContent.page(GeneralContentPage.CONTACT).title,
                        subtitle = settingsContent.page(GeneralContentPage.CONTACT).subtitle,
                        onClick = { openedGeneralPage = GeneralContentPage.CONTACT },
                    )
                    SettingsOptionRow(
                        icon = Icons.AutoMirrored.Filled.Article,
                        title = settingsContent.page(GeneralContentPage.TERMS).title,
                        subtitle = settingsContent.page(GeneralContentPage.TERMS).subtitle,
                        onClick = { openedGeneralPage = GeneralContentPage.TERMS },
                    )
                    SettingsOptionRow(
                        icon = Icons.Default.Policy,
                        title = settingsContent.page(GeneralContentPage.PRIVACY).title,
                        subtitle = settingsContent.page(GeneralContentPage.PRIVACY).subtitle,
                        onClick = { openedGeneralPage = GeneralContentPage.PRIVACY },
                    )
                    SettingsOptionRow(
                        icon = Icons.Default.Gavel,
                        title = settingsContent.page(GeneralContentPage.DATA_PROCESSING).title,
                        subtitle = settingsContent.page(GeneralContentPage.DATA_PROCESSING).subtitle,
                        onClick = { openedGeneralPage = GeneralContentPage.DATA_PROCESSING },
                    )
                    SettingsOptionRow(
                        icon = Icons.Default.Description,
                        title = settingsContent.page(GeneralContentPage.IMPRINT).title,
                        subtitle = settingsContent.page(GeneralContentPage.IMPRINT).subtitle,
                        onClick = { openedGeneralPage = GeneralContentPage.IMPRINT },
                    )
                    SettingsOptionRow(
                        icon = Icons.Default.History,
                        title = settingsContent.page(GeneralContentPage.CHANGELOG).title,
                        subtitle = settingsContent.page(GeneralContentPage.CHANGELOG).subtitle,
                        onClick = { openedGeneralPage = GeneralContentPage.CHANGELOG },
                    )
                },
            )
        } else {
            GeneralContentDetail(
                page = selectedGeneralPage,
                content = settingsContent,
                onBack = { openedGeneralPage = null },
                contactSubPage = contactDetailSubPage,
                onContactSubPage = { contactDetailSubPage = it },
                getSettingsMailCooldownRemainingMs = getSettingsMailCooldownRemainingMs,
                recordSettingsMailSubmit = recordSettingsMailSubmit,
            )
        }

        if (openedGeneralPage == null) {
            SettingsVersionFooter()
        }
    }

    if (themeDialog) {
        SelectionDialog(
            title = stringResource(R.string.theme_dialog_title),
            onDismiss = { themeDialog = false },
        ) {
            ThemeMode.entries.forEach { mode ->
                DialogOptionRow(
                    text = themeLabel(mode),
                    selected = mode == themeMode,
                    onClick = {
                        onTheme(mode)
                        themeDialog = false
                    },
                )
            }
        }
    }
    if (langDialog) {
        SelectionDialog(
            title = stringResource(R.string.language_dialog_title),
            onDismiss = { langDialog = false },
        ) {
            LanguageMode.entries.forEach { mode ->
                DialogOptionRow(
                    text = languageModeLabel(mode),
                    selected = mode == languageMode,
                    onClick = {
                        onLanguageMode(mode)
                        langDialog = false
                    },
                )
            }
        }
    }
    if (navDialog) {
        SelectionDialog(
            title = stringResource(R.string.menu_type_label),
            onDismiss = { navDialog = false },
        ) {
            DialogOptionRow(
                text = stringResource(R.string.nav_style_bottom),
                selected = navStyle == NavStyle.BOTTOM_BAR,
                onClick = {
                    onNavStyle(NavStyle.BOTTOM_BAR)
                    navDialog = false
                },
            )
            DialogOptionRow(
                text = stringResource(R.string.nav_style_side_panel),
                selected = navStyle == NavStyle.DRAWER,
                onClick = {
                    onNavStyle(NavStyle.DRAWER)
                    navDialog = false
                },
            )
        }
    }
}

@Composable
private fun SelectionDialog(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Column(content = content) },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun helpFaqEntries(): List<SettingsFaqText> {
    val faqPairs = listOf(
        R.string.settings_detail_help_faq_question_1 to R.string.settings_detail_help_faq_answer_1,
        R.string.settings_detail_help_faq_question_2 to R.string.settings_detail_help_faq_answer_2,
        R.string.settings_detail_help_faq_question_3 to R.string.settings_detail_help_faq_answer_3,
        R.string.settings_detail_help_faq_question_4 to R.string.settings_detail_help_faq_answer_4,
        R.string.settings_detail_help_faq_question_5 to R.string.settings_detail_help_faq_answer_5,
        R.string.settings_detail_help_faq_question_6 to R.string.settings_detail_help_faq_answer_6,
        R.string.settings_detail_help_faq_question_7 to R.string.settings_detail_help_faq_answer_7,
    )
    return faqPairs.map { (q, a) ->
        SettingsFaqText(stringResource(q), stringResource(a))
    }
}

@Composable
private fun helpTipEntries(): List<String> = listOf(
    R.string.settings_detail_help_tip_1,
    R.string.settings_detail_help_tip_2,
    R.string.settings_detail_help_tip_3,
    R.string.settings_detail_help_tip_4,
).map { stringResource(it) }

@Composable
private fun changelogSections(): List<SettingsInfoSectionText> = listOf(
    SettingsInfoSectionText(
        title = stringResource(R.string.settings_changelog_v6_title),
        points = listOf(
            stringResource(R.string.settings_changelog_v6_item_1),
            stringResource(R.string.settings_changelog_v6_item_2),
            stringResource(R.string.settings_changelog_v6_item_3),
            stringResource(R.string.settings_changelog_v6_item_4),
        ),
    ),
    SettingsInfoSectionText(
        title = stringResource(R.string.settings_changelog_v5_title),
        points = listOf(
            stringResource(R.string.settings_changelog_v5_item_1),
            stringResource(R.string.settings_changelog_v5_item_2),
        ),
    ),
    SettingsInfoSectionText(
        title = stringResource(R.string.settings_changelog_v4_title),
        points = listOf(
            stringResource(R.string.settings_changelog_v4_item_1),
        ),
    ),
    SettingsInfoSectionText(
        title = stringResource(R.string.settings_changelog_v3_title),
        points = listOf(
            stringResource(R.string.settings_changelog_v3_item_1),
            stringResource(R.string.settings_changelog_v3_item_2),
        ),
    ),
    SettingsInfoSectionText(
        title = stringResource(R.string.settings_changelog_v2_title),
        points = listOf(
            stringResource(R.string.settings_changelog_v2_item_1),
            stringResource(R.string.settings_changelog_v2_item_2),
            stringResource(R.string.settings_changelog_v2_item_3),
            stringResource(R.string.settings_changelog_v2_item_4),
            stringResource(R.string.settings_changelog_v2_item_5),
        ),
    ),
    SettingsInfoSectionText(
        title = stringResource(R.string.settings_changelog_v1_title),
        points = listOf(
            stringResource(R.string.settings_changelog_v1_item_1),
        ),
    ),
)

@Composable
private fun defaultSettingsContentText(): SettingsContentText = SettingsContentText(
    helpSectionTitle = stringResource(R.string.settings_help_section),
    generalSectionTitle = stringResource(R.string.settings_general_section),
    helpBack = stringResource(R.string.settings_help_back),
    generalBack = stringResource(R.string.settings_general_back),
    pages = mapOf(
        GeneralContentPage.HELP to SettingsPageText(
            title = stringResource(R.string.settings_help_title),
            subtitle = stringResource(R.string.settings_help_subtitle),
            intro = stringResource(R.string.settings_detail_help_intro),
            faqTitle = stringResource(R.string.settings_detail_help_faq_title),
            faqs = helpFaqEntries(),
            tipsTitle = stringResource(R.string.settings_detail_help_tips_title),
            tips = helpTipEntries(),
        ),
        GeneralContentPage.CONTACT to SettingsPageText(
            title = stringResource(R.string.settings_contact_title),
            subtitle = stringResource(R.string.settings_contact_subtitle),
            intro = stringResource(R.string.settings_detail_contact_intro),
            contactForm = SettingsContactFormText(
                title = stringResource(R.string.settings_detail_contact_form_title),
                nameLabel = stringResource(R.string.settings_contact_name_label),
                emailLabel = stringResource(R.string.settings_contact_email_label),
                messageLabel = stringResource(R.string.settings_contact_message_label),
                sendLabel = stringResource(R.string.settings_contact_send),
                missingMessage = stringResource(R.string.settings_contact_missing_message),
                noEmailAppMessage = stringResource(R.string.settings_contact_no_email_app),
                chooserTitle = stringResource(R.string.settings_contact_chooser_title),
                emailSubject = stringResource(R.string.settings_contact_email_subject),
                supportEmail = stringResource(R.string.settings_contact_support_email),
            ),
        ),
        GeneralContentPage.TERMS to SettingsPageText(
            title = stringResource(R.string.settings_terms_title),
            subtitle = stringResource(R.string.settings_terms_subtitle),
            intro = stringResource(R.string.settings_detail_terms_intro),
            infoSections = listOf(
                SettingsInfoSectionText(
                    title = stringResource(R.string.settings_detail_terms_usage_title),
                    points = listOf(
                        stringResource(R.string.settings_detail_terms_usage_item_1),
                        stringResource(R.string.settings_detail_terms_usage_item_2),
                    ),
                ),
                SettingsInfoSectionText(
                    title = stringResource(R.string.settings_detail_terms_pro_title),
                    points = listOf(
                        stringResource(R.string.settings_detail_terms_pro_item_1),
                        stringResource(R.string.settings_detail_terms_pro_item_2),
                    ),
                ),
                SettingsInfoSectionText(
                    title = stringResource(R.string.settings_detail_terms_account_title),
                    points = listOf(
                        stringResource(R.string.settings_detail_terms_account_item_1),
                        stringResource(R.string.settings_detail_terms_account_item_2),
                    ),
                ),
            ),
        ),
        GeneralContentPage.PRIVACY to SettingsPageText(
            title = stringResource(R.string.settings_privacy_title),
            subtitle = stringResource(R.string.settings_privacy_subtitle),
            intro = stringResource(R.string.settings_detail_privacy_intro),
            infoSections = listOf(
                SettingsInfoSectionText(
                    title = stringResource(R.string.settings_detail_privacy_local_title),
                    points = listOf(
                        stringResource(R.string.settings_detail_privacy_local_item_1),
                        stringResource(R.string.settings_detail_privacy_local_item_2),
                    ),
                ),
                SettingsInfoSectionText(
                    title = stringResource(R.string.settings_detail_privacy_cloud_title),
                    points = listOf(
                        stringResource(R.string.settings_detail_privacy_cloud_item_1),
                        stringResource(R.string.settings_detail_privacy_cloud_item_2),
                    ),
                ),
                SettingsInfoSectionText(
                    title = stringResource(R.string.settings_detail_privacy_purchase_title),
                    points = listOf(
                        stringResource(R.string.settings_detail_privacy_purchase_item_1),
                    ),
                ),
                SettingsInfoSectionText(
                    title = stringResource(R.string.settings_detail_privacy_notifications_title),
                    points = listOf(
                        stringResource(R.string.settings_detail_privacy_notifications_item_1),
                    ),
                ),
            ),
        ),
        GeneralContentPage.DATA_PROCESSING to SettingsPageText(
            title = stringResource(R.string.settings_data_processing_title),
            subtitle = stringResource(R.string.settings_data_processing_subtitle),
            intro = stringResource(R.string.settings_detail_processing_intro),
            infoSections = listOf(
                SettingsInfoSectionText(
                    title = stringResource(R.string.settings_detail_processing_basis_title),
                    points = listOf(
                        stringResource(R.string.settings_detail_processing_basis_item_1),
                        stringResource(R.string.settings_detail_processing_basis_item_2),
                    ),
                ),
                SettingsInfoSectionText(
                    title = stringResource(R.string.settings_detail_processing_account_title),
                    points = listOf(
                        stringResource(R.string.settings_detail_processing_account_item_1),
                        stringResource(R.string.settings_detail_processing_account_item_2),
                    ),
                ),
                SettingsInfoSectionText(
                    title = stringResource(R.string.settings_detail_processing_contact_title),
                    points = listOf(
                        stringResource(R.string.settings_detail_processing_contact_item_1),
                        stringResource(R.string.settings_detail_processing_contact_item_2),
                    ),
                ),
                SettingsInfoSectionText(
                    title = stringResource(R.string.settings_detail_processing_local_title),
                    points = listOf(
                        stringResource(R.string.settings_detail_processing_local_item_1),
                    ),
                ),
            ),
        ),
        GeneralContentPage.IMPRINT to SettingsPageText(
            title = stringResource(R.string.settings_imprint_title),
            subtitle = stringResource(R.string.settings_imprint_subtitle),
            intro = stringResource(R.string.settings_detail_imprint_intro),
            infoSections = listOf(
                SettingsInfoSectionText(
                    title = stringResource(R.string.settings_detail_imprint_provider_title),
                    points = listOf(
                        stringResource(R.string.settings_detail_imprint_provider_item_1),
                        stringResource(R.string.settings_detail_imprint_provider_item_2),
                    ),
                ),
                SettingsInfoSectionText(
                    title = stringResource(R.string.settings_detail_imprint_responsible_title),
                    points = listOf(
                        stringResource(R.string.settings_detail_imprint_responsible_item_1),
                    ),
                ),
            ),
        ),
        GeneralContentPage.CHANGELOG to SettingsPageText(
            title = stringResource(R.string.settings_changelog_title),
            subtitle = stringResource(R.string.settings_changelog_subtitle),
            intro = stringResource(R.string.settings_detail_changelog_intro),
            infoSections = changelogSections(),
        ),
    ),
)

private fun SettingsContentText.page(page: GeneralContentPage): SettingsPageText =
    pages.getValue(page)

private fun settingsMailCooldownToastSeconds(remainingMs: Long): Int =
    ((remainingMs + 999) / 1000).toInt().coerceAtLeast(1)

@Composable
private fun rememberSettingsMailCooldown(
    getRemainingMs: suspend () -> Long,
): SettingsMailCooldownState {
    var remainingMs by remember { mutableLongStateOf(0L) }
    val refresh: suspend () -> Unit = { remainingMs = getRemainingMs() }
    LaunchedEffect(Unit) { refresh() }
    LaunchedEffect(remainingMs) {
        if (remainingMs <= 0L) return@LaunchedEffect
        delay(minOf(1000L, remainingMs))
        refresh()
    }
    return SettingsMailCooldownState(remainingMs = remainingMs, refresh = refresh)
}

@Composable
private fun GeneralContentDetail(
    page: GeneralContentPage,
    content: SettingsContentText,
    onBack: () -> Unit,
    contactSubPage: ContactDetailSubPage? = null,
    onContactSubPage: (ContactDetailSubPage?) -> Unit = {},
    getSettingsMailCooldownRemainingMs: suspend () -> Long = { 0L },
    recordSettingsMailSubmit: suspend () -> Unit = {},
) {
    val pageText = content.page(page)
    val pageIcon = when (page) {
        GeneralContentPage.HELP -> Icons.AutoMirrored.Filled.Help
        GeneralContentPage.CONTACT -> Icons.Default.Email
        GeneralContentPage.TERMS -> Icons.AutoMirrored.Filled.Article
        GeneralContentPage.PRIVACY -> Icons.Default.Policy
        GeneralContentPage.DATA_PROCESSING -> Icons.Default.Gavel
        GeneralContentPage.IMPRINT -> Icons.Default.Description
        GeneralContentPage.CHANGELOG -> Icons.Default.History
    }
    val atContactSubPage = page == GeneralContentPage.CONTACT && contactSubPage != null

    TextButton(onClick = {
        if (atContactSubPage) onContactSubPage(null) else onBack()
    }) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
        Text(
            text = when {
                page == GeneralContentPage.HELP -> content.helpBack
                atContactSubPage -> stringResource(R.string.settings_contact_back_to_contact)
                else -> content.generalBack
            },
            modifier = Modifier.padding(start = 4.dp),
        )
    }

    if (!(page == GeneralContentPage.CONTACT && contactSubPage != null)) {
        SettingsSectionCard(
            title = pageText.title,
            options = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        modifier = Modifier.size(44.dp),
                        shape = CircleShape,
                        color = AppColors.settingsIconBackground(),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(pageIcon, contentDescription = null, tint = AppColors.iconContainerForeground())
                        }
                    }
                    Text(
                        text = pageText.intro,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
        )
    }

    when (page) {
        GeneralContentPage.HELP -> {
            if (pageText.faqTitle.isNotBlank() && pageText.faqs.isNotEmpty()) {
                HelpFaqCard(pageText.faqTitle, pageText.faqs.map { it.question to it.answer })
            }
            if (pageText.tipsTitle.isNotBlank() && pageText.tips.isNotEmpty()) {
                GeneralInfoCard(pageText.tipsTitle, Icons.Default.Lightbulb, pageText.tips)
            }
        }
        GeneralContentPage.CONTACT -> when (contactSubPage) {
            null -> SettingsSectionCard(
                title = stringResource(R.string.settings_contact_hub_section_title),
                options = {
                    SettingsOptionRow(
                        icon = Icons.Default.Email,
                        title = stringResource(R.string.settings_contact_hub_general_title),
                        subtitle = stringResource(R.string.settings_contact_hub_general_subtitle),
                        onClick = { onContactSubPage(ContactDetailSubPage.GENERAL_MESSAGE) },
                    )
                    SettingsOptionRow(
                        icon = Icons.Default.BugReport,
                        title = stringResource(R.string.settings_contact_hub_bug_title),
                        subtitle = stringResource(R.string.settings_contact_hub_bug_subtitle),
                        onClick = { onContactSubPage(ContactDetailSubPage.BUG_REPORT) },
                    )
                },
            )
            ContactDetailSubPage.GENERAL_MESSAGE -> pageText.contactForm?.let {
                ContactFormCard(it, getSettingsMailCooldownRemainingMs, recordSettingsMailSubmit)
            }
            ContactDetailSubPage.BUG_REPORT -> BugReportFormCard(
                getSettingsMailCooldownRemainingMs,
                recordSettingsMailSubmit,
            )
        }
        GeneralContentPage.TERMS,
        GeneralContentPage.PRIVACY,
        GeneralContentPage.DATA_PROCESSING,
        GeneralContentPage.IMPRINT,
        -> pageText.infoSections.forEach { section ->
            if (section.title.isNotBlank() && section.points.isNotEmpty()) {
                GeneralInfoCard(section.title, points = section.points)
            }
        }
        GeneralContentPage.CHANGELOG -> ChangelogSections(pageText.infoSections)
    }
}

@Composable
private fun ChangelogSections(sections: List<SettingsInfoSectionText>) {
    var olderVersionsExpanded by rememberSaveable { mutableStateOf(false) }
    val validSections = sections.filter { it.title.isNotBlank() && it.points.isNotEmpty() }
    val visibleSections = validSections.take(VISIBLE_CHANGELOG_VERSION_COUNT)
    val hiddenSections = validSections.drop(VISIBLE_CHANGELOG_VERSION_COUNT)
    visibleSections.forEach { GeneralInfoCard(it.title, points = it.points) }
    if (hiddenSections.isNotEmpty()) {
        if (olderVersionsExpanded) hiddenSections.forEach { GeneralInfoCard(it.title, points = it.points) }
        TextButton(
            onClick = { olderVersionsExpanded = !olderVersionsExpanded },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                if (olderVersionsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
            )
            Text(
                text = if (olderVersionsExpanded) {
                    stringResource(R.string.settings_changelog_show_less)
                } else {
                    stringResource(R.string.settings_changelog_show_older, hiddenSections.size)
                },
                modifier = Modifier.padding(start = 6.dp),
            )
        }
    }
}

@Composable
private fun GeneralInfoCard(
    title: String,
    icon: ImageVector? = null,
    points: List<String>,
) {
    val cardShape = RoundedCornerShape(6.dp)
    Card(
        modifier = AppColors.cardBorderModifier(cardShape, Modifier.fillMaxWidth()),
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (AppColors.isDarkTheme()) 0.dp else 1.dp,
        ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                icon?.let {
                    Icon(it, contentDescription = null, tint = AppColors.primaryBlue())
                }
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            }
            points.forEach { point ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                    Surface(
                        modifier = Modifier.padding(top = 7.dp).size(7.dp),
                        shape = CircleShape,
                        color = AppColors.primaryBlue(),
                    ) {}
                    Text(
                        point,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun HelpFaqCard(title: String, items: List<Pair<String, String>>) {
    val cardShape = RoundedCornerShape(6.dp)
    Card(
        modifier = AppColors.cardBorderModifier(cardShape, Modifier.fillMaxWidth()),
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (AppColors.isDarkTheme()) 0.dp else 1.dp,
        ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            items.forEach { (question, answer) ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(question, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    Text(answer, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

private fun getAppVersionName(context: Context): String {
    val pm = context.packageManager
    val pkg = context.packageName
    val pi = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        pm.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(0L))
    } else {
        @Suppress("DEPRECATION")
        pm.getPackageInfo(pkg, 0)
    }
    return pi.versionName ?: "?"
}

private fun hasEmailClient(context: Context): Boolean {
    val mailtoProbe = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"))
    if (mailtoProbe.resolveActivity(context.packageManager) != null) return true
    return Intent(Intent.ACTION_SEND).apply { type = "message/rfc822" }
        .resolveActivity(context.packageManager) != null
}

private fun createSettingsEmailChooserIntent(
    recipient: String,
    subject: String,
    body: String,
    chooserTitle: String,
): Intent {
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "message/rfc822"
        putExtra(Intent.EXTRA_EMAIL, arrayOf(recipient))
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, body)
    }
    return Intent.createChooser(sendIntent, chooserTitle)
}

private fun collectBugReportDeviceInfoLines(context: Context): List<String> {
    val pm = context.packageManager
    val pkg = context.packageName
    val pi = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        pm.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(0L))
    } else {
        @Suppress("DEPRECATION")
        pm.getPackageInfo(pkg, 0)
    }
    val versionName = pi.versionName ?: "?"
    val versionCode = PackageInfoCompat.getLongVersionCode(pi).toString()
    val displayMetrics = context.resources.displayMetrics
    return listOf(
        context.getString(R.string.settings_bug_device_model_line, Build.MODEL),
        context.getString(R.string.settings_bug_device_android_line, Build.VERSION.RELEASE, Build.VERSION.SDK_INT),
        context.getString(R.string.settings_bug_device_app_line, versionName, versionCode),
        context.getString(
            R.string.settings_bug_device_screen_line,
            displayMetrics.widthPixels,
            displayMetrics.heightPixels,
            displayMetrics.densityDpi,
        ),
    )
}

@Composable
private fun BugReportFormCard(
    getSettingsMailCooldownRemainingMs: suspend () -> Long,
    recordSettingsMailSubmit: suspend () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val cooldown = rememberSettingsMailCooldown(getSettingsMailCooldownRemainingMs)
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var category by remember { mutableStateOf(BugReportCategory.CRASH) }
    val supportEmail = stringResource(R.string.settings_contact_support_email)
    val emailSubject = stringResource(R.string.settings_bug_report_email_subject)
    val chooserTitle = stringResource(R.string.settings_contact_chooser_title)
    val noEmailAppMessage = stringResource(R.string.settings_contact_no_email_app)
    val cardShape = RoundedCornerShape(6.dp)

    Card(
        modifier = AppColors.cardBorderModifier(cardShape, Modifier.fillMaxWidth()),
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = if (AppColors.isDarkTheme()) 0.dp else 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(FormFieldDefaults.fieldSpacing),
        ) {
            Text(stringResource(R.string.settings_bug_report_form_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(stringResource(R.string.settings_bug_report_info), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            BugReportCategory.entries.forEach { option ->
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { category = option },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = category == option, onClick = { category = option })
                    Text(stringResource(option.labelRes), modifier = Modifier.padding(start = 4.dp))
                }
            }
            AppOutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = stringResource(R.string.settings_bug_report_name_label),
            )
            AppOutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = stringResource(R.string.settings_bug_report_email_label),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            )
            AppOutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = stringResource(R.string.settings_bug_report_description_label),
                singleLine = false,
                minLines = 5,
                maxLines = 8,
                compactHeight = false,
            )
            GeneralInfoCard(
                title = stringResource(R.string.settings_bug_report_device_heading),
                points = buildList {
                    add(stringResource(R.string.settings_bug_report_device_hint))
                    addAll(collectBugReportDeviceInfoLines(context))
                },
            )
            if (cooldown.remainingMs > 0L) {
                Text(
                    stringResource(R.string.settings_mail_cooldown_hint, settingsMailCooldownToastSeconds(cooldown.remainingMs)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(
                onClick = {
                    if (description.isBlank()) {
                        Toast.makeText(context, R.string.settings_bug_report_missing_description, Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    scope.launch {
                        val left = getSettingsMailCooldownRemainingMs()
                        if (left > 0L) {
                            Toast.makeText(context, context.getString(R.string.settings_mail_cooldown_toast, settingsMailCooldownToastSeconds(left)), Toast.LENGTH_SHORT).show()
                            return@launch
                        }
                        val body = buildString {
                            appendLine(context.getString(R.string.settings_bug_report_body_category, context.getString(category.labelRes)))
                            if (name.isNotBlank()) appendLine(context.getString(R.string.settings_bug_report_body_name_line, name.trim()))
                            if (email.isNotBlank()) appendLine(context.getString(R.string.settings_bug_report_body_email_line, email.trim()))
                            appendLine()
                            appendLine(description.trim())
                            appendLine()
                            collectBugReportDeviceInfoLines(context).forEach { appendLine(it) }
                        }
                        if (!hasEmailClient(context)) {
                            Toast.makeText(context, noEmailAppMessage, Toast.LENGTH_SHORT).show()
                            return@launch
                        }
                        runCatching {
                            context.startActivity(createSettingsEmailChooserIntent(supportEmail, emailSubject, body, chooserTitle))
                        }.onSuccess {
                            recordSettingsMailSubmit()
                            cooldown.refresh()
                        }.onFailure {
                            Toast.makeText(context, noEmailAppMessage, Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                enabled = cooldown.remainingMs <= 0L,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.settings_bug_report_send))
            }
        }
    }
}

@Composable
private fun ContactFormCard(
    text: SettingsContactFormText,
    getSettingsMailCooldownRemainingMs: suspend () -> Long,
    recordSettingsMailSubmit: suspend () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val cooldown = rememberSettingsMailCooldown(getSettingsMailCooldownRemainingMs)
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf("") }
    val cardShape = RoundedCornerShape(6.dp)

    Card(
        modifier = AppColors.cardBorderModifier(cardShape, Modifier.fillMaxWidth()),
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = if (AppColors.isDarkTheme()) 0.dp else 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(FormFieldDefaults.fieldSpacing),
        ) {
            Text(text.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            AppOutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = text.nameLabel,
            )
            AppOutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = text.emailLabel,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            )
            AppOutlinedTextField(
                value = message,
                onValueChange = { message = it },
                label = text.messageLabel,
                singleLine = false,
                minLines = 4,
                maxLines = 8,
                compactHeight = false,
            )
            if (cooldown.remainingMs > 0L) {
                Text(
                    stringResource(R.string.settings_mail_cooldown_hint, settingsMailCooldownToastSeconds(cooldown.remainingMs)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(
                onClick = {
                    if (message.isBlank()) {
                        Toast.makeText(context, text.missingMessage, Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    scope.launch {
                        val left = getSettingsMailCooldownRemainingMs()
                        if (left > 0L) {
                            Toast.makeText(context, context.getString(R.string.settings_mail_cooldown_toast, settingsMailCooldownToastSeconds(left)), Toast.LENGTH_SHORT).show()
                            return@launch
                        }
                        val body = buildString {
                            if (name.isNotBlank()) appendLine("Name: ${name.trim()}")
                            if (email.isNotBlank()) appendLine("E-Mail: ${email.trim()}")
                            if (name.isNotBlank() || email.isNotBlank()) appendLine()
                            append(message.trim())
                        }
                        if (!hasEmailClient(context)) {
                            Toast.makeText(context, text.noEmailAppMessage, Toast.LENGTH_SHORT).show()
                            return@launch
                        }
                        runCatching {
                            context.startActivity(createSettingsEmailChooserIntent(text.supportEmail, text.emailSubject, body, text.chooserTitle))
                        }.onSuccess {
                            recordSettingsMailSubmit()
                            cooldown.refresh()
                        }.onFailure {
                            Toast.makeText(context, text.noEmailAppMessage, Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                enabled = cooldown.remainingMs <= 0L,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text.sendLabel)
            }
        }
    }
}

@Composable
private fun SettingsVersionFooter() {
    val context = LocalContext.current
    val versionName = remember(context) { getAppVersionName(context) }
    Text(
        text = stringResource(R.string.settings_app_version, versionName),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp),
    )
}

@Composable
private fun SettingsHeader(isProUser: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = AppColors.textPrimary(),
            )
            Text(
                text = stringResource(R.string.settings_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.textSecondary(),
            )
        }
        SettingsVersionBadge(isProUser = isProUser)
    }
}

@Composable
private fun SettingsVersionBadge(isProUser: Boolean) {
    val badgeColor = if (isProUser) {
        AppColors.primaryBlue()
    } else {
        AppColors.textSecondary()
    }
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = badgeColor.copy(alpha = 0.12f),
    ) {
        Text(
            text = stringResource(
                if (isProUser) R.string.pro_badge_label else R.string.free_badge_label,
            ),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = badgeColor,
        )
    }
}

@Composable
private fun SettingsSectionCard(
    title: String? = null,
    modifier: Modifier = Modifier,
    options: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (title != null) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (AppColors.isDarkTheme()) AppColors.primaryBlue() else MaterialTheme.colorScheme.onBackground,
            )
        }
        val sectionShape = RoundedCornerShape(6.dp)
        Card(
            modifier = AppColors.cardBorderModifier(sectionShape, Modifier.fillMaxWidth()),
            shape = sectionShape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = if (AppColors.isDarkTheme()) 0.dp else 1.dp),
        ) {
            Column(
                modifier = Modifier.padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                content = options,
            )
        }
    }
}

@Composable
private fun SettingsOptionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(modifier = Modifier.size(44.dp), shape = CircleShape, color = AppColors.settingsIconBackground()) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = AppColors.iconContainerForeground())
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = if (AppColors.isDarkTheme()) AppColors.primaryBlue().copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DialogOptionRow(text: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun themeLabel(mode: ThemeMode): String = when (mode) {
    ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
    ThemeMode.LIGHT -> stringResource(R.string.theme_light)
    ThemeMode.DARK -> stringResource(R.string.theme_dark)
}

@Composable
private fun languageModeLabel(mode: LanguageMode): String = when (mode) {
    LanguageMode.SYSTEM -> stringResource(R.string.language_system)
    LanguageMode.GERMAN -> stringResource(R.string.language_german)
    LanguageMode.ENGLISH -> stringResource(R.string.language_english)
}

@Composable
private fun navStyleLabel(style: NavStyle): String = when (style) {
    NavStyle.BOTTOM_BAR -> stringResource(R.string.nav_style_bottom)
    NavStyle.DRAWER -> stringResource(R.string.nav_style_side_panel)
}
