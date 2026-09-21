package com.example.game

import androidx.annotation.StringRes
import com.example.R
import com.example.data.NeuronRoles
import com.example.engine.BrainSnapshot
import com.example.engine.ConnectomeEngine

/**
 * Manages first-time scientific fact popups when a neuron role reaches >= 0.5 firing activation.
 * Enforces a strict 30-second cooldown between popups and only shows each fact once per session.
 */
class FactPopupManager {

    private val shownRoles = mutableSetOf<String>()
    private var lastPopupTimeMs = 0L

    data class FactPopup(
        val role: String,
        @StringRes val stringResId: Int
    )

    fun checkAndTriggerPopup(
        snapshot: BrainSnapshot,
        engine: ConnectomeEngine,
        currentTimeMs: Long
    ): FactPopup? {
        if (currentTimeMs - lastPopupTimeMs < GameConfig.FACT_POPUP_COOLDOWN_MS) {
            return null
        }

        // Roles to monitor
        val candidates = listOf(
            Triple(NeuronRoles.MOTOR_FEED, engine.motorFeedIndices, R.string.fact_motor_feed),
            Triple(NeuronRoles.MOTOR_ESCAPE, engine.motorEscapeIndices, R.string.fact_motor_escape),
            Triple(NeuronRoles.SENSORY_LOOM, engine.sensoryLoomIndices, R.string.fact_sensory_loom),
            Triple(NeuronRoles.REWARD_PAM, engine.rewardPamIndices, R.string.fact_reward_pam),
            Triple(NeuronRoles.SENSORY_REEL, engine.sensoryReelIndices, R.string.fact_sensory_reel),
            Triple(NeuronRoles.SENSORY_SUGAR, engine.sensorySugarIndices, R.string.fact_sensory_sugar),
            Triple(NeuronRoles.SENSORY_JON, engine.sensoryJonIndices, R.string.fact_sensory_jon)
        )

        for ((role, indices, stringRes) in candidates) {
            if (role !in shownRoles) {
                val hasFired = indices.any { snapshot.getActivation(it) >= GameConfig.MOTOR_ACTIVE_THRESHOLD }
                if (hasFired) {
                    shownRoles.add(role)
                    lastPopupTimeMs = currentTimeMs
                    return FactPopup(role, stringRes)
                }
            }
        }

        return null
    }

    fun reset() {
        shownRoles.clear()
        lastPopupTimeMs = 0L
    }
}
