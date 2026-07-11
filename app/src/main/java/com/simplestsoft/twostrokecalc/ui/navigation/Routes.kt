package com.simplestsoft.twostrokecalc.ui.navigation

import android.net.Uri

object Routes {
    const val SPLASH = "splash"
    const val CALCULATOR = "calculator"
    const val VEHICLES = "vehicles"
    const val VEHICLES_LIST = "vehicles/list"
    const val VEHICLE_DETAIL = "vehicles/detail?vehicleId={vehicleId}"
    const val VEHICLE_FUEL_LOG = "vehicles/fuel-log?vehicleId={vehicleId}"
    const val VEHICLE_COST_OVERVIEW = "vehicles/cost-overview?vehicleId={vehicleId}"
    const val SETTINGS = "settings"
    const val ACCOUNT = "account"
    const val ADMIN = "admin"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val FORGOT_PASSWORD = "forgot_password"

    fun vehicleDetailRoute(vehicleId: String): String =
        "vehicles/detail?vehicleId=${Uri.encode(vehicleId)}"

    fun vehicleFuelLogRoute(vehicleId: String): String =
        "vehicles/fuel-log?vehicleId=${Uri.encode(vehicleId)}"

    fun vehicleCostOverviewRoute(vehicleId: String): String =
        "vehicles/cost-overview?vehicleId=${Uri.encode(vehicleId)}"
}
