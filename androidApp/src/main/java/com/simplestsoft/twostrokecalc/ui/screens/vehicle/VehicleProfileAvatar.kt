package com.simplestsoft.twostrokecalc.ui.screens.vehicle

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.simplestsoft.twostrokecalc.ui.components.AppColors
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorIcon
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun VehicleProfileAvatar(
    imageFile: File?,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    fallbackIcon: ImageVector = Icons.Default.TwoWheeler,
    contentDescription: String? = null,
) {
    val targetSizePx = with(LocalDensity.current) { size.roundToPx() }
    val bitmap by produceState<ImageBitmap?>(
        initialValue = null,
        key1 = imageFile?.absolutePath,
        key2 = imageFile?.lastModified(),
        key3 = targetSizePx,
    ) {
        value = imageFile?.takeIf { it.exists() }?.let { file ->
            withContext(Dispatchers.IO) {
                decodeSampledBitmap(file, targetSizePx)?.asImageBitmap()
            }
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(AppColors.settingsIconBackground()),
        contentAlignment = Alignment.Center,
    ) {
        val imageBitmap = bitmap
        if (imageBitmap != null) {
            Image(
                bitmap = imageBitmap,
                contentDescription = contentDescription,
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
        } else {
            CalculatorIcon(
                imageVector = fallbackIcon,
                size = size * 0.75f,
            )
        }
    }
}

private fun decodeSampledBitmap(file: File, targetSizePx: Int): Bitmap? {
    if (targetSizePx <= 0) return null

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.absolutePath, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    val sampleSize = calculateInSampleSize(
        width = bounds.outWidth,
        height = bounds.outHeight,
        reqWidth = targetSizePx,
        reqHeight = targetSizePx,
    )
    return BitmapFactory.Options().run {
        inSampleSize = sampleSize
        BitmapFactory.decodeFile(file.absolutePath, this)
    }
}

private fun calculateInSampleSize(
    width: Int,
    height: Int,
    reqWidth: Int,
    reqHeight: Int,
): Int {
    var inSampleSize = 1
    if (height > reqHeight || width > reqWidth) {
        val halfHeight = height / 2
        val halfWidth = width / 2
        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }
    return inSampleSize
}
