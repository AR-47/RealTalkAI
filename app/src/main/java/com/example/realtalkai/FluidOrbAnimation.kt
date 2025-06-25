package com.example.realtalkai

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun FluidOrbAnimation(isListening: Boolean) {
    val color1 = MaterialTheme.colorScheme.primary
    val color2 = MaterialTheme.colorScheme.secondary

    val transition = rememberInfiniteTransition(label = "orb_transition")

    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing)
        ),
        label = "rotation"
    )

    val scale by transition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            tween(7000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val phaseShift by animateFloatAsState(
        targetValue = if (isListening) 1.4f else 1f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessLow),
        label = "phase"
    )

    val randomOffsets = remember {
        List(12) { Random.nextFloat() * 0.4f + 0.8f }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawBlob(rotation, scale * phaseShift, Brush.radialGradient(listOf(color1, color2)), randomOffsets)
        }
        Icon(
            imageVector = Icons.Filled.Mic,
            contentDescription = "Mic",
            tint = Color.White.copy(alpha = 0.85f),
            modifier = Modifier.size(60.dp)
        )
    }
}

private fun DrawScope.drawBlob(
    rotation: Float,
    scale: Float,
    brush: Brush,
    randomOffsets: List<Float>
) {
    val radius = (size.minDimension / 3f) * scale
    val points = 12
    val path = Path()

    for (i in 0..points) {
        val angle = (i.toFloat() / points) * 360f + rotation
        val rad = Math.toRadians(angle.toDouble()).toFloat()
        val effectiveRadius = radius * randomOffsets[i % points]

        val x = center.x + cos(rad) * effectiveRadius
        val y = center.y + sin(rad) * effectiveRadius

        if (i == 0) {
            path.moveTo(x, y)
        } else {
            val prevAngle = ((i - 1).toFloat() / points) * 360f + rotation
            val prevRad = Math.toRadians(prevAngle.toDouble()).toFloat()
            val prevEffectiveRadius = radius * randomOffsets[(i - 1) % points]
            val prevX = center.x + cos(prevRad) * prevEffectiveRadius
            val prevY = center.y + sin(prevRad) * prevEffectiveRadius

            val controlX = (prevX + x) / 2
            val controlY = (prevY + y) / 2
            path.quadraticBezierTo(controlX, controlY, x, y)
        }
    }

    path.close()
    drawPath(path, brush = brush)
}
