package com.simplestsoft.twostrokecalc.ui.screens.vehicle

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.simplestsoft.twostrokecalc.R
import com.simplestsoft.twostrokecalc.domain.model.VehicleAttachment
import com.simplestsoft.twostrokecalc.domain.model.VehicleAttachmentCategory
import com.simplestsoft.twostrokecalc.ui.components.AppColors
import com.simplestsoft.twostrokecalc.ui.components.FormFieldDefaults
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorChoiceField
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorDropdownOption
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorSection
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@Composable
fun VehicleAttachmentsTabContent(
    attachments: List<VehicleAttachment>,
    onAddAttachments: (List<Uri>) -> Unit,
    onRemoveAttachment: (String) -> Unit,
    onUpdateAttachment: (VehicleAttachment) -> Unit,
    onOpenAttachment: (VehicleAttachment) -> Boolean,
) {
    val context = LocalContext.current
    var pendingDeleteId by rememberSaveable { mutableStateOf<String?>(null) }
    var editingAttachment by rememberSaveable { mutableStateOf<VehicleAttachment?>(null) }

    val documentPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris ->
        if (uris.isNotEmpty()) {
            onAddAttachments(uris)
        }
    }

    pendingDeleteId?.let { attachmentId ->
        AlertDialog(
            onDismissRequest = { pendingDeleteId = null },
            title = { Text(stringResource(R.string.vehicles_attachment_delete_title)) },
            text = { Text(stringResource(R.string.vehicles_attachment_delete_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRemoveAttachment(attachmentId)
                        pendingDeleteId = null
                    },
                ) {
                    Text(
                        stringResource(R.string.vehicles_delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteId = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    editingAttachment?.let { attachment ->
        AttachmentEditDialog(
            attachment = attachment,
            onDismiss = { editingAttachment = null },
            onConfirm = { updated ->
                onUpdateAttachment(updated)
                editingAttachment = null
            },
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = stringResource(R.string.vehicles_attachments_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.textSecondary(),
        )

        Button(
            onClick = {
                documentPicker.launch(
                    arrayOf(
                        "application/pdf",
                        "image/*",
                        "application/msword",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                        "text/plain",
                    ),
                )
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.vehicles_attachment_add))
        }

        if (attachments.isEmpty()) {
            CalculatorSection(title = stringResource(R.string.vehicles_attachments_empty_title)) {
                Text(
                    text = stringResource(R.string.vehicles_attachments_empty_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.textSecondary(),
                )
            }
        } else {
            attachments.sortedByDescending { it.addedAtMs }.forEach { attachment ->
                AttachmentListItem(
                    attachment = attachment,
                    onOpen = {
                        if (!onOpenAttachment(attachment)) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.vehicles_attachment_open_failed),
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    },
                    onEdit = { editingAttachment = attachment },
                    onDelete = { pendingDeleteId = attachment.id },
                )
            }
        }
    }
}

@Composable
private fun AttachmentListItem(
    attachment: VehicleAttachment,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    CalculatorSection(title = attachment.displayName.ifBlank { attachment.fileName }) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpen),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = attachmentIcon(attachment.mimeType),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = AppColors.primaryBlue(),
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = attachmentCategoryLabel(attachment.category),
                    style = MaterialTheme.typography.labelMedium,
                    color = AppColors.textSecondary(),
                )
                Text(
                    text = formatAttachmentMeta(attachment),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.textSecondary(),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (attachment.notes.isNotBlank()) {
                    Text(
                        text = attachment.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.textPrimary(),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.vehicles_attachment_edit))
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.vehicles_delete),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun AttachmentEditDialog(
    attachment: VehicleAttachment,
    onDismiss: () -> Unit,
    onConfirm: (VehicleAttachment) -> Unit,
) {
    var displayName by rememberSaveable(attachment.id) { mutableStateOf(attachment.displayName) }
    var categoryKey by rememberSaveable(attachment.id) { mutableStateOf(attachment.category.name) }
    var notes by rememberSaveable(attachment.id) { mutableStateOf(attachment.notes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.vehicles_attachment_edit)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(FormFieldDefaults.fieldSpacing)) {
                VehicleTextField(
                    value = displayName,
                    label = stringResource(R.string.vehicles_attachment_name),
                    onValueChange = { displayName = it },
                )
                CalculatorChoiceField(
                    label = stringResource(R.string.vehicles_attachment_category),
                    options = VehicleAttachmentCategory.entries.map {
                        CalculatorDropdownOption(it.name, attachmentCategoryLabel(it))
                    },
                    selectedKey = categoryKey,
                    onOptionSelected = { categoryKey = it },
                )
                VehicleTextField(
                    value = notes,
                    label = stringResource(R.string.vehicles_attachment_notes),
                    singleLine = false,
                    onValueChange = { notes = it },
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        attachment.copy(
                            displayName = displayName.trim(),
                            category = VehicleAttachmentCategory.valueOf(categoryKey),
                            notes = notes.trim(),
                        ),
                    )
                },
            ) {
                Text(stringResource(R.string.vehicles_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
private fun attachmentIcon(mimeType: String): ImageVector = when {
    mimeType.startsWith("image/") -> Icons.Default.Image
    mimeType == "application/pdf" -> Icons.Default.PictureAsPdf
    else -> Icons.Default.InsertDriveFile
}

@Composable
private fun attachmentCategoryLabel(category: VehicleAttachmentCategory): String = when (category) {
    VehicleAttachmentCategory.INSURANCE -> stringResource(R.string.vehicles_attachment_cat_insurance)
    VehicleAttachmentCategory.REGISTRATION -> stringResource(R.string.vehicles_attachment_cat_registration)
    VehicleAttachmentCategory.TUV -> stringResource(R.string.vehicles_attachment_cat_tuv)
    VehicleAttachmentCategory.PURCHASE -> stringResource(R.string.vehicles_attachment_cat_purchase)
    VehicleAttachmentCategory.INVOICE -> stringResource(R.string.vehicles_attachment_cat_invoice)
    VehicleAttachmentCategory.SERVICE -> stringResource(R.string.vehicles_attachment_cat_service)
    VehicleAttachmentCategory.OTHER -> stringResource(R.string.vehicles_attachment_cat_other)
}

@Composable
private fun formatAttachmentMeta(attachment: VehicleAttachment): String {
    val date = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault())
        .format(Date(attachment.addedAtMs))
    val size = formatFileSize(attachment.fileSizeBytes)
    return "$date · $size"
}

private fun formatFileSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> String.format(Locale.getDefault(), "%.1f KB", bytes / 1024.0)
    else -> String.format(Locale.getDefault(), "%.1f MB", bytes / (1024.0 * 1024.0))
}
