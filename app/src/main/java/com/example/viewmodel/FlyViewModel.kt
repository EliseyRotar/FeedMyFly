package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ConnectomeData
import com.example.data.ConnectomeParser
import com.example.data.Neuron
import com.example.data.NeuronRoles
import com.example.engine.BrainSnapshot
import com.example.engine.ConnectomeEngine
import com.example.game.AudioBuzzer
import com.example.game.FactPopupManager
import com.example.game.FlyVisualState
import com.example.game.GameConfig
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * UI State exposed by FlyViewModel.
 */
data class FlyUiState(
    val isInitialized: Boolean = false,
    val hunger: Float = 0.5f,
    val dopamine: Float = 0.1f,
    val visualState: FlyVisualState = FlyVisualState.IDLE,
    val brainSnapshot: BrainSnapshot? = null,
    val selectedNeuron: Neuron? = null,
    val activeFactPopup: FactPopupManager.FactPopup? = null,
    val isDebugMonitorEnabled: Boolean = true,
    val isBathModeActive: Boolean = false,
    val currentLanguage: String = "en",
    val feedButtonEnabled: Boolean = true
)

class FlyViewModel(application: Application) : AndroidViewModel(application) {

    lateinit var connectomeData: ConnectomeData
        private set
    lateinit var engine: ConnectomeEngine
        private set

    private val buzzer = AudioBuzzer()
    private val factPopupManager = FactPopupManager()

    private val _uiState = MutableStateFlow(FlyUiState())
    val uiState: StateFlow<FlyUiState> = _uiState.asStateFlow()

    private var returnFlightJob: Job? = null
    private var isCurrentlyEscaped = false

    init {
        initializeEngine()
    }

    private fun initializeEngine() {
        viewModelScope.launch {
            val context = getApplication<Application>().applicationContext
            connectomeData = ConnectomeParser.loadFromAssets(context, "connectome.json")
            engine = ConnectomeEngine(connectomeData)

            _uiState.update {
                it.copy(
                    isInitialized = true,
                    brainSnapshot = engine.snapshotFlow.value
                )
            }

            startGameLoop()
        }
    }

    private fun startGameLoop() {
        viewModelScope.launch {
            while (isActive) {
                // Execute connectome step
                val snapshot = engine.step()

                // Update game layer
                val currentHunger = _uiState.value.hunger
                val currentDopamine = _uiState.value.dopamine

                // Hunger logic:
                // Hunger rises 0.002 per tick. While MN9 >= 0.5 the fly eats and hunger drops 0.05 per tick.
                val updatedHunger = if (snapshot.isEating) {
                    (currentHunger - GameConfig.HUNGER_DROP_PER_EAT_TICK).coerceIn(0f, 1f)
                } else {
                    (currentHunger + GameConfig.HUNGER_RISE_PER_TICK).coerceIn(0f, 1f)
                }

                // Dopamine meter logic:
                // meter = clamp(meter + 0.05*meanPAM - 0.005, 0, 1)
                val updatedDopamine = (currentDopamine +
                        GameConfig.DOPAMINE_PAM_SCALE * snapshot.rewardPamMean -
                        GameConfig.DOPAMINE_DECAY).coerceIn(0f, 1f)

                // Escape flight triggering:
                // If either DNp01 >= 0.5, fly takes off and returns after 2 seconds
                if (snapshot.isEscaping && !isCurrentlyEscaped) {
                    triggerEscapeTakeoff()
                }

                // Sound logic: synthesized fly buzz while a DNp01 is active or during takeoff
                val isBuzzing = snapshot.isEscaping || isCurrentlyEscaped
                buzzer.setBuzzing(isBuzzing)

                // Check for new scientific fact popup
                val currentTime = System.currentTimeMillis()
                val newPopup = if (_uiState.value.activeFactPopup == null) {
                    factPopupManager.checkAndTriggerPopup(snapshot, engine, currentTime)
                } else {
                    null
                }

                // Compute visual state
                val visualState = when {
                    isCurrentlyEscaped -> FlyVisualState.ESCAPED
                    snapshot.isEscaping -> FlyVisualState.BUZZING
                    snapshot.isEating -> FlyVisualState.EATING
                    snapshot.isDopamineStare -> FlyVisualState.DOPAMINE_STARE
                    snapshot.isGrooming -> FlyVisualState.GROOMING
                    else -> FlyVisualState.IDLE
                }

                val canFeed = updatedHunger > GameConfig.FEED_MIN_HUNGER

                _uiState.update { prev ->
                    prev.copy(
                        hunger = updatedHunger,
                        dopamine = updatedDopamine,
                        visualState = visualState,
                        brainSnapshot = snapshot,
                        activeFactPopup = prev.activeFactPopup ?: newPopup,
                        feedButtonEnabled = canFeed
                    )
                }

                delay(GameConfig.TICK_INTERVAL_MS)
            }
        }
    }

    /**
     * FEED button: inject 1.0 into every SENSORY_SUGAR neuron for 20 ticks, only if hunger > 0.1.
     */
    fun onFeedClicked() {
        if (_uiState.value.hunger > GameConfig.FEED_MIN_HUNGER) {
            engine.injectRole(NeuronRoles.SENSORY_SUGAR, 1.0, GameConfig.FEED_INJECT_TICKS)
        }
    }

    /**
     * Tap on the fly area (not on buttons or reels):
     * inject 1.0 for 3 ticks into SENSORY_LOOM neurons on the tapped side (left half of screen = side "left").
     */
    fun onFlyTapped(isLeftSide: Boolean) {
        val side = if (isLeftSide) "left" else "right"
        engine.injectRoleSide(NeuronRoles.SENSORY_LOOM, side, 1.0, GameConfig.LOOM_INJECT_TICKS)
    }

    /**
     * Reel area: a vertical pager of colorful fake reel cards.
     * Every swipe injects 1.0 into all SENSORY_REEL neurons for 10 ticks.
     */
    fun onReelSwiped() {
        engine.injectRole(NeuronRoles.SENSORY_REEL, 1.0, GameConfig.REEL_INJECT_TICKS)
    }

    /**
     * Johnston's organ (JO) antennal mechanosensory stimulation by bath rubbing gesture.
     */
    fun onBathRubbing() {
        engine.injectRole(NeuronRoles.SENSORY_JON, 1.0, GameConfig.JON_INJECT_TICKS)
    }

    fun toggleBathMode() {
        _uiState.update { it.copy(isBathModeActive = !it.isBathModeActive) }
    }

    fun dismissFactPopup() {
        _uiState.update { it.copy(activeFactPopup = null) }
    }

    fun selectNeuron(neuron: Neuron?) {
        _uiState.update { it.copy(selectedNeuron = neuron) }
    }

    fun toggleDebugMonitor() {
        _uiState.update { it.copy(isDebugMonitorEnabled = !it.isDebugMonitorEnabled) }
    }

    private fun triggerEscapeTakeoff() {
        isCurrentlyEscaped = true
        returnFlightJob?.cancel()
        returnFlightJob = viewModelScope.launch {
            delay(GameConfig.FLY_RETURN_DELAY_MS)
            isCurrentlyEscaped = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        buzzer.release()
        engine.stop()
    }
}
