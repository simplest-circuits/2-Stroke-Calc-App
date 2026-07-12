package com.simplestsoft.twostrokecalc.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simplestsoft.twostrokecalc.R
import kotlinx.coroutines.delay

private const val SplashDelayMs = 1_000L
private const val SplashIconFadeMs = 500
private const val SplashTextFadeMs = 600
private const val SplashTextFadeDelayMs = 150L
private const val SplashFadeOutMs = 400

private val SplashGold = Color(0xFFE8C547)

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var animationsStarted by remember { mutableStateOf(false) }
    var fadeOutStarted by remember { mutableStateOf(false) }

    val iconAlpha by animateFloatAsState(
        targetValue = if (animationsStarted) 1f else 0f,
        animationSpec = tween(
            durationMillis = SplashIconFadeMs,
            easing = FastOutSlowInEasing,
        ),
        label = "splashIconAlpha",
    )
    val textAlpha by animateFloatAsState(
        targetValue = if (animationsStarted) 1f else 0f,
        animationSpec = tween(
            durationMillis = SplashTextFadeMs,
            delayMillis = SplashTextFadeDelayMs.toInt(),
            easing = FastOutSlowInEasing,
        ),
        label = "splashTextAlpha",
    )
    val screenAlpha by animateFloatAsState(
        targetValue = if (fadeOutStarted) 0f else 1f,
        animationSpec = tween(
            durationMillis = SplashFadeOutMs,
            easing = FastOutSlowInEasing,
        ),
        label = "splashScreenAlpha",
    )

    LaunchedEffect(Unit) {
        animationsStarted = true
        delay(SplashDelayMs)
        fadeOutStarted = true
        delay(SplashFadeOutMs.toLong())
        onSplashFinished()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .alpha(screenAlpha)
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.appstore),
                contentDescription = null,
                modifier = Modifier
                    .size(120.dp)
                    .alpha(iconAlpha),
            )
            Spacer(modifier = Modifier.height(20.dp))
            GlowingTitleText(
                text = "2StrokeCalc",
                alpha = textAlpha,
            )
            Spacer(modifier = Modifier.height(8.dp))
            GlowingSloganText(
                text = stringResource(R.string.splash_slogan),
                alpha = textAlpha,
            )
        }
    }
}

@Composable
private fun GlowingTitleText(
    text: String,
    alpha: Float,
    modifier: Modifier = Modifier,
) {
    val textStyle = TextStyle(
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp,
    )

    Box(
        modifier = modifier.alpha(alpha),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = textStyle.copy(
                color = SplashGold.copy(alpha = 0.4f),
                shadow = Shadow(
                    color = SplashGold.copy(alpha = 0.95f),
                    offset = Offset.Zero,
                    blurRadius = 48f,
                ),
            ),
        )
        Text(
            text = text,
            style = textStyle.copy(
                color = SplashGold,
                shadow = Shadow(
                    color = SplashGold.copy(alpha = 0.75f),
                    offset = Offset.Zero,
                    blurRadius = 18f,
                ),
            ),
        )
    }
}

@Composable
private fun GlowingSloganText(
    text: String,
    alpha: Float,
    modifier: Modifier = Modifier,
) {
    val textStyle = TextStyle(
        fontSize = 15.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.3.sp,
    )

    Box(
        modifier = modifier.alpha(alpha),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = textStyle.copy(
                color = SplashGold.copy(alpha = 0.35f),
                shadow = Shadow(
                    color = SplashGold.copy(alpha = 0.7f),
                    offset = Offset.Zero,
                    blurRadius = 24f,
                ),
            ),
        )
        Text(
            text = text,
            style = textStyle.copy(
                color = SplashGold.copy(alpha = 0.85f),
                shadow = Shadow(
                    color = SplashGold.copy(alpha = 0.5f),
                    offset = Offset.Zero,
                    blurRadius = 10f,
                ),
            ),
        )
    }
}
