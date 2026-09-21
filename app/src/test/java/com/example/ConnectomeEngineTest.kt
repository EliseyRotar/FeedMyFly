package com.example

import com.example.data.ConnectomeParser
import com.example.data.NeuronRoles
import com.example.engine.ConnectomeEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ConnectomeEngineTest {

    private lateinit var engine: ConnectomeEngine

    @Before
    fun setUp() {
        val file = File("src/main/assets/connectome.json")
        val jsonString = if (file.exists()) {
            file.readText()
        } else {
            File("app/src/main/assets/connectome.json").readText()
        }
        val data = ConnectomeParser.parse(jsonString)
        engine = ConnectomeEngine(data)
    }

    @Test
    fun testDeterminism_sameStimulusSequenceGivesIdenticalStates() {
        // Run sequence on engine 1
        val engine1 = ConnectomeEngine(engine.data)
        for (t in 0 until 50) {
            if (t % 10 == 0) {
                engine1.injectRole(NeuronRoles.SENSORY_SUGAR, 1.0, 3)
            }
            if (t % 15 == 0) {
                engine1.injectRole(NeuronRoles.SENSORY_LOOM, 0.8, 2)
            }
            engine1.step()
        }
        val snapshot1 = engine1.snapshotFlow.value

        // Run exact same sequence on engine 2
        val engine2 = ConnectomeEngine(engine.data)
        for (t in 0 until 50) {
            if (t % 10 == 0) {
                engine2.injectRole(NeuronRoles.SENSORY_SUGAR, 1.0, 3)
            }
            if (t % 15 == 0) {
                engine2.injectRole(NeuronRoles.SENSORY_LOOM, 0.8, 2)
            }
            engine2.step()
        }
        val snapshot2 = engine2.snapshotFlow.value

        assertEquals("Ticks must match", snapshot1.tick, snapshot2.tick)
        for (i in 0 until engine.neuronCount) {
            assertEquals(
                "Neuron $i activation must be perfectly identical",
                snapshot1.activations[i],
                snapshot2.activations[i],
                0.000001f
            )
        }
    }

    @Test
    fun testSelfTest_sensorySugar() {
        engine.reset()
        // Inject 1.0 into all SENSORY_SUGAR neurons for 150 ticks
        engine.injectRole(NeuronRoles.SENSORY_SUGAR, intensity = 1.0, durationTicks = 150)
        var lastSnapshot = engine.snapshotFlow.value
        for (t in 0 until 150) {
            lastSnapshot = engine.step()
        }

        // MOTOR_FEED expected: [0.71, 0.72] within 0.02
        val feed = lastSnapshot.motorFeed
        assertEquals("MOTOR_FEED count", 2, feed.size)
        assertEquals(0.71f, feed[0], 0.02f)
        assertEquals(0.72f, feed[1], 0.02f)

        // Crosstalk checks
        for (esc in lastSnapshot.motorEscape) {
            assertEquals(0.0f, esc, 0.02f)
        }
        for (groom in lastSnapshot.motorGroom) {
            assertEquals(0.0f, groom, 0.02f)
        }
        assertEquals(0.0f, lastSnapshot.rewardPamMean, 0.02f)
    }

    @Test
    fun testSelfTest_sensoryLoom() {
        engine.reset()
        engine.injectRole(NeuronRoles.SENSORY_LOOM, intensity = 1.0, durationTicks = 150)
        var lastSnapshot = engine.snapshotFlow.value
        for (t in 0 until 150) {
            lastSnapshot = engine.step()
        }

        // MOTOR_ESCAPE expected: [0.92, 0.90] within 0.02
        val esc = lastSnapshot.motorEscape
        assertEquals("MOTOR_ESCAPE count", 2, esc.size)
        assertEquals(0.92f, esc[0], 0.02f)
        assertEquals(0.90f, esc[1], 0.02f)

        // MOTOR_FEED should be 0
        for (f in lastSnapshot.motorFeed) {
            assertEquals(0.0f, f, 0.02f)
        }
        // MOTOR_GROOM ~ 0.01
        for (g in lastSnapshot.motorGroom) {
            assertEquals(0.01f, g, 0.02f)
        }
        // REWARD_PAM_mean ~ 0.07
        assertEquals(0.07f, lastSnapshot.rewardPamMean, 0.02f)
    }

    @Test
    fun testSelfTest_sensoryReel() {
        engine.reset()
        engine.injectRole(NeuronRoles.SENSORY_REEL, intensity = 1.0, durationTicks = 150)
        var lastSnapshot = engine.snapshotFlow.value
        for (t in 0 until 150) {
            lastSnapshot = engine.step()
        }

        // REWARD_PAM_mean ~ 0.84 within 0.02
        assertEquals(0.84f, lastSnapshot.rewardPamMean, 0.02f)

        // MOTOR_FEED ~ [0.01, 0.0]
        assertEquals(0.01f, lastSnapshot.motorFeed[0], 0.02f)
        assertEquals(0.0f, lastSnapshot.motorFeed[1], 0.02f)

        // MOTOR_ESCAPE ~ [0.0, 0.0]
        for (esc in lastSnapshot.motorEscape) {
            assertEquals(0.0f, esc, 0.02f)
        }
    }

    @Test
    fun testSelfTest_sensoryJon() {
        engine.reset()
        engine.injectRole(NeuronRoles.SENSORY_JON, intensity = 1.0, durationTicks = 150)
        var lastSnapshot = engine.snapshotFlow.value
        for (t in 0 until 150) {
            lastSnapshot = engine.step()
        }

        // MOTOR_GROOM ~ [0.83, 0.85] within 0.02
        val groom = lastSnapshot.motorGroom
        assertEquals(0.83f, groom[0], 0.02f)
        assertEquals(0.85f, groom[1], 0.02f)

        // MOTOR_FEED ~ [0.0, 0.0]
        for (f in lastSnapshot.motorFeed) {
            assertEquals(0.0f, f, 0.02f)
        }
        // MOTOR_ESCAPE ~ [0.03, 0.03]
        for (e in lastSnapshot.motorEscape) {
            assertEquals(0.03f, e, 0.02f)
        }
        // REWARD_PAM_mean ~ 0.0
        assertEquals(0.0f, lastSnapshot.rewardPamMean, 0.02f)
    }

    @Test
    fun testSelfTest_loomSideSpecific() {
        // LOOM_left: right: 0.51, left: 0.75
        engine.reset()
        engine.injectRoleSide(NeuronRoles.SENSORY_LOOM, "left", intensity = 1.0, durationTicks = 150)
        var lastSnapshot = engine.snapshotFlow.value
        for (t in 0 until 150) {
            lastSnapshot = engine.step()
        }
        // index 72 is DNp01 right, index 73 is DNp01 left
        val dnp01Right = engine.data.neurons.first { it.role == NeuronRoles.MOTOR_ESCAPE && it.side == "right" }.index
        val dnp01Left = engine.data.neurons.first { it.role == NeuronRoles.MOTOR_ESCAPE && it.side == "left" }.index

        assertEquals(0.51f, lastSnapshot.getActivation(dnp01Right), 0.02f)
        assertEquals(0.75f, lastSnapshot.getActivation(dnp01Left), 0.02f)

        // LOOM_right: right: 0.82, left: 0.57
        engine.reset()
        engine.injectRoleSide(NeuronRoles.SENSORY_LOOM, "right", intensity = 1.0, durationTicks = 150)
        for (t in 0 until 150) {
            lastSnapshot = engine.step()
        }
        assertEquals(0.82f, lastSnapshot.getActivation(dnp01Right), 0.02f)
        assertEquals(0.57f, lastSnapshot.getActivation(dnp01Left), 0.02f)
    }
}
