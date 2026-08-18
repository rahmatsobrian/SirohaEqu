package com.rahmatsobrian.sirohaequ.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.audiofx.AudioEffect
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.rahmatsobrian.sirohaequ.MainActivity
import com.rahmatsobrian.sirohaequ.audio.AudioEngine
import com.rahmatsobrian.sirohaequ.logging.AppLogger

/**
 * Foreground service that:
 *  1. Listens for [AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION], the
 *     standard broadcast cooperating player apps send when they start
 *     playback, to learn which audio session id to attach the equalizer to.
 *  2. Re-attaches automatically on [AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION]
 *     followed by a new open broadcast (covers Bluetooth reconnect, wired
 *     headset reconnect, and route changes that cause the player to recycle
 *     its session).
 *
 * Kept intentionally lightweight: no polling, no wakelocks beyond what the
 * foreground-service contract itself requires, to avoid unnecessary battery
 * drain per spec section 11.
 */
class AudioProcessingService : Service() {

    private lateinit var audioEngine: AudioEngine

    private val sessionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            try {
                when (intent.action) {
                    AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION -> {
                        val sessionId = intent.getIntExtra(AudioEffect.EXTRA_AUDIO_SESSION, -1)
                        if (sessionId != -1) {
                            audioEngine.attachToSession(sessionId)
                            AppLogger.log("AudioProcessingService", "Attached to session $sessionId")
                        }
                    }
                    AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION -> {
                        audioEngine.release()
                        AppLogger.log("AudioProcessingService", "Session closed, engine released")
                    }
                }
            } catch (e: Exception) {
                AppLogger.logError("AudioProcessingService", "sessionReceiver.onReceive failed", e)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        audioEngine = AudioEngine(applicationContext)
        createNotificationChannel()

        val filter = IntentFilter().apply {
            addAction(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION)
            addAction(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(sessionReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(sessionReceiver, filter)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())
        // START_STICKY: if the system kills the process under memory pressure,
        // request a restart so audio processing resumes without user action.
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        try {
            unregisterReceiver(sessionReceiver)
        } catch (_: IllegalArgumentException) {
            // Not registered — safe to ignore.
        }
        audioEngine.release()
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = android.app.PendingIntent.getActivity(
            this, 0, openAppIntent,
            android.app.PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Siroha Equ aktif")
            .setContentText("Equalizer sedang memproses audio")
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode_off)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Audio Processing", NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "audio_processing"
        private const val NOTIFICATION_ID = 1001
    }
}
