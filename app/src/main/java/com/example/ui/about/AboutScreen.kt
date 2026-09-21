package com.example.ui.about

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

@Composable
fun AboutScreen(
    connectomeData: ConnectomeData,
    snapshot: BrainSnapshot?,
    isDebugEnabled: Boolean,
    onToggleDebug: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredNeurons = remember(connectomeData.neurons, searchQuery) {
        if (searchQuery.isBlank()) {
            connectomeData.neurons
        } else {
            val q = searchQuery.trim().lowercase()
            connectomeData.neurons.filter { n ->
                n.cellType.lowercase().contains(q) ||
                        n.role.lowercase().contains(q) ||
                        n.side.lowercase().contains(q) ||
                        n.id.contains(q)
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
            .testTag("about_screen_lazy_column")
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            // Screen Title
            Text(
                text = stringResource(R.string.about_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // --- 1. Dataset & License ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.about_dataset_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.about_dataset_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.about_license_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = connectomeData.meta.license.ifBlank { "CC-BY 4.0 (FlyWire Consortium)" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // --- 2. Scientific Citations ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Science,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.about_citations_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    CitationItem(stringResource(R.string.about_citation_1))
                    CitationItem(stringResource(R.string.about_citation_2))
                    CitationItem(stringResource(R.string.about_citation_3))
                }
            }
        }

        // --- 3. Live Neural Activity Debug Monitor ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .testTag("about_debug_monitor_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1E2330)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.about_debug_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Tick: ${snapshot?.tick ?: 0L}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF90CAF9)
                            )
                        }
                        Switch(
                            checked = isDebugEnabled,
                            onCheckedChange = { onToggleDebug() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF00E676),
                                checkedTrackColor = Color(0x6600E676)
                            )
                        )
                    }

                    if (isDebugEnabled && snapshot != null) {
                        Spacer(modifier = Modifier.height(12.dp))

                        // MN9 Proboscis motor
                        val mn9Mean = if (snapshot.motorFeed.isNotEmpty()) snapshot.motorFeed.average().toFloat() else 0f
                        DebugActivationRow(
                            label = stringResource(R.string.about_debug_mn9),
                            value = mn9Mean,
                            activeThreshold = 0.5f,
                            color = Color(0xFFF39C12)
                        )

                        // DNp01 Giant Fiber Left
                        val dnp01Left = connectomeData.neurons.firstOrNull { it.role == NeuronRoles.MOTOR_ESCAPE && it.side == "left" }?.index
                        val dnp01LeftVal = if (dnp01Left != null) snapshot.getActivation(dnp01Left) else 0f
                        DebugActivationRow(
                            label = stringResource(R.string.about_debug_dnp01_l),
                            value = dnp01LeftVal,
                            activeThreshold = 0.5f,
                            color = Color(0xFFE74C3C)
                        )

                        // DNp01 Giant Fiber Right
                        val dnp01Right = connectomeData.neurons.firstOrNull { it.role == NeuronRoles.MOTOR_ESCAPE && it.side == "right" }?.index
                        val dnp01RightVal = if (dnp01Right != null) snapshot.getActivation(dnp01Right) else 0f
                        DebugActivationRow(
                            label = stringResource(R.string.about_debug_dnp01_r),
                            value = dnp01RightVal,
                            activeThreshold = 0.5f,
                            color = Color(0xFFE74C3C)
                        )

                        // Mean PAM Dopamine
                        DebugActivationRow(
                            label = stringResource(R.string.about_debug_pam),
                            value = snapshot.rewardPamMean,
                            activeThreshold = 0.5f,
                            color = Color(0xFFFFD54F)
                        )
                    }
                }
            }
        }

        // --- 4. All 127 Neurons Catalog ---
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.about_all_neurons_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("neuron_search_field"),
                placeholder = { Text(stringResource(R.string.about_search_hint)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors()
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        items(filteredNeurons, key = { it.id }) { neuron ->
            NeuronRowCard(
                neuron = neuron,
                activation = snapshot?.getActivation(neuron.index) ?: 0f
            )
        }

        item {
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun CitationItem(text: String) {
    Row(
        modifier = Modifier.padding(vertical = 3.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text("• ", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DebugActivationRow(
    label: String,
    value: Float,
    activeThreshold: Float,
    color: Color
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, color = Color(0xFFCFD8DC), fontSize = 12.sp)
            Text(
                text = "%.3f %s".format(value, if (value >= activeThreshold) "(ACTIVE)" else ""),
                color = if (value >= activeThreshold) Color(0xFF00E676) else color,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        LinearProgressIndicator(
            progress = { value.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = if (value >= activeThreshold) Color(0xFF00E676) else color,
            trackColor = Color(0x33FFFFFF)
        )
    }
}

@Composable
private fun NeuronRowCard(
    neuron: Neuron,
    activation: Float
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = neuron.cellType,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = neuron.side,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = neuron.role,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "ID: ${neuron.id}",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "%.3f".format(activation),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (activation >= 0.5f) Color(0xFFE53935) else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "#${neuron.index}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}
