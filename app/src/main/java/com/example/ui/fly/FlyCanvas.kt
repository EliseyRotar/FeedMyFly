package com.example.ui.fly

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import com.example.game.FlyVisualState
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Procedural Drosophila fruit fly drawn on Compose Canvas with layered paths, soft gradients,
 * realistic-ish friendly anatomy, translucent wings with veins, animated legs, proboscis and eyes.
 * Zero random numbers are used.
 */
@Composable
fun FlyCanvas(
    visualState: FlyVisualState,
    hunger: Float,
    dopamine: Float,
    isBathMode: Boolean,
    onTap: (isLeftSide: Boolean) -> Unit,
    onRub: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "fly_motion")

    // Idle breathing & subtle resting sway
    val breathing by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing"
    )

    // Rapid wing flap for buzzing & flight (220 Hz simulation cadence)
    val wingFlap by infiniteTransition.animateFloat(
        initialValue = -35f,
        targetValue = 35f,
        animationSpec = infiniteRepeatable(
            animation = tween(45, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wing_flap"
    )

    // Grooming leg motion cadence
    val groomingCycle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "grooming_cycle"
    )

    // Spiral rotation for dopamine stare pupils
    val spiralRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spiral_rotation"
    )

    // Smooth proboscis extension progress
    val proboscisExtension by animateFloatAsState(
        targetValue = if (visualState == FlyVisualState.EATING) 1f else 0f,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "proboscis_extension"
    )

    // Escape flight animation (fly zooms off-screen and scales down)
    val escapeOffsetProgress by animateFloatAsState(
        targetValue = if (visualState == FlyVisualState.ESCAPED) 1f else 0f,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "escape_flight"
    )

    Box(
        modifier = modifier
            .testTag("fly_canvas_area")
            .pointerInput(visualState, isBathMode) {
                if (isBathMode) {
                    detectDragGestures { _, _ ->
                        onRub()
                    }
                } else {
                    detectTapGestures { offset ->
                        val isLeft = offset.x < (size.width / 2f)
                        onTap(isLeft)
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f

            // 1. Draw warm room / wooden table background with ambient illumination
            drawRoomBackground(size, isBathMode)

            // When escaped, fly zooms off-screen with parabolic trajectory
            val escapeY = -escapeOffsetProgress * size.height * 0.8f
            val escapeX = escapeOffsetProgress * size.width * 0.3f
            val escapeScale = (1f - escapeOffsetProgress * 0.7f).coerceAtLeast(0.01f)
            val escapeAlpha = (1f - escapeOffsetProgress).coerceIn(0f, 1f)

            if (escapeAlpha > 0.05f) {
                translate(left = escapeX, top = escapeY) {
                    scale(scale = escapeScale, pivot = Offset(cx, cy)) {
                        // Draw soft drop shadow on table
                        val shadowY = cy + 90f * (1f - escapeOffsetProgress)
                        drawOval(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0x352B1E12), Color.Transparent),
                                center = Offset(cx, shadowY),
                                radius = 95f * (1f - escapeOffsetProgress * 0.5f)
                            ),
                            topLeft = Offset(cx - 95f, shadowY - 35f),
                            size = Size(190f, 70f)
                        )

                        // Sugar droplet if eating
                        if (proboscisExtension > 0.05f) {
                            drawSugarDroplet(cx, cy + 140f, proboscisExtension)
                        }

                        // Fly body
                        drawFruitFly(
                            cx = cx,
                            cy = cy,
                            visualState = visualState,
                            breathing = breathing,
                            wingFlap = wingFlap,
                            groomingCycle = groomingCycle,
                            spiralRotation = spiralRotation,
                            proboscisProgress = proboscisExtension,
                            dopamine = dopamine,
                            alpha = escapeAlpha
                        )
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawRoomBackground(size: Size, isBathMode: Boolean) {
    if (isBathMode) {
        // Soft pastel blue-teal bath tile & porcelain surface
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFE2F3F5),
                    Color(0xFFCEEAEF),
                    Color(0xFFBCE3E8)
                )
            ),
            size = size
        )
        // Draw decorative soft bubbles/suds
        drawCircle(Color(0x40FFFFFF), radius = 50f, center = Offset(size.width * 0.25f, size.height * 0.25f))
        drawCircle(Color(0x30FFFFFF), radius = 80f, center = Offset(size.width * 0.8f, size.height * 0.3f))
        drawCircle(Color(0x40FFFFFF), radius = 60f, center = Offset(size.width * 0.7f, size.height * 0.75f))
        drawCircle(Color(0x35FFFFFF), radius = 40f, center = Offset(size.width * 0.2f, size.height * 0.7f))
    } else {
        // Warm organic fruit table / laboratory background
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFBF4EB),
                    Color(0xFFF3E7D8),
                    Color(0xFFE5D5C2)
                ),
                center = Offset(size.width / 2f, size.height * 0.45f),
                radius = size.width * 0.85f
            ),
            size = size
        )

        // Subtle wooden texture rings
        drawOval(
            color = Color(0x10A67C52),
            topLeft = Offset(size.width * 0.05f, size.height * 0.15f),
            size = Size(size.width * 0.9f, size.height * 0.7f),
            style = Stroke(width = 3f)
        )
        drawOval(
            color = Color(0x0DA67C52),
            topLeft = Offset(size.width * 0.15f, size.height * 0.22f),
            size = Size(size.width * 0.7f, size.height * 0.55f),
            style = Stroke(width = 2f)
        )
    }
}

private fun DrawScope.drawSugarDroplet(cx: Float, cy: Float, progress: Float) {
    val dropRadius = 24f * progress
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFF275),
                Color(0xFFFFD152),
                Color(0xEEF39C12),
                Color.Transparent
            ),
            center = Offset(cx - 6f, cy - 6f),
            radius = dropRadius
        ),
        radius = dropRadius,
        center = Offset(cx, cy)
    )
    // Specular shine on sugar drop
    drawCircle(
        color = Color(0xCCFFFFFF),
        radius = dropRadius * 0.3f,
        center = Offset(cx - dropRadius * 0.35f, cy - dropRadius * 0.35f)
    )
}

private fun DrawScope.drawFruitFly(
    cx: Float,
    cy: Float,
    visualState: FlyVisualState,
    breathing: Float,
    wingFlap: Float,
    groomingCycle: Float,
    spiralRotation: Float,
    proboscisProgress: Float,
    dopamine: Float,
    alpha: Float
) {
    val isBuzzing = visualState == FlyVisualState.BUZZING || visualState == FlyVisualState.ESCAPED

    // --- 1. Jointed Legs ---
    drawJointedLegs(cx, cy, visualState, groomingCycle, alpha)

    // --- 2. Abdomen (posterior, darker banded) ---
    val abdCy = cy + 48f
    val abdWidth = 62f * breathing
    val abdHeight = 88f * breathing

    // Abdomen base shape
    drawOval(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF8B5A2B).copy(alpha = alpha),
                Color(0xFF653E1C).copy(alpha = alpha),
                Color(0xFF4A2B15).copy(alpha = alpha),
                Color(0xFF321A0C).copy(alpha = alpha)
            )
        ),
        topLeft = Offset(cx - abdWidth / 2f, abdCy - abdHeight / 2f),
        size = Size(abdWidth, abdHeight)
    )

    // Abdominal transverse dark bands (hallmark of Drosophila melanogaster)
    val bandYOffsets = floatArrayOf(-22f, -8f, 6f, 20f, 32f)
    for (i in bandYOffsets.indices) {
        val y = abdCy + bandYOffsets[i] * breathing
        val bandW = (abdWidth * (1f - (i * 0.12f))).coerceAtLeast(20f)
        drawOval(
            color = Color(0xFF26140A).copy(alpha = alpha * 0.85f),
            topLeft = Offset(cx - bandW / 2f + 2f, y - 4f),
            size = Size(bandW - 4f, 8f)
        )
    }

    // --- 3. Thorax (warm-brown chitin oval) ---
    val thWidth = 58f
    val thHeight = 62f
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFA66E38).copy(alpha = alpha),
                Color(0xFF8B572A).copy(alpha = alpha),
                Color(0xFF5B3416).copy(alpha = alpha)
            ),
            center = Offset(cx - 8f, cy - 10f),
            radius = 35f
        ),
        topLeft = Offset(cx - thWidth / 2f, cy - thHeight / 2f),
        size = Size(thWidth, thHeight)
    )

    // Scutellum & fine bristles on thorax
    drawThoraxBristles(cx, cy, alpha)

    // --- 4. Translucent Wings with Veins ---
    drawTranslucentWings(cx, cy, isBuzzing, wingFlap, alpha)

    // --- 5. Head & Eyes ---
    val headCy = cy - 42f
    val headWidth = 46f
    val headHeight = 36f

    // Chitin Head base
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF8B572A).copy(alpha = alpha),
                Color(0xFF5C3314).copy(alpha = alpha)
            ),
            center = Offset(cx, headCy),
            radius = 24f
        ),
        topLeft = Offset(cx - headWidth / 2f, headCy - headHeight / 2f),
        size = Size(headWidth, headHeight)
    )

    // Proboscis extending smoothly when eating
    if (proboscisProgress > 0.01f) {
        val proboscisLen = 42f * proboscisProgress
        val proboscisY = headCy - 8f
        val probPath = Path().apply {
            moveTo(cx - 6f, proboscisY)
            lineTo(cx - 8f, proboscisY + proboscisLen * 0.6f)
            lineTo(cx - 4f, proboscisY + proboscisLen)
            lineTo(cx + 4f, proboscisY + proboscisLen)
            lineTo(cx + 8f, proboscisY + proboscisLen * 0.6f)
            lineTo(cx + 6f, proboscisY)
            close()
        }
        drawPath(
            path = probPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF7A4822).copy(alpha = alpha),
                    Color(0xFFA66E38).copy(alpha = alpha),
                    Color(0xFFF39C12).copy(alpha = alpha)
                )
            )
        )
        // Labellum sponges at tip
        drawCircle(
            color = Color(0xFFE67E22).copy(alpha = alpha),
            radius = 6f * proboscisProgress,
            center = Offset(cx, proboscisY + proboscisLen)
        )
    }

    // Antennae (Johnston's Organ resides at the base)
    drawAntennae(cx, headCy - 14f, alpha)

    // Large Red Compound Eyes (friendly ruby red with facet texture)
    drawCompoundEyes(
        cx = cx,
        headCy = headCy,
        visualState = visualState,
        spiralRotation = spiralRotation,
        dopamine = dopamine,
        alpha = alpha
    )
}

private fun DrawScope.drawThoraxBristles(cx: Float, cy: Float, alpha: Float) {
    val bristleColor = Color(0xFF2A1508).copy(alpha = alpha * 0.65f)
    val offsets = floatArrayOf(
        -18f, -12f, 18f, -12f,
        -14f, -4f, 14f, -4f,
        -16f, 10f, 16f, 10f,
        -8f, 18f, 8f, 18f
    )
    for (i in 0 until offsets.size step 2) {
        val bx = cx + offsets[i]
        val by = cy + offsets[i + 1]
        drawLine(
            color = bristleColor,
            start = Offset(bx, by),
            end = Offset(bx + (if (offsets[i] < 0) -8f else 8f), by - 10f),
            strokeWidth = 1.5f,
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawAntennae(cx: Float, y: Float, alpha: Float) {
    val antColor = Color(0xFF4A2B15).copy(alpha = alpha)
    // Left antenna
    val leftPath = Path().apply {
        moveTo(cx - 6f, y)
        quadraticTo(cx - 14f, y - 12f, cx - 18f, y - 22f)
    }
    drawPath(leftPath, antColor, style = Stroke(width = 2f, cap = StrokeCap.Round))
    // Side branches (arista feathers)
    drawLine(antColor, Offset(cx - 12f, y - 9f), Offset(cx - 18f, y - 11f), strokeWidth = 1f)
    drawLine(antColor, Offset(cx - 15f, y - 15f), Offset(cx - 22f, y - 18f), strokeWidth = 1f)

    // Right antenna
    val rightPath = Path().apply {
        moveTo(cx + 6f, y)
        quadraticTo(cx + 14f, y - 12f, cx + 18f, y - 22f)
    }
    drawPath(rightPath, antColor, style = Stroke(width = 2f, cap = StrokeCap.Round))
    drawLine(antColor, Offset(cx + 12f, y - 9f), Offset(cx + 18f, y - 11f), strokeWidth = 1f)
    drawLine(antColor, Offset(cx + 15f, y - 15f), Offset(cx + 22f, y - 18f), strokeWidth = 1f)
}

private fun DrawScope.drawCompoundEyes(
    cx: Float,
    headCy: Float,
    visualState: FlyVisualState,
    spiralRotation: Float,
    dopamine: Float,
    alpha: Float
) {
    val isDopamineStare = visualState == FlyVisualState.DOPAMINE_STARE
    val eyeW = if (isDopamineStare) 26f else 22f
    val eyeH = if (isDopamineStare) 30f else 26f
    val leftEyeCenter = Offset(cx - 21f, headCy - 2f)
    val rightEyeCenter = Offset(cx + 21f, headCy - 2f)

    for (eyeCenter in listOf(leftEyeCenter, rightEyeCenter)) {
        // Deep ruby red gradient
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFE71D36).copy(alpha = alpha),
                    Color(0xFFB80C1F).copy(alpha = alpha),
                    Color(0xFF6B000C).copy(alpha = alpha)
                ),
                center = eyeCenter - Offset(3f, 4f),
                radius = eyeW * 0.8f
            ),
            topLeft = Offset(eyeCenter.x - eyeW / 2f, eyeCenter.y - eyeH / 2f),
            size = Size(eyeW, eyeH)
        )

        // Facet ommatidia grid texture
        val facetColor = Color(0x30FFFFFF)
        for (dx in -8..8 step 4) {
            for (dy in -10..10 step 5) {
                if (dx * dx + dy * dy < 70) {
                    drawCircle(facetColor, radius = 0.9f, center = eyeCenter + Offset(dx.toFloat(), dy.toFloat()))
                }
            }
        }

        // Dopamine Stare: Animated spiral pupil
        if (isDopamineStare) {
            rotate(spiralRotation, pivot = eyeCenter) {
                val spiralPath = Path()
                var r = 1f
                for (a in 0..720 step 20) {
                    val rad = a * PI / 180.0
                    val sx = eyeCenter.x + (r * cos(rad)).toFloat()
                    val sy = eyeCenter.y + (r * sin(rad)).toFloat()
                    if (a == 0) spiralPath.moveTo(sx, sy) else spiralPath.lineTo(sx, sy)
                    r += 0.16f
                }
                drawPath(spiralPath, Color(0xFFFFF176).copy(alpha = alpha * 0.9f), style = Stroke(width = 1.6f))
            }
        }

        // Soft specular highlight on eye surface
        drawCircle(
            color = Color(0xCCFFFFFF).copy(alpha = alpha),
            radius = 3.2f,
            center = Offset(eyeCenter.x - 3.5f, eyeCenter.y - 4.5f)
        )
        drawCircle(
            color = Color(0x77FFFFFF).copy(alpha = alpha),
            radius = 1.8f,
            center = Offset(eyeCenter.x + 2f, eyeCenter.y - 6f)
        )
    }
}

private fun DrawScope.drawTranslucentWings(
    cx: Float,
    cy: Float,
    isBuzzing: Boolean,
    wingFlap: Float,
    alpha: Float
) {
    val wingAttachmentY = cy - 6f
    val wingLen = 135f
    val wingW = 44f

    val leftAngle = if (isBuzzing) 65f + wingFlap else 18f
    val rightAngle = if (isBuzzing) -65f - wingFlap else -18f

    // Left Wing
    rotate(leftAngle, pivot = Offset(cx - 10f, wingAttachmentY)) {
        drawSingleWing(cx - 10f, wingAttachmentY, wingLen, wingW, isLeft = true, alpha = alpha)
    }

    // Right Wing
    rotate(rightAngle, pivot = Offset(cx + 10f, wingAttachmentY)) {
        drawSingleWing(cx + 10f, wingAttachmentY, wingLen, wingW, isLeft = false, alpha = alpha)
    }
}

private fun DrawScope.drawSingleWing(
    attachX: Float,
    attachY: Float,
    length: Float,
    width: Float,
    isLeft: Boolean,
    alpha: Float
) {
    val sign = if (isLeft) -1f else 1f
    val wingPath = Path().apply {
        moveTo(attachX, attachY)
        cubicTo(
            attachX + sign * width * 0.9f, attachY + length * 0.25f,
            attachX + sign * width * 1.1f, attachY + length * 0.75f,
            attachX + sign * width * 0.3f, attachY + length
        )
        cubicTo(
            attachX, attachY + length * 0.95f,
            attachX - sign * width * 0.2f, attachY + length * 0.6f,
            attachX, attachY
        )
        close()
    }

    // Pearlescent translucent wing membrane
    drawPath(
        path = wingPath,
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xC0E2F0F7).copy(alpha = alpha * 0.75f),
                Color(0x95C7E1ED).copy(alpha = alpha * 0.55f),
                Color(0x80D2D8F0).copy(alpha = alpha * 0.45f)
            )
        )
    )
    drawPath(
        path = wingPath,
        color = Color(0x604A6275).copy(alpha = alpha),
        style = Stroke(width = 1.2f)
    )

    // Structural wing veins (Costa, Radial, Medial, Cubital)
    val veinColor = Color(0x804A6478).copy(alpha = alpha)
    // Main longitudinal vein
    drawLine(
        color = veinColor,
        start = Offset(attachX, attachY),
        end = Offset(attachX + sign * width * 0.4f, attachY + length * 0.85f),
        strokeWidth = 1.4f
    )
    // Subcostal vein
    drawLine(
        color = veinColor,
        start = Offset(attachX, attachY),
        end = Offset(attachX + sign * width * 0.8f, attachY + length * 0.45f),
        strokeWidth = 1.2f
    )
    // Cross veins
    drawLine(
        color = veinColor,
        start = Offset(attachX + sign * width * 0.25f, attachY + length * 0.4f),
        end = Offset(attachX + sign * width * 0.6f, attachY + length * 0.42f),
        strokeWidth = 1f
    )
    drawLine(
        color = veinColor,
        start = Offset(attachX + sign * width * 0.3f, attachY + length * 0.65f),
        end = Offset(attachX + sign * width * 0.65f, attachY + length * 0.68f),
        strokeWidth = 1f
    )
}

private fun DrawScope.drawJointedLegs(
    cx: Float,
    cy: Float,
    visualState: FlyVisualState,
    groomingCycle: Float,
    alpha: Float
) {
    val legColor = Color(0xFF4A2B15).copy(alpha = alpha)
    val legWidth = 2.4f

    val isGrooming = visualState == FlyVisualState.GROOMING
    val groomOffset = if (isGrooming) sin(groomingCycle * 2.0 * PI).toFloat() * 14f else 0f

    // 1. Prothoracic Legs (Front) - Wipes antennae when grooming
    // Left front
    drawSegmentedLeg(
        start = Offset(cx - 18f, cy - 18f),
        joint1 = Offset(cx - 38f + (if (isGrooming) 12f else 0f), cy - 40f - groomOffset),
        joint2 = Offset(cx - 45f + (if (isGrooming) 22f else 0f), cy - 65f - groomOffset),
        end = Offset(cx - 28f + (if (isGrooming) 16f else 0f), cy - 72f - groomOffset),
        color = legColor,
        width = legWidth
    )
    // Right front
    drawSegmentedLeg(
        start = Offset(cx + 18f, cy - 18f),
        joint1 = Offset(cx + 38f - (if (isGrooming) 12f else 0f), cy - 40f - groomOffset),
        joint2 = Offset(cx + 45f - (if (isGrooming) 22f else 0f), cy - 65f - groomOffset),
        end = Offset(cx + 28f - (if (isGrooming) 16f else 0f), cy - 72f - groomOffset),
        color = legColor,
        width = legWidth
    )

    // 2. Mesothoracic Legs (Middle)
    drawSegmentedLeg(
        start = Offset(cx - 24f, cy),
        joint1 = Offset(cx - 55f, cy - 8f),
        joint2 = Offset(cx - 82f, cy + 15f),
        end = Offset(cx - 95f, cy + 32f),
        color = legColor,
        width = legWidth
    )
    drawSegmentedLeg(
        start = Offset(cx + 24f, cy),
        joint1 = Offset(cx + 55f, cy - 8f),
        joint2 = Offset(cx + 82f, cy + 15f),
        end = Offset(cx + 95f, cy + 32f),
        color = legColor,
        width = legWidth
    )

    // 3. Metathoracic Legs (Hind)
    drawSegmentedLeg(
        start = Offset(cx - 22f, cy + 22f),
        joint1 = Offset(cx - 52f, cy + 42f),
        joint2 = Offset(cx - 78f, cy + 85f),
        end = Offset(cx - 88f, cy + 120f),
        color = legColor,
        width = legWidth
    )
    drawSegmentedLeg(
        start = Offset(cx + 22f, cy + 22f),
        joint1 = Offset(cx + 52f, cy + 42f),
        joint2 = Offset(cx + 78f, cy + 85f),
        end = Offset(cx + 88f, cy + 120f),
        color = legColor,
        width = legWidth
    )
}

private fun DrawScope.drawSegmentedLeg(
    start: Offset,
    joint1: Offset,
    joint2: Offset,
    end: Offset,
    color: Color,
    width: Float
) {
    val path = Path().apply {
        moveTo(start.x, start.y)
        lineTo(joint1.x, joint1.y)
        lineTo(joint2.x, joint2.y)
        lineTo(end.x, end.y)
    }
    drawPath(path, color, style = Stroke(width = width, cap = StrokeCap.Round, join = StrokeJoin.Round))
    // Tiny joint nodes
    drawCircle(color, radius = width * 0.9f, center = joint1)
    drawCircle(color, radius = width * 0.9f, center = joint2)
}
