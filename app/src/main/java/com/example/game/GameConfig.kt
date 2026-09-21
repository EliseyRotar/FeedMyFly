package com.example.game

/**
 * Plain constants for the game layer (these are NOT neurons).
 */
object GameConfig {
    // Hunger parameters
    const val HUNGER_RISE_PER_TICK = 0.002f
    const val HUNGER_DROP_PER_EAT_TICK = 0.05f
    const val FEED_MIN_HUNGER = 0.1f
    const val FEED_INJECT_TICKS = 20

    // Dopamine parameters
    // Dopamine meter: meter = clamp(meter + 0.05*meanPAM - 0.005, 0, 1)
    const val DOPAMINE_PAM_SCALE = 0.05f
    const val DOPAMINE_DECAY = 0.005f

    // Stimulus durations
    const val REEL_INJECT_TICKS = 10
    const val LOOM_INJECT_TICKS = 3
    const val JON_INJECT_TICKS = 5

    // Behavior activation threshold
    const val MOTOR_ACTIVE_THRESHOLD = 0.5f

    // Fly escape flight duration in ms before returning
    const val FLY_RETURN_DELAY_MS = 2000L

    // Fact popup cooldown in milliseconds (at most one popup every 30 seconds)
    const val FACT_POPUP_COOLDOWN_MS = 30000L

    // Engine tick rate
    const val TICK_INTERVAL_MS = 100L
}
