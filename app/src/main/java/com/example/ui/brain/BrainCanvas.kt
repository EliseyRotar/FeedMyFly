package com.example.ui.brain

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.ConnectomeData
import com.example.data.Neuron
import com.example.data.NeuronRoles
import com.example.engine.BrainSnapshot
import kotlin.math.hypot
import kotlin.math.min

/**
 * Deterministic node position in the 4-band layout.
 */
data class NodePosition(
    val neuron: Neuron,
    val x: Float,
    val y: Float,
    val bandIndex: Int,
    val columnGroup: Int // 0=Sensory, 1=Inter, 2=Motor/PAM
)

@Composable
fun BrainCanvas(
    connectomeData: ConnectomeData,
    snapshot: BrainSnapshot?,
    selectedNeuron: Neuron?,
    onNeuronSelected: (Neuron?) -> Unit,
    modifier: Modifier = Modifier
) {
    // Precompute incoming and outgoing synapse counts per neuron
    val synapseCountsIn = remember(connectomeData) {
        val counts = IntArray(connectomeData.neurons.size)
        for (syn in connectomeData.synapses) {
            counts[syn.postIndex]++
        }
        counts
    }
    val synapseCountsOut = remember(connectomeData) {
        val counts = IntArray(connectomeData.neurons.size)
        for (syn in connectomeData.synapses) {
            counts[syn.preIndex]++
        }
        counts
    }

    // High firing path check (MN9 >= 0.5 or DNp01 >= 0.5)
    val isMn9Firing = snapshot?.isEating == true
    val isDnp01Firing = snapshot?.isEscaping == true

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF12141A))
            .testTag("brain_canvas_root")
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("brain_graph_canvas")
                .pointerInput(connectomeData) {
                    detectTapGestures { tapOffset ->
                        val positions = computeDeterministicPositions(connectomeData, size.width.toFloat(), size.height.toFloat())
                        // Find closest node within touch radius
                        var closest: Neuron? = null
                        var minDistance = 32.dp.toPx()
                        for (pos in positions) {
                            val dist = hypot(pos.x - tapOffset.x, pos.y - tapOffset.y)
                            if (dist < minDistance) {
                                minDistance = dist
                                closest = pos.neuron
                            }
                        }
                        onNeuronSelected(closest)
                    }
                }
        ) {
            val w = size.width
            val h = size.height
            val positions = computeDeterministicPositions(connectomeData, w, h)
            val posMap = positions.associateBy { it.neuron.index }

            // 1. Draw 4 horizontal circuit bands
            drawBands(w, h)

            // 2. Draw Synapse Edges
            drawSynapseEdges(
                connectomeData = connectomeData,
                posMap = posMap,
                snapshot = snapshot,
                isMn9Firing = isMn9Firing,
                isDnp01Firing = isDnp01Firing
            )

            // 3. Draw Neuron Nodes
            drawNeuronNodes(
                positions = positions,
                snapshot = snapshot,
                selectedNeuron = selectedNeuron
            )
        }

        // Top Legend Bar
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .align(Alignment.TopCenter),
            color = Color(0xDD1E222D),
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendItem(color = Color(0xFF00D2FF), label = stringResource(R.string.brain_legend_sensors))
                LegendItem(color = Color(0xFFFFB300), label = stringResource(R.string.brain_legend_inter))
                LegendItem(color = Color(0xFFFF5252), label = stringResource(R.string.brain_legend_motor))
                if (isMn9Firing || isDnp01Firing) {
                    Text(
                        text = stringResource(R.string.brain_firing_path_active),
                        color = Color(0xFF00E676),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Selected Neuron Card (bottom overlay)
        AnimatedVisibility(
            visible = selectedNeuron != null,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            if (selectedNeuron != null) {
                val act = snapshot?.getActivation(selectedNeuron.index) ?: 0f
                NeuronInfoCard(
                    neuron = selectedNeuron,
                    activation = act,
                    synapsesIn = synapseCountsIn[selectedNeuron.index],
                    synapsesOut = synapseCountsOut[selectedNeuron.index],
                    onDismiss = { onNeuronSelected(null) }
                )
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(text = label, color = Color(0xFFCCCCCC), fontSize = 11.sp)
    }
}

@Composable
private fun NeuronInfoCard(
    neuron: Neuron,
    activation: Float,
    synapsesIn: Int,
    synapsesOut: Int,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("neuron_info_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xF0202534)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = neuron.cellType,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${stringResource(R.string.brain_card_role)}: ${neuron.role}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF90CAF9)
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.btn_dismiss),
                        tint = Color.LightGray
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Stats grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.brain_card_root_id),
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                    Text(
                        text = neuron.id,
                        color = Color(0xFFECEFF1),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(R.string.brain_card_side),
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                    Text(
                        text = neuron.side.replaceFirstChar { it.uppercase() },
                        color = Color(0xFFECEFF1),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.brain_card_synapses_in),
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "$synapsesIn synapses",
                        color = Color(0xFF81C784),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(R.string.brain_card_synapses_out),
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "$synapsesOut synapses",
                        color = Color(0xFFFFB74D),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Activation",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "%.3f".format(activation),
                        color = if (activation >= 0.5f) Color(0xFFFF5252) else Color(0xFF64B5F6),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Computes deterministic 2D positions for all 127 neurons.
 * Four horizontal bands: FEED, ESCAPE, REWARD, GROOM.
 * Inside each band: Sensors on the left, INTER in the middle, Motor/PAM on the right.
 * Position by index, zero randomness.
 */
private fun computeDeterministicPositions(
    data: ConnectomeData,
    width: Float,
    height: Float
): List<NodePosition> {
    val topInset = 50f
    val bottomInset = 30f
    val usableHeight = (height - topInset - bottomInset).coerceAtLeast(100f)
    val bandHeight = usableHeight / 4f

    // Sort neurons into their primary band (first matching circuit)
    val bandNeurons = Array(4) { mutableListOf<Neuron>() }
    for (n in data.neurons) {
        val band = when {
            n.circuits.contains("FEED") -> 0
            n.circuits.contains("ESCAPE") -> 1
            n.circuits.contains("REWARD") -> 2
            n.circuits.contains("GROOM") -> 3
            else -> 0
        }
        bandNeurons[band].add(n)
    }

    val positions = mutableListOf<NodePosition>()

    for (band in 0 until 4) {
        val bandTop = topInset + band * bandHeight
        val bandCy = bandTop + bandHeight / 2f
        val neuronsInBand = bandNeurons[band]

        // Group into 3 columns:
        // 0 = Sensory, 1 = Inter, 2 = Motor/PAM
        val sensory = neuronsInBand.filter { it.role.startsWith("SENSORY_") }.sortedBy { it.index }
        val inter = neuronsInBand.filter { it.role == NeuronRoles.INTER }.sortedBy { it.index }
        val motor = neuronsInBand.filter { it.role.startsWith("MOTOR_") || it.role == NeuronRoles.REWARD_PAM }.sortedBy { it.index }

        // Layout sensory (left zone: 0.08w to 0.32w)
        layoutGroup(sensory, band, 0, width * 0.07f, width * 0.32f, bandTop, bandHeight, positions)
        // Layout inter (middle zone: 0.36w to 0.64w)
        layoutGroup(inter, band, 1, width * 0.38f, width * 0.64f, bandTop, bandHeight, positions)
        // Layout motor (right zone: 0.70w to 0.94w)
        layoutGroup(motor, band, 2, width * 0.70f, width * 0.94f, bandTop, bandHeight, positions)
    }

    return positions
}

private fun layoutGroup(
    group: List<Neuron>,
    band: Int,
    colGroup: Int,
    minX: Float,
    maxX: Float,
    bandTop: Float,
    bandHeight: Float,
    positions: MutableList<NodePosition>
) {
    if (group.isEmpty()) return

    // Multi-row deterministic layout for dense clusters
    val count = group.size
    val cols = when {
        count > 15 -> 4
        count > 8 -> 3
        count > 4 -> 2
        else -> 1
    }
    val rows = (count + cols - 1) / cols

    val xStep = if (cols > 1) (maxX - minX) / (cols - 1) else 0f
    val startX = if (cols == 1) (minX + maxX) / 2f else minX

    val marginY = 14f
    val usableH = bandHeight - 2f * marginY
    val yStep = if (rows > 1) usableH / (rows - 1) else 0f
    val startY = if (rows == 1) bandTop + bandHeight / 2f else bandTop + marginY

    for (i in 0 until count) {
        val r = i / cols
        val c = i % cols
        val x = startX + c * xStep
        val y = startY + r * yStep
        positions.add(NodePosition(group[i], x, y, band, colGroup))
    }
}

private fun DrawScope.drawBands(w: Float, h: Float) {
    val topInset = 50f
    val bottomInset = 30f
    val usableHeight = (h - topInset - bottomInset).coerceAtLeast(100f)
    val bandHeight = usableHeight / 4f

    val bandLabels = listOf("FEED", "ESCAPE", "REWARD", "GROOM")
    val bandGradients = listOf(
        Color(0x18F39C12),
        Color(0x18E74C3C),
        Color(0x189B59B6),
        Color(0x181ABC9C)
    )

    for (b in 0 until 4) {
        val y = topInset + b * bandHeight
        // Band background
        drawRect(
            color = bandGradients[b],
            topLeft = Offset(0f, y),
            size = Size(w, bandHeight)
        )
        // Band divider line
        drawLine(
            color = Color(0x33FFFFFF),
            start = Offset(0f, y),
            end = Offset(w, y),
            strokeWidth = 1f
        )
    }
}

private fun DrawScope.drawSynapseEdges(
    connectomeData: ConnectomeData,
    posMap: Map<Int, NodePosition>,
    snapshot: BrainSnapshot?,
    isMn9Firing: Boolean,
    isDnp01Firing: Boolean
) {
    val maxCount = connectomeData.synapses.maxOfOrNull { it.count } ?: 1

    for (syn in connectomeData.synapses) {
        val prePos = posMap[syn.preIndex] ?: continue
        val postPos = posMap[syn.postIndex] ?: continue

        val preAct = snapshot?.getActivation(syn.preIndex) ?: 0f
        val postAct = snapshot?.getActivation(syn.postIndex) ?: 0f

        // High firing highlight
        val isFeedEdge = isMn9Firing && prePos.neuron.circuits.contains("FEED") && postPos.neuron.circuits.contains("FEED") && preAct > 0.1f
        val isEscapeEdge = isDnp01Firing && prePos.neuron.circuits.contains("ESCAPE") && postPos.neuron.circuits.contains("ESCAPE") && preAct > 0.1f
        val isHighlighted = isFeedEdge || isEscapeEdge

        // Edges: green for sign +1, red for sign -1, alpha proportional to synapse count
        val baseColor = if (syn.sign > 0) Color(0xFF2ECC71) else Color(0xFFE74C3C)
        val normalizedWeight = (syn.count.toFloat() / maxCount).coerceIn(0f, 1f)

        val alpha = if (isHighlighted) {
            0.95f
        } else {
            (0.12f + 0.55f * normalizedWeight).coerceIn(0.08f, 0.85f)
        }

        val strokeWidth = if (isHighlighted) {
            2.8f
        } else {
            (1f + 2f * normalizedWeight).coerceIn(0.8f, 2.5f)
        }

        val drawColor = if (isHighlighted) {
            Color(0xFF00E676)
        } else {
            baseColor.copy(alpha = alpha)
        }

        drawLine(
            color = drawColor,
            start = Offset(prePos.x, prePos.y),
            end = Offset(postPos.x, postPos.y),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawNeuronNodes(
    positions: List<NodePosition>,
    snapshot: BrainSnapshot?,
    selectedNeuron: Neuron?
) {
    for (pos in positions) {
        val act = snapshot?.getActivation(pos.neuron.index) ?: 0f
        val isSelected = selectedNeuron?.index == pos.neuron.index

        val baseHue = when (pos.columnGroup) {
            0 -> Color(0xFF00D2FF) // Sensory
            1 -> Color(0xFFFFB300) // Inter
            else -> Color(0xFFFF5252) // Motor / PAM
        }

        // Live brightness = activation
        val brightnessAlpha = (0.35f + 0.65f * act).coerceIn(0.35f, 1f)
        val nodeColor = baseHue.copy(alpha = brightnessAlpha)

        val radius = when {
            isSelected -> 8f
            act >= 0.5f -> 6.5f
            else -> 4.5f
        }

        // Active halo glow
        if (act >= 0.3f || isSelected) {
            val haloRadius = radius * (1.5f + act * 1.5f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(baseHue.copy(alpha = 0.6f * act), Color.Transparent),
                    center = Offset(pos.x, pos.y),
                    radius = haloRadius
                ),
                radius = haloRadius,
                center = Offset(pos.x, pos.y)
            )
        }

        // Core neuron node
        drawCircle(
            color = nodeColor,
            radius = radius,
            center = Offset(pos.x, pos.y)
        )

        // Center highlight
        drawCircle(
            color = Color.White.copy(alpha = (0.2f + 0.8f * act).coerceIn(0.2f, 1f)),
            radius = radius * 0.4f,
            center = Offset(pos.x, pos.y)
        )

        // Selected ring
        if (isSelected) {
            drawCircle(
                color = Color.White,
                radius = radius + 3.5f,
                center = Offset(pos.x, pos.y),
                style = Stroke(width = 1.8f)
            )
        }
    }
}
