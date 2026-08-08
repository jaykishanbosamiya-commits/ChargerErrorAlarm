package com.example.chargererroralarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat

class ChargerMonitorService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private var chargerConnected = false
    private var alarmPlaying = false
    private var ringtone: Ringtone? = null
    private lateinit var powerReceiver: BroadcastReceiver

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(10, buildNotification("Monitoring charger status"))

        powerReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_POWER_CONNECTED -> {
                        chargerConnected = true
                        checkAfterDelay()
                    }
                    Intent.ACTION_POWER_DISCONNECTED -> {
                        chargerConnected = false
                        stopAlarm()
                        updateNotification("Charger disconnected")
                    }
                    Intent.ACTION_BATTERY_CHANGED -> {
                        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
                        chargerConnected = plugged != 0
                        if (chargerConnected) {
                            if (status == BatteryManager.BATTERY_STATUS_CHARGING) {
                                stopAlarm()
                                updateNotification("Charging normally")
                            } else {
                                checkAfterDelay()
                            }
                        } else {
                            stopAlarm()
                            updateNotification("Charger disconnected")
                        }
                    }
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_BATTERY_CHANGED)
        }

        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(powerReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(powerReceiver, filter)
        }
    }

    private fun checkAfterDelay() {
        handler.removeCallbacksAndMessages(null)

        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        if (!prefs.getBoolean("enabled", true)) {
            stopAlarm()
            return
        }

        val delaySeconds = prefs.getInt("delay_seconds", 5)

        handler.postDelayed({
            val batteryManager = getSystemService(BATTERY_SERVICE) as BatteryManager
            val isCharging = batteryManager.isCharging

            if (chargerConnected && !isCharging) {
                startAlarm()
                updateNotification("ERROR: charger connected but NOT charging")
            } else {
                stopAlarm()
                updateNotification("Charging normally")
            }
        }, delaySeconds * 1000L)
    }

    private fun startAlarm() {
        if (alarmPlaying) return
        alarmPlaying = true

        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        ringtone = RingtoneManager.getRingtone(applicationContext, uri)
        ringtone?.audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        ringtone?.isLooping = true
        ringtone?.play()

        val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        if (vibrator.hasVibrator()) {
            vibrator.vibrate(
                VibrationEffect.createWaveform(
                    longArrayOf(0, 400, 300),
                    0
                )
            )
        }
    }

    private fun stopAlarm() {
        if (!alarmPlaying) return
        alarmPlaying = false
        ringtone?.stop()
        ringtone = null

        val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        vibrator.cancel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                "charger_monitor",
                "Charger Monitoring",
                NotificationManager.IMPORTANCE_LOW
            )
            channel.description = "Shows charger monitoring status"
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String): Notification {
        return NotificationCompat.Builder(this, "charger_monitor")
            .setContentTitle("Charger Error Alarm")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String) {
        getSystemService(NotificationManager::class.java)
            .notify(10, buildNotification(text))
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        stopAlarm()
        unregisterReceiver(powerReceiver)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
