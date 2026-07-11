package com.simplestsoft.twostrokecalc.ui.components.calculator

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.simplestsoft.twostrokecalc.R
import com.simplestsoft.twostrokecalc.ui.components.AppColors
import kotlinx.coroutines.launch
import com.simplestsoft.twostrokecalc.ui.components.AppOutlinedTextField
import com.simplestsoft.twostrokecalc.ui.components.FormFieldDefaults
import com.simplestsoft.twostrokecalc.ui.components.FormFieldsColumn
import com.simplestsoft.twostrokecalc.ui.pro.LocalCalculatorEditingEnabled
import com.simplestsoft.twostrokecalc.ui.theme.ContainerCornerRadius
import com.simplestsoft.twostrokecalc.ui.util.keyboardAwareScroll

@Composable
fun CalculatorIcon(
    imageVector: ImageVector,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    size: Dp = 32.dp,
) {
    Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        modifier = modifier
            .size(size)
            .then(AppColors.calculatorIconGlowModifier()),
        tint = AppColors.primaryBlue(),
    )
}

@Composable
fun CalculatorScaffold(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.background())
            .keyboardAwareScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        content = content,
    )
}

@Composable
fun CalculatorHeader(
    title: String,
    subtitle: String? = null,
    infoTooltip: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = AppColors.textPrimary(),
            )
            if (infoTooltip != null) {
                CalculatorInfoTooltip(text = infoTooltip)
            }
        }
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.textSecondary(),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalculatorInfoTooltip(text: String) {
    val tooltipState = rememberTooltipState(isPersistent = true)
    val scope = rememberCoroutineScope()
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = {
            PlainTooltip {
                Text(text)
            }
        },
        state = tooltipState,
    ) {
        IconButton(
            onClick = {
                scope.launch {
                    if (tooltipState.isVisible) {
                        tooltipState.dismiss()
                    } else {
                        tooltipState.show()
                    }
                }
            },
            modifier = Modifier.size(32.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = stringResource(R.string.calculator_info_tooltip_cd),
                modifier = Modifier.size(18.dp),
                tint = AppColors.textSecondary(),
            )
        }
    }
}

@Composable
fun CalculatorCollapsibleSection(
    title: String,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(initiallyExpanded) }
    val shape = RoundedCornerShape(ContainerCornerRadius)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(AppColors.cardBorderModifier(shape)),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = AppColors.surface()),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.textPrimary(),
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = AppColors.textSecondary(),
                )
            }
            if (expanded) {
                FormFieldsColumn(modifier = Modifier.padding(top = 12.dp), content = content)
            }
        }
    }
}

@Composable
fun CalculatorSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(ContainerCornerRadius)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(AppColors.cardBorderModifier(shape)),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = AppColors.surface()),
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            if (title.isNotBlank()) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.textPrimary(),
                )
            }
            FormFieldsColumn(
                modifier = Modifier.padding(top = if (title.isNotBlank()) 8.dp else 0.dp),
                content = content,
            )
        }
    }
}

@Composable
fun CalculatorResultCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(ContainerCornerRadius)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(AppColors.cardBorderModifier(shape)),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = AppColors.surface()),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (title.isNotBlank()) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.textPrimary(),
                )
            }
            content()
        }
    }
}

@Composable
fun CalculatorResultRow(
    label: String,
    value: String,
    hint: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.textSecondary(),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.textPrimary(),
        )
        if (hint != null) {
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.textSecondary(),
            )
        }
    }
}

@Composable
fun CalculatorEmptyResultText(message: String) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = AppColors.textSecondary(),
    )
}

@Composable
fun CalculatorInvalidResultText(message: String) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error,
    )
}

@Composable
fun CalculatorPrimaryResultText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = AppColors.primaryBlue(),
    )
}

@Composable
fun CalculatorSecondaryResultText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = AppColors.textPrimary(),
    )
}

@Composable
fun CalculatorResultDivider() {
    HorizontalDivider(color = AppColors.borderSubtle())
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CalculatorFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val editingEnabled = LocalCalculatorEditingEnabled.current
    FilterChip(
        selected = selected,
        onClick = onClick,
        enabled = enabled && editingEnabled,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
            )
        },
        modifier = modifier,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = AppColors.accentSurfaceBackground(),
            selectedLabelColor = AppColors.primaryBlue(),
        ),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CalculatorFilterChipRow(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = { content() },
    )
}

data class CalculatorDropdownOption(
    val key: String,
    val label: String,
)

private const val CalculatorDropdownMinOptions = 3

@Composable
fun CalculatorChoiceField(
    label: String,
    options: List<CalculatorDropdownOption>,
    selectedKey: String,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    enabled: Boolean = true,
) {
    if (options.size >= CalculatorDropdownMinOptions) {
        CalculatorDropdownField(
            label = label,
            options = options,
            selectedKey = selectedKey,
            onOptionSelected = onOptionSelected,
            modifier = modifier,
            supportingText = supportingText,
            enabled = enabled,
        )
    } else {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (label.isNotBlank()) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = AppColors.textPrimary(),
                )
            }
            CalculatorFilterChipRow {
                options.forEach { option ->
                    CalculatorFilterChip(
                        label = option.label,
                        selected = selectedKey == option.key,
                        onClick = { onOptionSelected(option.key) },
                        enabled = enabled,
                    )
                }
            }
            if (supportingText != null) {
                Text(
                    text = supportingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.textSecondary(),
                )
            }
        }
    }
}

@Composable
fun CalculatorDropdownField(
    label: String,
    options: List<CalculatorDropdownOption>,
    selectedKey: String,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    enabled: Boolean = true,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.key == selectedKey }?.label.orEmpty()
    val editingEnabled = LocalCalculatorEditingEnabled.current
    val fieldEnabled = enabled && editingEnabled
    val openMenu = { if (fieldEnabled) expanded = true }
    val closeMenu = { expanded = false }

    Box(modifier = modifier.fillMaxWidth()) {
        AppOutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            label = label,
            readOnly = true,
            enabled = fieldEnabled,
            supportingText = supportingText,
            trailingIcon = {
                IconButton(
                    onClick = openMenu,
                    enabled = fieldEnabled,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = if (expanded) {
                            Icons.Default.ArrowDropUp
                        } else {
                            Icons.Default.ArrowDropDown
                        },
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(end = 40.dp)
                .clickable(
                    enabled = fieldEnabled,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = openMenu,
                ),
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = closeMenu,
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        onOptionSelected(option.key)
                        closeMenu()
                    },
                )
            }
        }
    }
}

@Composable
fun CalculatorInputModeSwitch(
    optionALabel: String,
    optionBLabel: String,
    useOptionA: Boolean,
    onUseOptionAChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    CalculatorFilterChipRow(modifier = modifier) {
        CalculatorFilterChip(
            label = optionALabel,
            selected = useOptionA,
            onClick = { onUseOptionAChange(true) },
        )
        CalculatorFilterChip(
            label = optionBLabel,
            selected = !useOptionA,
            onClick = { onUseOptionAChange(false) },
        )
    }
}

@Composable
fun CalculatorBidirectionalField(
    optionALabel: String,
    optionBLabel: String,
    useOptionA: Boolean,
    onUseOptionAChange: (Boolean) -> Unit,
    value: String,
    onValueChange: (String) -> Unit,
    fieldLabelA: String,
    fieldLabelB: String,
    modifier: Modifier = Modifier,
    suffixA: String? = null,
    suffixB: String? = null,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(FormFieldDefaults.fieldSpacing)) {
        CalculatorInputModeSwitch(
            optionALabel = optionALabel,
            optionBLabel = optionBLabel,
            useOptionA = useOptionA,
            onUseOptionAChange = onUseOptionAChange,
        )
        CalculatorDecimalField(
            value = value,
            onValueChange = onValueChange,
            label = if (useOptionA) fieldLabelA else fieldLabelB,
            suffix = if (useOptionA) suffixA else suffixB,
        )
    }
}

@Composable
fun CalculatorSliderField(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    valueDisplay: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
) {
    val editingEnabled = LocalCalculatorEditingEnabled.current
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.textPrimary(),
                modifier = Modifier.weight(1f),
            )
            Text(
                text = valueDisplay,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.primaryBlue(),
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            enabled = editingEnabled,
            valueRange = valueRange,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = AppColors.primaryBlue(),
                activeTrackColor = AppColors.primaryBlue(),
                inactiveTrackColor = AppColors.borderSubtle(),
            ),
        )
        if (supportingText != null) {
            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.textSecondary(),
            )
        }
    }
}

@Composable
fun CalculatorDecimalField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    suffix: String? = null,
    supportingText: String? = null,
    isActiveInput: Boolean = false,
    enabled: Boolean = true,
) {
    val editingEnabled = LocalCalculatorEditingEnabled.current
    AppOutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        supportingText = supportingText,
        suffix = suffix,
        enabled = enabled && editingEnabled,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        colors = if (isActiveInput) {
            OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AppColors.primaryBlue(),
                unfocusedBorderColor = AppColors.primaryBlue(),
            )
        } else {
            OutlinedTextFieldDefaults.colors()
        },
    )
}
