package com.simplestsoft.twostrokecalc.ui.screens.vehicle

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.simplestsoft.twostrokecalc.R
import com.simplestsoft.twostrokecalc.ui.components.AppColors
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorSection
import java.io.File

@Composable
fun VehicleProfileImageSection(
    imageFile: File?,
    hasProfileImage: Boolean,
    onImageSelected: (Uri) -> Unit,
    onRemoveImage: () -> Unit,
) {
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri != null) {
            onImageSelected(uri)
        }
    }

    CalculatorSection(title = stringResource(R.string.vehicles_section_profile_image)) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            VehicleProfileAvatar(
                imageFile = imageFile,
                size = 96.dp,
                modifier = Modifier.clickable { imagePicker.launch("image/*") },
                contentDescription = stringResource(R.string.vehicles_profile_image_change),
            )
            Text(
                text = stringResource(R.string.vehicles_profile_image_hint),
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.textSecondary(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = { imagePicker.launch("image/*") }) {
                    Text(stringResource(R.string.vehicles_profile_image_change))
                }
                if (hasProfileImage) {
                    TextButton(onClick = onRemoveImage) {
                        Text(
                            text = stringResource(R.string.vehicles_profile_image_remove),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }
}
