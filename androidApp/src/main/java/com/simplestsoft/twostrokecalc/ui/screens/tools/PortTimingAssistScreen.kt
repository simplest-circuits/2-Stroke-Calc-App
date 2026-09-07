package com.simplestsoft.twostrokecalc.ui.screens.tools

import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.simplestsoft.twostrokecalc.R
import com.simplestsoft.twostrokecalc.domain.model.ToolId
import com.simplestsoft.twostrokecalc.domain.model.ToolMeasurementSession
import com.simplestsoft.twostrokecalc.domain.porttiming.IntakeSystem
import com.simplestsoft.twostrokecalc.domain.tools.porttiming.PortTimingAssistStep
import com.simplestsoft.twostrokecalc.domain.tools.porttiming.PortTimingMeasurementDraft
import com.simplestsoft.twostrokecalc.domain.tools.porttiming.PortTimingMeasurementSession
import com.simplestsoft.twostrokecalc.ui.components.AppColors
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorDecimalField
import com.simplestsoft.twostrokecalc.ui.components.calculator.filterDecimalInput
import com.simplestsoft.twostrokecalc.ui.components.calculator.parseDecimal
import com.simplestsoft.twostrokecalc.ui.porttiming.PortTimingDiagram
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

@Composable
fun PortTimingAssistScreen(
    canSave: Boolean,
    onSaveSession: (ToolMeasurementSession) -> Unit,
    onRequestPro: () -> Unit,
) {
    val context = LocalContext.current
    var session by remember {
        mutableStateOf(
            PortTimingMeasurementSession(
                PortTimingMeasurementDraft(
                    intakeSystem = IntakeSystem.REED_VALVE.name,
                    step = PortTimingAssistStep.IDLE.name,
                ),
            ),
        )
    }
    var strokeText by remember { mutableStateOf("") }
    var connectingRodText by remember { mutableStateOf("") }
    var exhaustText by remember { mutableStateOf("") }
    var transferText by remember { mutableStateOf("") }
    var intakeOpenText by remember { mutableStateOf("") }
    var intakeCloseText by remember { mutableStateOf("") }
    val draft = session.draft
    val result = session.calculate()

    fun updateDraft(block: PortTimingMeasurementDraft.() -> PortTimingMeasurementDraft) {
        session = session.copy(draft = draft.block())
    }

    fun onDecimalFieldChange(
        raw: String,
        setText: (String) -> Unit,
        apply: PortTimingMeasurementDraft.(Double) -> PortTimingMeasurementDraft,
    ) {
        val filtered = raw.filterDecimalInput()
        setText(filtered)
        updateDraft { apply(parseDecimal(filtered) ?: 0.0) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = when (session.step) {
                PortTimingAssistStep.IDLE -> stringResource(R.string.tool_port_start)
                PortTimingAssistStep.MARK_TDC -> stringResource(R.string.tool_port_mark_tdc)
                PortTimingAssistStep.MEASURE_EXHAUST -> stringResource(R.string.tool_port_measure_exhaust)
                PortTimingAssistStep.MEASURE_TRANSFER -> stringResource(R.string.tool_port_measure_transfer)
                PortTimingAssistStep.MEASURE_INTAKE -> stringResource(R.string.tool_port_measure_intake)
                PortTimingAssistStep.REVIEW -> stringResource(R.string.tool_port_review)
            },
            style = MaterialTheme.typography.titleMedium,
            color = AppColors.textPrimary(),
        )

        when (session.step) {
            PortTimingAssistStep.IDLE, PortTimingAssistStep.MARK_TDC -> {
                Text(
                    text = stringResource(R.string.pt_intake_system),
                    style = MaterialTheme.typography.labelLarge,
                    color = AppColors.textSecondary(),
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .selectable(
                                selected = session.intakeSystem == IntakeSystem.REED_VALVE,
                                onClick = { session = session.withIntakeSystem(IntakeSystem.REED_VALVE) },
                                role = Role.RadioButton,
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = session.intakeSystem == IntakeSystem.REED_VALVE,
                            onClick = { session = session.withIntakeSystem(IntakeSystem.REED_VALVE) },
                        )
                        Text(stringResource(R.string.pt_reed_valve))
                    }
                    Row(
                        modifier = Modifier
                            .selectable(
                                selected = session.intakeSystem == IntakeSystem.ROTARY_VALVE,
                                onClick = { session = session.withIntakeSystem(IntakeSystem.ROTARY_VALVE) },
                                role = Role.RadioButton,
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = session.intakeSystem == IntakeSystem.ROTARY_VALVE,
                            onClick = { session = session.withIntakeSystem(IntakeSystem.ROTARY_VALVE) },
                        )
                        Text(stringResource(R.string.pt_rotary_valve))
                    }
                }
                Text(
                    text = if (session.measuresIntake) {
                        stringResource(R.string.tool_port_hint_rotary)
                    } else {
                        stringResource(R.string.tool_port_hint_reed)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.textSecondary(),
                )
                CalculatorDecimalField(
                    value = strokeText,
                    onValueChange = {
                        onDecimalFieldChange(it, { strokeText = it }) { copy(strokeMm = it) }
                    },
                    label = stringResource(R.string.pt_stroke),
                    placeholder = "57",
                )
                CalculatorDecimalField(
                    value = connectingRodText,
                    onValueChange = {
                        onDecimalFieldChange(it, { connectingRodText = it }) { copy(connectingRodMm = it) }
                    },
                    label = stringResource(R.string.pt_connecting_rod),
                    placeholder = "110",
                )
            }
            PortTimingAssistStep.MEASURE_EXHAUST -> {
                CalculatorDecimalField(
                    value = exhaustText,
                    onValueChange = {
                        onDecimalFieldChange(it, { exhaustText = it }) { copy(exhaustPortMm = it) }
                    },
                    label = stringResource(R.string.pt_exhaust),
                    placeholder = "36,2",
                )
            }
            PortTimingAssistStep.MEASURE_TRANSFER -> {
                CalculatorDecimalField(
                    value = transferText,
                    onValueChange = {
                        onDecimalFieldChange(it, { transferText = it }) { copy(transferPortMm = it) }
                    },
                    label = stringResource(R.string.pt_transfer),
                    placeholder = "47,5",
                )
            }
            PortTimingAssistStep.MEASURE_INTAKE -> {
                Text(
                    text = stringResource(R.string.tool_port_hint_rotary),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.textSecondary(),
                )
                CalculatorDecimalField(
                    value = intakeOpenText,
                    onValueChange = {
                        onDecimalFieldChange(it, { intakeOpenText = it }) { copy(intakeOpenMm = it) }
                    },
                    label = stringResource(R.string.pt_intake_open),
                    placeholder = "44,2",
                )
                CalculatorDecimalField(
                    value = intakeCloseText,
                    onValueChange = {
                        onDecimalFieldChange(it, { intakeCloseText = it }) { copy(intakeCloseMm = it) }
                    },
                    label = stringResource(R.string.pt_intake_close),
                    placeholder = "15,9",
                )
            }
            PortTimingAssistStep.REVIEW -> {
                result?.let { r ->
                    val input = session.toInput()
                    Text(
                        text = "${stringResource(R.string.pt_exhaust)}: " +
                            "${r.exhaust?.duration?.let { formatDec(it) } ?: "–"}° / " +
                            "${stringResource(R.string.pt_transfer)}: " +
                            "${r.transfer?.duration?.let { formatDec(it) } ?: "–"}° / " +
                            "${stringResource(R.string.pt_blowdown)}: " +
                            "${r.blowdown?.let { formatDec(it) } ?: "–"}°",
                        color = AppColors.textPrimary(),
                    )
                    if (session.measuresIntake) {
                        Text(
                            text = "${stringResource(R.string.pt_intake_duration)}: " +
                                "${r.intake?.duration?.let { formatDec(it) } ?: "–"}°",
                            color = AppColors.textPrimary(),
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.tool_port_reed_no_intake),
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.textSecondary(),
                        )
                    }
                    if (input != null) {
                        PortTimingDiagram(
                            input = input,
                            result = r,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }

        Button(
            onClick = { session = session.nextStep() },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                if (session.step == PortTimingAssistStep.IDLE) {
                    stringResource(R.string.tool_port_start)
                } else {
                    stringResource(R.string.tool_port_next)
                },
            )
        }
        if (session.step != PortTimingAssistStep.IDLE) {
            OutlinedButton(
                onClick = { session = session.previousStep() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.tool_port_back))
            }
        }
        if (session.step == PortTimingAssistStep.REVIEW) {
            OutlinedButton(
                onClick = {
                    if (!canSave) {
                        onRequestPro()
                        return@OutlinedButton
                    }
                    onSaveSession(
                        ToolMeasurementSession(
                            toolId = ToolId.PORT_TIMING_ASSIST,
                            title = context.getString(R.string.tool_tab_port_timing),
                            resultJson = "{\"stroke\":${draft.strokeMm},\"exhaust\":${draft.exhaustPortMm}," +
                                "\"transfer\":${draft.transferPortMm},\"intakeSystem\":\"${draft.intakeSystem}\"}",
                        ),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.tool_port_save))
            }
            OutlinedButton(
                onClick = {
                    if (!canSave) {
                        onRequestPro()
                        return@OutlinedButton
                    }
                    val path = exportDegreeWheelPdf(context, draft.strokeMm)
                    Toast.makeText(
                        context,
                        context.getString(R.string.tool_port_pdf_saved, path),
                        Toast.LENGTH_LONG,
                    ).show()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.tool_port_export_pdf))
            }
        }
    }
}

private fun formatDec(value: Double): String =
    String.format(Locale.getDefault(), "%.1f", value)

private fun exportDegreeWheelPdf(context: android.content.Context, strokeMm: Double): String {
    val doc = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
    val page = doc.startPage(pageInfo)
    val canvas = page.canvas
    val paint = Paint().apply {
        textSize = 18f
        isAntiAlias = true
    }
    val formattedStroke = String.format(Locale.getDefault(), "%.1f", strokeMm)
    canvas.drawText(context.getString(R.string.tool_port_pdf_title), 40f, 60f, paint)
    canvas.drawText(
        context.getString(R.string.tool_port_pdf_stroke, formattedStroke),
        40f,
        90f,
        paint,
    )
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 2f
    canvas.drawCircle(297f, 420f, 180f, paint)
    paint.style = Paint.Style.FILL
    paint.textSize = 12f
    for (deg in 0 until 360 step 30) {
        val rad = Math.toRadians(deg.toDouble() - 90)
        val x = (297 + 200 * Math.cos(rad)).toFloat()
        val y = (420 + 200 * Math.sin(rad)).toFloat()
        canvas.drawText("$deg°", x, y, paint)
    }
    doc.finishPage(page)
    val dir = File(context.getExternalFilesDir(null), "degree_wheels")
    dir.mkdirs()
    val file = File(dir, "degree_wheel_${System.currentTimeMillis()}.pdf")
    FileOutputStream(file).use { doc.writeTo(it) }
    doc.close()
    return file.absolutePath
}
