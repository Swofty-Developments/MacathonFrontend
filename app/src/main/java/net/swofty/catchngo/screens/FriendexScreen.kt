package net.swofty.catchngo.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
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
import net.swofty.catchngo.models.FriendexViewModel
import net.swofty.catchngo.models.GameViewModel

/**
 * Friendex screen showing discovered friends and undiscovered players.
 */
class FriendexScreen {

    /* ── colours ───────────────────────────────────────────────────────── */
    private val whiteBackground = Color.White
    private val lightSurface    = Color(0xFFF5F8FA)
    private val accentBlue      = Color(0xFF1DA1F2)
    private val accentPurple    = Color(0xFF9C27B0)
    private val accentRed       = Color(0xFFE0245E)
    private val textPrimary     = Color(0xFF14171A)
    private val textSecondary   = Color(0xFF657786)
    private val undiscoveredGray = Color(0xFFAAB8C2)
    private val cardBorder      = Color(0xFFE1E8ED)
    private val highlightBg     = Color(0xFFE8F5FE)

    /* ── fonts ─────────────────────────────────────────────────────────── */
    private val poppinsFamily = FontFamily(
        Font(R.font.poppins_regular,  FontWeight.Normal),
        Font(R.font.poppins_medium,   FontWeight.Medium),
        Font(R.font.poppins_semibold, FontWeight.SemiBold),
        Font(R.font.poppins_bold,     FontWeight.Bold)
    )

    /* pagination size */
    private val pageSize = 3

    /* ──────────────────────────────────────────────────────────────────── */
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun FriendexContent(
        onBackClick: () -> Unit,
        gameViewModel: GameViewModel = viewModel(),
        friendexViewModel: FriendexViewModel = viewModel()
    ) {

        /* ── state from VMs ───────────────────────────────────────────── */
        val userId            = remember { derivedStateOf { gameViewModel.getUserId() } }
        val coroutineScope    = rememberCoroutineScope()

        val userEntryState     by friendexViewModel.userEntryState.observeAsState()
        val friendsState       by friendexViewModel.friendsState.observeAsState()
        val unmetPlayersState  by friendexViewModel.unmetPlayersState.observeAsState()
        val selectUserState    by friendexViewModel.selectUserState.observeAsState()
        val addFriendState     by friendexViewModel.addFriendState.observeAsState()

        /* ── local UI state ───────────────────────────────────────────── */
        var currentPage      by remember { mutableStateOf(0) }
        var selectedFriend   by remember { mutableStateOf<ApiModels.UserEntry?>(null) }
        var isRefreshing     by remember { mutableStateOf(false) }
        val friendsListState = rememberLazyListState()
        val unmetListState   = rememberLazyListState()

        val refreshRotation by animateFloatAsState(
            targetValue = if (isRefreshing) 360f else 0f,
            label = "refreshRotation"
        )

        /* friends id list from state */
        val friendIds = when (friendsState) {
            is FriendexViewModel.FriendsState.Success ->
                (friendsState as FriendexViewModel.FriendsState.Success).friends
            else -> emptyList()
        }

        /* full friend objects */
        val friendsData = remember { mutableStateListOf<ApiModels.UserEntry>() }

        /* unmet-player ids */
        val unmetPlayers = when (unmetPlayersState) {
            is FriendexViewModel.UnmetPlayersState.Success ->
                (unmetPlayersState as FriendexViewModel.UnmetPlayersState.Success).players
            else -> emptyList()
        }

        /* pagination of friendsData */
        val paginatedFriends = friendsData.chunked(pageSize)
        val currentFriends   = paginatedFriends.getOrElse(currentPage) { emptyList() }
        val pageCount        = paginatedFriends.size.coerceAtLeast(1)

        /* ── initial fetches ──────────────────────────────────────────── */
        LaunchedEffect(userId.value) {
            if (userId.value.isNotEmpty()) {
                friendexViewModel.getUserEntry(userId.value)
                friendexViewModel.getUserFriends(userId.value)
                friendexViewModel.getUnmetPlayers(userId.value)
            }
        }

        /* fetch friend details whenever friendIds changes */
        LaunchedEffect(friendIds) {
            friendsData.clear()
            friendIds.forEach { fid ->
                coroutineScope.launch { friendexViewModel.getUserEntry(fid) }
            }
        }

        /* on each userEntryState success, cache it */
        LaunchedEffect(userEntryState) {
            if (userEntryState is FriendexViewModel.UserEntryState.Success) {
                val entry = (userEntryState as FriendexViewModel.UserEntryState.Success).userEntry
                if (friendsData.none { it.id == entry.id }) friendsData.add(entry)
            }
        }

        /* handle select/add friend states for refresh */
        LaunchedEffect(selectUserState, addFriendState) {
            val needRefresh =
                selectUserState is FriendexViewModel.SelectUserState.Success ||
                        addFriendState   is FriendexViewModel.AddFriendState.Success

            if (needRefresh && userId.value.isNotEmpty()) {
                friendexViewModel.getUserFriends(userId.value)
                friendexViewModel.getUnmetPlayers(userId.value)
                friendexViewModel.resetStates()
            }
        }

        /* ── UI ───────────────────────────────────────────────────────── */
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text("Friendex",
                            fontFamily = poppinsFamily,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White)
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White)
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                isRefreshing = true
                                coroutineScope.launch {
                                    if (userId.value.isNotEmpty()) {
                                        friendexViewModel.getUserFriends(userId.value)
                                        friendexViewModel.getUnmetPlayers(userId.value)
                                    }
                                    isRefreshing = false
                                }
                            }
                        ) {
                            Icon(Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                modifier = Modifier.rotate(refreshRotation),
                                tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = accentBlue
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

                    /* ── MY FRIENDS ─────────────────────────────────────── */
                    Text("My Friends",
                        color = textPrimary,
                        fontFamily = poppinsFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp)

                    Spacer(Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (selectedFriend != null) 360.dp else 200.dp)
                    ) {
                        when (friendsState) {
                            is FriendexViewModel.FriendsState.Loading ->
                                CenteredProgress()

                            is FriendexViewModel.FriendsState.Error ->
                                CenteredError((friendsState as FriendexViewModel.FriendsState.Error).message)

                            is FriendexViewModel.FriendsState.Success -> {
                                Column(Modifier.fillMaxWidth()) {

                                    if (friendIds.isEmpty()) {
                                        PlaceholderCard(
                                            "You haven't met any friends yet!\nGo explore to meet new people.")
                                    } else {
                                        LazyColumn(
                                            state = friendsListState,
                                            contentPadding = PaddingValues(vertical = 8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            items(currentFriends) { friend ->
                                                FriendCard(
                                                    friend = friend,
                                                    isSelected = selectedFriend?.id == friend.id,
                                                    onClick = {
                                                        selectedFriend =
                                                            if (selectedFriend?.id == friend.id) null
                                                            else friend
                                                    }
                                                )
                                            }
                                        }

                                        AnimatedVisibility(
                                            visible = selectedFriend != null,
                                            enter = expandVertically() + fadeIn(),
                                            exit = shrinkVertically() + fadeOut()
                                        ) {
                                            selectedFriend?.let { FriendDetails(it) }
                                        }

                                        if (paginatedFriends.size > 1) {
                                            PagerControls(
                                                currentPage,
                                                pageCount,
                                                onPrev = {
                                                    if (currentPage > 0) {
                                                        currentPage--
                                                        selectedFriend = null
                                                        coroutineScope.launch {
                                                            friendsListState.animateScrollToItem(0)
                                                        }
                                                    }
                                                },
                                                onNext = {
                                                    if (currentPage < pageCount - 1) {
                                                        currentPage++
                                                        selectedFriend = null
                                                        coroutineScope.launch {
                                                            friendsListState.animateScrollToItem(0)
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            else -> {}
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    /* ── UNDISCOVERED ──────────────────────────────────── */
                    Text("Undiscovered Players",
                        color = textPrimary,
                        fontFamily = poppinsFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp)

                    Spacer(Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        when (unmetPlayersState) {
                            is FriendexViewModel.UnmetPlayersState.Loading ->
                                CenteredProgress()

                            is FriendexViewModel.UnmetPlayersState.Error ->
                                CenteredError((unmetPlayersState as FriendexViewModel.UnmetPlayersState.Error).message)

                            is FriendexViewModel.UnmetPlayersState.Success -> {
                                if (unmetPlayers.isEmpty()) {
                                    PlaceholderCard(
                                        "You've discovered everyone!\nCongratulations!")
                                } else {
                                    LazyRow(
                                        state = unmetListState,
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        items(unmetPlayers) { pid ->
                                            UndiscoveredPlayerCard(pid)
                                        }
                                    }
                                }
                            }
                            else -> {}
                        }
                    }
                }

                /* overlays for select / add friend actions */
                when (selectUserState) {
                    is FriendexViewModel.SelectUserState.Loading ->
                        LoadingOverlay("Selecting player…")

                    is FriendexViewModel.SelectUserState.Error ->
                        ErrorSnackbar(
                            message = (selectUserState as FriendexViewModel.SelectUserState.Error).message,
                            onDismiss = { friendexViewModel.resetStates() })

                    else -> {}
                }

                when (addFriendState) {
                    is FriendexViewModel.AddFriendState.Loading ->
                        LoadingOverlay("Adding friend…")

                    is FriendexViewModel.AddFriendState.Error ->
                        ErrorSnackbar(
                            message = (addFriendState as FriendexViewModel.AddFriendState.Error).message,
                            onDismiss = { friendexViewModel.resetStates() })

                    else -> {}
                }
            }
        }
    }

    /* ── FRIEND CARD ──────────────────────────────────────────────────── */
    @Composable
    private fun FriendCard(
        friend: ApiModels.UserEntry,
        isSelected: Boolean,
        onClick: () -> Unit
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clickable(onClick = onClick),
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected) highlightBg else whiteBackground
            ),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, if (isSelected) accentBlue else cardBorder),
            elevation = CardDefaults.cardElevation(
                defaultElevation = if (isSelected) 4.dp else 1.dp
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(accentBlue.copy(alpha = 0.1f))
                        .border(2.dp, accentBlue, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Friend",
                        tint = accentBlue,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(Modifier.weight(1f)) {
                    Text(friend.name,
                        color = textPrimary,
                        fontFamily = poppinsFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp)

                    Text("${friend.points} points",
                        color = textSecondary,
                        fontFamily = poppinsFamily,
                        fontSize = 14.sp)
                }

                Icon(
                    imageVector =
                        if (isSelected) Icons.Default.KeyboardArrowUp
                        else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = accentBlue
                )
            }
        }
    }

    /* ── FRIEND DETAILS ───────────────────────────────────────────────── */
    @Composable
    private fun FriendDetails(friend: ApiModels.UserEntry) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(highlightBg),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, accentBlue),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("Profile Information",
                    color = textPrimary,
                    fontFamily = poppinsFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp)

                Spacer(Modifier.height(8.dp))

                friend.questions.forEachIndexed { index, q ->
                    QuestionAnswerItem(q.id, q.answer)
                    if (index < friend.questions.size - 1) {
                        Divider(color = cardBorder, modifier = Modifier.padding(vertical = 8.dp))
                    }
                }
            }
        }
    }

    @Composable
    private fun QuestionAnswerItem(questionNumber: Int, answer: String) {
        Column {
            Text("Question #$questionNumber",
                color = textSecondary,
                fontFamily = poppinsFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp)
            Spacer(Modifier.height(4.dp))
            Text(answer,
                color = textPrimary,
                fontFamily = poppinsFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp)
        }
    }

    /* ── UNDISCOVERED PLAYER CARD (NO SELECT BUTTON) ─────────────────── */
    @Composable
    private fun UndiscoveredPlayerCard(playerId: String) {
        Card(
            modifier = Modifier
                .width(120.dp)
                .height(160.dp),
            colors = CardDefaults.cardColors(lightSurface),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, cardBorder),
            elevation = CardDefaults.cardElevation(1.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(undiscoveredGray.copy(alpha = 0.2f))
                        .border(2.dp, undiscoveredGray, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Undiscovered Player",
                        tint = undiscoveredGray,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(Modifier.height(12.dp))

                Text("???",
                    color = textSecondary,
                    fontFamily = poppinsFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center)

                /*  button removed – selection happens elsewhere  */
            }
        }
    }

    /* ── REUSABLE SMALL UI PIECES ─────────────────────────────────────── */
    @Composable
    private fun PlaceholderCard(text: String) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(lightSurface)
                .border(1.dp, cardBorder, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(text,
                color = textSecondary,
                fontFamily = poppinsFamily,
                textAlign = TextAlign.Center)
        }
    }

    @Composable
    private fun CenteredProgress() {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = accentBlue)
        }
    }

    @Composable
    private fun CenteredError(msg: String) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(msg,
                color = accentRed,
                fontFamily = poppinsFamily,
                textAlign = TextAlign.Center)
        }
    }

    @Composable
    private fun PagerControls(
        currentPage: Int,
        pageCount: Int,
        onPrev: () -> Unit,
        onNext: () -> Unit
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onPrev, enabled = currentPage > 0) {
                Icon(
                    Icons.Default.ArrowBack, null,
                    tint = if (currentPage > 0) accentBlue else accentBlue.copy(alpha = 0.3f)
                )
            }
            Text("Page ${currentPage + 1} of $pageCount",
                color = textPrimary,
                fontFamily = poppinsFamily,
                fontWeight = FontWeight.Medium)
            IconButton(onNext, enabled = currentPage < pageCount - 1) {
                Icon(
                    Icons.Default.ArrowForward, null,
                    tint = if (currentPage < pageCount - 1) accentBlue else accentBlue.copy(alpha = 0.3f)
                )
            }
        }
    }

    @Composable
    private fun LoadingOverlay(message: String) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f)),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.width(200.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = accentBlue)
                    Spacer(Modifier.height(16.dp))
                    Text(message,
                        color = textPrimary,
                        fontFamily = poppinsFamily,
                        textAlign = TextAlign.Center)
                }
            }
        }
    }

    @Composable
    private fun ErrorSnackbar(message: String, onDismiss: () -> Unit) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(accentRed),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Close, null, tint = Color.White)
                    Spacer(Modifier.width(12.dp))
                    Text(message,
                        color = Color.White,
                        fontFamily = poppinsFamily,
                        modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, null, tint = Color.White)
                    }
                }
            }
        }
    }
}
