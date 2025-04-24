package net.swofty.catchngo.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import net.swofty.catchngo.models.GameViewModel

/**
 * Main home screen that displays after login
 */
class HomeScreen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun HomeContent(
        onLogout: () -> Unit,
        gameViewModel: GameViewModel = viewModel()
    ) {
        val userScore by gameViewModel.score.collectAsState()
        val nearbyPlayers by gameViewModel.nearbyPlayers.collectAsState()

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Catch N Go") },
                    actions = {
                        IconButton(onClick = onLogout) {
                            Icon(Icons.Default.ExitToApp, contentDescription = "Logout")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                // Score Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Your Score",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = userScore.toString(),
                            style = MaterialTheme.typography.headlineLarge,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        LinearProgressIndicator(
                            progress = { (userScore % 100) / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        )
                        Text(
                            text = "Level ${userScore / 100 + 1}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // Map View Placeholder
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(bottom = 16.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        // In a real app, this would be a Google Map or similar
                        Text(
                            text = "Map View\n(Would show your location and nearby players)",
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Location Tracking Status
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Location Tracking")
                        }

                        Switch(
                            checked = gameViewModel.locationTrackingEnabled.value,
                            onCheckedChange = { gameViewModel.toggleLocationTracking() }
                        )
                    }
                }

                // Nearby Players Section
                Text(
                    text = "Nearby Players",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                if (nearbyPlayers.isEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No players nearby. Try scanning!")
                        }
                    }
                } else {
                    nearbyPlayers.forEach { player ->
                        NearbyPlayerCard(
                            playerName = player.username,
                            distance = player.distanceMeters,
                            onCatchClick = { gameViewModel.catchPlayer(player.id) }
                        )
                    }
                }

                // Scan button
                Button(
                    onClick = { gameViewModel.scanForPlayers() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    Icon(Icons.Default.Search, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Scan for Players")
                }
            }
        }
    }

    @Composable
    fun NearbyPlayerCard(
        playerName: String,
        distance: Int,
        onCatchClick: () -> Unit
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = playerName,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "$distance meters away",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Button(onClick = onCatchClick) {
                    Text("Catch!")
                }
            }
        }
    }
}