package com.simplestsoft.twostrokecalc.ui.screens.calculators

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.simplestsoft.twostrokecalc.R
import com.simplestsoft.twostrokecalc.domain.model.CalculatorId
import com.simplestsoft.twostrokecalc.domain.portarea.ExhaustChordRating
import com.simplestsoft.twostrokecalc.domain.portarea.ExhaustPortInput
import com.simplestsoft.twostrokecalc.domain.portarea.ExhaustPortOptimizer
import com.simplestsoft.twostrokecalc.domain.portarea.ExhaustPortOptimizer.RingProfile
import com.simplestsoft.twostrokecalc.domain.portarea.ExhaustPortShape
import com.simplestsoft.twostrokecalc.domain.portarea.ExhaustPortType
import com.simplestsoft.twostrokecalc.domain.portarea.PortAreaAssessment
import com.simplestsoft.twostrokecalc.domain.portarea.PortAreaCalculator
import com.simplestsoft.twostrokecalc.domain.portarea.PortAreaResult
import com.simplestsoft.twostrokecalc.domain.portarea.PortGeometryLayout
import com.simplestsoft.twostrokecalc.domain.portarea.TransferPortInput
import com.simplestsoft.twostrokecalc.ui.components.AppColors
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorCollapsibleSection
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorDecimalField
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorEmptyResultText
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorChoiceField
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorDropdownOption
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorHeader
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorInvalidResultText
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorPrimaryResultText
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorResultCard
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorResultDivider
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorResultRow
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorScaffold
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorSection
import com.simplestsoft.twostrokecalc.ui.components.calculator.filterDecimalInput
import com.simplestsoft.twostrokecalc.ui.components.calculator.parseDecimal
import com.simplestsoft.twostrokecalc.ui.portarea.ExhaustPortGeometryDiagram
import com.simplestsoft.twostrokecalc.ui.portarea.PortAreaCalculatorViewModel
import com.simplestsoft.twostrokecalc.ui.portarea.formatPortAreaDecimal
import java.util.Locale

@Composable
fun PortAreaCalculatorScreen(
    onNavigateToCalculator: (CalculatorId) -> Unit = {},
    viewModel: PortAreaCalculatorViewModel = hiltViewModel(),
) {
    var boreText by rememberSaveable { mutableStateOf("54") }
    var strokeText by rememberSaveable { mutableStateOf("57") }
    var transferWidthText by rememberSaveable { mutableStateOf("18") }
    var transferHeightText by rememberSaveable { mutableStateOf("28") }
    var transferChannelsText by rememberSaveable { mutableStateOf("2") }
    var transferSideAngleText by rememberSaveable { mutableStateOf("") }
    var transferCorrectionText by rememberSaveable { mutableStateOf("1.0") }
    var transferDurationText by rememberSaveable { mutableStateOf("112") }
    var exhaustTypeName by rememberSaveable { mutableStateOf(ExhaustPortType.SINGLE.name) }
    var exhaustShapeName by rememberSaveable { mutableStateOf(ExhaustPortShape.TRAPEZOID.name) }
    var exhaustHeightText by rememberSaveable { mutableStateOf("20") }
    var exhaustSpanText by rememberSaveable { mutableStateOf("28") }
    var exhaustTopSpanText by rememberSaveable { mutableStateOf("48") }
    var exhaustCornerRadiusText by rememberSaveable { mutableStateOf("5") }
    var exhaustOtCornerRadiusText by rememberSaveable { mutableStateOf("3") }
    var exhaustTopEdgeRadiusText by rememberSaveable { mutableStateOf("0") }
    var exhaustBridgeText by rememberSaveable { mutableStateOf("8") }
    var exhaustBridge2Text by rememberSaveable { mutableStateOf("") }
    var mainBottomSpanText by rememberSaveable { mutableStateOf("24") }
    var mainTopSpanText by rememberSaveable { mutableStateOf("48") }
    var mainShapeName by rememberSaveable { mutableStateOf(ExhaustPortShape.TRAPEZOID.name) }
    var boosterTopSpanText by rememberSaveable { mutableStateOf("10") }
    var boosterHeightText by rememberSaveable { mutableStateOf("10") }
    var boosterBottomSpanText by rememberSaveable { mutableStateOf("4") }
    var leftBoosterBottomSpanText by rememberSaveable { mutableStateOf("4") }
    var rightBoosterBottomSpanText by rememberSaveable { mutableStateOf("4") }
    var boosterCornerRadiusText by rememberSaveable { mutableStateOf("2") }
    var boosterOtCornerRadiusText by rememberSaveable { mutableStateOf("0") }
    var boosterTopEdgeRadiusText by rememberSaveable { mutableStateOf("0") }
    var leftBoosterShapeName by rememberSaveable { mutableStateOf(ExhaustPortShape.TRIANGULAR.name) }
    var rightBoosterShapeName by rememberSaveable { mutableStateOf(ExhaustPortShape.TRIANGULAR.name) }
    var ringProfileName by rememberSaveable { mutableStateOf(RingProfile.SERIES_SPORT.name) }
    var optimizeChordPercent by remember { mutableStateOf<Double?>(null) }
    var optimizeTimeArea by remember { mutableStateOf<Double?>(null) }
    var exhaustDurationText by rememberSaveable { mutableStateOf("166") }
    var timingImported by rememberSaveable { mutableStateOf(false) }

    val exhaustType = ExhaustPortType.entries.firstOrNull { it.name == exhaustTypeName }
        ?: ExhaustPortType.SINGLE
    val exhaustShape = ExhaustPortShape.entries.firstOrNull { it.name == exhaustShapeName }
        ?: ExhaustPortShape.RECTANGULAR
    val leftBoosterShape = ExhaustPortShape.entries.firstOrNull { it.name == leftBoosterShapeName }
        ?: ExhaustPortShape.TRIANGULAR
    val rightBoosterShape = ExhaustPortShape.entries.firstOrNull { it.name == rightBoosterShapeName }
        ?: ExhaustPortShape.TRIANGULAR
    val mainShape = ExhaustPortShape.entries.firstOrNull { it.name == mainShapeName }
        ?: ExhaustPortShape.TRAPEZOID
    val ringProfile = RingProfile.entries.firstOrNull { it.name == ringProfileName }
        ?: RingProfile.SERIES_SPORT
    val isTripleCompound = exhaustType == ExhaustPortType.TRIPLE
    val showUtCornerRadius = isTripleCompound ||
        exhaustShape == ExhaustPortShape.ROUNDED_RECT ||
        exhaustShape == ExhaustPortShape.TRAPEZOID ||
        exhaustShape == ExhaustPortShape.RECTANGULAR
    val showOtCornerRadius = isTripleCompound ||
        exhaustShape != ExhaustPortShape.OVAL
    val showTopEdgeRadius = showOtCornerRadius
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()

    LaunchedEffect(exhaustTypeName) {
        if (exhaustType == ExhaustPortType.TWIN && exhaustShape == ExhaustPortShape.TRIANGULAR) {
            exhaustShapeName = ExhaustPortShape.V_SHAPE.name
        }
        if (exhaustType == ExhaustPortType.SINGLE && exhaustShape == ExhaustPortShape.V_SHAPE) {
            exhaustShapeName = ExhaustPortShape.TRAPEZOID.name
        }
    }

    LaunchedEffect(viewModel.session) {
        val session = viewModel.session
        if (session.hasTimingData() && !timingImported) {
            session.strokeMm?.let { strokeText = formatPortAreaDecimal(it, 1, locale) }
            session.transferDurationDeg?.let {
                transferDurationText = formatPortAreaDecimal(it, 1, locale)
            }
            session.exhaustDurationDeg?.let {
                exhaustDurationText = formatPortAreaDecimal(it, 1, locale)
            }
            timingImported = true
        }
    }

    val bore = parseDecimal(boreText)
    val stroke = parseDecimal(strokeText)
    val transferWidth = parseDecimal(transferWidthText)
    val transferHeight = parseDecimal(transferHeightText)
    val transferChannels = parseDecimal(transferChannelsText)?.toInt()
    val transferSideAngle = parseDecimal(transferSideAngleText)
    val transferCorrection = parseDecimal(transferCorrectionText) ?: 1.0
    val transferDuration = parseDecimal(transferDurationText)
    val exhaustHeight = parseDecimal(exhaustHeightText)
    val exhaustSpan = parseDecimal(exhaustSpanText)
    val exhaustTopSpan = parseDecimal(exhaustTopSpanText)
    val exhaustCornerRadius = parseDecimal(exhaustCornerRadiusText)
    val exhaustOtCornerRadius = parseDecimal(exhaustOtCornerRadiusText)
    val exhaustTopEdgeRadius = parseDecimal(exhaustTopEdgeRadiusText)
    val exhaustBridge = parseDecimal(exhaustBridgeText) ?: 0.0
    val exhaustBridge2 = parseDecimal(exhaustBridge2Text)
    val mainBottomSpan = parseDecimal(mainBottomSpanText)
    val mainTopSpan = parseDecimal(mainTopSpanText)
    val boosterTopSpan = parseDecimal(boosterTopSpanText)
    val boosterHeight = parseDecimal(boosterHeightText)
    val leftBoosterBottomSpan = parseDecimal(leftBoosterBottomSpanText)
    val rightBoosterBottomSpan = parseDecimal(rightBoosterBottomSpanText)
    val boosterBottomSpan = parseDecimal(boosterBottomSpanText)
    val boosterCornerRadius = parseDecimal(boosterCornerRadiusText)
    val boosterOtCornerRadius = parseDecimal(boosterOtCornerRadiusText)
    val boosterTopEdgeRadius = parseDecimal(boosterTopEdgeRadiusText)
    val exhaustDuration = parseDecimal(exhaustDurationText)

    fun applyOptimizedExhaust(result: ExhaustPortOptimizer.Result) {
        val input = result.input
        exhaustTypeName = input.type.name
        exhaustDurationText = formatPortAreaDecimal(input.durationDeg, 1, locale)
        exhaustHeightText = formatPortAreaDecimal(input.portHeightMm, 1, locale)
        exhaustBridgeText = formatPortAreaDecimal(input.bridgeWidthMm, 1, locale)
        exhaustBridge2Text = input.bridgeWidth2Mm?.let { formatPortAreaDecimal(it, 1, locale) } ?: ""
        input.cornerRadiusMm?.let { exhaustCornerRadiusText = formatPortAreaDecimal(it, 1, locale) }
        input.otCornerRadiusMm?.let { exhaustOtCornerRadiusText = formatPortAreaDecimal(it, 1, locale) }
        input.topEdgeRadiusMm?.let { exhaustTopEdgeRadiusText = formatPortAreaDecimal(it, 1, locale) }
        input.boosterCornerRadiusMm?.let { boosterCornerRadiusText = formatPortAreaDecimal(it, 1, locale) }
        input.boosterOtCornerRadiusMm?.let { boosterOtCornerRadiusText = formatPortAreaDecimal(it, 1, locale) }
        input.boosterTopEdgeRadiusMm?.let { boosterTopEdgeRadiusText = formatPortAreaDecimal(it, 1, locale) }
        when (input.type) {
            ExhaustPortType.TRIPLE -> {
                mainBottomSpanText = formatPortAreaDecimal(input.mainBottomSpanMm!!, 1, locale)
                mainTopSpanText = formatPortAreaDecimal(input.mainTopSpanMm!!, 1, locale)
                boosterTopSpanText = formatPortAreaDecimal(input.boosterTopSpanMm!!, 1, locale)
                boosterHeightText = formatPortAreaDecimal(input.boosterHeightMm!!, 1, locale)
                input.boosterBottomSpanMm?.let {
                    leftBoosterBottomSpanText = formatPortAreaDecimal(it, 1, locale)
                    rightBoosterBottomSpanText = formatPortAreaDecimal(it, 1, locale)
                    boosterBottomSpanText = formatPortAreaDecimal(it, 1, locale)
                }
                input.leftBoosterBottomSpanMm?.let {
                    leftBoosterBottomSpanText = formatPortAreaDecimal(it, 1, locale)
                }
                input.rightBoosterBottomSpanMm?.let {
                    rightBoosterBottomSpanText = formatPortAreaDecimal(it, 1, locale)
                }
                val legacyBoosterShape = input.boosterShape ?: ExhaustPortShape.TRIANGULAR
                leftBoosterShapeName = (input.leftBoosterShape ?: legacyBoosterShape).name
                rightBoosterShapeName = (input.rightBoosterShape ?: legacyBoosterShape).name
                mainShapeName = (input.mainShape ?: ExhaustPortShape.TRAPEZOID).name
            }
            else -> {
                exhaustShapeName = input.shape.name
                exhaustSpanText = formatPortAreaDecimal(input.totalSpanMm, 1, locale)
                input.topSpanMm?.let { exhaustTopSpanText = formatPortAreaDecimal(it, 1, locale) }
            }
        }
        optimizeChordPercent = result.chordMetrics.chordPercentOfBore
        optimizeTimeArea = result.portResult.timeAreaMm2Deg
    }

    fun buildExhaustInput(boreValue: Double, strokeValue: Double, duration: Double): ExhaustPortInput {
        if (isTripleCompound) {
            return ExhaustPortInput(
                boreMm = boreValue,
                strokeMm = strokeValue,
                type = exhaustType,
                portHeightMm = exhaustHeight ?: 0.0,
                totalSpanMm = 0.0,
                bridgeWidthMm = exhaustBridge,
                bridgeWidth2Mm = exhaustBridge2?.takeIf { exhaustBridge2Text.isNotBlank() },
                mainBottomSpanMm = mainBottomSpan,
                mainTopSpanMm = mainTopSpan,
                boosterTopSpanMm = boosterTopSpan,
                boosterHeightMm = boosterHeight,
                boosterBottomSpanMm = boosterBottomSpan?.takeIf {
                    leftBoosterShape == ExhaustPortShape.TRAPEZOID &&
                        rightBoosterShape == ExhaustPortShape.TRAPEZOID &&
                        leftBoosterBottomSpanText == rightBoosterBottomSpanText
                },
                leftBoosterBottomSpanMm = leftBoosterBottomSpan?.takeIf {
                    leftBoosterShape == ExhaustPortShape.TRAPEZOID
                },
                rightBoosterBottomSpanMm = rightBoosterBottomSpan?.takeIf {
                    rightBoosterShape == ExhaustPortShape.TRAPEZOID
                },
                leftBoosterShape = leftBoosterShape,
                rightBoosterShape = rightBoosterShape,
                mainShape = mainShape,
                cornerRadiusMm = exhaustCornerRadius?.takeIf {
                    mainShape == ExhaustPortShape.ROUNDED_RECT || (exhaustCornerRadius ?: 0.0) > 0.0
                },
                otCornerRadiusMm = exhaustOtCornerRadius?.takeIf { (it) > 0.0 },
                topEdgeRadiusMm = exhaustTopEdgeRadius?.takeIf { (it) > 0.0 },
                boosterCornerRadiusMm = boosterCornerRadius?.takeIf { (it) > 0.0 },
                boosterOtCornerRadiusMm = boosterOtCornerRadius?.takeIf { (it) > 0.0 },
                boosterTopEdgeRadiusMm = boosterTopEdgeRadius?.takeIf { (it) > 0.0 },
                durationDeg = duration,
            )
        }
        return ExhaustPortInput(
            boreMm = boreValue,
            strokeMm = strokeValue,
            type = exhaustType,
            shape = exhaustShape,
            portHeightMm = exhaustHeight ?: 0.0,
            totalSpanMm = exhaustSpan ?: 0.0,
            topSpanMm = exhaustTopSpan?.takeIf {
                exhaustShape == ExhaustPortShape.TRAPEZOID ||
                    exhaustShape == ExhaustPortShape.TRIANGULAR ||
                    exhaustShape == ExhaustPortShape.V_SHAPE
            },
            bridgeWidthMm = if (exhaustType == ExhaustPortType.SINGLE) 0.0 else exhaustBridge,
            cornerRadiusMm = exhaustCornerRadius?.takeIf {
                exhaustShape == ExhaustPortShape.ROUNDED_RECT || (exhaustCornerRadius ?: 0.0) > 0.0
            },
            otCornerRadiusMm = exhaustOtCornerRadius?.takeIf { (it) > 0.0 },
            topEdgeRadiusMm = exhaustTopEdgeRadius?.takeIf { (it) > 0.0 },
            durationDeg = duration,
        )
    }

    val displacement = if (bore != null && stroke != null && bore > 0.0 && stroke > 0.0) {
        PortAreaCalculator.displacementCc(bore, stroke)
    } else {
        null
    }

    val transferResult = if (
        bore != null && stroke != null &&
        transferWidth != null && transferHeight != null &&
        transferChannels != null && transferDuration != null
    ) {
        PortAreaCalculator.calculateTransfer(
            TransferPortInput(
                boreMm = bore,
                strokeMm = stroke,
                widthMm = transferWidth,
                heightMm = transferHeight,
                channelCount = transferChannels,
                sideAngleDeg = transferSideAngle?.takeIf { transferSideAngleText.isNotBlank() },
                correctionFactor = transferCorrection,
                durationDeg = transferDuration,
            ),
        )
    } else {
        null
    }

    val exhaustLayout = if (exhaustHeight != null && (
            isTripleCompound ||
                (usesTopSpanOnly(exhaustShape) && exhaustTopSpan != null) ||
                (!usesTopSpanOnly(exhaustShape) && exhaustSpan != null)
            )
    ) {
        PortAreaCalculator.calculateExhaustLayout(
            buildExhaustInput(
                boreValue = bore ?: 0.0,
                strokeValue = stroke ?: 0.0,
                duration = exhaustDuration ?: 0.0,
            ),
        )
    } else {
        null
    }

    val exhaustHasInput = bore != null && stroke != null &&
        exhaustHeight != null && exhaustDuration != null &&
        if (isTripleCompound) {
            mainBottomSpan != null && mainTopSpan != null && boosterTopSpan != null &&
                boosterHeight != null &&
                (leftBoosterShape != ExhaustPortShape.TRAPEZOID || leftBoosterBottomSpan != null) &&
                (rightBoosterShape != ExhaustPortShape.TRAPEZOID || rightBoosterBottomSpan != null)
        } else {
            when (exhaustShape) {
                ExhaustPortShape.TRAPEZOID -> exhaustSpan != null && exhaustTopSpan != null
                ExhaustPortShape.TRIANGULAR,
                ExhaustPortShape.V_SHAPE,
                -> exhaustTopSpan != null
                ExhaustPortShape.ROUNDED_RECT -> exhaustSpan != null && exhaustCornerRadius != null
                else -> exhaustSpan != null
            }
        }

    val exhaustResult = if (exhaustHasInput) {
        PortAreaCalculator.calculateExhaust(
            buildExhaustInput(bore!!, stroke!!, exhaustDuration!!),
        )
    } else {
        null
    }

    CalculatorScaffold {
        CalculatorHeader(
            title = stringResource(R.string.port_area_calculator_title),
            subtitle = stringResource(R.string.port_area_calculator_subtitle),
        )

        CalculatorSection(title = stringResource(R.string.port_area_section_engine)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CalculatorDecimalField(
                    value = boreText,
                    onValueChange = { boreText = it.filterDecimalInput() },
                    label = stringResource(R.string.port_area_bore_label),
                    suffix = stringResource(R.string.port_area_unit_mm),
                    modifier = Modifier.weight(1f),
                )
                CalculatorDecimalField(
                    value = strokeText,
                    onValueChange = { strokeText = it.filterDecimalInput() },
                    label = stringResource(R.string.port_area_stroke_label),
                    suffix = stringResource(R.string.port_area_unit_mm),
                    modifier = Modifier.weight(1f),
                )
            }
            if (displacement != null) {
                Text(
                    text = stringResource(
                        R.string.port_area_displacement_value,
                        formatPortAreaDecimal(displacement, 1, locale),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.primaryBlue(),
                )
            }
        }

        CalculatorSection(title = stringResource(R.string.port_area_section_timing_link)) {
            Text(
                text = stringResource(R.string.port_area_timing_link_hint),
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.textSecondary(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(onClick = { onNavigateToCalculator(CalculatorId.TIMING) }) {
                    Text(stringResource(R.string.calculator_link_timing))
                }
                TextButton(
                    onClick = {
                        val session = viewModel.session
                        session.strokeMm?.let { strokeText = formatPortAreaDecimal(it, 1, locale) }
                        session.transferDurationDeg?.let {
                            transferDurationText = formatPortAreaDecimal(it, 1, locale)
                        }
                        session.exhaustDurationDeg?.let {
                            exhaustDurationText = formatPortAreaDecimal(it, 1, locale)
                        }
                    },
                    enabled = viewModel.session.hasTimingData(),
                ) {
                    Text(stringResource(R.string.port_area_import_timing))
                }
            }
        }

        CalculatorSection(title = stringResource(R.string.port_area_section_transfer)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CalculatorDecimalField(
                    value = transferWidthText,
                    onValueChange = { transferWidthText = it.filterDecimalInput() },
                    label = stringResource(R.string.port_area_transfer_width_label),
                    suffix = stringResource(R.string.port_area_unit_mm),
                    modifier = Modifier.weight(1f),
                )
                CalculatorDecimalField(
                    value = transferHeightText,
                    onValueChange = { transferHeightText = it.filterDecimalInput() },
                    label = stringResource(R.string.port_area_transfer_height_label),
                    suffix = stringResource(R.string.port_area_unit_mm),
                    modifier = Modifier.weight(1f),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CalculatorDecimalField(
                    value = transferChannelsText,
                    onValueChange = { transferChannelsText = it.filterDecimalInput() },
                    label = stringResource(R.string.port_area_transfer_channels_label),
                    modifier = Modifier.weight(1f),
                )
                CalculatorDecimalField(
                    value = transferDurationText,
                    onValueChange = { transferDurationText = it.filterDecimalInput() },
                    label = stringResource(R.string.port_area_transfer_duration_label),
                    suffix = stringResource(R.string.port_area_unit_deg),
                    modifier = Modifier.weight(1f),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CalculatorDecimalField(
                    value = transferSideAngleText,
                    onValueChange = { transferSideAngleText = it.filterDecimalInput() },
                    label = stringResource(R.string.port_area_transfer_side_angle_label),
                    suffix = stringResource(R.string.port_area_unit_deg),
                    supportingText = stringResource(R.string.port_area_transfer_side_angle_hint),
                    modifier = Modifier.weight(1f),
                )
                CalculatorDecimalField(
                    value = transferCorrectionText,
                    onValueChange = { transferCorrectionText = it.filterDecimalInput() },
                    label = stringResource(R.string.port_area_transfer_correction_label),
                    supportingText = stringResource(R.string.port_area_transfer_correction_hint),
                    enabled = transferSideAngleText.isBlank(),
                    modifier = Modifier.weight(1f),
                )
            }
        }

        PortAreaResultCard(
            title = stringResource(R.string.port_area_transfer_result_title),
            result = transferResult,
            hasInput = bore != null && stroke != null &&
                transferWidth != null && transferHeight != null &&
                transferChannels != null && transferDuration != null,
            ratioLabel = stringResource(R.string.port_area_transfer_bore_ratio_label),
            locale = locale,
        )

        CalculatorSection(title = stringResource(R.string.port_area_section_exhaust)) {
            val exhaustTypeOptions = ExhaustPortType.entries.map { type ->
                CalculatorDropdownOption(type.name, stringResource(type.labelRes()))
            }
            CalculatorChoiceField(
                label = stringResource(R.string.port_area_exhaust_type_label),
                options = exhaustTypeOptions,
                selectedKey = exhaustType.name,
                onOptionSelected = { key ->
                    val type = ExhaustPortType.entries.first { it.name == key }
                    exhaustTypeName = type.name
                    if (type == ExhaustPortType.TWIN) {
                        exhaustShapeName = ExhaustPortShape.TRAPEZOID.name
                        val (bottomUt, chord) = PortAreaCalculator.wikiTwinDefaultsForBore(
                            boreMm = parseDecimal(boreText) ?: 54.0,
                            bridgeWidthMm = parseDecimal(exhaustBridgeText) ?: 8.0,
                        )
                        exhaustSpanText = formatPortAreaDecimal(bottomUt, 1, locale)
                        exhaustTopSpanText = formatPortAreaDecimal(chord, 1, locale)
                    }
                },
                supportingText = stringResource(exhaustType.descriptionRes()),
            )
            val ringProfileOptions = RingProfile.entries.map { profile ->
                CalculatorDropdownOption(profile.name, stringResource(profile.labelRes()))
            }
            CalculatorChoiceField(
                label = stringResource(R.string.port_area_exhaust_optimize_label),
                options = ringProfileOptions,
                selectedKey = ringProfile.name,
                onOptionSelected = { ringProfileName = it },
                supportingText = stringResource(R.string.port_area_exhaust_optimize_hint),
            )
            TextButton(
                onClick = {
                    val b = parseDecimal(boreText) ?: return@TextButton
                    val s = parseDecimal(strokeText) ?: return@TextButton
                    val h = parseDecimal(exhaustHeightText) ?: return@TextButton
                    val d = parseDecimal(exhaustDurationText) ?: return@TextButton
                    val optimized = ExhaustPortOptimizer.optimize(
                        ExhaustPortOptimizer.Request(
                            boreMm = b,
                            strokeMm = s,
                            durationDeg = d,
                            portHeightMm = h,
                            type = exhaustType,
                            ringProfile = ringProfile,
                            bridgeWidthMm = parseDecimal(exhaustBridgeText) ?: 8.0,
                            bridgeWidth2Mm = parseDecimal(exhaustBridge2Text),
                            topEdgeRadiusMm = parseDecimal(exhaustTopEdgeRadiusText) ?: 3.0,
                            cornerRadiusMm = parseDecimal(exhaustCornerRadiusText) ?: 0.0,
                        ),
                    )
                    optimized?.let { applyOptimizedExhaust(it) }
                },
                enabled = bore != null && stroke != null && exhaustHeight != null && exhaustDuration != null,
            ) {
                Text(stringResource(R.string.port_area_exhaust_optimize_action))
            }
            val appliedChord = optimizeChordPercent
            val appliedTimeArea = optimizeTimeArea
            if (appliedChord != null && appliedTimeArea != null) {
                Text(
                    text = stringResource(
                        R.string.port_area_exhaust_optimize_applied,
                        formatPortAreaDecimal(appliedChord, 0, locale),
                        formatPortAreaDecimal(appliedTimeArea, 0, locale),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.primaryBlue(),
                )
            }
            if (isTripleCompound) {
                Text(
                    text = stringResource(R.string.port_area_exhaust_triple_layout_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.textSecondary(),
                    modifier = Modifier.padding(top = 4.dp),
                )
                Text(
                    text = stringResource(R.string.port_area_exhaust_booster_height_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.textSecondary(),
                )
                val leftBoosterShapeOptions = listOf(
                    ExhaustPortShape.TRIANGULAR,
                    ExhaustPortShape.TRAPEZOID,
                ).map { shape ->
                    CalculatorDropdownOption(shape.name, stringResource(shape.labelRes()))
                }
                CalculatorChoiceField(
                    label = stringResource(R.string.port_area_exhaust_left_booster_shape_label),
                    options = leftBoosterShapeOptions,
                    selectedKey = leftBoosterShape.name,
                    onOptionSelected = { leftBoosterShapeName = it },
                    supportingText = stringResource(
                        if (leftBoosterShape == ExhaustPortShape.TRIANGULAR) {
                            R.string.port_area_exhaust_booster_triangular_desc
                        } else {
                            R.string.port_area_exhaust_booster_trapezoid_desc
                        },
                    ),
                )
                val rightBoosterShapeOptions = listOf(
                    ExhaustPortShape.TRIANGULAR,
                    ExhaustPortShape.TRAPEZOID,
                ).map { shape ->
                    CalculatorDropdownOption(shape.name, stringResource(shape.labelRes()))
                }
                CalculatorChoiceField(
                    label = stringResource(R.string.port_area_exhaust_right_booster_shape_label),
                    options = rightBoosterShapeOptions,
                    selectedKey = rightBoosterShape.name,
                    onOptionSelected = { rightBoosterShapeName = it },
                    supportingText = stringResource(
                        if (rightBoosterShape == ExhaustPortShape.TRIANGULAR) {
                            R.string.port_area_exhaust_booster_triangular_desc
                        } else {
                            R.string.port_area_exhaust_booster_trapezoid_desc
                        },
                    ),
                )
                val mainShapeOptions = listOf(
                    ExhaustPortShape.TRAPEZOID,
                    ExhaustPortShape.ROUNDED_RECT,
                ).map { shape ->
                    CalculatorDropdownOption(shape.name, stringResource(shape.labelRes()))
                }
                CalculatorChoiceField(
                    label = stringResource(R.string.port_area_exhaust_main_shape_label),
                    options = mainShapeOptions,
                    selectedKey = mainShape.name,
                    onOptionSelected = { mainShapeName = it },
                )
            } else {
                val exhaustShapeOptions = exhaustShapesForType(exhaustType).map { shape ->
                    CalculatorDropdownOption(shape.name, stringResource(shape.labelRes()))
                }
                CalculatorChoiceField(
                    label = stringResource(R.string.port_area_exhaust_shape_label),
                    options = exhaustShapeOptions,
                    selectedKey = exhaustShape.name,
                    onOptionSelected = { exhaustShapeName = it },
                    supportingText = stringResource(exhaustShape.descriptionRes()),
                )
            }
            Text(
                text = stringResource(R.string.port_area_exhaust_orientation_hint),
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.textSecondary(),
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                text = stringResource(R.string.port_area_exhaust_shape_time_area_hint),
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.textSecondary(),
            )
            CalculatorDecimalField(
                value = exhaustHeightText,
                onValueChange = { exhaustHeightText = it.filterDecimalInput() },
                label = stringResource(
                    if (isTripleCompound) {
                        R.string.port_area_exhaust_main_height_label
                    } else {
                        R.string.port_area_exhaust_height_label
                    },
                ),
                suffix = stringResource(R.string.port_area_unit_mm),
                supportingText = if (isTripleCompound) {
                    stringResource(R.string.port_area_exhaust_main_height_hint)
                } else {
                    null
                },
            )
            if (isTripleCompound) {
                Text(
                    text = stringResource(R.string.port_area_exhaust_main_port_label),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.textPrimary(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CalculatorDecimalField(
                        value = mainBottomSpanText,
                        onValueChange = { mainBottomSpanText = it.filterDecimalInput() },
                        label = stringResource(R.string.port_area_exhaust_main_bottom_label),
                        suffix = stringResource(R.string.port_area_unit_mm),
                        modifier = Modifier.weight(1f),
                    )
                    CalculatorDecimalField(
                        value = mainTopSpanText,
                        onValueChange = { mainTopSpanText = it.filterDecimalInput() },
                        label = stringResource(R.string.port_area_exhaust_main_top_label),
                        suffix = stringResource(R.string.port_area_unit_mm),
                        supportingText = stringResource(R.string.port_area_exhaust_chord_hint),
                        modifier = Modifier.weight(1f),
                    )
                }
                Text(
                    text = stringResource(R.string.port_area_exhaust_booster_port_label),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.textPrimary(),
                )
                CalculatorDecimalField(
                    value = boosterTopSpanText,
                    onValueChange = { boosterTopSpanText = it.filterDecimalInput() },
                    label = stringResource(R.string.port_area_exhaust_booster_top_label),
                    suffix = stringResource(R.string.port_area_unit_mm),
                    supportingText = stringResource(R.string.port_area_exhaust_booster_top_hint),
                )
                CalculatorDecimalField(
                    value = boosterHeightText,
                    onValueChange = { boosterHeightText = it.filterDecimalInput() },
                    label = stringResource(R.string.port_area_exhaust_booster_height_label),
                    suffix = stringResource(R.string.port_area_unit_mm),
                    supportingText = stringResource(R.string.port_area_exhaust_booster_height_field_hint),
                )
                if (leftBoosterShape == ExhaustPortShape.TRAPEZOID ||
                    rightBoosterShape == ExhaustPortShape.TRAPEZOID
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (leftBoosterShape == ExhaustPortShape.TRAPEZOID) {
                            CalculatorDecimalField(
                                value = leftBoosterBottomSpanText,
                                onValueChange = { leftBoosterBottomSpanText = it.filterDecimalInput() },
                                label = stringResource(R.string.port_area_exhaust_left_booster_bottom_label),
                                suffix = stringResource(R.string.port_area_unit_mm),
                                supportingText = stringResource(R.string.port_area_exhaust_booster_bottom_hint),
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (rightBoosterShape == ExhaustPortShape.TRAPEZOID) {
                            CalculatorDecimalField(
                                value = rightBoosterBottomSpanText,
                                onValueChange = { rightBoosterBottomSpanText = it.filterDecimalInput() },
                                label = stringResource(R.string.port_area_exhaust_right_booster_bottom_label),
                                suffix = stringResource(R.string.port_area_unit_mm),
                                supportingText = stringResource(R.string.port_area_exhaust_booster_bottom_hint),
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            } else if (usesTopSpanOnly(exhaustShape)) {
                CalculatorDecimalField(
                    value = exhaustTopSpanText,
                    onValueChange = { exhaustTopSpanText = it.filterDecimalInput() },
                    label = stringResource(R.string.port_area_exhaust_top_span_label),
                    suffix = stringResource(R.string.port_area_unit_mm),
                    supportingText = stringResource(
                        if (exhaustShape == ExhaustPortShape.V_SHAPE) {
                            R.string.port_area_exhaust_vshape_top_hint
                        } else {
                            R.string.port_area_exhaust_triangle_top_hint
                        },
                    ),
                )
            } else {
                CalculatorDecimalField(
                    value = exhaustSpanText,
                    onValueChange = { exhaustSpanText = it.filterDecimalInput() },
                    label = stringResource(
                        if (exhaustShape == ExhaustPortShape.TRAPEZOID) {
                            R.string.port_area_exhaust_ut_width_label
                        } else {
                            R.string.port_area_exhaust_span_label
                        },
                    ),
                    suffix = stringResource(R.string.port_area_unit_mm),
                    supportingText = stringResource(
                        if (exhaustShape == ExhaustPortShape.TRAPEZOID) {
                            R.string.port_area_exhaust_ut_width_hint
                        } else {
                            R.string.port_area_exhaust_span_hint
                        },
                    ),
                )
                if (exhaustShape == ExhaustPortShape.TRAPEZOID) {
                    CalculatorDecimalField(
                        value = exhaustTopSpanText,
                        onValueChange = { exhaustTopSpanText = it.filterDecimalInput() },
                        label = stringResource(R.string.port_area_exhaust_chord_label),
                        suffix = stringResource(R.string.port_area_unit_mm),
                        supportingText = stringResource(R.string.port_area_exhaust_chord_hint),
                    )
                }
            }
            if (exhaustType != ExhaustPortType.SINGLE) {
                CalculatorDecimalField(
                    value = exhaustBridgeText,
                    onValueChange = { exhaustBridgeText = it.filterDecimalInput() },
                    label = stringResource(
                        if (isTripleCompound) {
                            R.string.port_area_exhaust_bridge_left_label
                        } else {
                            R.string.port_area_exhaust_bridge_label
                        },
                    ),
                    suffix = stringResource(R.string.port_area_unit_mm),
                    supportingText = stringResource(R.string.port_area_exhaust_bridge_hint),
                )
            }
            if (isTripleCompound) {
                CalculatorDecimalField(
                    value = exhaustBridge2Text,
                    onValueChange = { exhaustBridge2Text = it.filterDecimalInput() },
                    label = stringResource(R.string.port_area_exhaust_bridge_right_label),
                    suffix = stringResource(R.string.port_area_unit_mm),
                    supportingText = stringResource(R.string.port_area_exhaust_bridge_right_hint),
                )
            }
            if (showTopEdgeRadius) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (showUtCornerRadius) {
                        CalculatorDecimalField(
                            value = exhaustCornerRadiusText,
                            onValueChange = { exhaustCornerRadiusText = it.filterDecimalInput() },
                            label = stringResource(
                                if (isTripleCompound) {
                                    R.string.port_area_exhaust_main_ut_corner_radius_label
                                } else {
                                    R.string.port_area_exhaust_ut_corner_radius_label
                                },
                            ),
                            suffix = stringResource(R.string.port_area_unit_mm),
                            supportingText = stringResource(R.string.port_area_exhaust_ut_corner_radius_hint),
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (showOtCornerRadius) {
                        CalculatorDecimalField(
                            value = exhaustOtCornerRadiusText,
                            onValueChange = { exhaustOtCornerRadiusText = it.filterDecimalInput() },
                            label = stringResource(
                                if (isTripleCompound) {
                                    R.string.port_area_exhaust_main_ot_corner_radius_label
                                } else {
                                    R.string.port_area_exhaust_ot_corner_radius_label
                                },
                            ),
                            suffix = stringResource(R.string.port_area_unit_mm),
                            supportingText = stringResource(R.string.port_area_exhaust_ot_corner_radius_hint),
                            modifier = Modifier.weight(1f),
                        )
                    }
                    CalculatorDecimalField(
                        value = exhaustTopEdgeRadiusText,
                        onValueChange = { exhaustTopEdgeRadiusText = it.filterDecimalInput() },
                        label = stringResource(
                            if (isTripleCompound) {
                                R.string.port_area_exhaust_main_top_edge_radius_label
                            } else {
                                R.string.port_area_exhaust_top_edge_radius_label
                            },
                        ),
                        suffix = stringResource(R.string.port_area_unit_mm),
                        supportingText = stringResource(R.string.port_area_exhaust_top_edge_radius_hint),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            if (isTripleCompound) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CalculatorDecimalField(
                        value = boosterCornerRadiusText,
                        onValueChange = { boosterCornerRadiusText = it.filterDecimalInput() },
                        label = stringResource(R.string.port_area_exhaust_booster_ut_corner_radius_label),
                        suffix = stringResource(R.string.port_area_unit_mm),
                        modifier = Modifier.weight(1f),
                    )
                    CalculatorDecimalField(
                        value = boosterOtCornerRadiusText,
                        onValueChange = { boosterOtCornerRadiusText = it.filterDecimalInput() },
                        label = stringResource(R.string.port_area_exhaust_booster_ot_corner_radius_label),
                        suffix = stringResource(R.string.port_area_unit_mm),
                        modifier = Modifier.weight(1f),
                    )
                    CalculatorDecimalField(
                        value = boosterTopEdgeRadiusText,
                        onValueChange = { boosterTopEdgeRadiusText = it.filterDecimalInput() },
                        label = stringResource(R.string.port_area_exhaust_booster_top_edge_radius_label),
                        suffix = stringResource(R.string.port_area_unit_mm),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            CalculatorDecimalField(
                value = exhaustDurationText,
                onValueChange = { exhaustDurationText = it.filterDecimalInput() },
                label = stringResource(R.string.port_area_exhaust_duration_label),
                suffix = stringResource(R.string.port_area_unit_deg),
            )
        }

        CalculatorSection(title = stringResource(R.string.port_area_section_exhaust_diagram)) {
            if (exhaustLayout != null) {
                ExhaustPortGeometryDiagram(
                    layout = exhaustLayout,
                    locale = locale,
                )
            } else {
                CalculatorEmptyResultText(stringResource(R.string.port_area_exhaust_diagram_empty))
            }
        }

        PortAreaResultCard(
            title = stringResource(R.string.port_area_exhaust_result_title),
            result = exhaustResult,
            layout = exhaustLayout,
            boreMm = bore,
            hasInput = exhaustHasInput,
            ratioLabel = stringResource(R.string.port_area_exhaust_bore_ratio_label),
            locale = locale,
        )

        CalculatorCollapsibleSection(
            title = stringResource(R.string.port_area_exhaust_wiki_section),
            initiallyExpanded = false,
        ) {
            ExhaustWikiHelpBlock(
                title = stringResource(R.string.port_area_exhaust_wiki_theory_title),
                body = stringResource(R.string.port_area_exhaust_wiki_theory_body),
            )
            ExhaustWikiHelpBlock(
                title = stringResource(R.string.port_area_exhaust_wiki_shapes_title),
                body = stringResource(R.string.port_area_exhaust_wiki_shapes_body),
            )
            ExhaustWikiHelpBlock(
                title = stringResource(R.string.port_area_exhaust_wiki_twin_title),
                body = stringResource(R.string.port_area_exhaust_wiki_twin_body),
            )
            ExhaustWikiHelpBlock(
                title = stringResource(R.string.port_area_exhaust_wiki_durability_title),
                body = stringResource(R.string.port_area_exhaust_wiki_durability_body),
            )
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun ExhaustWikiHelpBlock(title: String, body: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = AppColors.textPrimary(),
    )
    Text(
        text = body,
        style = MaterialTheme.typography.bodySmall,
        color = AppColors.textSecondary(),
        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
    )
}

@Composable
private fun PortAreaResultCard(
    title: String,
    result: PortAreaResult?,
    hasInput: Boolean,
    ratioLabel: String,
    locale: Locale,
    layout: PortGeometryLayout? = null,
    boreMm: Double? = null,
) {
    val chordMetrics = if (layout != null && boreMm != null && boreMm > 0.0) {
        PortAreaCalculator.exhaustChordMetrics(boreMm, layout)
    } else {
        null
    }
    val chordRating = chordMetrics?.let { PortAreaCalculator.rateChordPercent(it.chordPercentOfBore) }

    CalculatorResultCard(title = title) {
        when {
            !hasInput -> CalculatorEmptyResultText(stringResource(R.string.port_area_result_empty))
            result == null -> CalculatorInvalidResultText(stringResource(R.string.port_area_result_invalid))
            else -> {
                CalculatorPrimaryResultText(
                    text = stringResource(
                        R.string.port_area_result_time_area,
                        formatPortAreaDecimal(result.timeAreaMm2Deg, 0, locale),
                    ),
                )
                Text(
                    text = stringResource(assessmentRes(result.assessment)),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = assessmentColor(result.assessment),
                )
                CalculatorResultDivider()
                CalculatorResultRow(
                    label = stringResource(R.string.port_area_result_area),
                    value = stringResource(
                        R.string.port_area_result_area_value,
                        formatPortAreaDecimal(result.areaMm2, 0, locale),
                    ),
                )
                CalculatorResultRow(
                    label = ratioLabel,
                    value = formatPortAreaDecimal(result.areaToBoreRatio, 3, locale),
                )
                CalculatorResultRow(
                    label = stringResource(R.string.port_area_result_ta_per_cc),
                    value = stringResource(
                        R.string.port_area_result_ta_per_cc_value,
                        formatPortAreaDecimal(result.timeAreaPerCc, 0, locale),
                    ),
                    hint = stringResource(R.string.port_area_result_ta_per_cc_hint),
                )
                CalculatorResultRow(
                    label = stringResource(R.string.port_area_result_displacement),
                    value = stringResource(
                        R.string.port_area_result_displacement_value,
                        formatPortAreaDecimal(result.displacementCc, 1, locale),
                    ),
                )
                if (chordMetrics != null) {
                    CalculatorResultDivider()
                    CalculatorResultRow(
                        label = stringResource(R.string.port_area_exhaust_chord_label),
                        value = stringResource(
                            R.string.port_area_exhaust_chord_value,
                            formatPortAreaDecimal(chordMetrics.topChordMm, 1, locale),
                            formatPortAreaDecimal(chordMetrics.chordPercentOfBore, 0, locale),
                        ),
                        hint = stringResource(R.string.port_area_exhaust_chord_percent_hint),
                    )
                    CalculatorResultRow(
                        label = stringResource(R.string.port_area_exhaust_ut_width_label),
                        value = stringResource(
                            R.string.port_area_exhaust_ut_width_value,
                            formatPortAreaDecimal(chordMetrics.bottomWidthUtMm, 1, locale),
                        ),
                        hint = stringResource(R.string.port_area_exhaust_ut_width_result_hint),
                    )
                    chordRating?.let { rating ->
                        Text(
                            text = stringResource(chordRatingRes(rating)),
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.textSecondary(),
                        )
                    }
                }
            }
        }
    }
}

private fun chordRatingRes(rating: ExhaustChordRating): Int = when (rating) {
    ExhaustChordRating.CONSERVATIVE -> R.string.port_area_exhaust_chord_rating_conservative
    ExhaustChordRating.EVERYDAY -> R.string.port_area_exhaust_chord_rating_everyday
    ExhaustChordRating.SERIES_SPORT -> R.string.port_area_exhaust_chord_rating_series
    ExhaustChordRating.TUNER -> R.string.port_area_exhaust_chord_rating_tuner
}

private fun usesTopSpanOnly(shape: ExhaustPortShape): Boolean =
    shape == ExhaustPortShape.TRIANGULAR || shape == ExhaustPortShape.V_SHAPE

private fun exhaustShapesForType(type: ExhaustPortType): List<ExhaustPortShape> =
    ExhaustPortShape.entries.filter { shape ->
        when (shape) {
            ExhaustPortShape.TRIANGULAR -> false
            ExhaustPortShape.V_SHAPE -> type == ExhaustPortType.TWIN
            else -> true
        }
    }

private fun ExhaustPortType.labelRes(): Int = when (this) {
    ExhaustPortType.SINGLE -> R.string.port_area_exhaust_type_single
    ExhaustPortType.TWIN -> R.string.port_area_exhaust_type_twin
    ExhaustPortType.TRIPLE -> R.string.port_area_exhaust_type_triple
}

private fun ExhaustPortType.descriptionRes(): Int = when (this) {
    ExhaustPortType.SINGLE -> R.string.port_area_exhaust_type_single_desc
    ExhaustPortType.TWIN -> R.string.port_area_exhaust_type_twin_desc
    ExhaustPortType.TRIPLE -> R.string.port_area_exhaust_type_triple_desc
}

private fun ExhaustPortShape.labelRes(): Int = when (this) {
    ExhaustPortShape.RECTANGULAR -> R.string.port_area_exhaust_shape_rectangular
    ExhaustPortShape.ROUNDED_RECT -> R.string.port_area_exhaust_shape_rounded
    ExhaustPortShape.OVAL -> R.string.port_area_exhaust_shape_oval
    ExhaustPortShape.TRAPEZOID -> R.string.port_area_exhaust_shape_trapezoid
    ExhaustPortShape.TRIANGULAR -> R.string.port_area_exhaust_shape_triangular
    ExhaustPortShape.V_SHAPE -> R.string.port_area_exhaust_shape_vshape
}

private fun ExhaustPortShape.descriptionRes(): Int = when (this) {
    ExhaustPortShape.RECTANGULAR -> R.string.port_area_exhaust_shape_rectangular_desc
    ExhaustPortShape.ROUNDED_RECT -> R.string.port_area_exhaust_shape_rounded_desc
    ExhaustPortShape.OVAL -> R.string.port_area_exhaust_shape_oval_desc
    ExhaustPortShape.TRAPEZOID -> R.string.port_area_exhaust_shape_trapezoid_desc
    ExhaustPortShape.TRIANGULAR -> R.string.port_area_exhaust_shape_triangular_desc
    ExhaustPortShape.V_SHAPE -> R.string.port_area_exhaust_shape_vshape_desc
}

private fun RingProfile.labelRes(): Int = when (this) {
    RingProfile.CAST -> R.string.port_area_exhaust_ring_cast
    RingProfile.CHROME -> R.string.port_area_exhaust_ring_chrome
    RingProfile.SERIES_SPORT -> R.string.port_area_exhaust_ring_series
    RingProfile.TUNER -> R.string.port_area_exhaust_ring_tuner
}

private fun assessmentRes(assessment: PortAreaAssessment): Int = when (assessment) {
    PortAreaAssessment.CRITICAL_LOW -> R.string.port_area_assessment_critical
    PortAreaAssessment.SERIES -> R.string.port_area_assessment_series
    PortAreaAssessment.SPORTY -> R.string.port_area_assessment_sporty
    PortAreaAssessment.AGGRESSIVE -> R.string.port_area_assessment_aggressive
}

@Composable
private fun assessmentColor(assessment: PortAreaAssessment) = when (assessment) {
    PortAreaAssessment.CRITICAL_LOW -> MaterialTheme.colorScheme.error
    PortAreaAssessment.SERIES -> AppColors.textPrimary()
    PortAreaAssessment.SPORTY -> AppColors.primaryBlue()
    PortAreaAssessment.AGGRESSIVE -> AppColors.primaryBlue()
}
