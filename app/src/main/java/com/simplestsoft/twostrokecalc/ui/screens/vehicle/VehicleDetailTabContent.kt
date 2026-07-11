package com.simplestsoft.twostrokecalc.ui.screens.vehicle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.simplestsoft.twostrokecalc.R
import com.simplestsoft.twostrokecalc.domain.model.EngineCycleType
import com.simplestsoft.twostrokecalc.domain.model.MaintenanceEntry
import com.simplestsoft.twostrokecalc.domain.model.MaintenanceType
import com.simplestsoft.twostrokecalc.domain.model.Vehicle
import com.simplestsoft.twostrokecalc.domain.model.VehicleCarbIgnitionSpecs
import com.simplestsoft.twostrokecalc.domain.model.VehicleChassisSpecs
import com.simplestsoft.twostrokecalc.domain.model.VehicleDocumentInfo
import com.simplestsoft.twostrokecalc.domain.model.VehicleDrivetrainSpecs
import com.simplestsoft.twostrokecalc.domain.model.VehicleElectricalSpecs
import com.simplestsoft.twostrokecalc.domain.model.VehicleEngineSpecs
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.simplestsoft.twostrokecalc.domain.model.VehicleAttachment
import com.simplestsoft.twostrokecalc.domain.model.VehicleServiceSchedule
import com.simplestsoft.twostrokecalc.ui.theme.ContainerCornerRadius
import com.simplestsoft.twostrokecalc.domain.model.VehicleType
import com.simplestsoft.twostrokecalc.domain.vehicles.VehicleCapabilities
import com.simplestsoft.twostrokecalc.domain.vehicles.resolvedValveClearances
import com.simplestsoft.twostrokecalc.domain.vehicles.withCylinderCount
import com.simplestsoft.twostrokecalc.domain.vehicles.withValveClearances
import com.simplestsoft.twostrokecalc.ui.components.AppColors
import com.simplestsoft.twostrokecalc.ui.pro.LocalVehicleEditingEnabled
import com.simplestsoft.twostrokecalc.ui.components.FormFieldDefaults
import com.simplestsoft.twostrokecalc.ui.components.AppOutlinedTextField
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorDecimalField
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorChoiceField
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorDropdownOption
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorInputModeSwitch
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorSection

@Composable
fun VehicleBasicDataTabContent(
    vehicle: Vehicle,
    profileImageFile: java.io.File?,
    onUpdate: (Vehicle) -> Unit,
    onProfileImageSelected: (android.net.Uri) -> Unit,
    onProfileImageRemove: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        VehicleProfileImageSection(
            imageFile = profileImageFile,
            hasProfileImage = vehicle.profileImageFileName != null,
            onImageSelected = onProfileImageSelected,
            onRemoveImage = onProfileImageRemove,
        )
        CalculatorSection(title = stringResource(R.string.vehicles_section_basic)) {
            VehicleTextField(vehicle.name, stringResource(R.string.vehicles_field_name)) {
                onUpdate(vehicle.copy(name = it))
            }
            VehicleTextField(vehicle.brand, stringResource(R.string.vehicles_field_brand)) {
                onUpdate(vehicle.copy(brand = it))
            }
            VehicleTextField(vehicle.model, stringResource(R.string.vehicles_field_model)) {
                onUpdate(vehicle.copy(model = it))
            }
            VehicleTextField(vehicle.year, stringResource(R.string.vehicles_field_year)) {
                onUpdate(vehicle.copy(year = it))
            }
            CalculatorChoiceField(
                label = stringResource(R.string.vehicles_field_type),
                options = VehicleType.entries.map {
                    CalculatorDropdownOption(it.name, vehicleTypeLabel(it))
                },
                selectedKey = vehicle.vehicleType.name,
                onOptionSelected = { key -> onUpdate(vehicle.copy(vehicleType = VehicleType.valueOf(key))) },
            )
            CalculatorDecimalField(
                value = vehicle.currentOdometerKm,
                onValueChange = { onUpdate(vehicle.copy(currentOdometerKm = it)) },
                label = stringResource(R.string.vehicles_field_odometer),
                suffix = "km",
            )
            CalculatorDecimalField(
                value = vehicle.currentOperatingHours,
                onValueChange = { onUpdate(vehicle.copy(currentOperatingHours = it)) },
                label = stringResource(R.string.vehicles_field_operating_hours),
                suffix = "h",
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.vehicles_field_active),
                    style = MaterialTheme.typography.bodyLarge,
                    color = AppColors.textPrimary(),
                )
                Switch(
                    checked = vehicle.isActive,
                    onCheckedChange = { onUpdate(vehicle.copy(isActive = it)) },
                    enabled = LocalVehicleEditingEnabled.current,
                )
            }
            VehicleTextField(vehicle.color, stringResource(R.string.vehicles_field_color)) {
                onUpdate(vehicle.copy(color = it))
            }
            VehicleDatePickerField(
                value = vehicle.purchaseDate,
                label = stringResource(R.string.vehicles_field_purchase_date),
                onValueChange = { onUpdate(vehicle.copy(purchaseDate = it)) },
            )
            CalculatorDecimalField(
                value = vehicle.purchasePrice,
                onValueChange = { onUpdate(vehicle.copy(purchasePrice = it)) },
                label = stringResource(R.string.vehicles_field_purchase_price),
                suffix = "€",
            )
        }
        CalculatorSection(title = stringResource(R.string.vehicles_section_identification)) {
            VehicleTextField(vehicle.licensePlate, stringResource(R.string.vehicles_field_license_plate)) {
                onUpdate(vehicle.copy(licensePlate = it))
            }
            VehicleTextField(vehicle.frameNumber, stringResource(R.string.vehicles_field_frame_number)) {
                onUpdate(vehicle.copy(frameNumber = it))
            }
            VehicleTextField(vehicle.engineNumber, stringResource(R.string.vehicles_field_engine_number)) {
                onUpdate(vehicle.copy(engineNumber = it))
            }
        }
    }
}

@Composable
fun VehicleEngineTabContent(
    engine: VehicleEngineSpecs,
    onUpdate: (VehicleEngineSpecs) -> Unit,
) {
    val isTwoStroke = engine.cycleType == EngineCycleType.TWO_STROKE

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        CalculatorSection(title = stringResource(R.string.vehicles_section_engine_cycle)) {
            CalculatorInputModeSwitch(
                optionALabel = stringResource(R.string.vehicles_engine_two_stroke),
                optionBLabel = stringResource(R.string.vehicles_engine_four_stroke),
                useOptionA = isTwoStroke,
                onUseOptionAChange = { twoStroke ->
                    onUpdate(
                        engine.copy(
                            cycleType = if (twoStroke) {
                                EngineCycleType.TWO_STROKE
                            } else {
                                EngineCycleType.FOUR_STROKE
                            },
                        ),
                    )
                },
            )
        }
        CalculatorSection(title = stringResource(R.string.vehicles_section_engine_geometry)) {
            CalculatorDecimalField(engine.displacementCc, { onUpdate(engine.copy(displacementCc = it)) },
                stringResource(R.string.vehicles_field_displacement), suffix = "cm³")
            CalculatorDecimalField(engine.boreMm, { onUpdate(engine.copy(boreMm = it)) },
                stringResource(R.string.vehicles_field_bore), suffix = "mm")
            CalculatorDecimalField(engine.strokeMm, { onUpdate(engine.copy(strokeMm = it)) },
                stringResource(R.string.vehicles_field_stroke), suffix = "mm")
            VehicleTextField(engine.compressionRatio, stringResource(R.string.vehicles_field_compression)) {
                onUpdate(engine.copy(compressionRatio = it))
            }
            if (isTwoStroke) {
                CalculatorDecimalField(engine.squishClearanceMm, { onUpdate(engine.copy(squishClearanceMm = it)) },
                    stringResource(R.string.vehicles_field_squish), suffix = "mm")
            }
        }
        CalculatorSection(title = stringResource(R.string.vehicles_section_engine_components)) {
            VehicleTextField(engine.cylinderHead, stringResource(R.string.vehicles_field_cylinder_head)) {
                onUpdate(engine.copy(cylinderHead = it))
            }
            VehicleTextField(engine.piston, stringResource(R.string.vehicles_field_piston)) {
                onUpdate(engine.copy(piston = it))
            }
            if (isTwoStroke) {
                VehicleTextField(engine.reedValve, stringResource(R.string.vehicles_field_reed_valve)) {
                    onUpdate(engine.copy(reedValve = it))
                }
            }
            VehicleTextField(engine.intakeSystem, stringResource(R.string.vehicles_field_intake_system)) {
                onUpdate(engine.copy(intakeSystem = it))
            }
            VehicleTextField(engine.coolingType, stringResource(R.string.vehicles_field_cooling)) {
                onUpdate(engine.copy(coolingType = it))
            }
        }
        CalculatorSection(
            title = stringResource(
                if (isTwoStroke) {
                    R.string.vehicles_section_engine_timing
                } else {
                    R.string.vehicles_section_engine_valves
                },
            ),
        ) {
            if (isTwoStroke) {
                VehicleTextField(
                    value = engine.portTimingNotes,
                    label = stringResource(R.string.vehicles_field_port_timing),
                    supportingText = stringResource(R.string.vehicles_field_port_timing_hint),
                    singleLine = false,
                    onValueChange = { onUpdate(engine.copy(portTimingNotes = it)) },
                )
            } else {
                CalculatorDecimalField(
                    value = engine.cylinderCount,
                    onValueChange = { onUpdate(engine.withCylinderCount(it)) },
                    label = stringResource(R.string.vehicles_field_cylinder_count),
                    suffix = "",
                )
                val clearances = engine.resolvedValveClearances()
                clearances.forEachIndexed { index, clearance ->
                    val label = if (clearances.size == 1) {
                        stringResource(R.string.vehicles_field_valve_clearance)
                    } else {
                        stringResource(R.string.vehicles_field_valve_clearance_cylinder, index + 1)
                    }
                    VehicleTextField(
                        value = clearance,
                        label = label,
                        supportingText = if (index == 0) {
                            stringResource(R.string.vehicles_field_valve_clearance_hint)
                        } else {
                            null
                        },
                        singleLine = false,
                        onValueChange = { newValue ->
                            val updated = clearances.toMutableList().also { it[index] = newValue }
                            onUpdate(engine.withValveClearances(updated))
                        },
                    )
                }
            }
            VehicleTextField(engine.exhaustSystem, stringResource(R.string.vehicles_field_exhaust)) {
                onUpdate(engine.copy(exhaustSystem = it))
            }
            VehicleTextField(
                value = engine.engineNotes,
                label = stringResource(R.string.vehicles_field_engine_notes),
                singleLine = false,
                onValueChange = { onUpdate(engine.copy(engineNotes = it)) },
            )
        }
    }
}

@Composable
fun VehicleTuningTabContent(
    specs: VehicleCarbIgnitionSpecs,
    capabilities: VehicleCapabilities,
    onUpdate: (VehicleCarbIgnitionSpecs) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        CalculatorSection(title = stringResource(R.string.vehicles_section_carburetor)) {
            VehicleTextField(specs.carbType, stringResource(R.string.vehicles_field_carb_type)) {
                onUpdate(specs.copy(carbType = it))
            }
            VehicleTextField(specs.mainJet, stringResource(R.string.vehicles_field_main_jet)) {
                onUpdate(specs.copy(mainJet = it))
            }
            VehicleTextField(specs.pilotJet, stringResource(R.string.vehicles_field_pilot_jet)) {
                onUpdate(specs.copy(pilotJet = it))
            }
            VehicleTextField(specs.needle, stringResource(R.string.vehicles_field_needle)) {
                onUpdate(specs.copy(needle = it))
            }
        }
        CalculatorSection(title = stringResource(R.string.vehicles_section_fuel)) {
            if (capabilities.showFuelMix) {
                VehicleTextField(
                    value = specs.fuelMixRatio,
                    label = stringResource(R.string.vehicles_field_fuel_mix),
                    supportingText = stringResource(R.string.vehicles_field_fuel_mix_hint),
                    onValueChange = { onUpdate(specs.copy(fuelMixRatio = it)) },
                )
            }
            VehicleTextField(specs.fuelType, stringResource(R.string.vehicles_field_fuel_type)) {
                onUpdate(specs.copy(fuelType = it))
            }
            VehicleTextField(specs.oilType, stringResource(R.string.vehicles_field_oil_type)) {
                onUpdate(specs.copy(oilType = it))
            }
            VehicleTextField(specs.oilBrand, stringResource(R.string.vehicles_field_oil_brand)) {
                onUpdate(specs.copy(oilBrand = it))
            }
        }
        CalculatorSection(title = stringResource(R.string.vehicles_section_ignition)) {
            VehicleTextField(specs.sparkPlug, stringResource(R.string.vehicles_field_spark_plug)) {
                onUpdate(specs.copy(sparkPlug = it))
            }
            CalculatorDecimalField(specs.ignitionTimingDeg, { onUpdate(specs.copy(ignitionTimingDeg = it)) },
                stringResource(R.string.vehicles_field_ignition_timing), suffix = "° vOT")
            VehicleTextField(specs.ignitionSystem, stringResource(R.string.vehicles_field_ignition_system)) {
                onUpdate(specs.copy(ignitionSystem = it))
            }
            VehicleTextField(specs.cdiBox, stringResource(R.string.vehicles_field_cdi)) {
                onUpdate(specs.copy(cdiBox = it))
            }
            VehicleTextField(
                value = specs.tuningNotes,
                label = stringResource(R.string.vehicles_field_tuning_notes),
                singleLine = false,
                onValueChange = { onUpdate(specs.copy(tuningNotes = it)) },
            )
        }
    }
}

@Composable
fun VehicleDrivetrainTabContent(
    specs: VehicleDrivetrainSpecs,
    capabilities: VehicleCapabilities,
    onUpdate: (VehicleDrivetrainSpecs) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (capabilities.showVariator) {
            CalculatorSection(title = stringResource(R.string.vehicles_section_variator)) {
                VehicleTextField(specs.driveType, stringResource(R.string.vehicles_field_drive_type)) {
                    onUpdate(specs.copy(driveType = it))
                }
                VehicleTextField(specs.variatorBrand, stringResource(R.string.vehicles_field_variator_brand)) {
                    onUpdate(specs.copy(variatorBrand = it))
                }
                VehicleTextField(specs.variatorWeightsG, stringResource(R.string.vehicles_field_variator_weights)) {
                    onUpdate(specs.copy(variatorWeightsG = it))
                }
                VehicleTextField(specs.variatorRollers, stringResource(R.string.vehicles_field_variator_rollers)) {
                    onUpdate(specs.copy(variatorRollers = it))
                }
                VehicleTextField(specs.variatorBelt, stringResource(R.string.vehicles_field_variator_belt)) {
                    onUpdate(specs.copy(variatorBelt = it))
                }
                VehicleTextField(specs.variatorGuide, stringResource(R.string.vehicles_field_variator_guide)) {
                    onUpdate(specs.copy(variatorGuide = it))
                }
            }
        }
        if (capabilities.showClutch) {
            CalculatorSection(title = stringResource(R.string.vehicles_section_clutch)) {
                VehicleTextField(specs.clutchType, stringResource(R.string.vehicles_field_clutch_type)) {
                    onUpdate(specs.copy(clutchType = it))
                }
                VehicleTextField(specs.clutchSprings, stringResource(R.string.vehicles_field_clutch_springs)) {
                    onUpdate(specs.copy(clutchSprings = it))
                }
            }
        }
        if (capabilities.showChainGearing) {
            CalculatorSection(title = stringResource(R.string.vehicles_section_gearing)) {
                VehicleTextField(specs.gearingPrimary, stringResource(R.string.vehicles_field_gearing_primary)) {
                    onUpdate(specs.copy(gearingPrimary = it))
                }
                VehicleTextField(specs.gearingSecondary, stringResource(R.string.vehicles_field_gearing_secondary)) {
                    onUpdate(specs.copy(gearingSecondary = it))
                }
                VehicleTextField(specs.frontSprocketTeeth, stringResource(R.string.vehicles_field_front_sprocket)) {
                    onUpdate(specs.copy(frontSprocketTeeth = it))
                }
                VehicleTextField(specs.rearSprocketTeeth, stringResource(R.string.vehicles_field_rear_sprocket)) {
                    onUpdate(specs.copy(rearSprocketTeeth = it))
                }
                VehicleTextField(specs.chainType, stringResource(R.string.vehicles_field_chain_type)) {
                    onUpdate(specs.copy(chainType = it))
                }
                VehicleTextField(specs.chainLinks, stringResource(R.string.vehicles_field_chain_links)) {
                    onUpdate(specs.copy(chainLinks = it))
                }
            }
        }
        if (capabilities.showTransmission) {
            CalculatorSection(title = stringResource(R.string.vehicles_section_drivetrain)) {
                VehicleTextField(specs.driveType, stringResource(R.string.vehicles_field_drive_type)) {
                    onUpdate(specs.copy(driveType = it))
                }
                VehicleTextField(specs.gearingPrimary, stringResource(R.string.vehicles_field_gearing_primary)) {
                    onUpdate(specs.copy(gearingPrimary = it))
                }
                VehicleTextField(specs.gearingSecondary, stringResource(R.string.vehicles_field_gearing_secondary)) {
                    onUpdate(specs.copy(gearingSecondary = it))
                }
                VehicleTextField(specs.clutchType, stringResource(R.string.vehicles_field_clutch_type)) {
                    onUpdate(specs.copy(clutchType = it))
                }
            }
        }
        CalculatorSection(title = stringResource(R.string.vehicles_field_drivetrain_notes)) {
            VehicleTextField(
                value = specs.finalDriveNotes,
                label = stringResource(R.string.vehicles_field_drivetrain_notes),
                singleLine = false,
                onValueChange = { onUpdate(specs.copy(finalDriveNotes = it)) },
            )
        }
    }
}

@Composable
fun VehicleChassisTabContent(
    specs: VehicleChassisSpecs,
    onUpdate: (VehicleChassisSpecs) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        CalculatorSection(title = stringResource(R.string.vehicles_section_tires)) {
            VehicleTextField(specs.frontTire, stringResource(R.string.vehicles_field_front_tire)) {
                onUpdate(specs.copy(frontTire = it))
            }
            VehicleTextField(specs.rearTire, stringResource(R.string.vehicles_field_rear_tire)) {
                onUpdate(specs.copy(rearTire = it))
            }
            CalculatorDecimalField(specs.tirePressureFront, { onUpdate(specs.copy(tirePressureFront = it)) },
                stringResource(R.string.vehicles_field_tire_pressure_front), suffix = "bar")
            CalculatorDecimalField(specs.tirePressureRear, { onUpdate(specs.copy(tirePressureRear = it)) },
                stringResource(R.string.vehicles_field_tire_pressure_rear), suffix = "bar")
            VehicleTextField(specs.rimFront, stringResource(R.string.vehicles_field_rim_front)) {
                onUpdate(specs.copy(rimFront = it))
            }
            VehicleTextField(specs.rimRear, stringResource(R.string.vehicles_field_rim_rear)) {
                onUpdate(specs.copy(rimRear = it))
            }
        }
        CalculatorSection(title = stringResource(R.string.vehicles_section_brakes)) {
            VehicleTextField(specs.frontBrake, stringResource(R.string.vehicles_field_front_brake)) {
                onUpdate(specs.copy(frontBrake = it))
            }
            VehicleTextField(specs.rearBrake, stringResource(R.string.vehicles_field_rear_brake)) {
                onUpdate(specs.copy(rearBrake = it))
            }
            VehicleTextField(specs.brakePadsFront, stringResource(R.string.vehicles_field_brake_pads_front)) {
                onUpdate(specs.copy(brakePadsFront = it))
            }
            VehicleTextField(specs.brakePadsRear, stringResource(R.string.vehicles_field_brake_pads_rear)) {
                onUpdate(specs.copy(brakePadsRear = it))
            }
            VehicleTextField(specs.brakeFluid, stringResource(R.string.vehicles_field_brake_fluid)) {
                onUpdate(specs.copy(brakeFluid = it))
            }
        }
        CalculatorSection(title = stringResource(R.string.vehicles_section_suspension)) {
            VehicleTextField(specs.forkOil, stringResource(R.string.vehicles_field_fork_oil)) {
                onUpdate(specs.copy(forkOil = it))
            }
            VehicleTextField(specs.rearShock, stringResource(R.string.vehicles_field_rear_shock)) {
                onUpdate(specs.copy(rearShock = it))
            }
            VehicleTextField(specs.steeringBearing, stringResource(R.string.vehicles_field_steering_bearing)) {
                onUpdate(specs.copy(steeringBearing = it))
            }
            VehicleTextField(
                value = specs.chassisNotes,
                label = stringResource(R.string.vehicles_field_chassis_notes),
                singleLine = false,
                onValueChange = { onUpdate(specs.copy(chassisNotes = it)) },
            )
        }
    }
}

@Composable
fun VehicleElectricalTabContent(
    specs: VehicleElectricalSpecs,
    onUpdate: (VehicleElectricalSpecs) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        CalculatorSection(title = stringResource(R.string.vehicles_section_battery)) {
            VehicleTextField(specs.battery, stringResource(R.string.vehicles_field_battery)) {
                onUpdate(specs.copy(battery = it))
            }
            VehicleTextField(specs.batteryYear, stringResource(R.string.vehicles_field_battery_year)) {
                onUpdate(specs.copy(batteryYear = it))
            }
            VehicleTextField(specs.alternator, stringResource(R.string.vehicles_field_alternator)) {
                onUpdate(specs.copy(alternator = it))
            }
            VehicleTextField(specs.regulator, stringResource(R.string.vehicles_field_regulator)) {
                onUpdate(specs.copy(regulator = it))
            }
        }
        CalculatorSection(title = stringResource(R.string.vehicles_section_ignition_electrical)) {
            VehicleTextField(specs.ignitionCoil, stringResource(R.string.vehicles_field_ignition_coil)) {
                onUpdate(specs.copy(ignitionCoil = it))
            }
            VehicleTextField(
                value = specs.wiringNotes,
                label = stringResource(R.string.vehicles_field_wiring_notes),
                singleLine = false,
                onValueChange = { onUpdate(specs.copy(wiringNotes = it)) },
            )
        }
        CalculatorSection(title = stringResource(R.string.vehicles_section_lights)) {
            VehicleTextField(specs.headlightBulb, stringResource(R.string.vehicles_field_headlight)) {
                onUpdate(specs.copy(headlightBulb = it))
            }
            VehicleTextField(specs.tailLightBulb, stringResource(R.string.vehicles_field_taillight)) {
                onUpdate(specs.copy(tailLightBulb = it))
            }
            VehicleTextField(specs.horn, stringResource(R.string.vehicles_field_horn)) {
                onUpdate(specs.copy(horn = it))
            }
            VehicleTextField(
                value = specs.electricalNotes,
                label = stringResource(R.string.vehicles_field_electrical_notes),
                singleLine = false,
                onValueChange = { onUpdate(specs.copy(electricalNotes = it)) },
            )
        }
    }
}

@Composable
fun VehicleMaintenanceTabContent(
    schedule: VehicleServiceSchedule,
    entries: List<MaintenanceEntry>,
    capabilities: VehicleCapabilities,
    onScheduleUpdate: (VehicleServiceSchedule) -> Unit,
    onEditEntry: (MaintenanceEntry) -> Unit,
    onDeleteEntry: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        CalculatorSection(title = stringResource(R.string.vehicles_section_service_intervals)) {
            CalculatorDecimalField(schedule.oilChangeIntervalKm, { onScheduleUpdate(schedule.copy(oilChangeIntervalKm = it)) },
                stringResource(R.string.vehicles_field_oil_interval), suffix = "km")
            CalculatorDecimalField(schedule.sparkPlugIntervalKm, { onScheduleUpdate(schedule.copy(sparkPlugIntervalKm = it)) },
                stringResource(R.string.vehicles_field_spark_interval), suffix = "km")
            if (capabilities.showVariatorMaintenance) {
                CalculatorDecimalField(schedule.variatorServiceIntervalKm, { onScheduleUpdate(schedule.copy(variatorServiceIntervalKm = it)) },
                    stringResource(R.string.vehicles_field_variator_interval), suffix = "km")
            }
            CalculatorDecimalField(schedule.brakeServiceIntervalKm, { onScheduleUpdate(schedule.copy(brakeServiceIntervalKm = it)) },
                stringResource(R.string.vehicles_field_brake_interval), suffix = "km")
            CalculatorDecimalField(schedule.tireServiceIntervalKm, { onScheduleUpdate(schedule.copy(tireServiceIntervalKm = it)) },
                stringResource(R.string.vehicles_field_tire_interval), suffix = "km")
            VehicleDatePickerField(
                value = schedule.lastOilChangeDate,
                label = stringResource(R.string.vehicles_field_last_oil_date),
                onValueChange = { onScheduleUpdate(schedule.copy(lastOilChangeDate = it)) },
            )
            CalculatorDecimalField(schedule.lastOilChangeKm, { onScheduleUpdate(schedule.copy(lastOilChangeKm = it)) },
                stringResource(R.string.vehicles_field_last_oil_km), suffix = "km")
            VehicleDatePickerField(
                value = schedule.nextServiceDueDate,
                label = stringResource(R.string.vehicles_field_next_service_date),
                onValueChange = { onScheduleUpdate(schedule.copy(nextServiceDueDate = it)) },
            )
            CalculatorDecimalField(schedule.nextServiceDueKm, { onScheduleUpdate(schedule.copy(nextServiceDueKm = it)) },
                stringResource(R.string.vehicles_field_next_service_km), suffix = "km")
        }
        CalculatorSection(title = stringResource(R.string.vehicles_section_maintenance)) {
            if (entries.isEmpty()) {
                Text(
                    text = stringResource(R.string.vehicles_maintenance_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.textSecondary(),
                )
            } else {
                entries.sortedByDescending { it.date }.forEach { entry ->
                    MaintenanceEntryCard(
                        entry = entry,
                        onEdit = { onEditEntry(entry) },
                        onDelete = { onDeleteEntry(entry.id) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleDocumentsSection(
    documents: VehicleDocumentInfo,
    schedule: VehicleServiceSchedule,
    notes: String,
    attachments: List<VehicleAttachment>,
    onDocumentsUpdate: (VehicleDocumentInfo) -> Unit,
    onScheduleUpdate: (VehicleServiceSchedule) -> Unit,
    onNotesUpdate: (String) -> Unit,
    onAddAttachments: (List<android.net.Uri>) -> Unit,
    onRemoveAttachment: (String) -> Unit,
    onUpdateAttachment: (VehicleAttachment) -> Unit,
    onOpenAttachment: (VehicleAttachment) -> Boolean,
) {
    var subTabIndex by rememberSaveable { mutableIntStateOf(0) }
    val cardShape = RoundedCornerShape(ContainerCornerRadius)

    Column(verticalArrangement = Arrangement.spacedBy(FormFieldDefaults.fieldSpacing)) {
        Card(modifier = Modifier.fillMaxWidth(), shape = cardShape) {
            ScrollableTabRow(
                selectedTabIndex = subTabIndex,
                edgePadding = 8.dp,
                divider = {},
            ) {
                Tab(
                    selected = subTabIndex == 0,
                    onClick = { subTabIndex = 0 },
                    modifier = Modifier.height(44.dp),
                    text = { Text(stringResource(R.string.vehicles_subtab_document_info)) },
                )
                Tab(
                    selected = subTabIndex == 1,
                    onClick = { subTabIndex = 1 },
                    modifier = Modifier.height(44.dp),
                    text = { Text(stringResource(R.string.vehicles_subtab_attachments)) },
                )
            }
        }

        when (subTabIndex) {
            0 -> VehicleDocumentInfoTabContent(
                documents = documents,
                schedule = schedule,
                notes = notes,
                onDocumentsUpdate = onDocumentsUpdate,
                onScheduleUpdate = onScheduleUpdate,
                onNotesUpdate = onNotesUpdate,
            )
            else -> VehicleAttachmentsTabContent(
                attachments = attachments,
                onAddAttachments = onAddAttachments,
                onRemoveAttachment = onRemoveAttachment,
                onUpdateAttachment = onUpdateAttachment,
                onOpenAttachment = onOpenAttachment,
            )
        }
    }
}

@Composable
private fun VehicleDocumentInfoTabContent(
    documents: VehicleDocumentInfo,
    schedule: VehicleServiceSchedule,
    notes: String,
    onDocumentsUpdate: (VehicleDocumentInfo) -> Unit,
    onScheduleUpdate: (VehicleServiceSchedule) -> Unit,
    onNotesUpdate: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        CalculatorSection(title = stringResource(R.string.vehicles_section_deadlines)) {
            VehicleDatePickerField(
                value = schedule.tuvInspectionDate,
                label = stringResource(R.string.vehicles_field_tuv_date),
                onValueChange = { onScheduleUpdate(schedule.copy(tuvInspectionDate = it)) },
            )
            VehicleDatePickerField(
                value = schedule.insuranceExpiryDate,
                label = stringResource(R.string.vehicles_field_insurance_date),
                onValueChange = { onScheduleUpdate(schedule.copy(insuranceExpiryDate = it)) },
            )
        }
        CalculatorSection(title = stringResource(R.string.vehicles_section_insurance)) {
            VehicleTextField(documents.insuranceCompany, stringResource(R.string.vehicles_field_insurance_company)) {
                onDocumentsUpdate(documents.copy(insuranceCompany = it))
            }
            VehicleTextField(documents.insurancePolicyNumber, stringResource(R.string.vehicles_field_insurance_policy)) {
                onDocumentsUpdate(documents.copy(insurancePolicyNumber = it))
            }
            VehicleTextField(documents.insuranceType, stringResource(R.string.vehicles_field_insurance_type)) {
                onDocumentsUpdate(documents.copy(insuranceType = it))
            }
        }
        CalculatorSection(title = stringResource(R.string.vehicles_section_registration)) {
            VehicleTextField(documents.registrationCertNumber, stringResource(R.string.vehicles_field_registration_cert)) {
                onDocumentsUpdate(documents.copy(registrationCertNumber = it))
            }
            VehicleTextField(documents.ownerOnPaper, stringResource(R.string.vehicles_field_owner)) {
                onDocumentsUpdate(documents.copy(ownerOnPaper = it))
            }
            VehicleTextField(documents.purchaseFrom, stringResource(R.string.vehicles_field_purchase_from)) {
                onDocumentsUpdate(documents.copy(purchaseFrom = it))
            }
            VehicleTextField(documents.storageLocation, stringResource(R.string.vehicles_field_storage)) {
                onDocumentsUpdate(documents.copy(storageLocation = it))
            }
            VehicleTextField(documents.keyLocation, stringResource(R.string.vehicles_field_keys)) {
                onDocumentsUpdate(documents.copy(keyLocation = it))
            }
            VehicleTextField(
                value = documents.documentNotes,
                label = stringResource(R.string.vehicles_field_document_notes),
                singleLine = false,
                onValueChange = { onDocumentsUpdate(documents.copy(documentNotes = it)) },
            )
        }
        CalculatorSection(title = stringResource(R.string.vehicles_section_notes)) {
            VehicleTextField(
                value = notes,
                label = stringResource(R.string.vehicles_field_notes),
                singleLine = false,
                onValueChange = onNotesUpdate,
            )
        }
    }
}

@Composable
private fun MaintenanceEntryCard(
    entry: MaintenanceEntry,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    CalculatorSection(title = maintenanceEntryTitle(entry)) {
        if (entry.date.isNotBlank()) {
            MaintenanceInfoRow(stringResource(R.string.vehicles_maintenance_date), entry.date)
        }
        if (entry.odometerKm.isNotBlank()) {
            MaintenanceInfoRow(stringResource(R.string.vehicles_maintenance_odometer), "${entry.odometerKm} km")
        }
        MaintenanceInfoRow(stringResource(R.string.vehicles_maintenance_type), maintenanceTypeLabel(entry.type))
        if (entry.description.isNotBlank()) {
            MaintenanceInfoRow(stringResource(R.string.vehicles_maintenance_description), entry.description)
        }
        if (entry.cost.isNotBlank()) {
            MaintenanceInfoRow(stringResource(R.string.vehicles_maintenance_cost), "${entry.cost} €")
        }
        if (entry.partsUsed.isNotBlank()) {
            MaintenanceInfoRow(stringResource(R.string.vehicles_maintenance_parts), entry.partsUsed)
        }
        if (entry.workshop.isNotBlank()) {
            MaintenanceInfoRow(stringResource(R.string.vehicles_maintenance_workshop), entry.workshop)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onEdit) {
                Text(stringResource(R.string.vehicles_maintenance_edit))
            }
            TextButton(onClick = onDelete) {
                Text(stringResource(R.string.vehicles_delete), color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun MaintenanceInfoRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.padding(bottom = 4.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = AppColors.textSecondary())
        Text(value, style = MaterialTheme.typography.bodyMedium, color = AppColors.textPrimary())
    }
}

@Composable
internal fun VehicleTextField(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    singleLine: Boolean = true,
    onValueChange: (String) -> Unit,
) {
    AppOutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        supportingText = supportingText,
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 2,
        maxLines = if (singleLine) 1 else 6,
        compactHeight = singleLine,
    )
}

@Composable
private fun maintenanceEntryTitle(entry: MaintenanceEntry): String {
    val typeLabel = maintenanceTypeLabel(entry.type)
    return if (entry.date.isNotBlank()) "$typeLabel · ${entry.date}" else typeLabel
}

@Composable
internal fun engineCycleLabel(type: EngineCycleType): String = when (type) {
    EngineCycleType.TWO_STROKE -> stringResource(R.string.vehicles_engine_two_stroke)
    EngineCycleType.FOUR_STROKE -> stringResource(R.string.vehicles_engine_four_stroke)
}

@Composable
internal fun vehicleTypeLabel(type: VehicleType): String = when (type) {
    VehicleType.MOFA -> stringResource(R.string.vehicles_type_mofa)
    VehicleType.MOKICK -> stringResource(R.string.vehicles_type_mokick)
    VehicleType.ROLLER -> stringResource(R.string.vehicles_type_roller)
    VehicleType.CROSS -> stringResource(R.string.vehicles_type_cross)
    VehicleType.FOUR_WHEEL -> stringResource(R.string.vehicles_type_four_wheel)
    VehicleType.OTHER -> stringResource(R.string.vehicles_type_other)
}

@Composable
internal fun maintenanceTypeLabel(type: MaintenanceType): String = when (type) {
    MaintenanceType.OIL_CHANGE -> stringResource(R.string.vehicles_maint_oil)
    MaintenanceType.SPARK_PLUG -> stringResource(R.string.vehicles_maint_spark)
    MaintenanceType.VARIATOR -> stringResource(R.string.vehicles_maint_variator)
    MaintenanceType.CARBURETOR -> stringResource(R.string.vehicles_maint_carb)
    MaintenanceType.EXHAUST -> stringResource(R.string.vehicles_maint_exhaust)
    MaintenanceType.TIRES -> stringResource(R.string.vehicles_maint_tires)
    MaintenanceType.BRAKES -> stringResource(R.string.vehicles_maint_brakes)
    MaintenanceType.BEARING -> stringResource(R.string.vehicles_maint_bearing)
    MaintenanceType.TOP_END -> stringResource(R.string.vehicles_maint_top_end)
    MaintenanceType.BOTTOM_END -> stringResource(R.string.vehicles_maint_bottom_end)
    MaintenanceType.ELECTRICAL -> stringResource(R.string.vehicles_maint_electrical)
    MaintenanceType.INSPECTION_TUV -> stringResource(R.string.vehicles_maint_tuv)
    MaintenanceType.GENERAL -> stringResource(R.string.vehicles_maint_general)
    MaintenanceType.OTHER -> stringResource(R.string.vehicles_maint_other)
}
