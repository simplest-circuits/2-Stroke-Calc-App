package com.simplestsoft.twostrokecalc.domain.calculation

enum class CompressionCalculationMode {
    FORWARD,
    TARGET,
    CHANGE,
}

data class CompressionResult(
    val displacementMl: Double,
    val squishBandVolumeMl: Double,
    val compressionRatio: Double,
    val squishAreaPercent: Double,
    val boreStrokeRatio: Double,
)

data class CompressionTargetResult(
    val displacementMl: Double,
    val targetCompressionRatio: Double,
    val totalChamberVolumeMl: Double,
    val domeVolumeMl: Double?,
)

data class CompressionChangeResult(
    val displacementMl: Double,
    val currentCompressionRatio: Double,
    val targetCompressionRatio: Double,
    val currentChamberVolumeMl: Double,
    val targetChamberVolumeMl: Double,
    val volumeToRemoveMl: Double,
    /** Flat milling depth across full bore (head face or cylinder base). */
    val millingDepthMm: Double,
)

object CompressionCalculator {
    private const val PI = 3.1415

    /**
     * Calculates compression ratio and related values from the Excel sheet "Verdichtung":
     * - Displacement = (bore² × π/4 × stroke) / 1000
     * - Squish band volume = (bore² × π/4 × squishBand) / 1000
     * - Compression = (displacement + squishBandVolume + domeVolume) / (domeVolume + squishBandVolume)
     * - Squish area = (bore² − domeDiameter²) / bore² × 100
     * - Bore/stroke ratio = bore / stroke
     */
    fun calculate(
        boreMm: Double,
        domeDiameterMm: Double,
        strokeMm: Double,
        domeVolumeMl: Double,
        squishBandMm: Double,
    ): CompressionResult? {
        if (
            boreMm <= 0.0 ||
            strokeMm <= 0.0 ||
            domeDiameterMm < 0.0 ||
            domeVolumeMl < 0.0 ||
            squishBandMm < 0.0
        ) {
            return null
        }

        val boreArea = boreMm * boreMm * PI / 4.0
        val displacementMl = boreArea * strokeMm / 1000.0
        val squishBandVolumeMl = boreArea * squishBandMm / 1000.0
        val combustionChamberVolume = domeVolumeMl + squishBandVolumeMl
        if (combustionChamberVolume <= 0.0) return null

        val compressionRatio = (displacementMl + squishBandVolumeMl + domeVolumeMl) / combustionChamberVolume
        val squishAreaPercent = (boreArea - domeDiameterMm * domeDiameterMm * PI / 4.0) / boreArea * 100.0
        val boreStrokeRatio = boreMm / strokeMm

        return CompressionResult(
            displacementMl = displacementMl,
            squishBandVolumeMl = squishBandVolumeMl,
            compressionRatio = compressionRatio,
            squishAreaPercent = squishAreaPercent,
            boreStrokeRatio = boreStrokeRatio,
        )
    }

    /**
     * Calculates the combustion chamber volume required for a target compression ratio:
     * - Displacement = (bore² × π/4 × stroke) / 1000
     * - Total chamber volume = displacement / (targetCompression − 1)
     * - Dome volume = total chamber volume − pistonConstant (optional)
     */
    fun calculateTarget(
        boreMm: Double,
        strokeMm: Double,
        targetCompressionRatio: Double,
        pistonConstantMl: Double? = null,
    ): CompressionTargetResult? {
        if (boreMm <= 0.0 || strokeMm <= 0.0 || targetCompressionRatio <= 1.0) {
            return null
        }
        if (pistonConstantMl != null && pistonConstantMl < 0.0) {
            return null
        }

        val displacementMl = displacementFromBoreStroke(boreMm, strokeMm)
        val totalChamberVolumeMl = displacementMl / (targetCompressionRatio - 1.0)
        val domeVolumeMl = pistonConstantMl?.let { totalChamberVolumeMl - it }

        return CompressionTargetResult(
            displacementMl = displacementMl,
            targetCompressionRatio = targetCompressionRatio,
            totalChamberVolumeMl = totalChamberVolumeMl,
            domeVolumeMl = domeVolumeMl,
        )
    }

    /**
     * Calculates how much chamber volume must be removed to change compression:
     * - Current chamber = displacement / (currentCompression − 1)
     * - Target chamber = displacement / (targetCompression − 1)
     * - Volume to remove = current chamber − target chamber
     */
    fun calculateChange(
        boreMm: Double,
        strokeMm: Double,
        currentCompressionRatio: Double,
        targetCompressionRatio: Double,
    ): CompressionChangeResult? {
        if (
            boreMm <= 0.0 ||
            strokeMm <= 0.0 ||
            currentCompressionRatio <= 1.0 ||
            targetCompressionRatio <= 1.0
        ) {
            return null
        }

        val displacementMl = displacementFromBoreStroke(boreMm, strokeMm)
        val currentChamberVolumeMl = displacementMl / (currentCompressionRatio - 1.0)
        val targetChamberVolumeMl = displacementMl / (targetCompressionRatio - 1.0)
        val volumeToRemoveMl = currentChamberVolumeMl - targetChamberVolumeMl
        val boreArea = boreMm * boreMm * PI / 4.0
        val millingDepthMm = kotlin.math.abs(volumeToRemoveMl) * 1000.0 / boreArea

        return CompressionChangeResult(
            displacementMl = displacementMl,
            currentCompressionRatio = currentCompressionRatio,
            targetCompressionRatio = targetCompressionRatio,
            currentChamberVolumeMl = currentChamberVolumeMl,
            targetChamberVolumeMl = targetChamberVolumeMl,
            volumeToRemoveMl = volumeToRemoveMl,
            millingDepthMm = millingDepthMm,
        )
    }

    private fun displacementFromBoreStroke(boreMm: Double, strokeMm: Double): Double {
        val boreArea = boreMm * boreMm * PI / 4.0
        return boreArea * strokeMm / 1000.0
    }
}
