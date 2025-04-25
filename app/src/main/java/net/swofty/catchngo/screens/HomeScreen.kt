package net.swofty.catchngo.screens

import android.content.Context
import android.graphics.Bitmap
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.graphics.createBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mapbox.bindgen.Expected
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.maps.QueriedRenderedFeature
import com.mapbox.maps.RenderedQueryGeometry
import com.mapbox.maps.RenderedQueryOptions
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.style.MapStyle
import com.mapbox.maps.extension.style.expressions.dsl.generated.get
import com.mapbox.maps.extension.style.image.image
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.symbolLayer
import com.mapbox.maps.extension.style.layers.properties.generated.IconAnchor
import com.mapbox.maps.extension.style.layers.properties.generated.IconPitchAlignment
import com.mapbox.maps.extension.style.layers.properties.generated.IconRotationAlignment
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.sources.getSourceAs
import com.mapbox.maps.plugin.gestures.OnMapClickListener
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
import com.mapbox.maps.plugin.gestures.removeOnMapClickListener
import kotlinx.coroutines.launch
import net.swofty.catchngo.R
import net.swofty.catchngo.api.ApiModels
import net.swofty.catchngo.models.*
import kotlin.math.min
import kotlin.math.round

class HomeScreen {

    private val styleUri = "mapbox://styles/swofty/cm9vmlbd000ub01sof3zegw34"
    private val zoom3D = 19.5
    private val pitch3D = 60.0

    // ── colours ──────────────────────────────────────────────────────────────
    private val darkBackground = Color(0xFF15202B)
    private val darkSurface = Color(0xFF1E2732)
    private val accentBlue = Color(0xFF1DA1F2)
    private val accentRed = Color(0xFFE0245E)
    private val accentPurple = Color(0xFF9C27B0)
    private val accentGreen = Color(0xFF4CAF50)
    private val textWhite = Color(0xFFE7E9EA)
    private val textSecondary = Color(0xFF8899A6)

    // Poppins
    private val poppinsFamily = FontFamily(
        Font(R.font.poppins_regular, FontWeight.Normal),
        Font(R.font.poppins_medium, FontWeight.Medium),
        Font(R.font.poppins_semibold, FontWeight.SemiBold),
        Font(R.font.poppins_bold, FontWeight.Bold)
    )

    /* ─────────────────────────────────────────────────────────────────────── */
    /*  Main content                                                          */
    /* ─────────────────────────────────────────────────────────────────────── */
    @OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
    @Composable
    fun HomeContent(
        onLogout: () -> Unit,
        onFriendexClick: () -> Unit = {},
        onDeleteAccountClick: () -> Unit = {},
        gameViewModel: GameViewModel = viewModel(),
        leaderboardViewModel: LeaderboardViewModel = viewModel(),
        friendexViewModel: FriendexViewModel = viewModel(),
        locationViewModel: LocationViewModel = viewModel(),
        authViewModel: AuthViewModel = viewModel()
    ) {
        val ctx = LocalContext.current
        val coroutineScope = rememberCoroutineScope()

        /* ── live state ──────────────────────────────────────────────────── */
        val loc by gameViewModel.location.collectAsState()
        val heading by rememberDeviceHeading()
        val nearbyState by locationViewModel.nearbyUsers.observeAsState()
        val points by remember { derivedStateOf { gameViewModel.getPoints() } }
        val userId by remember { derivedStateOf { gameViewModel.getUserId() } }

        // Selection status from FriendexViewModel
        val selectionStatus by friendexViewModel.selectionStatusFlow.collectAsState()

        // Leaderboard + friendex …
        val userRankState by leaderboardViewModel.userRank.collectAsState()
        var showLeaderboard by remember { mutableStateOf(false) }
        var showFriendex by remember { mutableStateOf(false) }
        val friendsState by friendexViewModel.friendsState.observeAsState()
        val friendCount = remember { mutableStateOf(0) }
        var showDeleteDialog by remember { mutableStateOf(false) }

        /* ── "info bubble" state ─────────────────────────────────────────── */
        var selectedUserId by remember { mutableStateOf<String?>(null) }

        val leaderboardPosition by remember {
            derivedStateOf {
                when (val st = userRankState) {
                    is LeaderboardViewModel.UserRankState.Success -> st.rank
                    else -> 0
                }
            }
        }

        /* ── keep a live list of friend-IDs ─────────────────────────────── */
        val friendIds by remember {
            derivedStateOf {
                when (val st = friendsState) {
                    is FriendexViewModel.FriendsState.Success -> st.friends
                    else -> emptyList()
                }
            }
        }

        /* ── Start selection status polling when tracking someone ────────── */
        LaunchedEffect(Unit) {
            locationViewModel.startNearbyUsersWatch()
            gameViewModel.refreshProfile()
            friendexViewModel.checkSelectionStatus()
            friendexViewModel.startSelectionStatusPolling()
        }

        /* ── hook LocationViewModel once we know our ID ─────────────────── */
        LaunchedEffect(userId) { if (userId.isNotEmpty()) locationViewModel.setUserId(userId) }

        /* ── map camera ──────────────────────────────────────────────────── */
        val viewport = rememberMapViewportState()
        LaunchedEffect(loc, heading) {
            loc ?: return@LaunchedEffect
            viewport.setCameraOptions {
                center(Point.fromLngLat(loc!!.longitude, loc!!.latitude))
                zoom(zoom3D)
                pitch(pitch3D)
                bearing(heading.toDouble())
            }
        }

        /* ── Get currently tracked user info ─────────────────────────────── */
        val trackedUser = remember(selectionStatus, nearbyState) {
            val status = selectionStatus ?: return@remember null
            val selectedId = status.selectedFriend ?: return@remember null
            val nearby = (nearbyState as? LocationViewModel.NearbyUsersState.Success)?.users
                ?: return@remember null

            nearby.firstOrNull { it.id == selectedId }
        }

        /* ── UI shell ────────────────────────────────────────────────────── */
        Box(Modifier.fillMaxSize().background(darkBackground)) {

            /* ───────────────────────── Map ────────────────────────────── */
            MapboxMap(
                modifier = Modifier.fillMaxSize(),
                mapViewportState = viewport,
                style = { MapStyle(style = styleUri) }
            ) {

                /* ── one-time style boot ──────────────────────────────── */
                MapEffect(Unit) { mapView ->
                    val map = mapView.getMapboxMap()
                    map.getStyle { style ->
                        // Player
                        if (style.getStyleImage("player_arrow") == null) {
                            style.addImage(
                                "player_arrow",
                                vectorToBitmap(ctx, R.drawable.ic_player_arrow),
                                false
                            )
                        }
                        // Stranger arrow
                        if (style.getStyleImage("nearby_arrow") == null) {
                            style.addImage(
                                "nearby_arrow",
                                vectorToBitmap(ctx, R.drawable.ic_player_arrow_question),
                                false
                            )
                        }
                        // Friend arrow
                        if (style.getStyleImage("friend_arrow") == null) {
                            style.addImage(
                                "friend_arrow",
                                vectorToBitmap(ctx, R.drawable.ic_player_arrow_friend),
                                false
                            )
                        }
                        // Tracking arrow
                        if (style.getStyleImage("tracking_arrow") == null) {
                            style.addImage(
                                "tracking_arrow",
                                vectorToBitmap(ctx, R.drawable.ic_player_arrow_tracking),
                                false
                            )
                        }

                        /* ── data sources ───────────────────────────── */
                        if (!style.styleSourceExists("player_src")) {
                            style.addSource(
                                geoJsonSource("player_src") {
                                    featureCollection(FeatureCollection.fromFeatures(listOf()))
                                }
                            )
                        }
                        if (!style.styleSourceExists("nearby_src")) {
                            style.addSource(
                                geoJsonSource("nearby_src") {
                                    featureCollection(FeatureCollection.fromFeatures(listOf()))
                                }
                            )
                        }

                        /* ── layers ────────────────────────────────── */
                        if (!style.styleLayerExists("player_layer")) {
                            style.addLayer(
                                symbolLayer("player_layer", "player_src") {
                                    slot("top")
                                    iconImage("player_arrow")
                                    iconSize(2.0)
                                    iconAnchor(IconAnchor.CENTER)
                                    iconPitchAlignment(IconPitchAlignment.MAP)
                                    iconRotationAlignment(IconRotationAlignment.MAP)
                                    iconRotate(get("bearing"))
                                    iconAllowOverlap(true)
                                }
                            )
                        }
                        if (!style.styleLayerExists("nearby_layer")) {
                            style.addLayer(
                                symbolLayer("nearby_layer", "nearby_src") {
                                    slot("top")
                                    iconImage(get("icon"))
                                    iconSize(1.6)
                                    iconAnchor(IconAnchor.CENTER)
                                    iconPitchAlignment(IconPitchAlignment.MAP)
                                    iconRotationAlignment(IconRotationAlignment.MAP)
                                    iconRotate(get("bearing"))
                                    iconAllowOverlap(true)
                                }
                            )
                        }
                    }
                }

                /* ── keep player arrow in sync ──────────────────────── */
                MapEffect(loc, heading) { mapView ->
                    val l = loc ?: return@MapEffect
                    mapView.getMapboxMap().getStyle { style ->
                        style.getSourceAs<GeoJsonSource>("player_src")?.apply {
                            val feat = Feature.fromGeometry(Point.fromLngLat(l.longitude,l.latitude))
                            feat.addNumberProperty("bearing", heading.toDouble())
                            featureCollection(FeatureCollection.fromFeatures(listOf(feat)))
                        }
                    }
                }

                /* ── update nearby arrows (friends vs strangers) ──── */
                MapEffect(loc, nearbyState, friendIds, selectionStatus) { mapView ->
                    val myLoc = loc ?: return@MapEffect
                    val st = nearbyState
                    val trackedUserId = selectionStatus?.selectedFriend

                    mapView.getMapboxMap().getStyle { style ->
                        val feats = mutableListOf<Feature>()

                        if (st is LocationViewModel.NearbyUsersState.Success) {
                            st.users.forEach { u ->
                                val dist = u.distanceTo(myLoc.latitude, myLoc.longitude)
                                if (dist <= 50f) {
                                    val bearing = FloatArray(3).let {
                                        android.location.Location.distanceBetween(
                                            myLoc.latitude,
                                            myLoc.longitude,
                                            u.latitude,
                                            u.longitude,
                                            it
                                        ); it[1].toDouble()
                                    }

                                    // Determine icon based on relationship
                                    val icon = when {
                                        trackedUserId == u.id -> "tracking_arrow"
                                        friendIds.contains(u.id) -> "friend_arrow"
                                        else -> "nearby_arrow"
                                    }

                                    Feature.fromGeometry(Point.fromLngLat(u.longitude, u.latitude)).apply {
                                        addNumberProperty("bearing", bearing)
                                        addStringProperty("icon", icon)
                                        addStringProperty("user_id", u.id)
                                        feats += this
                                    }
                                }
                            }
                        }
                        mapView.getMapboxMap().getStyle {
                            it.getSourceAs<GeoJsonSource>("nearby_src")
                                ?.featureCollection(FeatureCollection.fromFeatures(feats))
                        }
                    }
                }

                /* ── tap detection on arrows ───────────────────────── */
                MapEffect(Unit) { mapView ->
                    val mapboxMap = mapView.getMapboxMap()

                    val clickL: OnMapClickListener = OnMapClickListener { point ->
                        val screen = mapboxMap.pixelForCoordinate(point)

                        mapboxMap.queryRenderedFeatures(
                            RenderedQueryGeometry.valueOf(screen),
                            RenderedQueryOptions(listOf("nearby_layer"), null)
                        ) { res: Expected<String, List<QueriedRenderedFeature>> ->
                            res.fold({}, { list ->
                                val pickedId = list
                                    .mapNotNull { it.queriedFeature.feature }
                                    .firstNotNullOfOrNull { f ->
                                        Log.i("pog", f.properties().toString())
                                        if (f.hasProperty("user_id"))
                                            f.getStringProperty("user_id") else null
                                    }
                                if (pickedId != null) selectedUserId = pickedId
                            })
                        }; true
                    }

                    mapboxMap.addOnMapClickListener(clickL)
                    //onDispose { mapboxMap.removeOnMapClickListener(clickL) }
                }
            }

            /* ────────────────────── info bubble ───────────────────── */
            AnimatedVisibility(
                visible = selectedUserId != null,
                enter = slideInVertically { -it } + fadeIn(),
                exit  = slideOutVertically { -it } + fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 50.dp)
                    .zIndex(2f)                      // always above corner buttons
            ) {
                /* Pre-compute friend / name status --------------------- */
                val isFriend = selectedUserId?.let { friendIds.contains(it) } == true
                val nearbyName = remember(selectedUserId, nearbyState) {
                    (nearbyState as? LocationViewModel.NearbyUsersState.Success)
                        ?.users?.firstOrNull { it.id == selectedUserId }?.name
                }
                val isCurrentlyTracked = selectionStatus?.selectedFriend == selectedUserId

                Surface(
                    tonalElevation = 4.dp,
                    shape = RoundedCornerShape(16.dp),
                    shadowElevation = 8.dp,
                    color = Color.White,
                    modifier = Modifier
                        .wrapContentWidth()
                        .border(1.dp, accentBlue, RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        /* Headline ------------------------------------ */
                        Text(
                            text = nearbyName ?: (if (isFriend) "Friend" else "Not In Friendex!"),
                            fontFamily = poppinsFamily,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Black,
                            fontSize = 16.sp
                        )
                        /* Sub-headline (ID) --------------------------- */
                        Text(
                            text = selectedUserId ?: "",
                            fontFamily = poppinsFamily,
                            fontWeight = FontWeight.Medium,
                            color = textSecondary,
                            fontSize = 12.sp
                        )

                        Spacer(Modifier.height(8.dp))

                        /* Action button ------------------------------ */
                        Button(
                            onClick = {
                                selectedUserId?.let { id ->
                                    if (isCurrentlyTracked) {
                                        // Already tracking this user, deselect them
                                        coroutineScope.launch {
                                            friendexViewModel.deselectUser()
                                            friendexViewModel.resetStates()
                                        }
                                    } else if (isFriend) {
                                        showFriendex = true     // just open Friendex
                                    } else {
                                        coroutineScope.launch {
                                            friendexViewModel.selectUser(id)
                                            friendexViewModel.resetStates()
                                        }
                                    }
                                }
                                selectedUserId = null          // close bubble
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = when {
                                    isCurrentlyTracked -> accentRed
                                    isFriend -> accentPurple
                                    else -> accentBlue
                                },
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                when {
                                    isCurrentlyTracked -> "Stop Tracking"
                                    isFriend -> "Go to Friendex"
                                    else -> "Start Tracking"
                                },
                                fontFamily = poppinsFamily,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        /* Close icon (top-right) --------------------- */
                        IconButton(
                            onClick = { selectedUserId = null },
                            modifier = Modifier
                                .align(Alignment.End)
                                .size(22.dp)
                        ) {
                            Icon(Icons.Default.Close, "Dismiss", tint = accentBlue)
                        }
                    }
                }
            }

            /* ── action buttons ─────────────────────────────────────────── */
            Surface(
                modifier = Modifier
                    .padding(start = 5.dp, top = 31.dp)
                    .align(Alignment.TopStart),
                color = Color.Transparent,
                shadowElevation = 0.dp
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircleIconButton(
                        Icons.Default.Person, "Friendex", friendCount.value
                    ) {
                        showFriendex = true
                        coroutineScope.launch {
                            friendexViewModel.getUserFriends(userId)
                            friendexViewModel.getUnmetPlayers(userId)
                        }
                    }

                    CircleIconButton(
                        Icons.Default.Menu, "Leaderboard", color = accentPurple
                    ) {
                        showLeaderboard = true
                        coroutineScope.launch {
                            leaderboardViewModel.fetchTopUsers(50)
                            if (userId.isNotEmpty()) {
                                leaderboardViewModel.fetchUserRank(userId)
                            }
                        }
                    }

                    // Only show stop tracking button if actively tracking someone
                    if (selectionStatus?.selectedFriend != null) {
                        CircleIconButton(
                            Icons.Default.Close, "Stop", color = accentRed
                        ) {
                            coroutineScope.launch {
                                friendexViewModel.deselectUser()
                                friendexViewModel.resetStates()
                            }
                        }
                    }

                    CircleIconButton(
                        Icons.Default.Delete, "Delete", color = accentRed
                    ) { showDeleteDialog = true }
                }
            }

            /* ── bottom panel ───────────────────────────────────────────── */
            BottomPanel(
                points = points ?: 0,
                friendCount = friendCount.value,
                leaderboardPosition = leaderboardPosition,
                selectionStatus = selectionStatus,
                trackedUser = trackedUser,
                onStopTrackingClick = {
                    coroutineScope.launch {
                        friendexViewModel.deselectUser()
                        friendexViewModel.resetStates()
                    }
                },
                onFriendexClick = { showFriendex = true },
                onLeaderboardClick = { showLeaderboard = true },
                onDeleteAccountClick = { showDeleteDialog = true },
                modifier = Modifier.align(Alignment.BottomCenter),
                gameViewModel = gameViewModel
            )

            /* ── overlay screens ────────────────────────────────────────── */
            AnimatedVisibility(
                visible = showLeaderboard,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                LeaderboardScreen().LeaderboardContent(
                    onBackClick = { showLeaderboard = false },
                    gameViewModel = gameViewModel,
                    leaderboardViewModel = leaderboardViewModel
                )
            }

            AnimatedVisibility(
                visible = showFriendex,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                FriendexScreen().FriendexContent(
                    onBackClick = { showFriendex = false },
                    gameViewModel = gameViewModel,
                    friendexViewModel = friendexViewModel
                )
            }

            /* ── delete-account dialog ─────────────────────────────────── */
            if (showDeleteDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    title = {
                        Text(
                            "Delete Account",
                            fontFamily = poppinsFamily,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    },
                    text = {
                        Column {
                            Text(
                                "If you've made enough friends, delete your account here! " +
                                        "This also stops all location tracking.",
                                fontFamily = poppinsFamily,
                                color = Color.Black
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "This action cannot be undone. " +
                                        "All your data and progress will be permanently deleted.",
                                fontFamily = poppinsFamily,
                                color = accentRed,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            colors = ButtonDefaults.buttonColors(containerColor = accentRed),
                            onClick = {
                                showDeleteDialog = false
                                coroutineScope.launch { onLogout() }
                            }
                        ) {
                            Text("Delete Account", fontFamily = poppinsFamily)
                        }
                    },
                    dismissButton = {
                        OutlinedButton(onClick = { showDeleteDialog = false }) {
                            Text("Cancel", fontFamily = poppinsFamily)
                        }
                    },
                    containerColor = Color.White,
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }
    }

    /* ─────────────────────────────────────────────────────────────────────── */
    /*  Bottom panel (updated)                                                 */
    /* ─────────────────────────────────────────────────────────────────────── */
    @Composable
    private fun BottomPanel(
        points: Int,
        friendCount: Int,
        leaderboardPosition: Int,
        selectionStatus: ApiModels.SelectionStatus?,
        trackedUser: ApiModels.NearbyUser?,
        onStopTrackingClick: () -> Unit,
        gameViewModel: GameViewModel,
        onFriendexClick: () -> Unit,
        onLeaderboardClick: () -> Unit,
        onDeleteAccountClick: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .fillMaxHeight(0.30f)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .border(2.dp, accentBlue, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(Color.White)
                .padding(16.dp)
        ) {
            Column(Modifier.fillMaxSize()) {

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "$points points",
                        color = accentBlue,
                        fontFamily = poppinsFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )

                    Text(
                        "Leaderboard Position: #$leaderboardPosition",
                        color = accentPurple,
                        fontFamily = poppinsFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        modifier = Modifier.clickable(onClick = onLeaderboardClick)
                    )
                }

                Spacer(Modifier.height(16.dp))

                if (selectionStatus?.selectedFriend != null && trackedUser != null) {
                    // Display tracking information
                    TrackingInfo(
                        selectionStatus = selectionStatus,
                        trackedUser = trackedUser,
                        onStopClick = onStopTrackingClick
                    )
                } else {
                    // No active tracking - show default content
                    Text(
                        "Tracking: Nobody :(",
                        fontFamily = poppinsFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )

                    Text(
                        "Click on someones arrow to track them, and be within 5 metres of them for 20 out of any 30 minute block to get points!",
                        color = textSecondary,
                        fontFamily = poppinsFamily,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(Modifier.weight(1f))
            }
        }
    }

    /* ─────────────────────────────────────────────────────────────────────── */
    /*  Tracking info component (new)                                          */
    /* ─────────────────────────────────────────────────────────────────────── */
    @Composable
    private fun TrackingInfo(
        selectionStatus: ApiModels.SelectionStatus,
        trackedUser: ApiModels.NearbyUser,
        onStopClick: () -> Unit
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Tracked user information
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Tracking: ${trackedUser.name}",
                        fontFamily = poppinsFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = accentBlue
                    )

                    Text(
                        text = "Points accumulated: ${selectionStatus.pointsAccumulated}",
                        fontFamily = poppinsFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = accentGreen
                    )
                }

                Button(
                    onClick = onStopClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentRed,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        "Stop",
                        fontFamily = poppinsFamily,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Timer information
            val timeRemaining = selectionStatus.timeRemaining.toInt()
            val remainingMinutes = timeRemaining / 60
            val remainingSeconds = timeRemaining % 60

            Text(
                text = "Time remaining: ${remainingMinutes}m ${remainingSeconds}s",
                fontFamily = poppinsFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = Color.Black
            )

            Spacer(Modifier.height(8.dp))

            // Progress bar - shows percentage of 30 minute session completed
            val progressPercent = (selectionStatus.elapsedTime / (selectionStatus.elapsedTime + selectionStatus.timeRemaining)).toFloat()
            val progressPercentDisplay = (progressPercent * 100).toInt()

            Column(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Progress",
                        fontFamily = poppinsFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        color = textSecondary
                    )
                    Text(
                        "$progressPercentDisplay%",
                        fontFamily = poppinsFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        color = textSecondary
                    )
                }

                Spacer(Modifier.height(4.dp))

                // Custom progress bar with gradient
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(Color.LightGray.copy(alpha = 0.3f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progressPercent)
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(accentBlue, accentPurple)
                                ),
                                shape = RoundedCornerShape(5.dp)
                            )
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Information about the tracking process
            Text(
                text = if (selectionStatus.isInitiator)
                    "You initiated tracking"
                else
                    "${trackedUser.name} initiated tracking",
                fontFamily = poppinsFamily,
                fontSize = 12.sp,
                color = textSecondary
            )
        }
    }

    /* ─────────────────────────────────────────────────────────────────────── */
    /*  Circular icon button                                                  */
    /* ─────────────────────────────────────────────────────────────────────── */
    @Composable
    private fun CircleIconButton(
        icon: ImageVector,
        label: String,
        badgeCount: Int? = null,
        color: Color = accentBlue,
        onClick: () -> Unit
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box {
                IconButton(
                    onClick = onClick,
                    modifier = Modifier
                        .size(56.dp)
                        .border(2.dp, color, CircleShape)
                        .background(Color.White, CircleShape)
                ) {
                    Icon(icon, label, tint = color, modifier = Modifier.size(28.dp))
                }

                if (badgeCount != null && badgeCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(2.dp, (-2).dp)
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(Color.Red)
                            .border(1.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            badgeCount.toString(),
                            color = Color.White,
                            fontFamily = poppinsFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Text(
                label,
                color = Color.Black,
                fontFamily = poppinsFamily,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }

    /* ─────────────────────────────────────────────────────────────────────── */
    /*  Heading sensor helper                                                 */
    /* ─────────────────────────────────────────────────────────────────────── */
    @Composable
    private fun rememberDeviceHeading(): State<Float> {
        val ctx = LocalContext.current
        val heading = remember { mutableStateOf(0f) }

        DisposableEffect(Unit) {
            val mgr = ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            val rotSensor = mgr.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
                ?: return@DisposableEffect onDispose {}
            val R = FloatArray(9)
            val orient = FloatArray(3)

            val listener = object : SensorEventListener {
                override fun onSensorChanged(e: SensorEvent) {
                    SensorManager.getRotationMatrixFromVector(R, e.values)
                    SensorManager.getOrientation(R, orient)
                    heading.value =
                        ((Math.toDegrees(orient[0].toDouble()) + 360) % 360).toFloat()
                }

                override fun onAccuracyChanged(s: Sensor?, a: Int) {}
            }
            mgr.registerListener(listener, rotSensor, SensorManager.SENSOR_DELAY_UI)
            onDispose { mgr.unregisterListener(listener) }
        }
        return heading
    }
}

/* ─────────────────────────────────────────────────────────────────────────── */
/*  Helper: convert vector drawable → bitmap                                  */
/* ─────────────────────────────────────────────────────────────────────────── */
private fun vectorToBitmap(ctx: Context, resId: Int): Bitmap {
    val drawable = androidx.core.content.ContextCompat.getDrawable(ctx, resId)
        ?: error("Drawable $resId not found")
    val bmp = createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight)
    val canvas = android.graphics.Canvas(bmp)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bmp
}