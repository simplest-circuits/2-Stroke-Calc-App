package com.simplestsoft.twostrokecalc.ui.screens.vehicle

import androidx.annotation.StringRes
import com.simplestsoft.twostrokecalc.R

enum class VehicleDetailTab(@StringRes val titleRes: Int) {
    OVERVIEW(R.string.vehicles_tab_overview),
    BASIC(R.string.vehicles_tab_basic),
    ENGINE(R.string.vehicles_tab_engine),
    TUNING(R.string.vehicles_tab_tuning),
    DRIVETRAIN(R.string.vehicles_tab_drivetrain),
    CHASSIS(R.string.vehicles_tab_chassis),
    ELECTRICAL(R.string.vehicles_tab_electrical),
    MAINTENANCE(R.string.vehicles_tab_maintenance),
    DOCUMENTS(R.string.vehicles_tab_documents),
    MEASUREMENTS(R.string.vehicles_tab_measurements),
}
