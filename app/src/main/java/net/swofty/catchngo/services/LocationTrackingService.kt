package net.swofty.catchngo.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import net.swofty.catchngo.MainActivity
import net.swofty.catchngo.R

class LocationTrackingService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    // 10 s desired interval, 5 s fastest, 15 s max defer
    private val locationRequest = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY, 10_000
    )
        .setMinUpdateIntervalMillis(5_000)
        .setMaxUpdateDelayMillis(15_000)
        .setWaitForAccurateLocation(false)
        .build()

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let(::handleLocationUpdate)
            }
        }
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())
        startLocationUpdates()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopLocationUpdates()
        super.onDestroy()
    }

    private fun startLocationUpdates() = try {
        fusedLocationClient.requestLocationUpdates(
            locationRequest, locationCallback, Looper.getMainLooper()
        )
    } catch (_: SecurityException) { /* permission denied; ignore */ }

    private fun stopLocationUpdates() =
        fusedLocationClient.removeLocationUpdates(locationCallback)

    private fun handleLocationUpdate(location: Location) = serviceScope.launch {
        // TODO: Send to backend via ApiManager when ready
        android.util.Log.d(
            "LocationTrackingService",
            "Location: ${location.latitude}, ${location.longitude}"
        )
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Catch N Go")
            .setContentText("Tracking location to award points")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Location Tracking", NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Used for tracking location in background"
                setShowBadge(false)
            }
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    companion object {
        private const val NOTIFICATION_ID = 12345
        private const val CHANNEL_ID = "location_channel"
    }
}
