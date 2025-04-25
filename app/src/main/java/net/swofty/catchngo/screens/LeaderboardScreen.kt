package net.swofty.catchngo.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import net.swofty.catchngo.R
import net.swofty.catchngo.api.ApiModels
import net.swofty.catchngo.models.GameViewModel
import net.swofty.catchngo.models.LeaderboardViewModel

/**
 * Leaderboard screen showing top users and the current user's rank.
 * Designed with a light (white) theme.
 */
class LeaderboardScreen {

    // Custom Colors (Light Theme)
    private val whiteBackground = Color.White
    private val lightSurface = Color(0xFFF5F8FA)
    private val accentBlue = Color(0xFF1DA1F2)
    private val accentPurple = Color(0xFF9C27B0)
    private val accentGold = Color(0xFFFFD700)
    private val accentSilver = Color(0xFFC0C0C0)
    private val accentBronze = Color(0xFFCD7F32)
    private val textPrimary = Color(0xFF14171A)
    private val textSecondary = Color(0xFF657786)
    private val highlightYellow = Color(0xFFFFF9C4)
    private val cardBorder = Color(0xFFE1E8ED)

    // Poppins Font Family
    private val poppinsFamily = FontFamily(
        Font(R.font.poppins_regular, FontWeight.Normal),
        Font(R.font.poppins_medium, FontWeight.Medium),
        Font(R.font.poppins_semibold, FontWeight.SemiBold),
        Font(R.font.poppins_bold, FontWeight.Bold)
    )

    // Pagination settings
    private val pageSize = 10

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun LeaderboardContent(
        onBackClick: () -> Unit,
        gameViewModel: GameViewModel = viewModel(),
        leaderboardViewModel: LeaderboardViewModel = viewModel()
    ) {
        // State
        val userId = remember { derivedStateOf { gameViewModel.getUserId() } }
        val username = remember { derivedStateOf { gameViewModel.getUsername() } }
        val points = remember { derivedStateOf { gameViewModel.getPoints() } }

        val topUsersState by leaderboardViewModel.topUsers.collectAsState()
        val userRankState by leaderboardViewModel.userRank.collectAsState()

        val coroutineScope = rememberCoroutineScope()
        val listState = rememberLazyListState()

        // Local pagination state
        var currentPage by remember { mutableStateOf(0) }
        var isRefreshing by remember { mutableStateOf(false) }
        val refreshRotation by animateFloatAsState(
            targetValue = if (isRefreshing) 360f else 0f,
            label = "refreshRotation"
        )

        // Top 50 users in chunks of 10 for pagination
        val allUsers = when (topUsersState) {
            is LeaderboardViewModel.TopUsersState.Success -> {
                (topUsersState as LeaderboardViewModel.TopUsersState.Success).users
            }
            else -> emptyList()
        }

        val paginatedUsers = allUsers.chunked(pageSize)
        val currentUsers = paginatedUsers.getOrElse(currentPage) { emptyList() }
        val pageCount = paginatedUsers.size.coerceAtLeast(1)

        // User's rank
        val userRank = when (userRankState) {
            is LeaderboardViewModel.UserRankState.Success -> {
                (userRankState as LeaderboardViewModel.UserRankState.Success).rank
            }
            else -> -1
        }

        // Initial data load
        LaunchedEffect(Unit) {
            leaderboardViewModel.fetchTopUsers(50) // Fetch top 50 users
            if (userId.value.isNotEmpty()) {
                leaderboardViewModel.fetchUserRank(userId.value)
            }
        }

        // Scaffold with TopAppBar
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Leaderboard",
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
                                    leaderboardViewModel.fetchTopUsers(50)
                                    if (userId.value.isNotEmpty()) {
                                        leaderboardViewModel.fetchUserRank(userId.value)
                                    }
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
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(whiteBackground)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Leaderboard header
                    LeaderboardHeader()

                    Spacer(modifier = Modifier.height(8.dp))

                    // Loading or error state
                    when (topUsersState) {
                        is LeaderboardViewModel.TopUsersState.Loading -> {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = accentBlue)
                            }
                        }
                        is LeaderboardViewModel.TopUsersState.Error -> {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = (topUsersState as LeaderboardViewModel.TopUsersState.Error).message,
                                    color = Color.Red,
                                    fontFamily = poppinsFamily,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        else -> {
                            // Leaderboard list
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                            ) {
                                LazyColumn(
                                    state = listState,
                                    contentPadding = PaddingValues(vertical = 8.dp)
                                ) {
                                    items(currentUsers) { user ->
                                        val index = allUsers.indexOf(user) + 1
                                        LeaderboardItem(
                                            rank = index,
                                            user = user,
                                            isCurrentUser = user.id == userId.value
                                        )
                                    }
                                }
                            }

                            // Pagination controls
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Previous page button
                                IconButton(
                                    onClick = {
                                        if (currentPage > 0) {
                                            currentPage--
                                            coroutineScope.launch {
                                                listState.animateScrollToItem(0)
                                            }
                                        }
                                    },
                                    enabled = currentPage > 0
                                ) {
                                    Icon(
                                        Icons.Default.ArrowBack,
                                        contentDescription = "Previous page",
                                        tint = if (currentPage > 0) accentBlue else accentBlue.copy(alpha = 0.3f)
                                    )
                                }

                                // Page indicators
                                Text(
                                    text = "Page ${currentPage + 1} of $pageCount",
                                    color = textPrimary,
                                    fontFamily = poppinsFamily,
                                    fontWeight = FontWeight.Medium
                                )

                                // Next page button
                                IconButton(
                                    onClick = {
                                        if (currentPage < pageCount - 1) {
                                            currentPage++
                                            coroutineScope.launch {
                                                listState.animateScrollToItem(0)
                                            }
                                        }
                                    },
                                    enabled = currentPage < pageCount - 1
                                ) {
                                    Icon(
                                        Icons.Default.ArrowForward,
                                        contentDescription = "Next page",
                                        tint = if (currentPage < pageCount - 1) accentBlue else accentBlue.copy(alpha = 0.3f)
                                    )
                                }
                            }
                        }
                    }

                    // Current user's rank (always displayed at bottom)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        when (userRankState) {
                            is LeaderboardViewModel.UserRankState.Loading -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(70.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(lightSurface),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(28.dp),
                                        color = accentBlue,
                                        strokeWidth = 2.dp
                                    )
                                }
                            }
                            is LeaderboardViewModel.UserRankState.Error -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(70.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(lightSurface),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Couldn't load your rank",
                                        color = textPrimary,
                                        fontFamily = poppinsFamily
                                    )
                                }
                            }
                            is LeaderboardViewModel.UserRankState.Success -> {
                                CurrentUserItem(
                                    rank = userRank,
                                    name = username.value,
                                    points = points.value
                                )
                            }
                            else -> { /* No-op */ }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun LeaderboardHeader() {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank column
            Text(
                text = "Rank",
                color = textSecondary,
                fontFamily = poppinsFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                modifier = Modifier.width(60.dp)
            )

            // Name column (takes most of the space)
            Text(
                text = "Name",
                color = textSecondary,
                fontFamily = poppinsFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f)
            )

            // Points column
            Text(
                text = "Points",
                color = textSecondary,
                fontFamily = poppinsFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                textAlign = TextAlign.End,
                modifier = Modifier.width(80.dp)
            )
        }

        // Divider
        Divider(
            color = cardBorder,
            thickness = 1.dp,
            modifier = Modifier.padding(top = 4.dp)
        )
    }

    @Composable
    private fun LeaderboardItem(
        rank: Int,
        user: ApiModels.LeaderboardUser,
        isCurrentUser: Boolean
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clip(RoundedCornerShape(8.dp))
                .then(
                    if (isCurrentUser) {
                        Modifier
                            .border(
                                width = 1.dp,
                                color = accentBlue,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .background(highlightYellow.copy(alpha = 0.2f))
                    } else {
                        Modifier.background(if (rank % 2 == 0) lightSurface else whiteBackground)
                    }
                ),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            shape = RoundedCornerShape(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = if (isCurrentUser) 2.dp else 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Rank with medal for top 3
                Box(
                    modifier = Modifier.width(60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when (rank) {
                        1 -> RankMedal(rank = rank, color = accentGold)
                        2 -> RankMedal(rank = rank, color = accentSilver)
                        3 -> RankMedal(rank = rank, color = accentBronze)
                        else -> Text(
                            text = "#$rank",
                            color = if (isCurrentUser) accentBlue else textPrimary,
                            fontFamily = poppinsFamily,
                            fontWeight = if (isCurrentUser) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 16.sp
                        )
                    }
                }

                // Name
                Text(
                    text = user.name,
                    color = if (isCurrentUser) accentBlue else textPrimary,
                    fontFamily = poppinsFamily,
                    fontWeight = if (isCurrentUser) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                // Points
                Text(
                    text = "${user.points}",
                    color = if (isCurrentUser) accentBlue else textPrimary,
                    fontFamily = poppinsFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(80.dp)
                )
            }
        }
    }

    @Composable
    private fun CurrentUserItem(
        rank: Int,
        name: String,
        points: Int
    ) {
        // Using a simple Box with border instead of complex nested components
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(
                    width = 2.dp,
                    color = accentBlue,
                    shape = RoundedCornerShape(12.dp)
                )
                .background(highlightYellow.copy(alpha = 0.3f))
                .padding(horizontal = 12.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Rank
                Box(
                    modifier = Modifier.width(60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when (rank) {
                        1 -> RankMedal(rank = rank, color = accentGold)
                        2 -> RankMedal(rank = rank, color = accentSilver)
                        3 -> RankMedal(rank = rank, color = accentBronze)
                        else -> Text(
                            text = "#$rank",
                            color = accentBlue,
                            fontFamily = poppinsFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }

                // Name with {You} indicator - make sure there's no extra styling here
                Text(
                    text = "$name {You}",
                    color = accentBlue,
                    fontFamily = poppinsFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                // Points
                Text(
                    text = "$points",
                    color = accentBlue,
                    fontFamily = poppinsFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(80.dp)
                )
            }
        }
    }

    @Composable
    private fun RankMedal(rank: Int, color: Color) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(color, CircleShape)
                .border(1.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$rank",
                color = Color.Black,
                fontFamily = poppinsFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}