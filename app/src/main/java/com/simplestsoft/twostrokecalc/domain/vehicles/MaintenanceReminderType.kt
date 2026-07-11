package com.simplestsoft.twostrokecalc.domain.vehicles

enum class MaintenanceReminderType {
    NEXT_SERVICE_DATE,
    NEXT_SERVICE_KM,
    TUV,
    INSURANCE,
    OIL_INTERVAL,
    SPARK_INTERVAL,
    VARIATOR_INTERVAL,
    BRAKE_INTERVAL,
    TIRE_INTERVAL,
}

data class MaintenanceDueAlert(
    val vehicleId: String,
    val vehicleTitle: String,
    val reminderKey: String,
    val type: MaintenanceReminderType,
    val detail: String,
    /** Calendar due date for date-based reminders; null for km-based until anchored in the worker. */
    val dueDate: java.time.LocalDate? = null,
)
