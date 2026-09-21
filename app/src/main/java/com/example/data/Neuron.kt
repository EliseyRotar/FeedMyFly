package com.example.data

/**
 * Data class representing a neuron in the FlyWire fruit fly connectome subgraph.
 *
 * @param id Unique root ID from the FlyWire dataset
 * @param cellType Cell type name (e.g., LB3, CB0701, DNp01, LPLC2, PAM12)
 * @param side Side of the brain ("left", "right", "center")
 * @param role Functional role in the circuit
 * @param circuits Circuit names this neuron participates in (e.g. FEED, ESCAPE, REWARD, GROOM)
 * @param index 0-based index in the connectome neurons array
 */
data class Neuron(
    val id: String,
    val cellType: String,
    val side: String,
    val role: String,
    val circuits: List<String>,
    val index: Int
)

/**
 * Functional roles for neurons in the Feed My Fly connectome.
 */
object NeuronRoles {
    const val SENSORY_SUGAR = "SENSORY_SUGAR"
    const val SENSORY_LOOM = "SENSORY_LOOM"
    const val SENSORY_REEL = "SENSORY_REEL"
    const val SENSORY_JON = "SENSORY_JON"
    const val INTER = "INTER"
    const val REWARD_PAM = "REWARD_PAM"
    const val MOTOR_FEED = "MOTOR_FEED"
    const val MOTOR_ESCAPE = "MOTOR_ESCAPE"
    const val MOTOR_GROOM = "MOTOR_GROOM"
}

/**
 * Data class representing a directed synapse between two neurons.
 *
 * @param preIndex Index of presynaptic neuron
 * @param postIndex Index of postsynaptic neuron
 * @param count Synapse count (number of anatomical connections)
 * @param sign Connection sign: +1 for excitatory, -1 for inhibitory
 */
data class Synapse(
    val preIndex: Int,
    val postIndex: Int,
    val count: Int,
    val sign: Int
)
