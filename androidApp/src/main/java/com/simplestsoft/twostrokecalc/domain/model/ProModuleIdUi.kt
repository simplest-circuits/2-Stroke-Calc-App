package com.simplestsoft.twostrokecalc.domain.model

import androidx.annotation.StringRes
import com.simplestsoft.twostrokecalc.R
import com.simplestsoft.twostrokecalc.ui.calculator.overviewDescriptionRes
import com.simplestsoft.twostrokecalc.ui.calculator.tabLabelRes

@StringRes
fun ProModuleId.labelRes(): Int = when (this) {
    ProModuleId.VEHICLES -> R.string.nav_vehicles
    else -> toCalculator()!!.tabLabelRes()
}

@StringRes
fun ProModuleId.overviewDescriptionRes(): Int = when (this) {
    ProModuleId.VEHICLES -> R.string.pro_module_vehicles_desc
    else -> toCalculator()!!.overviewDescriptionRes()
}
