package com.runanywhere.startup_hackathon20.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Splash Screen for SafeSphere
 * Shows animated logo and tagline before main app
 */
@Composable
fun SplashScreen(onSplashComplete: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    var logoScale by remember { mutableStateOf(0f) }
    var taglineAlpha by remember { mutableStateOf(0f) }

    // Animated values
    val animatedLogoScale by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "logo_scale"
    )

    val animatedTaglineAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(
            durationMillis = 1000,
            delayMillis = 500,
            easing = FastOutSlowInEasing
        ),
        label = "tagline_alpha"
    )

    // Trigger animations on launch
    LaunchedEffect(Unit) {
        visible = true
        delay(2500) // Total splash duration
        onSplashComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0A0E27),
                        Color(0xFF1A1F3A),
                        Color(0xFF0A0E27)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo/Icon
            Text(
                text = "🛡️",
                fontSize = 120.sp,
                modifier = Modifier.scale(animatedLogoScale)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // App Name
            Text(
                text = "SafeSphere",
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.alpha(animatedTaglineAlpha)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Tagline
            Text(
                text = "Your Privacy, Your Control",
                fontSize = 16.sp,
                color = Color(0xFF00D9FF),
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(animatedTaglineAlpha)
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Loading indicator
            LoadingDots(alpha = animatedTaglineAlpha)
        }

        // Version info at bottom
        Text(
            text = "v1.0.0",
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.5f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .alpha(animatedTaglineAlpha)
        )
    }
}

/**
 * Animated loading dots
 */
@Composable
fun LoadingDots(alpha: Float) {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.alpha(alpha)
    ) {
        repeat(3) { index ->
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.5f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 600,
                        delayMillis = index * 200,
                        easing = FastOutSlowInEasing
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot_$index"
            )

            Box(
                modifier = Modifier
                    .size(8.dp)
                    .scale(scale)
                    .background(
                        color = Color(0xFF00D9FF),
                        shape = androidx.compose.foundation.shape.CircleShape
                    )
            )
        }
    }
}
