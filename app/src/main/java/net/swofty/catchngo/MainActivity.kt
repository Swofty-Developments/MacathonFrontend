package net.swofty.catchngo

import android.Manifest
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mapbox.common.MapboxOptions
import net.swofty.catchngo.models.*
import net.swofty.catchngo.screens.*
import net.swofty.catchngo.services.LocationTrackingService
import net.swofty.catchngo.ui.theme.CatchNGoTheme

class MainActivity : ComponentActivity() {

    private lateinit var navController: NavHostController

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        MapboxOptions.accessToken = "pk.eyJ1Ijoic3dvZnR5IiwiYSI6ImNtOXZtanVnaDBsdnAycnB2N3NrYmJmdXMifQ.ICveA0UdI_uFxo_x8b6bWw"

        setContent {
            CatchNGoTheme {
                navController = rememberNavController()
                val authViewModel:      AuthViewModel      = viewModel()
                val gameViewModel:      GameViewModel      = viewModel()
                val questionsViewModel: QuestionsViewModel = viewModel()
                val imageViewModel:     ImageViewModel     = viewModel()

                Scaffold(Modifier.fillMaxSize()) { inner ->
                    AppNavHost(
                        navController        = navController,
                        authViewModel        = authViewModel,
                        gameViewModel        = gameViewModel,
                        questionsViewModel   = questionsViewModel,
                        startLocationService = ::startLocationTrackingService,
                        imageViewModel       = imageViewModel,
                        modifier             = Modifier.padding(inner)
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val fine = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val bg   = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!fine || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !bg)) {
            if (::navController.isInitialized) {
                navController.navigate("locationPermission") { popUpTo(0) }
            }
            stopService(Intent(this, LocationTrackingService::class.java))
        }
    }

    private fun startLocationTrackingService() {
        val fine = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (fine || coarse) {
            val i = Intent(this, LocationTrackingService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(i)
            else startService(i)
        }
    }
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    gameViewModel: GameViewModel,
    questionsViewModel: QuestionsViewModel,
    imageViewModel: ImageViewModel,
    startLocationService: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentBackStack by navController.currentBackStackEntryAsState()

    NavHost(
        navController    = navController,
        startDestination = "locationPermission",
        modifier         = modifier
    ) {
        composable("locationPermission") {
            LocationPermissionScreen().PermissionScreen(
                onPermissionGranted = {
                    startLocationService()
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
                onNavigateToRegister = { navController.navigate("register") },
                authViewModel        = authViewModel
            )
        }

        composable("register") {
            RegisterScreenClass().RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateToLogin  = { navController.popBackStack() },
                authViewModel      = authViewModel,
                questionsViewModel = questionsViewModel,
                imageViewModel     = imageViewModel
            )
        }

        composable("home") {
            // Ensure critical services are running whenever Home screen is shown
            LaunchedEffect (Unit) {
                gameViewModel.refreshProfile()
            }

            HomeScreen().HomeContent(
                onLogout      = {
                    authViewModel.logout()
                    navController.navigate("login") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                gameViewModel = gameViewModel
            )
        }

        composable("leaderboard") {
            LeaderboardScreen().LeaderboardContent(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable("friendex") {
            FriendexScreen().FriendexContent(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable("achievements") {
            AchievementsScreenClass().AchievementsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
