package com.simplestsoft.twostrokecalc.ui.screens.vehicle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.simplestsoft.twostrokecalc.R
import com.simplestsoft.twostrokecalc.ui.components.AppColors
import com.simplestsoft.twostrokecalc.ui.tools.ToolsViewModel
import com.simplestsoft.twostrokecalc.ui.tools.tabLabelRes
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@Composable
fun VehicleMeasurementsTabContent(
    vehicleId: String,
    viewModel: ToolsViewModel = hiltViewModel(),
) {
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val vehicleSessions = sessions.filter { it.vehicleId == vehicleId }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (vehicleSessions.isEmpty()) {
            Text(
                text = stringResource(R.string.vehicles_measurements_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.textSecondary(),
            )
        } else {
            vehicleSessions.forEach { session ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = AppColors.surface()),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = session.title.ifBlank { stringResource(session.toolId.tabLabelRes()) },
                            style = MaterialTheme.typography.titleMedium,
                            color = AppColors.textPrimary(),
                        )
                        Text(
                            text = stringResource(session.toolId.tabLabelRes()),
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.textSecondary(),
                        )
                        Text(
                            text = DateFormat.getDateTimeInstance(
                                DateFormat.MEDIUM,
                                DateFormat.SHORT,
                                Locale.getDefault(),
                            ).format(Date(session.createdAt)),
                            style = MaterialTheme.typography.labelSmall,
                            color = AppColors.textSecondary(),
                        )
                        if (session.notes.isNotBlank()) {
                            Text(session.notes, color = AppColors.textSecondary())
                        }
                    }
                }
            }
        }
    }
}
