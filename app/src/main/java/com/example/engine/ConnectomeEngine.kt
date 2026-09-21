package com.example.engine

import com.example.data.ConnectomeData
import com.example.data.Neuron
import com.example.data.NeuronRoles
import com.example.data.Synapse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Immutable snapshot of the connectome state published each tick.
 */
data class BrainSnapshot(
    val tick: Long,
    val activations: FloatArray,
    val motorFeed: FloatArray,
    val motorEscape: FloatArray,
    val motorGroom: FloatArray,
    val rewardPamMean: Float,
    val isEating: Boolean,
    val isEscaping: Boolean,
    val isGrooming: Boolean,
    val isDopamineStare: Boolean
) {
    fun getActivation(index: Int): Float {
        return if (index in activations.indices) activations[index] else 0f
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BrainSnapshot) return false
        return tick == other.tick &&
                activations.contentEquals(other.activations)
    }

    override fun hashCode(): Int {
        var result = tick.hashCode()
        result = 31 * result + activations.contentHashCode()
        return result
    }
}

/**
 * Precomputed incoming synapse connection for fast tick updates.
 */
data class IncomingEdge(
    val preIndex: Int,
    val weight: Double
)

/**
 * A stimulus command posted into the engine queue.
 */
data class TimedStimulus(
    val neuronIndices: IntArray,
    val intensity: Double,
    var remainingTicks: Int
)

/**
 * Connectome simulation engine executing the FlyWire connectome circuit.
 *
 * Double-buffered synchronous update:
 * x_j(t+1) = clamp(LEAK*x_j + (1-LEAK)*clamp(GAIN*sum_i(w_ij*x_i) + I_j, 0, 1), 0, 1)
 * w_ij = sign*count / (sum of excitatory counts entering j inside this subgraph).
 * LEAK = 0.6, GAIN = 0.95
 */
class ConnectomeEngine(val data: ConnectomeData) {

    val leak: Double = data.engineConfig.leak
    val gain: Double = data.engineConfig.gain
    val neuronCount: Int = data.neurons.size

    // Precomputed normalized incoming synapses for each postIndex j
    private val incomingEdges: Array<List<IncomingEdge>>

    // Pre-indexed neuron groups for fast access
    val sensorySugarIndices: IntArray = data.neurons.filter { it.role == NeuronRoles.SENSORY_SUGAR }.map { it.index }.toIntArray()
    val sensoryLoomIndices: IntArray = data.neurons.filter { it.role == NeuronRoles.SENSORY_LOOM }.map { it.index }.toIntArray()
    val sensoryLoomLeftIndices: IntArray = data.neurons.filter { it.role == NeuronRoles.SENSORY_LOOM && it.side == "left" }.map { it.index }.toIntArray()
    val sensoryLoomRightIndices: IntArray = data.neurons.filter { it.role == NeuronRoles.SENSORY_LOOM && it.side == "right" }.map { it.index }.toIntArray()
    val sensoryReelIndices: IntArray = data.neurons.filter { it.role == NeuronRoles.SENSORY_REEL }.map { it.index }.toIntArray()
    val sensoryJonIndices: IntArray = data.neurons.filter { it.role == NeuronRoles.SENSORY_JON }.map { it.index }.toIntArray()
    val motorFeedIndices: IntArray = data.neurons.filter { it.role == NeuronRoles.MOTOR_FEED }.map { it.index }.toIntArray()
    val motorEscapeIndices: IntArray = data.neurons.filter { it.role == NeuronRoles.MOTOR_ESCAPE }.map { it.index }.toIntArray()
    val motorGroomIndices: IntArray = data.neurons.filter { it.role == NeuronRoles.MOTOR_GROOM }.map { it.index }.toIntArray()
    val rewardPamIndices: IntArray = data.neurons.filter { it.role == NeuronRoles.REWARD_PAM }.map { it.index }.toIntArray()

    // Double buffering
    private var currentX = DoubleArray(neuronCount)
    private var nextX = DoubleArray(neuronCount)

    // Stimulus queue and active stimuli
    private val stimulusQueue = ConcurrentLinkedQueue<TimedStimulus>()
    private val activeStimuli = mutableListOf<TimedStimulus>()

    private var tickCount = 0L

    private val _snapshotFlow = MutableStateFlow(createSnapshot(0L, currentX))
    val snapshotFlow: StateFlow<BrainSnapshot> = _snapshotFlow.asStateFlow()

    private var engineJob: Job? = null

    init {
        // Precompute weights
        // w_ij = sign * count / (sum of excitatory counts entering j)
        val excCountsPerPost = IntArray(neuronCount)
        for (syn in data.synapses) {
            if (syn.sign > 0) {
                excCountsPerPost[syn.postIndex] += syn.count
            }
        }

        @Suppress("UNCHECKED_CAST")
        incomingEdges = Array(neuronCount) { j ->
            val excSum = excCountsPerPost[j]
            val list = mutableListOf<IncomingEdge>()
            for (syn in data.synapses) {
                if (syn.postIndex == j) {
                    val w = if (excSum > 0) {
                        (syn.sign * syn.count).toDouble() / excSum
                    } else {
                        0.0
                    }
                    list.add(IncomingEdge(syn.preIndex, w))
                }
            }
            list
        }
    }

    /**
     * Start background ticking every 100 ms.
     */
    fun start(scope: CoroutineScope, tickIntervalMs: Long = 100L) {
        if (engineJob?.isActive == true) return
        engineJob = scope.launch {
            while (isActive) {
                step()
                delay(tickIntervalMs)
            }
        }
    }

    fun stop() {
        engineJob?.cancel()
        engineJob = null
    }

    /**
     * Inject a stimulus into all neurons matching a given role.
     */
    fun injectRole(role: String, intensity: Double = 1.0, durationTicks: Int) {
        val indices = data.neurons.filter { it.role == role }.map { it.index }.toIntArray()
        if (indices.isNotEmpty() && durationTicks > 0) {
            stimulusQueue.offer(TimedStimulus(indices, intensity, durationTicks))
        }
    }

    /**
     * Inject a stimulus into all neurons matching a role and side (e.g. left vs right loom).
     */
    fun injectRoleSide(role: String, side: String, intensity: Double = 1.0, durationTicks: Int) {
        val indices = data.neurons.filter { it.role == role && it.side == side }.map { it.index }.toIntArray()
        if (indices.isNotEmpty() && durationTicks > 0) {
            stimulusQueue.offer(TimedStimulus(indices, intensity, durationTicks))
        }
    }

    /**
     * Inject a stimulus into specific neuron indices.
     */
    fun injectIndices(indices: IntArray, intensity: Double = 1.0, durationTicks: Int) {
        if (indices.isNotEmpty() && durationTicks > 0) {
            stimulusQueue.offer(TimedStimulus(indices, intensity, durationTicks))
        }
    }

    /**
     * Executes one synchronous tick.
     * Optional manualInputs can be provided for unit testing.
     */
    fun step(manualInputs: DoubleArray? = null): BrainSnapshot {
        tickCount++

        // 1. Drain incoming stimuli queue
        while (true) {
            val stim = stimulusQueue.poll() ?: break
            activeStimuli.add(stim)
        }

        // 2. Accumulate external inputs I_j for this tick
        val inputs = DoubleArray(neuronCount)
        if (manualInputs != null) {
            for (i in 0 until minOf(neuronCount, manualInputs.size)) {
                inputs[i] = manualInputs[i]
            }
        }

        val it = activeStimuli.iterator()
        while (it.hasNext()) {
            val stim = it.next()
            if (stim.remainingTicks > 0) {
                for (idx in stim.neuronIndices) {
                    inputs[idx] = maxOf(inputs[idx], stim.intensity)
                }
                stim.remainingTicks--
            }
            if (stim.remainingTicks <= 0) {
                it.remove()
            }
        }

        // 3. Double-buffered synchronous update
        // x_j(t+1) = clamp(LEAK*x_j + (1-LEAK)*clamp(GAIN*sum_i(w_ij*x_i) + I_j, 0, 1), 0, 1)
        for (j in 0 until neuronCount) {
            var sum = 0.0
            val edges = incomingEdges[j]
            for (e in edges.indices) {
                val edge = edges[e]
                sum += edge.weight * currentX[edge.preIndex]
            }

            val rawDrive = gain * sum + inputs[j]
            val clampedDrive = rawDrive.coerceIn(0.0, 1.0)
            val updated = (leak * currentX[j] + (1.0 - leak) * clampedDrive).coerceIn(0.0, 1.0)
            nextX[j] = updated
        }

        // Swap buffers
        val temp = currentX
        currentX = nextX
        nextX = temp

        val snapshot = createSnapshot(tickCount, currentX)
        _snapshotFlow.value = snapshot
        return snapshot
    }

    fun reset() {
        currentX.fill(0.0)
        nextX.fill(0.0)
        activeStimuli.clear()
        stimulusQueue.clear()
        tickCount = 0L
        _snapshotFlow.value = createSnapshot(0L, currentX)
    }

    private fun createSnapshot(tick: Long, x: DoubleArray): BrainSnapshot {
        val floatActs = FloatArray(x.size) { i -> x[i].toFloat() }

        val feedActs = FloatArray(motorFeedIndices.size) { i -> floatActs[motorFeedIndices[i]] }
        val escapeActs = FloatArray(motorEscapeIndices.size) { i -> floatActs[motorEscapeIndices[i]] }
        val groomActs = FloatArray(motorGroomIndices.size) { i -> floatActs[motorGroomIndices[i]] }

        var pamSum = 0f
        for (idx in rewardPamIndices) {
            pamSum += floatActs[idx]
        }
        val pamMean = if (rewardPamIndices.isNotEmpty()) pamSum / rewardPamIndices.size else 0f

        val isEating = feedActs.any { it >= 0.5f }
        val isEscaping = escapeActs.any { it >= 0.5f }
        val isGrooming = groomActs.any { it >= 0.5f }
        val isDopamineStare = pamMean >= 0.5f

        return BrainSnapshot(
            tick = tick,
            activations = floatActs,
            motorFeed = feedActs,
            motorEscape = escapeActs,
            motorGroom = groomActs,
            rewardPamMean = pamMean,
            isEating = isEating,
            isEscaping = isEscaping,
            isGrooming = isGrooming,
            isDopamineStare = isDopamineStare
        )
    }
}
