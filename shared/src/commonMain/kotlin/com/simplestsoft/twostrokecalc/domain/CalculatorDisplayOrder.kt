package com.simplestsoft.twostrokecalc.domain

import com.simplestsoft.twostrokecalc.domain.model.CalculatorId

val calculatorDisplayOrder: List<CalculatorId> = listOf(
    CalculatorId.TIMING,
    CalculatorId.PORT_AREA,
    CalculatorId.IGNITION,
    CalculatorId.DC_CABLE,
    CalculatorId.FLUID,
    CalculatorId.COMPRESSION,
    CalculatorId.SQUISH_BAND,
    CalculatorId.MEAN_PRESSURE,
    CalculatorId.GEAR,
    CalculatorId.EXHAUST,
    CalculatorId.COUNTERWEIGHT,
    CalculatorId.VARIATOR_WEIGHT,
    CalculatorId.FUEL_MIX,
    CalculatorId.CARB_JET,
    CalculatorId.DYNO_INERTIA,
    CalculatorId.VEHICLE_DYNAMICS,
)
