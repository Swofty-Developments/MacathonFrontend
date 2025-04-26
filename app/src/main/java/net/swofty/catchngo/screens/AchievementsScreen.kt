package net.swofty.catchngo.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import net.swofty.catchngo.R
import net.swofty.catchngo.api.ApiModels
import net.swofty.catchngo.models.AchievementsViewModel
import net.swofty.catchngo.models.GameViewModel

class AchievementsScreenClass {
    // Define custom colors using light theme like other screens
    private val whiteBackground = Color.White
    private val lightSurface = Color(0xFFF5F8FA)
    private val accentBlue = Color(0xFF1DA1F2)
    private val accentGold = Color(0xFFFFD700)
    private val textPrimary = Color(0xFF14171A)
    private val textSecondary = Color(0xFF657786)
    private val achievedGreen = Color(0xFF00BA7C)
    private val lockedGray = Color(0xFFAAB8C2)
    private val cardBorder = Color(0xFFE1E8ED)
    private val accentPurple = Color(0xFF9C27B0)

    // Poppins Font Family
    private val poppinsFamily = FontFamily(
        Font(R.font.poppins_regular, FontWeight.Normal),
        Font(R.font.poppins_medium, FontWeight.Medium),
        Font(R.font.poppins_semibold, FontWeight.SemiBold),
        Font(R.font.poppins_bold, FontWeight.Bold)
    )

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun AchievementsScreen(
        onBackClick: () -> Unit,
        achievementsViewModel: AchievementsViewModel = viewModel(),
        gameViewModel: GameViewModel = viewModel()
    ) {
        val coroutineScope = rememberCoroutineScope()

        // State
        val userAchievementsState by achievementsViewModel.userAchievementsState.observeAsState()
        val missingAchievementsState by achievementsViewModel.missingAchievementsState.observeAsState()
        val points by remember { derivedStateOf { gameViewModel.getPoints() ?: 0 } }

        // Track which tab is selected
        var selectedTab by remember { mutableStateOf(0) }
        var isRefreshing by remember { mutableStateOf(false) }

        val refreshRotation by animateFloatAsState(
            targetValue = if (isRefreshing) 360f else 0f,
            label = "refreshRotation"
        )

        // Load data when the screen is first displayed
        LaunchedEffect(Unit) {
            achievementsViewModel.getUserAchievements()
            achievementsViewModel.getMissingAchievements()
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Achievements",
                            fontFamily = poppinsFamily,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                isRefreshing = true
                                coroutineScope.launch {
                                    achievementsViewModel.getUserAchievements()
                                    achievementsViewModel.getMissingAchievements()
                                    isRefreshing = false
                                }
                            }
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                modifier = Modifier.rotate(refreshRotation),
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = accentBlue,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                        actionIconContentColor = Color.White
                    )
                )
            },
            containerColor = whiteBackground
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(whiteBackground)
                    .padding(padding)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Points summary at the top
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = lightSurface
                        ),
                        elevation = CardDefaults.cardElevation(4.dp),
                        border = BorderStroke(1.dp, cardBorder),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Your Achievement Points",
                                fontFamily = poppinsFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                                color = textPrimary
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                "$points",
                                fontFamily = poppinsFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 32.sp,
                                color = accentBlue
                            )

                            Text(
                                "Keep going to unlock more achievements!",
                                fontFamily = poppinsFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 14.sp,
                                color = textSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // Tabs
                    TabRow(
                        selectedTabIndex = selectedTab,
                        contentColor = accentBlue,
                        containerColor = whiteBackground,
                        divider = { Divider(color = cardBorder) },
                        indicator = { tabPositions ->
                            Box(
                                Modifier
                                    .tabIndicatorOffset(tabPositions[selectedTab])
                                    .height(3.dp)
                                    .background(accentBlue)
                            )
                        }
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = {
                                Text(
                                    "EARNED",
                                    fontSize = 14.sp,
                                    fontFamily = poppinsFamily,
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            icon = { Icon(Icons.Default.MoreVert, contentDescription = null) },
                            selectedContentColor = accentBlue,
                            unselectedContentColor = textSecondary
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = {
                                Text(
                                    "LOCKED",
                                    fontSize = 14.sp,
                                    fontFamily = poppinsFamily,
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            icon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            selectedContentColor = accentBlue,
                            unselectedContentColor = textSecondary
                        )
                    }

                    // Content based on selected tab
                    when (selectedTab) {
                        0 -> EarnedAchievementsTab(userAchievementsState)
                        1 -> LockedAchievementsTab(missingAchievementsState)
                    }
                }
            }
        }
    }

    @Composable
    private fun EarnedAchievementsTab(state: AchievementsViewModel.UserAchievementsState?) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (state) {
                is AchievementsViewModel.UserAchievementsState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = accentBlue
                    )
                }

                is AchievementsViewModel.UserAchievementsState.Success -> {
                    if (state.achievements.isEmpty()) {
                        EmptyStateMessage("You haven't earned any achievements yet.")
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(state.achievements) { achievement ->
                                AchievementCard(
                                    title = achievement.title,
                                    description = achievement.description,
                                    points = achievement.points,
                                    isEarned = true
                                )
                            }
                        }
                    }
                }

                is AchievementsViewModel.UserAchievementsState.Error -> {
                    ErrorStateMessage(state.message)
                }

                else -> {
                    // Initial state or null
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = accentBlue
                    )
                }
            }
        }
    }

    @Composable
    private fun LockedAchievementsTab(state: AchievementsViewModel.MissingAchievementsState?) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (state) {
                is AchievementsViewModel.MissingAchievementsState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = accentBlue
                    )
                }

                is AchievementsViewModel.MissingAchievementsState.Success -> {
                    if (state.achievements.isEmpty()) {
                        EmptyStateMessage("You've earned all possible achievements!")
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(state.achievements) { achievement ->
                                AchievementCard(
                                    title = achievement.title,
                                    description = achievement.description,
                                    points = achievement.points,
                                    isEarned = false,
                                    requirement = "Make at least ${achievement.minFriends} friends"
                                )
                            }
                        }
                    }
                }

                is AchievementsViewModel.MissingAchievementsState.Error -> {
                    ErrorStateMessage(state.message)
                }

                else -> {
                    // Initial state or null
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = accentBlue
                    )
                }
            }
        }
    }

    @Composable
    private fun AchievementCard(
        title: String,
        description: String,
        points: Int,
        isEarned: Boolean,
        requirement: String? = null
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isEarned) Color.White else lightSurface
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = if (isEarned) 4.dp else 1.dp
            ),
            border = BorderStroke(
                width = 1.dp,
                color = if (isEarned) achievedGreen else cardBorder
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Achievement badge/icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            if (isEarned) achievedGreen.copy(alpha = 0.1f) else lockedGray.copy(
                                alpha = 0.1f
                            ),
                            CircleShape
                        )
                        .border(
                            width = 2.dp,
                            color = if (isEarned) achievedGreen else lockedGray,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isEarned) Icons.Default.Lock else Icons.Default.Lock,
                        contentDescription = null,
                        tint = if (isEarned) achievedGreen else lockedGray,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Achievement details
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontFamily = poppinsFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = textPrimary
                    )

                    Text(
                        text = description,
                        fontFamily = poppinsFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 14.sp,
                        color = textSecondary
                    )

                    if (!isEarned && requirement != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = requirement,
                            fontFamily = poppinsFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            color = lockedGray
                        )
                    }
                }

                // Points
                Box(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (isEarned) achievedGreen.copy(alpha = 0.1f) else lockedGray.copy(
                                alpha = 0.1f
                            )
                        )
                        .border(
                            width = 1.dp,
                            color = if (isEarned) achievedGreen else lockedGray,
                            shape = RoundedCornerShape(50)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+$points",
                        fontFamily = poppinsFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (isEarned) achievedGreen else lockedGray
                    )
                }
            }
        }
    }

    @Composable
    private fun EmptyStateMessage(message: String) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                    tint = textSecondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = message,
                    fontFamily = poppinsFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    color = textSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    @Composable
    private fun ErrorStateMessage(message: String) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                    tint = Color.Red
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Error loading achievements",
                    fontFamily = poppinsFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = textPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = message,
                    fontFamily = poppinsFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = textSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}