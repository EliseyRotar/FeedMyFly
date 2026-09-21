package com.example.game

/**
 * Visual and behavioral states of the fruit fly.
 * No random numbers are used anywhere in the brain or behavior logic.
 */
enum class FlyVisualState {
    IDLE,
    BUZZING,
    EATING,
    DOPAMINE_STARE,
    GROOMING,
    ESCAPED
}
