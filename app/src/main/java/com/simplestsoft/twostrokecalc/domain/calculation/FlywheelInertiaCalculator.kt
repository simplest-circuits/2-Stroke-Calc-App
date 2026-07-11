package com.simplestsoft.twostrokecalc.domain.calculation

import kotlin.math.PI
import kotlin.math.pow

enum class FlywheelCylinderType {
    /** Vollzylinder / massive Scheibe. */
    SOLID,
    /** Hohlzylinder / Rohr / Hohlwelle. */
    HOLLOW,
}

enum class FlywheelMaterial {
    STEEL,
    CAST_IRON,
    ALUMINUM,
    CUSTOM,
}

enum class FlywheelCalcMode {
    /** Trägheitsmoment aus Abmessungen und Material. */
    GEOMETRY,
    /** Trägheitsmoment aus Masse und Radien. */
    FROM_MASS,
    /** Erforderliches Trägheitsmoment für Trägheits-Prüfstand. */
    ENGINE_DYNO_DESIGN,
    /** Trägheitsmoment auf andere Drehzahl umrechnen. */
    SPEED_REDUCTION,
    /** Trägheits-Prüfstandslauf simulieren. */
    SIMULATION,
}

enum class FlywheelTorqueModel {
    /** Konstantes Drehmoment (typisch für Dyno-Auslegung). */
    CONSTANT_TORQUE,
    /** Konstante Leistung über den Drehzahlbereich. */
    CONSTANT_POWER,
}

data class FlywheelGeometryInput(
    val cylinderType: FlywheelCylinderType,
    val outerDiameterMm: Double,
    val innerDiameterMm: Double = 0.0,
    val lengthMm: Double,
    val material: FlywheelMaterial,
    val customDensityKgM3: Double = FlywheelInertiaCalculator.DEFAULT_STEEL_DENSITY_KG_M3,
    val rollerRadiusMm: Double? = null,
    val additionalInertiaKgm2: Double = 0.0,
)

data class FlywheelMassInput(
    val cylinderType: FlywheelCylinderType,
    val massKg: Double,
    val outerDiameterMm: Double,
    val innerDiameterMm: Double = 0.0,
    val rollerRadiusMm: Double? = null,
    val additionalInertiaKgm2: Double = 0.0,
)

data class EngineDynoDesignInput(
    val peakPowerPs: Double,
    val peakEngineRpm: Double,
    val runTimeSeconds: Double,
    val engineToFlywheelRatio: Double = 1.0,
    val engineInertiaKgm2: Double = 0.0,
    val rollerRadiusMm: Double? = null,
)

data class SpeedReductionInput(
    val inertiaKgm2: Double,
    val sourceRpm: Double,
    val targetRpm: Double,
)

data class FlywheelInertiaResult(
    val inertiaKgm2: Double,
    val massKg: Double?,
    val equivalentMassKg: Double?,
    val volumeM3: Double? = null,
    val densityKgM3: Double? = null,
)

data class EngineDynoDesignResult(
    val requiredFlywheelInertiaKgm2: Double,
    val totalInertiaKgm2: Double,
    val equivalentMassKg: Double?,
    val estimatedRunTimeSeconds: Double?,
)

data class SpeedReductionResult(
    val reducedInertiaKgm2: Double,
)

data class DynoRunSimulationInput(
    val totalInertiaKgm2: Double,
    val peakPowerPs: Double,
    val peakEngineRpm: Double,
    val startRpm: Double,
    val engineToFlywheelRatio: Double = 1.0,
    val torqueModel: FlywheelTorqueModel = FlywheelTorqueModel.CONSTANT_TORQUE,
    val sampleCount: Int = 60,
)

data class DynoRunSamplePoint(
    val timeSeconds: Double,
    val engineRpm: Double,
    val torqueNm: Double,
    val powerPs: Double,
    val accelerationRpmPerSec: Double,
)

data class DynoRunSimulationResult(
    val samples: List<DynoRunSamplePoint>,
    val runTimeToPeakSeconds: Double,
    val peakTorqueNm: Double,
    val torqueModel: FlywheelTorqueModel,
)

object FlywheelInertiaCalculator {
    const val DEFAULT_STEEL_DENSITY_KG_M3 = 7850.0
    const val CAST_IRON_DENSITY_KG_M3 = 7200.0
    const val ALUMINUM_DENSITY_KG_M3 = 2700.0
    const val PS_TO_WATT = 735.49875
    const val DEFAULT_RUN_TIME_SECONDS = 10.0
    const val DEFAULT_SIMULATION_SAMPLES = 60

    fun density(material: FlywheelMaterial, customDensityKgM3: Double): Double? = when (material) {
        FlywheelMaterial.STEEL -> DEFAULT_STEEL_DENSITY_KG_M3
        FlywheelMaterial.CAST_IRON -> CAST_IRON_DENSITY_KG_M3
        FlywheelMaterial.ALUMINUM -> ALUMINUM_DENSITY_KG_M3
        FlywheelMaterial.CUSTOM -> customDensityKgM3.takeIf { it > 0.0 }
    }

    /**
     * Trägheitsmoment eines Zylinders um die Symmetrieachse.
     * Voll: J = ρ · π/32 · L · D⁴
     * Hohl: J = ρ · π/32 · L · (Dₐ⁴ − Dᵢ⁴)
     */
    fun inertiaFromGeometry(input: FlywheelGeometryInput): FlywheelInertiaResult? {
        val density = density(input.material, input.customDensityKgM3) ?: return null
        if (input.outerDiameterMm <= 0.0 || input.lengthMm <= 0.0) return null

        val outerM = input.outerDiameterMm / 1000.0
        val innerM = input.innerDiameterMm / 1000.0
        val lengthM = input.lengthMm / 1000.0

        if (input.cylinderType == FlywheelCylinderType.HOLLOW) {
            if (innerM <= 0.0 || innerM >= outerM) return null
        } else if (innerM > 0.0) {
            return null
        }

        val outerRadiusM = outerM / 2.0
        val innerRadiusM = innerM / 2.0
        val volume = cylinderVolumeM3(outerRadiusM, innerRadiusM, lengthM, input.cylinderType)
        val mass = volume * density
        val bodyInertia = inertiaFromMassAndRadii(
            massKg = mass,
            outerRadiusM = outerRadiusM,
            innerRadiusM = innerRadiusM,
            cylinderType = input.cylinderType,
        ) ?: return null

        val totalInertia = bodyInertia + input.additionalInertiaKgm2.coerceAtLeast(0.0)
        val equivalentMass = equivalentMassKg(totalInertia, input.rollerRadiusMm)

        return FlywheelInertiaResult(
            inertiaKgm2 = totalInertia,
            massKg = mass,
            equivalentMassKg = equivalentMass,
            volumeM3 = volume,
            densityKgM3 = density,
        )
    }

    /**
     * Voll: J = ½ · m · r²
     * Hohl: J = ½ · m · (rₐ² + rᵢ²)
     */
    fun inertiaFromMass(input: FlywheelMassInput): FlywheelInertiaResult? {
        if (input.massKg <= 0.0 || input.outerDiameterMm <= 0.0) return null

        val outerRadiusM = input.outerDiameterMm / 2000.0
        val innerRadiusM = input.innerDiameterMm / 2000.0

        if (input.cylinderType == FlywheelCylinderType.HOLLOW) {
            if (innerRadiusM <= 0.0 || innerRadiusM >= outerRadiusM) return null
        } else if (innerRadiusM > 0.0) {
            return null
        }

        val bodyInertia = inertiaFromMassAndRadii(
            massKg = input.massKg,
            outerRadiusM = outerRadiusM,
            innerRadiusM = innerRadiusM,
            cylinderType = input.cylinderType,
        ) ?: return null

        val totalInertia = bodyInertia + input.additionalInertiaKgm2.coerceAtLeast(0.0)
        return FlywheelInertiaResult(
            inertiaKgm2 = totalInertia,
            massKg = input.massKg,
            equivalentMassKg = equivalentMassKg(totalInertia, input.rollerRadiusMm),
        )
    }

    /**
     * Erforderliches Schwungrad-Trägheitsmoment für einen Trägheits-Motorprüfstand.
     * Annahme: konstantes Drehmoment über den Drehzahlbereich (PEREK / DTec).
     * J = P · t · i² / ω²  (P in Watt, ω in rad/s, i = Motordrehzahl / Schwungrad-Drehzahl)
     */
    fun designEngineDyno(input: EngineDynoDesignInput): EngineDynoDesignResult? {
        if (
            input.peakPowerPs <= 0.0 ||
            input.peakEngineRpm <= 0.0 ||
            input.runTimeSeconds <= 0.0 ||
            input.engineToFlywheelRatio <= 0.0
        ) {
            return null
        }

        val omegaEngine = rpmToRadPerSec(input.peakEngineRpm)
        val powerWatt = input.peakPowerPs * PS_TO_WATT
        val ratio = input.engineToFlywheelRatio
        val requiredFlywheel = powerWatt * input.runTimeSeconds * ratio.pow(2) / omegaEngine.pow(2)
        val engineInertia = input.engineInertiaKgm2.coerceAtLeast(0.0)
        val totalInertia = requiredFlywheel + engineInertia
        val equivalentMass = equivalentMassKg(totalInertia, input.rollerRadiusMm)
        val estimatedRunTime = if (powerWatt > 0.0) {
            totalInertia * omegaEngine.pow(2) / (powerWatt * ratio.pow(2))
        } else {
            null
        }

        return EngineDynoDesignResult(
            requiredFlywheelInertiaKgm2 = requiredFlywheel,
            totalInertiaKgm2 = totalInertia,
            equivalentMassKg = equivalentMass,
            estimatedRunTimeSeconds = estimatedRunTime,
        )
    }

    /** I_red = I · (n / n_target)² */
    fun reduceInertiaToSpeed(input: SpeedReductionInput): SpeedReductionResult? {
        if (input.inertiaKgm2 <= 0.0 || input.sourceRpm <= 0.0 || input.targetRpm <= 0.0) {
            return null
        }
        val ratio = input.sourceRpm / input.targetRpm
        return SpeedReductionResult(reducedInertiaKgm2 = input.inertiaKgm2 * ratio.pow(2))
    }

    /** Äquivalente Masse am Walzenumfang: m_eq = J / r² */
    fun equivalentMassKg(inertiaKgm2: Double, rollerRadiusMm: Double?): Double? {
        if (inertiaKgm2 <= 0.0 || rollerRadiusMm == null || rollerRadiusMm <= 0.0) return null
        val radiusM = rollerRadiusMm / 1000.0
        return inertiaKgm2 / radiusM.pow(2)
    }

    fun cylinderVolumeM3(
        outerRadiusM: Double,
        innerRadiusM: Double,
        lengthM: Double,
        cylinderType: FlywheelCylinderType,
    ): Double = when (cylinderType) {
        FlywheelCylinderType.SOLID -> PI * outerRadiusM.pow(2) * lengthM
        FlywheelCylinderType.HOLLOW -> PI * (outerRadiusM.pow(2) - innerRadiusM.pow(2)) * lengthM
    }

    fun inertiaFromMassAndRadii(
        massKg: Double,
        outerRadiusM: Double,
        innerRadiusM: Double,
        cylinderType: FlywheelCylinderType,
    ): Double? {
        if (massKg <= 0.0 || outerRadiusM <= 0.0) return null
        return when (cylinderType) {
            FlywheelCylinderType.SOLID -> 0.5 * massKg * outerRadiusM.pow(2)
            FlywheelCylinderType.HOLLOW -> {
                if (innerRadiusM <= 0.0 || innerRadiusM >= outerRadiusM) return null
                0.5 * massKg * (outerRadiusM.pow(2) + innerRadiusM.pow(2))
            }
        }
    }

    fun rpmToRadPerSec(rpm: Double): Double = rpm * 2.0 * PI / 60.0

    fun radPerSecToRpm(radPerSec: Double): Double = radPerSec * 60.0 / (2.0 * PI)

    /**
     * Simuliert einen Trägheits-Prüfstandslauf: τ = I·α, P = τ·ω.
     * Konstantes Drehmoment: T = P_spitze/ω_spitze, linearer Drehzahlverlauf.
     * Konstante Leistung: ω(t) = √(ω₀² + 2·P·t/I).
     */
    fun simulateDynoRun(input: DynoRunSimulationInput): DynoRunSimulationResult? {
        if (
            input.totalInertiaKgm2 <= 0.0 ||
            input.peakPowerPs <= 0.0 ||
            input.peakEngineRpm <= 0.0 ||
            input.startRpm < 0.0 ||
            input.engineToFlywheelRatio <= 0.0 ||
            input.sampleCount < 2
        ) {
            return null
        }

        val omegaStart = rpmToRadPerSec(input.startRpm)
        val omegaPeak = rpmToRadPerSec(input.peakEngineRpm)
        if (omegaPeak <= omegaStart) return null

        val powerWatt = input.peakPowerPs * PS_TO_WATT

        return when (input.torqueModel) {
            FlywheelTorqueModel.CONSTANT_TORQUE -> simulateConstantTorqueRun(
                input = input,
                omegaStart = omegaStart,
                omegaPeak = omegaPeak,
                powerWatt = powerWatt,
            )
            FlywheelTorqueModel.CONSTANT_POWER -> simulateConstantPowerRun(
                input = input,
                omegaStart = omegaStart,
                omegaPeak = omegaPeak,
                powerWatt = powerWatt,
            )
        }
    }

    private fun simulateConstantTorqueRun(
        input: DynoRunSimulationInput,
        omegaStart: Double,
        omegaPeak: Double,
        powerWatt: Double,
    ): DynoRunSimulationResult? {
        val torque = powerWatt / omegaPeak
        val alpha = torque / input.totalInertiaKgm2
        val runTime = (omegaPeak - omegaStart) / alpha
        if (runTime <= 0.0) return null

        val samples = buildSamples(input.sampleCount, runTime) { timeSeconds ->
            val omega = (omegaStart + alpha * timeSeconds).coerceAtMost(omegaPeak)
            val engineRpm = radPerSecToRpm(omega)
            val power = torque * omega
            DynoRunSamplePoint(
                timeSeconds = timeSeconds,
                engineRpm = engineRpm,
                torqueNm = torque,
                powerPs = power / PS_TO_WATT,
                accelerationRpmPerSec = radPerSecToRpm(alpha),
            )
        }

        return DynoRunSimulationResult(
            samples = samples,
            runTimeToPeakSeconds = runTime,
            peakTorqueNm = torque,
            torqueModel = FlywheelTorqueModel.CONSTANT_TORQUE,
        )
    }

    private fun simulateConstantPowerRun(
        input: DynoRunSimulationInput,
        omegaStart: Double,
        omegaPeak: Double,
        powerWatt: Double,
    ): DynoRunSimulationResult? {
        val inertia = input.totalInertiaKgm2
        val runTime = inertia * (omegaPeak.pow(2) - omegaStart.pow(2)) / (2.0 * powerWatt)
        if (runTime <= 0.0) return null

        val samples = buildSamples(input.sampleCount, runTime) { timeSeconds ->
            val omegaSquared = omegaStart.pow(2) + 2.0 * powerWatt * timeSeconds / inertia
            val omega = kotlin.math.sqrt(omegaSquared).coerceAtMost(omegaPeak)
            val torque = powerWatt / omega
            val alpha = torque / inertia
            DynoRunSamplePoint(
                timeSeconds = timeSeconds,
                engineRpm = radPerSecToRpm(omega),
                torqueNm = torque,
                powerPs = input.peakPowerPs,
                accelerationRpmPerSec = radPerSecToRpm(alpha),
            )
        }

        val peakTorque = powerWatt / omegaStart

        return DynoRunSimulationResult(
            samples = samples,
            runTimeToPeakSeconds = runTime,
            peakTorqueNm = peakTorque,
            torqueModel = FlywheelTorqueModel.CONSTANT_POWER,
        )
    }

    private fun buildSamples(
        sampleCount: Int,
        runTimeSeconds: Double,
        sampleAt: (Double) -> DynoRunSamplePoint,
    ): List<DynoRunSamplePoint> = (0..sampleCount).map { index ->
        val timeSeconds = runTimeSeconds * index / sampleCount
        sampleAt(timeSeconds)
    }
}
