package com.candies.simhuber.vowifi

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class VoWifiForegroundService : Service() {

    companion object {
        private const val CHANNEL_ID = "simhuber_vowifi_channel"
        private const val NOTIF_ID = 1001
        const val EXTRA_STATUS_TEXT = "status_text"

        fun startOrUpdate(context: Context, statusText: String) {
            val intent = Intent(context, VoWifiForegroundService::class.java).apply {
                putExtra(EXTRA_STATUS_TEXT, statusText)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, VoWifiForegroundService::class.java))
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ensureChannel()
        val text = intent?.getStringExtra(EXTRA_STATUS_TEXT) ?: "SIMHuber VoWiFi 通道保活中"
        startForeground(NOTIF_ID, buildNotification(text))
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID,
                "SIMHuber VoWiFi 保活服务",
                NotificationManager.IMPORTANCE_LOW
            )
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(content: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SIMHuber VoWiFi")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.stat_sys_phone_call)
            .setOngoing(true)
            .build()
    }
}
