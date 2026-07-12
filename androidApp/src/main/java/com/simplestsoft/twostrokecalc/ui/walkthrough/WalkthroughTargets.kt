package com.simplestsoft.twostrokecalc.ui.walkthrough

object WalkthroughTargets {
    const val CALCULATOR_NAV = "calculator_nav"
    const val CALCULATOR_LIST = "calculator_list"
    const val CALCULATOR_FREE = "calculator_free"
    const val CALCULATOR_LOCKED = "calculator_locked"
    const val VEHICLES_NAV = "vehicles_nav"
    const val VEHICLES_ADD = "vehicles_add"
    const val SETTINGS_NAV = "settings_nav"
    const val SETTINGS_PRO = "settings_pro"
    const val SETTINGS_APPEARANCE = "settings_appearance"
    const val NAV_SURFACE = "nav_surface"

    val calculatorOverviewTargets = setOf(
        CALCULATOR_LIST,
        CALCULATOR_FREE,
        CALCULATOR_LOCKED,
    )
}
