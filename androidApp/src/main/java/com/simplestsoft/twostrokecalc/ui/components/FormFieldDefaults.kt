package com.simplestsoft.twostrokecalc.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simplestsoft.twostrokecalc.ui.pro.LocalVehicleEditingEnabled

object FormFieldDefaults {
    const val minFieldHeight = 40
    val fieldSpacing = 12.dp

    @Composable
    fun fieldModifier(modifier: Modifier = Modifier, compact: Boolean = true): Modifier =
        if (compact) {
            modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = minFieldHeight.dp)
        } else {
            modifier.fillMaxWidth()
        }

    @Composable
    fun fieldContentPadding(): PaddingValues =
        OutlinedTextFieldDefaults.contentPadding(
            start = 12.dp,
            top = 6.dp,
            end = 12.dp,
            bottom = 6.dp,
        )

    @Composable
    fun Label(text: String) {
        Text(text = text, style = MaterialTheme.typography.bodySmall)
    }

    @Composable
    fun SupportingText(text: String) {
        Text(text = text, style = MaterialTheme.typography.labelSmall)
    }

    @Composable
    fun fieldTextStyle() = MaterialTheme.typography.bodyMedium.copy(lineHeight = 16.sp)
}

@Composable
fun FormFieldsColumn(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(FormFieldDefaults.fieldSpacing),
        content = content,
    )
}

/**
 * Invisible sentinel so the outlined label always stays in the top border cutout,
 * even when the field is empty and unfocused.
 */
private const val LABEL_FLOAT_SENTINEL = "\u200B"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    placeholder: String? = null,
    suffix: String? = null,
    singleLine: Boolean = true,
    minLines: Int = if (singleLine) 1 else 2,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    isError: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null,
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors(),
    compactHeight: Boolean = singleLine,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val vehicleEditingEnabled = LocalVehicleEditingEnabled.current
    val fieldEnabled = enabled && vehicleEditingEnabled
    val textStyle = FormFieldDefaults.fieldTextStyle()
    val textColor = when {
        !fieldEnabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        isError -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurface
    }
    val labelFloatValue = value.ifEmpty { LABEL_FLOAT_SENTINEL }
    // DecorationBox gets a sentinel when empty (to keep the label floated), so Material's
    // built-in placeholder would never show – render it ourselves instead.
    val showPlaceholder = value.isEmpty() && !placeholder.isNullOrEmpty()

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = FormFieldDefaults.fieldModifier(modifier, compact = compactHeight),
        enabled = fieldEnabled,
        readOnly = readOnly || !vehicleEditingEnabled,
        textStyle = textStyle.copy(color = textColor),
        keyboardOptions = keyboardOptions,
        visualTransformation = visualTransformation,
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        interactionSource = interactionSource,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        decorationBox = { innerTextField ->
            OutlinedTextFieldDefaults.DecorationBox(
                value = labelFloatValue,
                visualTransformation = visualTransformation,
                innerTextField = {
                    Box {
                        if (showPlaceholder) {
                            Text(
                                text = placeholder!!,
                                style = textStyle,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            )
                        }
                        innerTextField()
                    }
                },
                placeholder = null,
                label = { FormFieldDefaults.Label(label) },
                leadingIcon = null,
                trailingIcon = trailingIcon,
                prefix = null,
                suffix = suffix?.let { { Text(it, style = MaterialTheme.typography.bodySmall) } },
                supportingText = supportingText?.let { { FormFieldDefaults.SupportingText(it) } },
                singleLine = singleLine,
                enabled = fieldEnabled,
                isError = isError,
                interactionSource = interactionSource,
                colors = colors,
                contentPadding = FormFieldDefaults.fieldContentPadding(),
                container = {
                    OutlinedTextFieldDefaults.Container(
                        enabled = fieldEnabled,
                        isError = isError,
                        interactionSource = interactionSource,
                        colors = colors,
                        shape = OutlinedTextFieldDefaults.shape,
                    )
                },
            )
        },
    )
}
