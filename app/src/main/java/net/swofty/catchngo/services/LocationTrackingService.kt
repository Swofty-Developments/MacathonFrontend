// ────────────────────────────────────────────────────────────────────────────────
// LocationTrackingService.kt
// ────────────────────────────────────────────────────────────────────────────────
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
import kotlin.system.measureTimeMillis

class LocationTrackingService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var fused: FusedLocationProviderClient
    private lateinit var callback: LocationCallback

    // 1-second min interval, desired 2 s, deferrable 10 s for batching
    private val request = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY, 2_00
    )
        .setMinUpdateIntervalMillis(0)                 // deliver as fast as possible
        .setMaxUpdateDelayMillis(0)                    // **no batching**
        .setGranularity(Granularity.GRANULARITY_FINE)
        .setWaitForAccurateLocation(false)
        .build()

    /* keep track of last backend POST time (ms) */
    private var lastUpload = 0L

    override fun onCreate() {
        super.onCreate()
        fused = LocationServices.getFusedLocationProviderClient(this)

        callback = object : LocationCallback() {
            override fun onLocationResult(res: LocationResult) {
                res.lastLocation?.let(::handleLocation)
            }
        }
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(ID, foregroundNotification())
        startUpdates()
        return START_STICKY
    }

    override fun onBind(i: Intent?): IBinder? = null

    override fun onDestroy() {
        fused.removeLocationUpdates(callback)
        super.onDestroy()
    }

    private fun startUpdates() = try {
        fused.requestLocationUpdates(request, callback, Looper.getMainLooper())
    } catch (_: SecurityException) { /* missing permission */ }

    /* ---------------------------------------------------------------------- */
    /*  Handle each GPS fix                                                   */
    /* ---------------------------------------------------------------------- */
    private fun handleLocation(loc: Location) = scope.launch {
        // 1. push to in-app store immediately
        LocationStore.update(loc)

        // 2. throttle backend upload to once every 10 s
        val now = System.currentTimeMillis()
        if (now - lastUpload >= 10_000) {
            lastUpload = now
            // TODO: ApiManager.uploadLocation(loc)  ← your real call
            android.util.Log.d("LocationService", "Uploaded ${loc.latitude},${loc.longitude}")
        }
    }

    /* ---------------------------------------------------------------------- */
    /*  Notification helpers                                                  */
    /* ---------------------------------------------------------------------- */
    private fun foregroundNotification(): Notification {
        val pi = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Catch N Go")
            .setContentText("Tracking location…")
            .setContentIntent(pi)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                CHANNEL, "Location tracking", NotificationManager.IMPORTANCE_LOW
            ).apply { setShowBadge(false) }
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(ch)
        }
    }

    companion object {
        private const val ID = 12345
        private const val CHANNEL = "location_channel"
    }
}
