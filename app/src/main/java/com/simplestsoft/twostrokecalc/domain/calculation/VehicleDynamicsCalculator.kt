package com.simplestsoft.twostrokecalc.domain.calculation

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

enum class VehicleDynamicsEngineMode {
    POWER,
    TORQUE,
}

enum class VehicleDynamicsPreset {
    MOFA,
    MOKICK,
    ROLLER,
    CROSS,
    CUSTOM,
}

data class VehicleDynamicsPresetValues(
    val massKg: Double,
    val dragCoefficient: Double,
    val frontalAreaM2: Double,
    val rollingResistance: Double,
    val massFactor: Double,
    val powerPs: Double,
    val engineRpm: Double,
    val wheelCircumferenceMm: Double,
)

fun VehicleDynamicsPreset.values(): VehicleDynamicsPresetValues = when (this) {
    VehicleDynamicsPreset.MOFA -> VehicleDynamicsPresetValues(
        massKg = 120.0,
        dragCoefficient = 0.75,
        frontalAreaM2 = 0.45,
        rollingResistance = 0.018,
        massFactor = 1.08,
        powerPs = 3.0,
        engineRpm = 6000.0,
        wheelCircumferenceMm = 1307.0,
    )
    VehicleDynamicsPreset.MOKICK -> VehicleDynamicsPresetValues(
        massKg = 140.0,
        dragCoefficient = 0.70,
        frontalAreaM2 = 0.48,
        rollingResistance = 0.017,
        massFactor = 1.09,
        powerPs = 5.0,
        engineRpm = 7000.0,
        wheelCircumferenceMm = 1307.0,
    )
    VehicleDynamicsPreset.ROLLER -> VehicleDynamicsPresetValues(
        massKg = 180.0,
        dragCoefficient = 0.65,
        frontalAreaM2 = 0.50,
        rollingResistance = 0.015,
        massFactor = 1.10,
        powerPs = 12.0,
        engineRpm = 7500.0,
        wheelCircumferenceMm = 1307.0,
    )
    VehicleDynamicsPreset.CROSS -> VehicleDynamicsPresetValues(
        massKg = 120.0,
        dragCoefficient = 0.80,
        frontalAreaM2 = 0.55,
        rollingResistance = 0.020,
        massFactor = 1.08,
        powerPs = 45.0,
        engineRpm = 9000.0,
        wheelCircumferenceMm = 1400.0,
    )
    VehicleDynamicsPreset.CUSTOM -> VehicleDynamicsPresetValues(
        massKg = 180.0,
        dragCoefficient = 0.65,
        frontalAreaM2 = 0.50,
        rollingResistance = 0.015,
        massFactor = 1.10,
        powerPs = 12.0,
        engineRpm = 7500.0,
        wheelCircumferenceMm = 1307.0,
    )
}

data class VehicleDynamicsGearInput(
    val secondaryPinion: Int,
    val secondaryGear: Int,
)

data class VehicleDynamicsInput(
    val massKg: Double,
    val engineMode: VehicleDynamicsEngineMode,
    val powerPs: Double?,
    val torqueNm: Double?,
    val engineRpm: Double,
    val maxEngineRpm: Double,
    val primaryPinion: Int,
    val primaryGear: Int,
    val stages: List<VehicleDynamicsGearInput>,
    val wheelCircumferenceMm: Double,
    val shiftRpm: Double,
    val drivetrainEfficiency: Double,
    val dragCoefficient: Double,
    val frontalAreaM2: Double,
    val rollingResistance: Double,
    val airDensityKgM3: Double,
    val massFactor: Double,
    val gradientPercent: Double,
    val tractionLimitG: Double?,
    val analysisSpeedKmh: Double,
    val targetSpeedKmh: Double,
    val selectedGearIndex: Int,
    val rpmConstant: Double = GearCalculator.DEFAULT_RPM_CONSTANT,
)

data class ForceBreakdown(
    val driveForceN: Double,
    val dragForceN: Double,
    val rollingForceN: Double,
    val gradientForceN: Double,
    val netForceN: Double,
)

data class PowerBreakdown(
    val rollingPowerW: Double,
    val dragPowerW: Double,
    val gradientPowerW: Double,
    val totalResistancePowerW: Double,
    val accelerationPowerW: Double,
    val totalWheelPowerW: Double,
    val crankPowerW: Double,
    val rollingSharePercent: Double,
    val dragSharePercent: Double,
)

enum class TopSpeedLimit {
    AERODYNAMIC,
    RPM,
}

data class GearDynamicsSnapshot(
    val stageNumber: Int,
    val gearRatio: Double,
    val speedAtEngineRpmKmh: Double,
    val engineRpmAtAnalysisSpeed: Double,
    val forces: ForceBreakdown,
    val accelerationMs2: Double,
    val accelerationG: Double,
    val tractionLimited: Boolean,
    val maxSpeedInGearKmh: Double?,
    val rpmLimitedMaxSpeedKmh: Double,
)

data class SprintSimulationResult(
    val timeSeconds: Double,
    val distanceMeters: Double,
    val finalSpeedKmh: Double,
    val gearShifts: Int,
    val reachedTarget: Boolean,
)

data class AccelerationCurvePoint(
    val speedKmh: Double,
    val accelerationG: Double,
    val gear: Int,
)

data class VehicleDynamicsResult(
    val wheelRadiusM: Double,
    val torqueAtEngineNm: Double,
    val gearSnapshots: List<GearDynamicsSnapshot>,
    val selectedGear: GearDynamicsSnapshot?,
    val topSpeedKmh: Double?,
    val topSpeedGear: Int?,
    val topSpeedLimit: TopSpeedLimit?,
    val sprintToTarget: SprintSimulationResult?,
    val requiredGearRatioForTarget: Double?,
    val powerAtAnalysisSpeed: PowerBreakdown?,
    val accelerationCurves: Map<Int, List<AccelerationCurvePoint>>,
    val quarterMileEstimateSeconds: Double?,
    val quarterMileTrapSpeedKmh: Double?,
)

object VehicleDynamicsCalculator {
    const val GRAVITY_MS2 = 9.81
    const val DEFAULT_AIR_DENSITY_KG_M3 = 1.204
    const val PS_TO_WATT = 735.49875
    const val MPH_TO_KMH = 1.60934
    private const val MIN_SPEED_MS = 0.5
    private const val SIMULATION_DT_S = 0.05
    private const val SIMULATION_MAX_TIME_S = 120.0
    private const val CURVE_STEP_KMH = 2.0

    fun calculate(input: VehicleDynamicsInput): VehicleDynamicsResult? {
        if (!isValidInput(input)) return null

        val gearRatios = resolveGearRatios(input) ?: return null
        val wheelRadiusM = wheelRadiusM(input.wheelCircumferenceMm)
        val referenceTorqueNm = engineTorqueNm(
            mode = input.engineMode,
            powerPs = input.powerPs,
            torqueNm = input.torqueNm,
            rpm = input.engineRpm,
        ) ?: return null

        val gearSnapshots = gearRatios.mapIndexed { index, ratio ->
            buildGearSnapshot(
                input = input,
                stageNumber = index + 1,
                gearRatio = ratio,
                wheelRadiusM = wheelRadiusM,
                referenceTorqueNm = referenceTorqueNm,
                analysisSpeedKmh = input.analysisSpeedKmh,
            )
        }

        val selectedGear = gearSnapshots.getOrNull(input.selectedGearIndex.coerceIn(0, gearSnapshots.lastIndex))

        val topSpeed = resolveTopSpeed(
            input = input,
            gearRatios = gearRatios,
            wheelRadiusM = wheelRadiusM,
            referenceTorqueNm = referenceTorqueNm,
        )

        val sprint = simulateSprint(
            input = input,
            gearRatios = gearRatios,
            wheelRadiusM = wheelRadiusM,
            referenceTorqueNm = referenceTorqueNm,
            targetSpeedKmh = input.targetSpeedKmh,
        )

        val requiredRatio = requiredGearRatio(
            targetSpeedKmh = input.targetSpeedKmh,
            engineRpm = input.maxEngineRpm,
            wheelCircumferenceMm = input.wheelCircumferenceMm,
            rpmConstant = input.rpmConstant,
        )

        val powerBreakdown = selectedGear?.let {
            buildPowerBreakdown(
                input = input,
                speedKmh = input.analysisSpeedKmh,
                accelerationMs2 = it.accelerationMs2,
            )
        }

        val curves = buildAccelerationCurves(
            input = input,
            gearRatios = gearRatios,
            wheelRadiusM = wheelRadiusM,
            referenceTorqueNm = referenceTorqueNm,
        )

        val wheelPowerPs = when (input.engineMode) {
            VehicleDynamicsEngineMode.POWER -> (input.powerPs ?: 0.0) * input.drivetrainEfficiency
            VehicleDynamicsEngineMode.TORQUE -> {
                val powerW = referenceTorqueNm * engineOmegaRadPerSec(input.engineRpm)
                powerW / PS_TO_WATT * input.drivetrainEfficiency
            }
        }

        val quarterMile = estimateQuarterMile(
            massKg = input.massKg,
            wheelPowerPs = wheelPowerPs,
        )

        return VehicleDynamicsResult(
            wheelRadiusM = wheelRadiusM,
            torqueAtEngineNm = referenceTorqueNm,
            gearSnapshots = gearSnapshots,
            selectedGear = selectedGear,
            topSpeedKmh = topSpeed?.first,
            topSpeedGear = topSpeed?.second,
            topSpeedLimit = topSpeed?.third,
            sprintToTarget = sprint,
            requiredGearRatioForTarget = requiredRatio,
            powerAtAnalysisSpeed = powerBreakdown,
            accelerationCurves = curves,
            quarterMileEstimateSeconds = quarterMile?.first,
            quarterMileTrapSpeedKmh = quarterMile?.second,
        )
    }

    fun wheelRadiusM(wheelCircumferenceMm: Double): Double =
        wheelCircumferenceMm / 1000.0 / (2.0 * PI)

    fun engineTorqueNm(
        mode: VehicleDynamicsEngineMode,
        powerPs: Double?,
        torqueNm: Double?,
        rpm: Double,
    ): Double? {
        return when (mode) {
            VehicleDynamicsEngineMode.TORQUE -> torqueNm?.takeIf { it > 0.0 }
            VehicleDynamicsEngineMode.POWER -> {
                val power = powerPs?.takeIf { it > 0.0 } ?: return null
                if (rpm <= 0.0) return null
                (power * PS_TO_WATT) / engineOmegaRadPerSec(rpm)
            }
        }
    }

    fun driveForceN(
        torqueNm: Double,
        gearRatio: Double,
        drivetrainEfficiency: Double,
        wheelRadiusM: Double,
    ): Double {
        if (wheelRadiusM <= 0.0) return 0.0
        return torqueNm * gearRatio * drivetrainEfficiency / wheelRadiusM
    }

    fun dragForceN(
        speedMs: Double,
        dragCoefficient: Double,
        frontalAreaM2: Double,
        airDensityKgM3: Double,
    ): Double = 0.5 * airDensityKgM3 * dragCoefficient * frontalAreaM2 * speedMs * speedMs

    fun rollingForceN(
        massKg: Double,
        rollingResistance: Double,
        gradientPercent: Double,
    ): Double {
        val gradientRad = kotlin.math.atan(gradientPercent / 100.0)
        return rollingResistance * massKg * GRAVITY_MS2 * cos(gradientRad)
    }

    fun gradientForceN(massKg: Double, gradientPercent: Double): Double {
        val gradientRad = kotlin.math.atan(gradientPercent / 100.0)
        return massKg * GRAVITY_MS2 * sin(gradientRad)
    }

    fun resistanceForces(
        speedMs: Double,
        input: VehicleDynamicsInput,
    ): Triple<Double, Double, Double> {
        val drag = dragForceN(
            speedMs = speedMs,
            dragCoefficient = input.dragCoefficient,
            frontalAreaM2 = input.frontalAreaM2,
            airDensityKgM3 = input.airDensityKgM3,
        )
        val rolling = rollingForceN(
            massKg = input.massKg,
            rollingResistance = input.rollingResistance,
            gradientPercent = input.gradientPercent,
        )
        val gradient = gradientForceN(
            massKg = input.massKg,
            gradientPercent = input.gradientPercent,
        )
        return Triple(drag, rolling, gradient)
    }

    fun accelerationMs2(netForceN: Double, massKg: Double, massFactor: Double): Double {
        if (massKg <= 0.0 || massFactor <= 0.0) return 0.0
        return netForceN / (massFactor * massKg)
    }

    fun accelerationG(accelerationMs2: Double): Double = accelerationMs2 / GRAVITY_MS2

    fun requiredGearRatio(
        targetSpeedKmh: Double,
        engineRpm: Double,
        wheelCircumferenceMm: Double,
        rpmConstant: Double,
    ): Double? {
        if (targetSpeedKmh <= 0.0 || engineRpm <= 0.0 || wheelCircumferenceMm <= 0.0 || rpmConstant <= 0.0) {
            return null
        }
        return (engineRpm * wheelCircumferenceMm) / (targetSpeedKmh * rpmConstant)
    }

    fun estimateQuarterMile(massKg: Double, wheelPowerPs: Double): Pair<Double, Double>? {
        if (massKg <= 0.0 || wheelPowerPs <= 0.0) return null
        val etSeconds = 5.825 * (massKg / wheelPowerPs).pow(1.0 / 3.0)
        val trapMph = 234.0 * (wheelPowerPs / massKg).pow(1.0 / 3.0)
        return etSeconds to trapMph * MPH_TO_KMH
    }

    private fun isValidInput(input: VehicleDynamicsInput): Boolean {
        if (input.massKg <= 0.0) return false
        if (input.engineRpm <= 0.0 || input.maxEngineRpm <= 0.0) return false
        if (input.wheelCircumferenceMm <= 0.0) return false
        if (input.shiftRpm <= 0.0) return false
        if (input.drivetrainEfficiency <= 0.0 || input.drivetrainEfficiency > 1.0) return false
        if (input.dragCoefficient <= 0.0 || input.frontalAreaM2 <= 0.0) return false
        if (input.rollingResistance <= 0.0) return false
        if (input.airDensityKgM3 <= 0.0) return false
        if (input.massFactor <= 0.0) return false
        if (input.primaryPinion <= 0 || input.primaryGear <= 0) return false
        if (input.stages.isEmpty() || input.stages.size > GearCalculator.MAX_STAGE_COUNT) return false
        if (input.stages.any { it.secondaryPinion <= 0 || it.secondaryGear <= 0 }) return false
        return when (input.engineMode) {
            VehicleDynamicsEngineMode.POWER -> input.powerPs != null && input.powerPs > 0.0
            VehicleDynamicsEngineMode.TORQUE -> input.torqueNm != null && input.torqueNm > 0.0
        }
    }

    private fun resolveGearRatios(input: VehicleDynamicsInput): List<Double>? =
        input.stages.map { stage ->
            GearCalculator.gearRatio(
                primaryPinion = input.primaryPinion,
                primaryGear = input.primaryGear,
                secondaryPinion = stage.secondaryPinion,
                secondaryGear = stage.secondaryGear,
            )
        }.takeIf { ratios -> ratios.all { it != null } }?.map { it!! }

    private fun buildGearSnapshot(
        input: VehicleDynamicsInput,
        stageNumber: Int,
        gearRatio: Double,
        wheelRadiusM: Double,
        referenceTorqueNm: Double,
        analysisSpeedKmh: Double,
    ): GearDynamicsSnapshot {
        val speedAtRpm = GearCalculator.speedKmh(
            rpm = input.engineRpm,
            wheelCircumferenceMm = input.wheelCircumferenceMm,
            gearRatio = gearRatio,
            rpmConstant = input.rpmConstant,
        )
        val rpmAtAnalysis = if (analysisSpeedKmh <= 0.0) {
            input.engineRpm
        } else {
            GearCalculator.engineRpmAtOutputValue(
                outputValue = analysisSpeedKmh,
                gearRatio = gearRatio,
                outputType = GearOutputType.VEHICLE_SPEED,
                wheelCircumferenceMm = input.wheelCircumferenceMm,
                rpmConstant = input.rpmConstant,
            )
        }
        val torqueAtAnalysis = torqueForRpm(
            input = input,
            referenceTorqueNm = referenceTorqueNm,
            rpm = rpmAtAnalysis.coerceIn(1.0, input.maxEngineRpm),
        )
        val forces = forcesAtSpeed(
            input = input,
            gearRatio = gearRatio,
            wheelRadiusM = wheelRadiusM,
            torqueNm = torqueAtAnalysis,
            speedKmh = analysisSpeedKmh,
        )
        val (accelMs2, tractionLimited) = cappedAcceleration(
            netForceN = forces.netForceN,
            massKg = input.massKg,
            massFactor = input.massFactor,
            tractionLimitG = input.tractionLimitG,
        )
        val rpmLimitedMax = GearCalculator.speedKmh(
            rpm = input.maxEngineRpm,
            wheelCircumferenceMm = input.wheelCircumferenceMm,
            gearRatio = gearRatio,
            rpmConstant = input.rpmConstant,
        )
        val aeroMax = equilibriumSpeedKmh(
            input = input,
            gearRatio = gearRatio,
            wheelRadiusM = wheelRadiusM,
            referenceTorqueNm = referenceTorqueNm,
        )
        val maxInGear = when {
            aeroMax == null -> rpmLimitedMax
            else -> min(aeroMax, rpmLimitedMax)
        }.takeIf { it > 0.0 }

        return GearDynamicsSnapshot(
            stageNumber = stageNumber,
            gearRatio = gearRatio,
            speedAtEngineRpmKmh = speedAtRpm,
            engineRpmAtAnalysisSpeed = rpmAtAnalysis,
            forces = forces,
            accelerationMs2 = accelMs2,
            accelerationG = accelerationG(accelMs2),
            tractionLimited = tractionLimited,
            maxSpeedInGearKmh = maxInGear,
            rpmLimitedMaxSpeedKmh = rpmLimitedMax,
        )
    }

    private fun forcesAtSpeed(
        input: VehicleDynamicsInput,
        gearRatio: Double,
        wheelRadiusM: Double,
        torqueNm: Double,
        speedKmh: Double,
    ): ForceBreakdown {
        val drive = driveForceN(
            torqueNm = torqueNm,
            gearRatio = gearRatio,
            drivetrainEfficiency = input.drivetrainEfficiency,
            wheelRadiusM = wheelRadiusM,
        )
        val speedMs = max(speedKmh, 0.0) / 3.6
        val (drag, rolling, gradient) = resistanceForces(speedMs, input)
        val net = drive - drag - rolling - gradient
        return ForceBreakdown(
            driveForceN = drive,
            dragForceN = drag,
            rollingForceN = rolling,
            gradientForceN = gradient,
            netForceN = net,
        )
    }

    private fun cappedAcceleration(
        netForceN: Double,
        massKg: Double,
        massFactor: Double,
        tractionLimitG: Double?,
    ): Pair<Double, Boolean> {
        val raw = accelerationMs2(netForceN, massKg, massFactor)
        val limit = tractionLimitG?.let { it * GRAVITY_MS2 }
        if (limit != null && raw > 0.0 && raw > limit) {
            return limit to true
        }
        return raw to false
    }

    private fun torqueForRpm(
        input: VehicleDynamicsInput,
        referenceTorqueNm: Double,
        rpm: Double,
    ): Double = when (input.engineMode) {
        VehicleDynamicsEngineMode.TORQUE -> referenceTorqueNm
        VehicleDynamicsEngineMode.POWER -> {
            val powerW = (input.powerPs ?: 0.0) * PS_TO_WATT
            if (rpm <= 0.0) 0.0 else powerW / engineOmegaRadPerSec(rpm)
        }
    }

    private fun equilibriumSpeedKmh(
        input: VehicleDynamicsInput,
        gearRatio: Double,
        wheelRadiusM: Double,
        referenceTorqueNm: Double,
    ): Double? {
        val drive = driveForceN(
            torqueNm = referenceTorqueNm,
            gearRatio = gearRatio,
            drivetrainEfficiency = input.drivetrainEfficiency,
            wheelRadiusM = wheelRadiusM,
        )
        val gradientRad = kotlin.math.atan(input.gradientPercent / 100.0)
        val rolling = input.rollingResistance * input.massKg * GRAVITY_MS2 * cos(gradientRad)
        val gradient = input.massKg * GRAVITY_MS2 * sin(gradientRad)
        val excess = drive - rolling - gradient
        if (excess <= 0.0) return 0.0
        val denominator = 0.5 * input.airDensityKgM3 * input.dragCoefficient * input.frontalAreaM2
        if (denominator <= 0.0) return null
        val speedMs = sqrt(excess / denominator)
        return speedMs * 3.6
    }

    private fun resolveTopSpeed(
        input: VehicleDynamicsInput,
        gearRatios: List<Double>,
        wheelRadiusM: Double,
        referenceTorqueNm: Double,
    ): Triple<Double, Int, TopSpeedLimit>? {
        var bestSpeed = 0.0
        var bestGear = 0
        var bestLimit = TopSpeedLimit.AERODYNAMIC

        gearRatios.forEachIndexed { index, ratio ->
            val rpmMax = GearCalculator.speedKmh(
                rpm = input.maxEngineRpm,
                wheelCircumferenceMm = input.wheelCircumferenceMm,
                gearRatio = ratio,
                rpmConstant = input.rpmConstant,
            )
            val aeroMax = equilibriumSpeedKmh(
                input = input,
                gearRatio = ratio,
                wheelRadiusM = wheelRadiusM,
                referenceTorqueNm = referenceTorqueNm,
            ) ?: rpmMax

            val speed: Double
            val limit: TopSpeedLimit
            if (aeroMax <= rpmMax) {
                speed = aeroMax
                limit = TopSpeedLimit.AERODYNAMIC
            } else {
                speed = rpmMax
                limit = TopSpeedLimit.RPM
            }

            if (speed > bestSpeed) {
                bestSpeed = speed
                bestGear = index + 1
                bestLimit = limit
            }
        }

        return if (bestSpeed > 0.0) Triple(bestSpeed, bestGear, bestLimit) else null
    }

    private fun simulateSprint(
        input: VehicleDynamicsInput,
        gearRatios: List<Double>,
        wheelRadiusM: Double,
        referenceTorqueNm: Double,
        targetSpeedKmh: Double,
    ): SprintSimulationResult? {
        if (targetSpeedKmh <= 0.0) return null

        var speedMs = 0.0
        var distanceM = 0.0
        var timeS = 0.0
        var gearIndex = 0
        var shifts = 0
        val targetMs = targetSpeedKmh / 3.6

        while (timeS < SIMULATION_MAX_TIME_S && speedMs < targetMs) {
            val ratio = gearRatios[gearIndex]
            val speedKmh = speedMs * 3.6
            val rpm = if (speedMs < MIN_SPEED_MS) {
                input.engineRpm
            } else {
                GearCalculator.engineRpmAtOutputValue(
                    outputValue = speedKmh,
                    gearRatio = ratio,
                    outputType = GearOutputType.VEHICLE_SPEED,
                    wheelCircumferenceMm = input.wheelCircumferenceMm,
                    rpmConstant = input.rpmConstant,
                )
            }

            if (rpm >= input.shiftRpm && gearIndex < gearRatios.lastIndex) {
                gearIndex++
                shifts++
                continue
            }

            if (rpm > input.maxEngineRpm) break

            val torque = torqueForRpm(input, referenceTorqueNm, rpm.coerceIn(1.0, input.maxEngineRpm))
            val forces = forcesAtSpeed(
                input = input,
                gearRatio = ratio,
                wheelRadiusM = wheelRadiusM,
                torqueNm = torque,
                speedKmh = speedKmh,
            )
            val (accel, _) = cappedAcceleration(
                netForceN = forces.netForceN,
                massKg = input.massKg,
                massFactor = input.massFactor,
                tractionLimitG = input.tractionLimitG,
            )

            speedMs = max(0.0, speedMs + accel * SIMULATION_DT_S)
            distanceM += speedMs * SIMULATION_DT_S
            timeS += SIMULATION_DT_S
        }

        val finalKmh = speedMs * 3.6
        return SprintSimulationResult(
            timeSeconds = timeS,
            distanceMeters = distanceM,
            finalSpeedKmh = finalKmh,
            gearShifts = shifts,
            reachedTarget = finalKmh >= targetSpeedKmh - 0.5,
        )
    }

    private fun buildPowerBreakdown(
        input: VehicleDynamicsInput,
        speedKmh: Double,
        accelerationMs2: Double,
    ): PowerBreakdown? {
        if (speedKmh < 0.0) return null
        val speedMs = speedKmh / 3.6
        val (drag, rolling, gradient) = resistanceForces(speedMs, input)
        val rollingPower = rolling * speedMs
        val dragPower = drag * speedMs
        val gradientPower = gradient * speedMs
        val resistancePower = rollingPower + dragPower + gradientPower
        val accelPower = input.massFactor * input.massKg * accelerationMs2 * speedMs
        val totalWheel = resistancePower + max(0.0, accelPower)
        val crankPower = totalWheel / input.drivetrainEfficiency
        val total = max(resistancePower, 1.0)
        return PowerBreakdown(
            rollingPowerW = rollingPower,
            dragPowerW = dragPower,
            gradientPowerW = gradientPower,
            totalResistancePowerW = resistancePower,
            accelerationPowerW = max(0.0, accelPower),
            totalWheelPowerW = totalWheel,
            crankPowerW = crankPower,
            rollingSharePercent = rollingPower / total * 100.0,
            dragSharePercent = dragPower / total * 100.0,
        )
    }

    private fun buildAccelerationCurves(
        input: VehicleDynamicsInput,
        gearRatios: List<Double>,
        wheelRadiusM: Double,
        referenceTorqueNm: Double,
    ): Map<Int, List<AccelerationCurvePoint>> {
        val curves = mutableMapOf<Int, List<AccelerationCurvePoint>>()
        gearRatios.forEachIndexed { index, ratio ->
            val rpmMaxSpeed = GearCalculator.speedKmh(
                rpm = input.maxEngineRpm,
                wheelCircumferenceMm = input.wheelCircumferenceMm,
                gearRatio = ratio,
                rpmConstant = input.rpmConstant,
            )
            val points = mutableListOf<AccelerationCurvePoint>()
            var speed = 0.0
            while (speed <= rpmMaxSpeed) {
                val rpm = if (speed <= 0.0) {
                    input.engineRpm
                } else {
                    GearCalculator.engineRpmAtOutputValue(
                        outputValue = speed,
                        gearRatio = ratio,
                        outputType = GearOutputType.VEHICLE_SPEED,
                        wheelCircumferenceMm = input.wheelCircumferenceMm,
                        rpmConstant = input.rpmConstant,
                    )
                }
                val torque = torqueForRpm(
                    input = input,
                    referenceTorqueNm = referenceTorqueNm,
                    rpm = rpm.coerceIn(1.0, input.maxEngineRpm),
                )
                val forces = forcesAtSpeed(
                    input = input,
                    gearRatio = ratio,
                    wheelRadiusM = wheelRadiusM,
                    torqueNm = torque,
                    speedKmh = speed,
                )
                val (accel, _) = cappedAcceleration(
                    netForceN = forces.netForceN,
                    massKg = input.massKg,
                    massFactor = input.massFactor,
                    tractionLimitG = input.tractionLimitG,
                )
                points += AccelerationCurvePoint(
                    speedKmh = speed,
                    accelerationG = accelerationG(accel),
                    gear = index + 1,
                )
                speed += CURVE_STEP_KMH
            }
            curves[index + 1] = points
        }
        return curves
    }

    private fun engineOmegaRadPerSec(rpm: Double): Double = rpm * 2.0 * PI / 60.0
}
