package net.swofty.catchngo

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import android.Manifest
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import net.swofty.catchngo.models.AuthViewModel
import net.swofty.catchngo.screens.HomeScreen
import net.swofty.catchngo.screens.LocationPermissionScreen
import net.swofty.catchngo.screens.LoginScreenClass
import net.swofty.catchngo.screens.RegisterScreenClass
import net.swofty.catchngo.services.LocationTrackingService
import net.swofty.catchngo.ui.theme.CatchNGoTheme
import net.swofty.catchngo.models.GameViewModel

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CatchNGoTheme {
                val navController = rememberNavController()
                val authViewModel: AuthViewModel = viewModel()
                val gameViewModel: GameViewModel = viewModel()

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AppNavHost(
                        navController = navController,
                        authViewModel = authViewModel,
                        gameViewModel = gameViewModel,
                        startLocationService = { startLocationTrackingService() },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    private fun startLocationTrackingService() {
        val hasFine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        // Android 10+ also requires FOREGROUND_SERVICE_LOCATION declared in manifest
        if (hasFine || hasCoarse) {
            val serviceIntent = Intent(this, LocationTrackingService::class.java)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        } else {
            android.util.Log.e("MainActivity", "Location permissions not granted, not starting location service.")
        }
    }
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    gameViewModel: GameViewModel,
    startLocationService: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentDestination = currentBackStack?.destination?.route

    NavHost(
        navController = navController,
        startDestination = "locationPermission", // Changed to start with location permission
        modifier = modifier
    ) {
        // Add location permission screen
        composable("locationPermission") {
            LocationPermissionScreen().PermissionScreen(
                onPermissionGranted = {
                    // Start location tracking service
                    startLocationService()
                    // Navigate to login
                    navController.navigate("login") {
                        popUpTo("locationPermission") { inclusive = true }
                    }
                }
            )
        }

        composable("login") {
            LoginScreenClass().LoginScreen(
                onLoginSuccess = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate("register")
                },
                authViewModel = authViewModel
            )
        }

        composable("register") {
            RegisterScreenClass().RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                },
                authViewModel = authViewModel
            )
        }

        composable("home") {
            HomeScreen().HomeContent(
                onLogout = {
                    authViewModel.logout()
                    navController.navigate("login") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                gameViewModel = gameViewModel
            )
        }
    }
}