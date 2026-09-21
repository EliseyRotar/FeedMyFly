package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Bathtub
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.game.FlyVisualState
import com.example.ui.about.AboutScreen
import com.example.ui.brain.BrainCanvas
import com.example.ui.components.FactPopupDialog
import com.example.ui.fly.FlyCanvas
import com.example.ui.reels.ReelsView
import com.example.ui.settings.SettingsSheet
import com.example.viewmodel.FlyUiState
import com.example.viewmodel.FlyViewModel

enum class MainTab {
    FLY_ROOM,
    BRAIN_VIEW,
    REELS,
    ABOUT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: FlyViewModel,
    uiState: FlyUiState,
    onLanguageChanged: (String) -> Unit
) {
    var selectedTab by remember { mutableStateOf(MainTab.FLY_ROOM) }
    var showSettingsSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    // Quick brain toggle button
                    IconButton(
                        onClick = {
                            selectedTab = if (selectedTab == MainTab.BRAIN_VIEW) MainTab.FLY_ROOM else MainTab.BRAIN_VIEW
                        },
                        modifier = Modifier.testTag("top_bar_brain_toggle")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Hub,
                            contentDescription = stringResource(R.string.nav_brain),
                            tint = if (selectedTab == MainTab.BRAIN_VIEW) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    // Settings button
                    IconButton(
                        onClick = { showSettingsSheet = true },
                        modifier = Modifier.testTag("top_bar_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.nav_settings)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == MainTab.FLY_ROOM,
                    onClick = { selectedTab = MainTab.FLY_ROOM },
                    icon = { Icon(Icons.Default.Spa, contentDescription = stringResource(R.string.nav_home)) },
                    label = { Text(stringResource(R.string.nav_home)) },
                    modifier = Modifier.testTag("nav_tab_fly_room")
                )
                NavigationBarItem(
                    selected = selectedTab == MainTab.BRAIN_VIEW,
                    onClick = { selectedTab = MainTab.BRAIN_VIEW },
                    icon = { Icon(Icons.Default.Hub, contentDescription = stringResource(R.string.nav_brain)) },
                    label = { Text(stringResource(R.string.nav_brain)) },
                    modifier = Modifier.testTag("nav_tab_brain_view")
                )
                NavigationBarItem(
                    selected = selectedTab == MainTab.REELS,
                    onClick = { selectedTab = MainTab.REELS },
                    icon = { Icon(Icons.Default.PlayCircle, contentDescription = stringResource(R.string.nav_reels)) },
                    label = { Text(stringResource(R.string.nav_reels)) },
                    modifier = Modifier.testTag("nav_tab_reels")
                )
                NavigationBarItem(
                    selected = selectedTab == MainTab.ABOUT,
                    onClick = { selectedTab = MainTab.ABOUT },
                    icon = { Icon(Icons.Default.Info, contentDescription = stringResource(R.string.nav_about)) },
                    label = { Text(stringResource(R.string.nav_about)) },
                    modifier = Modifier.testTag("nav_tab_about")
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedTab) {
                MainTab.FLY_ROOM -> {
                    FlyRoomContent(
                        uiState = uiState,
                        onFeed = { viewModel.onFeedClicked() },
                        onTapFly = { isLeft -> viewModel.onFlyTapped(isLeft) },
                        onRubFly = { viewModel.onBathRubbing() },
                        onToggleBath = { viewModel.toggleBathMode() }
                    )
                }
                MainTab.BRAIN_VIEW -> {
                    if (uiState.isInitialized) {
                        BrainCanvas(
                            connectomeData = viewModel.connectomeData,
                            snapshot = uiState.brainSnapshot,
                            selectedNeuron = uiState.selectedNeuron,
                            onNeuronSelected = { viewModel.selectNeuron(it) }
                        )
                    }
                }
                MainTab.REELS -> {
                    ReelsView(
                        onSwipe = { viewModel.onReelSwiped() },
                        dopamineLevel = uiState.dopamine
                    )
                }
                MainTab.ABOUT -> {
                    if (uiState.isInitialized) {
                        AboutScreen(
                            connectomeData = viewModel.connectomeData,
                            snapshot = uiState.brainSnapshot,
                            isDebugEnabled = uiState.isDebugMonitorEnabled,
                            onToggleDebug = { viewModel.toggleDebugMonitor() }
                        )
                    }
                }
            }

            // Fact Popup Dialog (if triggered)
            uiState.activeFactPopup?.let { popup ->
                FactPopupDialog(
                    popup = popup,
                    onDismiss = { viewModel.dismissFactPopup() }
                )
            }

            // Settings Bottom Sheet
            if (showSettingsSheet) {
                SettingsSheet(
                    currentLanguage = uiState.currentLanguage,
                    onLanguageSelected = { lang ->
                        onLanguageChanged(lang)
                        showSettingsSheet = false
                    },
                    onDismiss = { showSettingsSheet = false }
                )
            }
        }
    }
}

@Composable
private fun FlyRoomContent(
    uiState: FlyUiState,
    onFeed: () -> Unit,
    onTapFly: (Boolean) -> Unit,
    onRubFly: () -> Unit,
    onToggleBath: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("fly_room_content")
    ) {
        // Top status card with Hunger & Dopamine meters and state pill
        MetersAndStatusHeader(
            hunger = uiState.hunger,
            dopamine = uiState.dopamine,
            visualState = uiState.visualState
        )

        // Interactive Fly Canvas Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            FlyCanvas(
                visualState = uiState.visualState,
                hunger = uiState.hunger,
                dopamine = uiState.dopamine,
                isBathMode = uiState.isBathModeActive,
                onTap = onTapFly,
                onRub = onRubFly,
                modifier = Modifier.fillMaxSize()
            )

            // Mode toggle chip (Bath Mode)
            FilterChip(
                selected = uiState.isBathModeActive,
                onClick = onToggleBath,
                label = { Text(stringResource(R.string.bath_mode_button)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Bathtub,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .testTag("bath_mode_chip"),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF00ACC1),
                    selectedLabelColor = Color.White
                )
            )

            // Hint text overlay at bottom of canvas
            Surface(
                color = Color(0xB3FFFFFF),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp)
            ) {
                Text(
                    text = if (uiState.isBathModeActive) {
                        stringResource(R.string.bath_hint)
                    } else {
                        stringResource(R.string.tap_hint)
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF424242),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }

        // Bottom Controls Area: FEED Button & Status
        BottomFeedingControls(
            feedEnabled = uiState.feedButtonEnabled,
            onFeed = onFeed
        )
    }
}

@Composable
private fun MetersAndStatusHeader(
    hunger: Float,
    dopamine: Float,
    visualState: FlyVisualState
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            // Behavioral State Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val stateText = when (visualState) {
                    FlyVisualState.IDLE -> stringResource(R.string.state_idle)
                    FlyVisualState.BUZZING -> stringResource(R.string.state_buzzing)
                    FlyVisualState.EATING -> stringResource(R.string.state_eating)
                    FlyVisualState.DOPAMINE_STARE -> stringResource(R.string.state_dopamine_stare)
                    FlyVisualState.GROOMING -> stringResource(R.string.state_grooming)
                    FlyVisualState.ESCAPED -> stringResource(R.string.state_escaped)
                }

                val stateBadgeColor = when (visualState) {
                    FlyVisualState.IDLE -> Color(0xFF78909C)
                    FlyVisualState.BUZZING, FlyVisualState.ESCAPED -> Color(0xFFE53935)
                    FlyVisualState.EATING -> Color(0xFFFB8C00)
                    FlyVisualState.DOPAMINE_STARE -> Color(0xFFAB47BC)
                    FlyVisualState.GROOMING -> Color(0xFF00897B)
                }

                Surface(
                    color = stateBadgeColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(stateBadgeColor, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stateText,
                            color = stateBadgeColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Hunger Meter
            MeterRow(
                label = stringResource(R.string.meter_hunger, (hunger * 100).toInt()),
                progress = hunger,
                barColor = Color(0xFFE65100),
                trackColor = Color(0x33E65100)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Dopamine Meter
            MeterRow(
                label = stringResource(R.string.meter_dopamine, (dopamine * 100).toInt()),
                progress = dopamine,
                barColor = Color(0xFF7B1FA2),
                trackColor = Color(0x337B1FA2)
            )
        }
    }
}

@Composable
private fun MeterRow(
    label: String,
    progress: Float,
    barColor: Color,
    trackColor: Color
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = barColor,
            trackColor = trackColor
        )
    }
}

@Composable
private fun BottomFeedingControls(
    feedEnabled: Boolean,
    onFeed: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = onFeed,
                enabled = feedEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("feed_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF9800),
                    disabledContainerColor = Color(0xFFBDBDBD)
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Restaurant,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.btn_feed),
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (feedEnabled) {
                    stringResource(R.string.feed_hint_hungry)
                } else {
                    stringResource(R.string.feed_hint_full)
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (feedEnabled) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFFD32F2F)
            )
        }
    }
}
