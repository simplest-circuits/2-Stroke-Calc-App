package com.simplestsoft.twostrokecalc.ui.walkthrough

import com.simplestsoft.twostrokecalc.R
import com.simplestsoft.twostrokecalc.data.config.ProAccessState
import com.simplestsoft.twostrokecalc.domain.model.CalculatorId
import com.simplestsoft.twostrokecalc.ui.calculator.calculatorDisplayOrder

fun ProAccessState.hasProWalkthrough(): Boolean = isPro || isAdmin

fun ProAccessState.exampleFreeCalculator(): CalculatorId? =
    calculatorDisplayOrder.firstOrNull { hasCalculatorAccess(it) }

fun ProAccessState.exampleReadOnlyCalculator(): CalculatorId? =
    calculatorDisplayOrder.firstOrNull { isCalculatorReadOnly(it) }

fun ProAccessState.exampleLockedCalculator(): CalculatorId? = exampleReadOnlyCalculator()

fun buildWalkthroughSteps(proAccess: ProAccessState): List<WalkthroughStep> =
    if (proAccess.hasProWalkthrough()) {
        buildProWalkthroughSteps()
    } else {
        buildFreeWalkthroughSteps()
    }

private fun buildFreeWalkthroughSteps(): List<WalkthroughStep> = buildList {
    add(WalkthroughStep(R.string.walkthrough_free_welcome_title, R.string.walkthrough_free_welcome_body))
    add(
        WalkthroughStep(
            R.string.walkthrough_calculator_nav_title,
            R.string.walkthrough_free_calculator_nav_body,
            WalkthroughTargets.CALCULATOR_NAV,
        ),
    )
    add(
        WalkthroughStep(
            R.string.walkthrough_free_calculator_title,
            R.string.walkthrough_free_calculator_body,
            WalkthroughTargets.CALCULATOR_FREE,
        ),
    )
    add(
        WalkthroughStep(
            R.string.walkthrough_locked_calculator_title,
            R.string.walkthrough_locked_calculator_body,
            WalkthroughTargets.CALCULATOR_LOCKED,
        ),
    )
    add(
        WalkthroughStep(
            R.string.walkthrough_tools_nav_title,
            R.string.walkthrough_tools_nav_body,
            WalkthroughTargets.TOOLS_NAV,
        ),
    )
    add(
        WalkthroughStep(
            R.string.walkthrough_free_vehicles_nav_title,
            R.string.walkthrough_free_vehicles_nav_body,
            WalkthroughTargets.VEHICLES_NAV,
        ),
    )
    add(
        WalkthroughStep(
            R.string.walkthrough_settings_nav_title,
            R.string.walkthrough_free_settings_nav_body,
            WalkthroughTargets.SETTINGS_NAV,
        ),
    )
    add(
        WalkthroughStep(
            R.string.walkthrough_settings_pro_title,
            R.string.walkthrough_settings_pro_body,
            WalkthroughTargets.SETTINGS_PRO,
        ),
    )
    add(
        WalkthroughStep(
            R.string.walkthrough_settings_appearance_title,
            R.string.walkthrough_settings_appearance_body,
            WalkthroughTargets.SETTINGS_APPEARANCE,
        ),
    )
}

private fun buildProWalkthroughSteps(): List<WalkthroughStep> = buildList {
    add(WalkthroughStep(R.string.walkthrough_pro_welcome_title, R.string.walkthrough_pro_welcome_body))
    add(
        WalkthroughStep(
            R.string.walkthrough_calculator_nav_title,
            R.string.walkthrough_pro_calculator_nav_body,
            WalkthroughTargets.CALCULATOR_NAV,
        ),
    )
    add(
        WalkthroughStep(
            R.string.walkthrough_pro_calculator_list_title,
            R.string.walkthrough_pro_calculator_list_body,
            WalkthroughTargets.CALCULATOR_LIST,
        ),
    )
    add(
        WalkthroughStep(
            R.string.walkthrough_tools_nav_title,
            R.string.walkthrough_tools_nav_body,
            WalkthroughTargets.TOOLS_NAV,
        ),
    )
    add(
        WalkthroughStep(
            R.string.walkthrough_vehicles_nav_title,
            R.string.walkthrough_pro_vehicles_nav_body,
            WalkthroughTargets.VEHICLES_NAV,
        ),
    )
    add(
        WalkthroughStep(
            R.string.walkthrough_vehicles_add_title,
            R.string.walkthrough_pro_vehicles_add_body,
            WalkthroughTargets.VEHICLES_ADD,
        ),
    )
    add(
        WalkthroughStep(
            R.string.walkthrough_settings_nav_title,
            R.string.walkthrough_pro_settings_nav_body,
            WalkthroughTargets.SETTINGS_NAV,
        ),
    )
    add(
        WalkthroughStep(
            R.string.walkthrough_settings_appearance_title,
            R.string.walkthrough_settings_appearance_body,
            WalkthroughTargets.SETTINGS_APPEARANCE,
        ),
    )
}
