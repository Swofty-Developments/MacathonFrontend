// HomeScreen.kt - integrated with bottom panel
package net.swofty.catchngo.screens

import android.content.Context
import android.graphics.Bitmap
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.maps.Style
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.style.MapStyle
import com.mapbox.maps.extension.style.expressions.dsl.generated.get
import com.mapbox.maps.extension.style.layers.generated.symbolLayer
import com.mapbox.maps.extension.style.layers.properties.generated.IconAnchor
import com.mapbox.maps.extension.style.layers.properties.generated.IconPitchAlignment
import com.mapbox.maps.extension.style.layers.properties.generated.IconRotationAlignment
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import net.swofty.catchngo.R
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.layers.generated.symbolLayer
import com.mapbox.maps.extension.style.layers.properties.generated.*
import com.mapbox.maps.extension.style.expressions.dsl.generated.get
import net.swofty.catchngo.models.GameViewModel
import com.mapbox.geojson.*
import com.mapbox.maps.extension.compose.*
import com.mapbox.maps.extension.compose.style.*
import com.mapbox.maps.extension.style.layers.generated.symbolLayer
import com.mapbox.maps.extension.style.layers.properties.generated.*
import com.mapbox.maps.extension.style.image.image
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.getSourceAs
import com.mapbox.maps.renderer.widget.BitmapWidget
import androidx.core.graphics.createBitmap


class HomeScreen {

    private val styleUri = "mapbox://styles/swofty/cm9vmlbd000ub01sof3zegw34"
    private val zoom3D   = 19.5
    private val pitch3D  = 60.0

    // Define custom colors
    private val darkBackground = Color(0xFF15202B)
    private val darkSurface    = Color(0xFF1E2732)
    private val accentBlue     = Color(0xFF1DA1F2)
    private val accentRed      = Color(0xFFE0245E)
    private val textWhite      = Color(0xFFE7E9EA)
    private val textSecondary  = Color(0xFF8899A6)

    /* ---------------------------------------------------------------------- */
    /*  Main composable                                                       */
    /* ---------------------------------------------------------------------- */
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun HomeContent(
        onLogout: () -> Unit,
        onFriendexClick: () -> Unit = {},
        onLeaderboardClick: () -> Unit = {},
        onDeleteAccountClick: () -> Unit = {},
        gameViewModel: GameViewModel = viewModel()
    ) {
        val ctx = LocalContext.current

        /* -------- live state -------- */
        val loc       by gameViewModel.location.collectAsState()
        val heading   by rememberDeviceHeading()
        val username  by remember { derivedStateOf { gameViewModel.getUsername() } }
        val points    by remember { derivedStateOf { gameViewModel.getPoints()   } }

        // Additional state for bottom panel
        val friendCount = remember { mutableStateOf(0) }
        val leaderboardPosition = remember { mutableStateOf(1) }

        /* -------- camera ---------- */
        val viewport = rememberMapViewportState()

        LaunchedEffect(loc, heading) {
            loc ?: return@LaunchedEffect
            viewport.setCameraOptions {
                center(Point.fromLngLat(loc!!.longitude, loc!!.latitude))
                zoom(zoom3D)
                pitch(pitch3D)
                bearing(heading.toDouble())          // rotates the map
            }
        }

        /* -------- UI shell without top bar -------- */
        Box(Modifier.fillMaxSize()) {
            // The map takes the entire screen
            MapboxMap(
                modifier = Modifier.fillMaxSize(),
                mapViewportState = viewport,
                style = { MapStyle(style = styleUri) }
            ) {
                MapEffect(Unit) { mapView ->
                    val ctx       = mapView.context
                    val mapboxMap = mapView.getMapboxMap()

                    mapboxMap.getStyle { style ->

                        // a) add bitmap once
                        if (style.getStyleImage("player_arrow") == null) {
                            val bmp = vectorToBitmap(ctx, R.drawable.ic_player_arrow)
                            style.addImage("player_arrow", bmp, /* sdf = */ false)   // ← keep colours
                        }

                        // b) GeoJSON source once
                        if (!style.styleSourceExists("player_src")) {
                            style.addSource(
                                geoJsonSource("player_src") {
                                    featureCollection(FeatureCollection.fromFeatures(listOf()))
                                }
                            )
                        }

                        // c) symbol layer once (flat on ground)
                        if (!style.styleLayerExists("player_layer")) {
                            style.addLayer(
                                symbolLayer("player_layer", "player_src") {
                                    slot("top")
                                    iconImage("player_arrow")
                                    iconSize(2.0)
                                    iconAnchor(IconAnchor.CENTER)
                                    iconPitchAlignment(IconPitchAlignment.MAP)      // lies flat
                                    iconRotationAlignment(IconRotationAlignment.MAP)
                                    iconRotate(get("bearing"))
                                    iconAllowOverlap(true)
                                }
                            )
                        }
                    }
                }


                /* every fix / heading change */
                MapEffect(loc, heading) { mapView ->
                    val location = loc ?: return@MapEffect

                    mapView.getMapboxMap().getStyle { style ->
                        val src = style.getSourceAs<GeoJsonSource>("player_src") ?: return@getStyle

                        // Create a feature with the current location
                        val feature = Feature.fromGeometry(
                            Point.fromLngLat(location.longitude, location.latitude)
                        )

                        // Add the heading as a property to the feature
                        feature.addNumberProperty("bearing", heading.toDouble())

                        // Update the source with a new FeatureCollection containing our feature
                        val featureCollection = FeatureCollection.fromFeatures(listOf(feature))
                        src.featureCollection(featureCollection)
                    }
                }
            }

            // Circular buttons with higher elevation to ensure visibility
            Surface(
                modifier = Modifier
                    .padding(start = 5.dp, top = 31.dp, end = 16.dp, bottom = 16.dp)
                    .align(Alignment.TopStart),
                color = Color.Transparent,
                shadowElevation = 0.dp
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Friendex Button
                    CircleIconButton(
                        icon = Icons.Default.Person,
                        label = "Friendex",
                        badgeCount = friendCount.value,
                        onClick = onFriendexClick
                    )

                    // Leaderboard Button
                    CircleIconButton(
                        icon = Icons.Default.Menu,
                        label = "Leaderboard",
                        onClick = onLeaderboardClick
                    )

                    // Delete Account Button
                    CircleIconButton(
                        icon = Icons.Default.Delete,
                        label = "Delete",
                        color = accentRed,
                        onClick = onDeleteAccountClick
                    )
                }
            }

            // Bottom Panel UI
            BottomPanel(
                points = points ?: 0,
                friendCount = friendCount.value,
                leaderboardPosition = leaderboardPosition.value,
                onFriendexClick = onFriendexClick,
                onLeaderboardClick = onLeaderboardClick,
                onDeleteAccountClick = onDeleteAccountClick,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }

    /* ---------------------------------------------------------------------- */
    /*  Bottom Panel Component                                                */
    /* ---------------------------------------------------------------------- */
    @Composable
    private fun BottomPanel(
        points: Int = 0,
        friendCount: Int = 0,
        leaderboardPosition: Int = 1,
        onFriendexClick: () -> Unit = {},
        onLeaderboardClick: () -> Unit = {},
        onDeleteAccountClick: () -> Unit = {},
        modifier: Modifier = Modifier
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .fillMaxHeight(0.3f)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .border(
                    width = 2.dp,
                    color = accentBlue,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                )
                .background(darkSurface)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Top row with points and leaderboard position
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$points points",
                        color = accentBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )

                    Text(
                        text = "Leaderboard Position: #$leaderboardPosition",
                        color = textSecondary,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Tracking status
                Text(
                    text = "Tracking: Nobody :(",
                    color = textWhite,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                // Tracking explanation
                Text(
                    text = "Be within 5 metres of someone you track for 20 out of any 30 minute block to get points!",
                    color = textSecondary,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    textAlign = TextAlign.Center
                )

                // No buttons anymore in bottom panel
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }

    @Composable
    private fun CircleIconButton(
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        label: String,
        badgeCount: Int? = null,
        color: Color = accentBlue,
        onClick: () -> Unit
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Circle button with icon
            Box {
                IconButton(
                    onClick = onClick,
                    modifier = Modifier
                        .size(56.dp)
                        .border(2.dp, color, CircleShape)
                        .background(Color.White, CircleShape)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = color,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Optional badge
                if (badgeCount != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 2.dp, y = (-2).dp)
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(Color.Red)
                            .border(1.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = badgeCount.toString(),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Label under the button
            Text(
                text = label,
                color = Color.Black,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }

    /* ---------------------------------------------------------------------- */
    /*  Heading sensor helper                                                */
    /* ---------------------------------------------------------------------- */
    @Composable
    private fun rememberDeviceHeading(): State<Float> {
        val ctx = LocalContext.current
        val heading = remember { mutableStateOf(0f) }

        DisposableEffect(Unit) {
            val mgr = ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            val rotSensor = mgr.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) ?: return@DisposableEffect onDispose{}
            val R = FloatArray(9); val orient = FloatArray(3)

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

// ---------- helper (place in the same file, outside any composable) ----------
private fun vectorToBitmap(ctx: Context, resId: Int): Bitmap {
    val drawable = androidx.core.content.ContextCompat.getDrawable(ctx, resId)
        ?: error("Drawable $resId not found")
    val bmp = createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight)
    val canvas = android.graphics.Canvas(bmp)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bmp
}