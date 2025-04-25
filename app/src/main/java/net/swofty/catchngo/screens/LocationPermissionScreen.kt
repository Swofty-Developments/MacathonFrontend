package net.swofty.catchngo.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.android.gms.location.*
import net.swofty.catchngo.R

@Suppress("PrivatePropertyName")
class LocationPermissionScreen {

    // ────────── brand colours ──────────
    private val darkBackground = Color(0xFF15202B)
    private val darkSurface    = Color(0xFF1E2732)
    private val accentBlue     = Color(0xFF1DA1F2)
    private val textWhite      = Color(0xFFE7E9EA)
    private val textSecondary  = Color(0xFF8899A6)

    // ────────── poppins font family ────────
    private val poppinsFamily = FontFamily(
        Font(R.font.poppins_regular, FontWeight.Normal),
        Font(R.font.poppins_medium, FontWeight.Medium),
        Font(R.font.poppins_semibold, FontWeight.SemiBold),
        Font(R.font.poppins_bold, FontWeight.Bold)
    )

    // ────────── settings shortcut ──────
    private fun launchAppSettings(context: Context) {
        val intent = Intent(
            android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null)
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    // ────────── main composable ────────
    @SuppressLint("MissingPermission")
    @Composable
    fun PermissionScreen(onPermissionGranted: () -> Unit) {

        val context        = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current
        val fusedClient    = remember {
            LocationServices.getFusedLocationProviderClient(context)
        }

        var currentLocation  by remember { mutableStateOf<Location?>(null) }
        var showLocation     by remember { mutableStateOf(false) }

        var hasFinePerm      by rememberSaveable { mutableStateOf(false) }
        var hasBgPerm        by rememberSaveable { mutableStateOf(false) }

        // ── refresh perms every time we come back from Settings ────────
        DisposableEffect(lifecycleOwner) {
            val obs = LifecycleEventObserver { _, e ->
                if (e == Lifecycle.Event.ON_START) {
                    hasFinePerm = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED

                    hasBgPerm = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED

                    if (!hasFinePerm) {
                        currentLocation = null
                        showLocation    = false
                    }
                }
            }
            lifecycleOwner.lifecycle.addObserver(obs)
            onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
        }

        // ── launchers ──────────────────────────────────────────────────
        val bgPermLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            hasBgPerm = granted
            // On Android 11+, a direct request almost always yields false.
            // Push user straight to Settings so they can choose "Always allow".
            if (!granted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                launchAppSettings(context)
            }
        }

        val finePermLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { map ->
            hasFinePerm = map[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        }

        // ── ask for permissions on first entry ─────────────────────────
        LaunchedEffect(Unit) {
            val gpsEnabled = (context.getSystemService(Context.LOCATION_SERVICE) as LocationManager)
                .isProviderEnabled(LocationManager.GPS_PROVIDER)

            if (!gpsEnabled) return@LaunchedEffect

            if (!hasFinePerm) {
                finePermLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !hasBgPerm) {
                bgPermLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
        }

        // ── once everything is granted, bubble up to caller ────────────
        LaunchedEffect(hasFinePerm, hasBgPerm) {
            if (hasFinePerm &&
                (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || hasBgPerm)
            ) onPermissionGranted()
        }

        // ── live updates guarded with try/catch ────────────────────────
        if (hasFinePerm) {
            DisposableEffect(Unit) {
                val req = LocationRequest.Builder(
                    Priority.PRIORITY_HIGH_ACCURACY, 5_000
                ).setMinUpdateIntervalMillis(3_000).build()

                val cb = object : LocationCallback() {
                    override fun onLocationResult(res: LocationResult) {
                        res.lastLocation?.let {
                            currentLocation = it
                            showLocation    = true
                        }
                    }
                }
                runCatching {
                    fusedClient.requestLocationUpdates(req, cb, Looper.getMainLooper())
                }.onFailure {
                    currentLocation = null; showLocation = false
                }
                onDispose { fusedClient.removeLocationUpdates(cb) }
            }
        }

        // ── UI ──────────────────────────────
        Box(
            Modifier.fillMaxSize().background(darkBackground).padding(16.dp)
        ) {
            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.LocationOn, null,
                    tint = accentBlue, modifier = Modifier.size(80.dp))

                Spacer(Modifier.height(24.dp))

                Text("Catch N Go",
                    fontFamily = poppinsFamily,
                    fontWeight = FontWeight.Bold,
                    color = textWhite,
                    fontSize = 32.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(16.dp))

                Text("Location Tracking Required",
                    fontFamily = poppinsFamily,
                    fontWeight = FontWeight.SemiBold,
                    color = textWhite,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    "We need 24/7 access (\"Always allow\") so we can award points for who you sit next to.",
                    fontFamily = poppinsFamily,
                    fontWeight = FontWeight.Normal,
                    color = textSecondary,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(24.dp))

                if (showLocation && currentLocation != null) {
                    LiveLocationCard(
                        latitude  = currentLocation!!.latitude,
                        longitude = currentLocation!!.longitude
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = onPermissionGranted,
                        shape   = RoundedCornerShape(50),
                        colors  = ButtonDefaults.buttonColors(accentBlue, Color.White),
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                            .border(
                                1.dp,
                                Brush.sweepGradient(
                                    listOf(
                                        Color.White.copy(0f), Color.White.copy(0f),
                                        Color.White.copy(0f), Color.White.copy(0.3f),
                                        Color.White.copy(0f), Color.White.copy(0f),
                                        Color.White.copy(0f)
                                    ), Offset.Zero
                                ),
                                RoundedCornerShape(50)
                            )
                    ) {
                        Text(
                            "Continue",
                            fontFamily = poppinsFamily,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                } else {
                    CircularProgressIndicator(color = accentBlue, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Waiting for location permissions…",
                        fontFamily = poppinsFamily,
                        fontWeight = FontWeight.Medium,
                        color = textSecondary,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    // ────────── pretty card ────────────
    @Composable
    private fun LiveLocationCard(latitude: Double, longitude: Double) {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .background(darkSurface, RoundedCornerShape(12.dp))
                .border(
                    1.dp,
                    Brush.sweepGradient(
                        listOf(
                            accentBlue.copy(0.2f), accentBlue.copy(0.5f),
                            accentBlue.copy(0.8f), accentBlue,
                            accentBlue.copy(0.8f), accentBlue.copy(0.5f),
                            accentBlue.copy(0.2f)
                        ), Offset.Zero
                    ),
                    RoundedCornerShape(12.dp)
                )
                .padding(16.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Your Current Location",
                    fontFamily = poppinsFamily,
                    fontWeight = FontWeight.SemiBold,
                    color = textWhite,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(8.dp))
                Text(
                    "Latitude: $latitude",
                    fontFamily = poppinsFamily,
                    fontWeight = FontWeight.Normal,
                    color = textWhite,
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Longitude: $longitude",
                    fontFamily = poppinsFamily,
                    fontWeight = FontWeight.Normal,
                    color = textWhite,
                    fontSize = 14.sp
                )
            }
        }
    }
}