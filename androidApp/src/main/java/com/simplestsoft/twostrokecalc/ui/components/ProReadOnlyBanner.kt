package com.simplestsoft.twostrokecalc.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.simplestsoft.twostrokecalc.R
import com.simplestsoft.twostrokecalc.ui.theme.ContainerCornerRadius

@Composable
fun ProReadOnlyBanner(
    onBuyPro: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(ContainerCornerRadius)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(AppColors.cardBorderModifier(shape)),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = AppColors.accentSurfaceBackground()),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(R.string.pro_read_only_banner_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.textPrimary(),
                )
                Text(
                    text = stringResource(R.string.pro_read_only_banner_message),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.textSecondary(),
                )
            }
            TextButton(onClick = onBuyPro) {
                Text(stringResource(R.string.settings_buy_pro))
            }
        }
    }
}
