package com.itantra.app.communication.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.itantra.app.ITantraApplication
import com.itantra.app.MainActivity
import com.itantra.app.R
import com.itantra.app.core.logging.ITantraLogger
import com.itantra.app.domain.model.ConnectionState
import com.itantra.app.domain.model.MessageType
import com.itantra.app.domain.model.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Foreground service that keeps the offline P2P communication link alive in background.
 *
 * Capabilities:
 *  - Maintains Wi-Fi Direct and Bluetooth sockets when screen is locked or app minimized.
 *  - Holds a CPU WakeLock and WifiLock (high performance) to prevent Android Doze mode
 *    from sleeping the offline radio.
 *  - Displays an ongoing status notification indicating connected peer.
 *  - Wakes device and triggers maximum-volume emergency alert when priority broadcast arrives.
 */
class ITantraCommunicationService : Service() {

    companion object {
        private const val TAG = "ITantraService"
        const val CHANNEL_ID = "itantra_p2p_channel"
        const val NOTIFICATION_ID = 26173

        const val ACTION_START = "com.itantra.app.action.START_SERVICE"
        const val ACTION_STOP = "com.itantra.app.action.STOP_SERVICE"
        const val EXTRA_PEER_NAME = "extra_peer_name"

        fun startService(context: Context, peerName: String? = null) {
            val intent = Intent(context, ITantraCommunicationService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_PEER_NAME, peerName ?: "Connected Peer")
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, ITantraCommunicationService::class.java).apply {
                action = ACTION_STOP
            }
            context.stopService(intent)
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null
    private var packetListenerJob: Job? = null
    private var currentPeerName: String = "Connected Peer"

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        acquireLocks()
        startPacketMonitoring()
        ITantraLogger.i(TAG, "ITantraCommunicationService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopForeground(true)
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                currentPeerName = intent?.getStringExtra(EXTRA_PEER_NAME) ?: "Connected Peer"
                val notification = buildNotification(currentPeerName)
                startForeground(NOTIFICATION_ID, notification)
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun acquireLocks() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "iTantra:P2PWakeLock"
            )?.apply {
                setReferenceCounted(false)
                acquire(12 * 60 * 60 * 1000L) // up to 12 hours
            }

            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            wifiLock = wifiManager?.createWifiLock(
                WifiManager.WIFI_MODE_FULL_HIGH_PERF,
                "iTantra:P2PWifiLock"
            )?.apply {
                setReferenceCounted(false)
                acquire()
            }
            ITantraLogger.i(TAG, "Acquired WakeLock & WifiLock for offline P2P persistence")
        } catch (e: Exception) {
            ITantraLogger.w(TAG, "Could not acquire wake locks: ${e.message}")
        }
    }

    private fun releaseLocks() {
        try {
            wakeLock?.let {
                if (it.isHeld) it.release()
            }
            wakeLock = null

            wifiLock?.let {
                if (it.isHeld) it.release()
            }
            wifiLock = null
            ITantraLogger.i(TAG, "Released WakeLock & WifiLock")
        } catch (e: Exception) {
            ITantraLogger.w(TAG, "Error releasing locks: ${e.message}")
        }
    }

    private fun startPacketMonitoring() {
        packetListenerJob?.cancel()
        packetListenerJob = serviceScope.launch {
            val app = ITantraApplication.instance
            val connectionManager = app.connectionManager

            launch {
                connectionManager.connectedDevice.collect { device ->
                    if (device != null) {
                        currentPeerName = device.deviceName
                        updateNotification(currentPeerName)
                    }
                }
            }

            launch {
                connectionManager.connectionState.collect { state ->
                    if (state == ConnectionState.DISCONNECTED || state == ConnectionState.FAILED) {
                        ITantraLogger.i(TAG, "P2P disconnected, stopping foreground service")
                        stopForeground(true)
                        stopSelf()
                    }
                }
            }

            launch {
                connectionManager.receive().collect { packet ->
                    if (packet.priority == Priority.EMERGENCY || packet.messageType == MessageType.ALERT) {
                        showEmergencyNotification(packet.payload)
                    }
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "iTantra Offline Communication",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Keeps offline Wi-Fi Direct and Bluetooth walkie-talkie active"
                setShowBadge(true)
                enableVibration(true)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(peerName: String): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("iTantra Active Walkie-Talkie")
            .setContentText("Offline P2P link connected to $peerName")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun updateNotification(peerName: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(NOTIFICATION_ID, buildNotification(peerName))
    }

    private fun showEmergencyNotification(alertText: String) {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            1,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val emergencyNotification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🚨 EMERGENCY ALERT RECEIVED")
            .setContentText(alertText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(NOTIFICATION_ID + 1, emergencyNotification)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        ITantraLogger.i(TAG, "App task removed from recents: forcing system Wi-Fi Direct and Bluetooth teardown")
        try {
            ITantraApplication.instance.wifiTransport.forceSystemDisconnect()
            ITantraApplication.instance.bluetoothTransport.let { bt ->
                kotlinx.coroutines.runBlocking {
                    bt.disconnect()
                }
            }
        } catch (e: Exception) {
            ITantraLogger.w(TAG, "Error disconnecting on task removed: ${e.message}")
        }
        stopForeground(true)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        packetListenerJob?.cancel()
        serviceScope.cancel()
        releaseLocks()
        try {
            ITantraApplication.instance.wifiTransport.forceSystemDisconnect()
        } catch (_: Exception) {}
        ITantraLogger.i(TAG, "ITantraCommunicationService destroyed")
    }
}
