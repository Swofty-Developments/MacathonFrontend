package net.swofty.catchngo.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.launch

class LocationPermissionScreen {
    // Define custom colors to match login/register screens
    private val darkBackground = Color(0xFF15202B) // Dark blue background like X
    private val darkSurface = Color(0xFF1E2732) // Slightly lighter for surfaces
    private val accentBlue = Color(0xFF1DA1F2) // Twitter/X blue
    private val textWhite = Color(0xFFE7E9EA) // Off-white text
    private val textSecondary = Color(0xFF8899A6) // Secondary text color

    @SuppressLint("MissingPermission")
    @Composable
    fun PermissionScreen(
        onPermissionGranted: () -> Unit
    ) {
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()

        // Current location state
        var currentLocation by remember { mutableStateOf<Location?>(null) }
        var showLocation by remember { mutableStateOf(false) }

        // Location client
        val fusedLocationClient: FusedLocationProviderClient = remember {
            LocationServices.getFusedLocationProviderClient(context)
        }

        // State to track if permissions are granted
        var hasBackgroundLocationPermission by remember {
            mutableStateOf(
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            )
        }
        var hasFineLocationPermission by remember {
            mutableStateOf(
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            )
        }

        // Define the background location permission launcher first
        val backgroundLocationPermissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            hasBackgroundLocationPermission = isGranted

            if (isGranted || hasFineLocationPermission) {
                getLocation(context, fusedLocationClient) { location ->
                    currentLocation = location
                    showLocation = true
                }
            }
        }

        // Then define the location permission launcher that uses it
        val locationPermissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            hasFineLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false

            // On Android 10+ (Q+), request background location separately after granting foreground
            if (hasFineLocationPermission && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                backgroundLocationPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            } else if (hasFineLocationPermission) {
                // For older Android versions, we're done with permissions
                getLocation(context, fusedLocationClient) { location ->
                    currentLocation = location
                    showLocation = true
                }
            }
        }

        // Effect to check if location is enabled
        LaunchedEffect(Unit) {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

            if (hasFineLocationPermission) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !hasBackgroundLocationPermission) {
                    backgroundLocationPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                } else {
                    // Safe to get location
                    getLocation(context, fusedLocationClient) { location ->
                        currentLocation = location
                        showLocation = true
                    }
                }
            } else {
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(darkBackground)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Location",
                    tint = accentBlue,
                    modifier = Modifier.size(80.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Catch N Go",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = textWhite,
                        fontSize = 32.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Location Tracking Required",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = textWhite
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Catch N Go requires your location 24/7 to award points based on who you sit next to. This is not optional to use the app.",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = textSecondary
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (showLocation && currentLocation != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .background(darkSurface, RoundedCornerShape(12.dp))
                            .border(
                                width = 1.dp,
                                brush = Brush.sweepGradient(
                                    colors = listOf(
                                        accentBlue.copy(alpha = 0.2f),
                                        accentBlue.copy(alpha = 0.5f),
                                        accentBlue.copy(alpha = 0.8f),
                                        accentBlue,
                                        accentBlue.copy(alpha = 0.8f),
                                        accentBlue.copy(alpha = 0.5f),
                                        accentBlue.copy(alpha = 0.2f)
                                    ),
                                    center = Offset.Zero
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(16.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Your Current Location",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = textWhite,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Latitude: ${currentLocation?.latitude}",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = textWhite
                                )
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Longitude: ${currentLocation?.longitude}",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = textWhite
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { onPermissionGranted() },
                        shape = RoundedCornerShape(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentBlue,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .border(
                                width = 1.dp,
                                brush = Brush.sweepGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.0f),
                                        Color.White.copy(alpha = 0.0f),
                                        Color.White.copy(alpha = 0.0f),
                                        Color.White.copy(alpha = 0.3f),
                                        Color.White.copy(alpha = 0.0f),
                                        Color.White.copy(alpha = 0.0f),
                                        Color.White.copy(alpha = 0.0f)
                                    ),
                                    center = Offset.Zero
                                ),
                                shape = RoundedCornerShape(50.dp)
                            )
                    ) {
                        Text(
                            "Continue",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    CircularProgressIndicator(
                        color = accentBlue,
                        modifier = Modifier.size(48.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Waiting for location permissions...",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = textSecondary
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLocation(
        context: Context,
        fusedLocationClient: FusedLocationProviderClient,
        onLocationReceived: (Location) -> Unit
    ) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) return

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    onLocationReceived(location)
                }
            }
    }

}