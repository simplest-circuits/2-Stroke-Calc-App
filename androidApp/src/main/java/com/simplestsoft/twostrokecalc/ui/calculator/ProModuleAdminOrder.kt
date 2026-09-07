package com.simplestsoft.twostrokecalc.ui.calculator

import com.simplestsoft.twostrokecalc.domain.model.ProModuleId

/** Display order for Pro module toggles in the admin panel. */
val proModuleAdminOrder: List<ProModuleId> =
    listOf(ProModuleId.VEHICLES, ProModuleId.TOOLS) +
        calculatorDisplayOrder.map { ProModuleId.fromCalculator(it) }
