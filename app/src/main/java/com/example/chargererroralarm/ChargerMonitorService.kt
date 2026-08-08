package com.example.chargererroralarm

import android.app.*
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.*
import androidx.core.app.NotificationCompat

class ChargerMonitorService : Service() {
    companion object {
        const val CHANNEL_ID = "charger_monitor"
        const val NOTIFICATION_ID = 10
        const val INTERVAL = 2000L
    }

    private val handler = Handler(Looper.getMainLooper())
    private var monitoring = false
    private var badSince = 0L
    private var alarmPlaying = false
    private var ringtone: Ringtone? = null

    private val checker = object : Runnable {
        override fun run() {
            if (!monitoring) return
            checkState()
            handler.postDelayed(this, INTERVAL)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!monitoring) {
            startAsForeground()
            monitoring = true
            handler.post(checker)
        }
        return START_STICKY
    }

    private fun startAsForeground() {
        val n = notification("Monitoring charger status")
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIFICATION_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, n)
        }
    }

    private fun checkState() {
        val i = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val status = i?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val plugged = i?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
        val connected = plugged != 0
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING

        if (!connected) {
            badSince = 0
            stopAlarm()
            update("Monitoring: charger disconnected")
            return
        }

        if (charging) {
            badSince = 0
            stopAlarm()
            update("Charging normally")
            return
        }

        if (!getSharedPreferences("settings", MODE_PRIVATE).getBoolean("enabled", true)) {
            badSince = 0
            stopAlarm()
            update("Alarm disabled")
            return
        }

        if (badSince == 0L) badSince = System.currentTimeMillis()
        val delay = getSharedPreferences("settings", MODE_PRIVATE).getInt("delay_seconds", 5) * 1000L
        val elapsed = System.currentTimeMillis() - badSince

        if (elapsed >= delay) {
            startAlarm()
            update("ERROR: charger connected but NOT charging")
        } else {
            val remaining = ((delay - elapsed + 999) / 1000)
            update("Charger connected — checking (${remaining}s)")
        }
    }

    private fun startAlarm() {
        if (alarmPlaying) return
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        ringtone = RingtoneManager.getRingtone(applicationContext, uri)
        ringtone?.audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        if (Build.VERSION.SDK_INT >= 28) ringtone?.isLooping = true
        ringtone?.play()
        alarmPlaying = true

        val v = getSystemService(VIBRATOR_SERVICE) as Vibrator
        if (v.hasVibrator()) v.vibrate(VibrationEffect.createWaveform(longArrayOf(0,500,300), 0))
    }

    private fun stopAlarm() {
        if (!alarmPlaying) return
        ringtone?.stop()
        ringtone = null
        alarmPlaying = false
        (getSystemService(VIBRATOR_SERVICE) as Vibrator).cancel()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val c = NotificationChannel(CHANNEL_ID, "Charger Monitoring", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(c)
        }
    }

    private fun notification(text: String): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Charger Error Alarm")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()

    private fun update(text: String) {
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification(text))
    }

    override fun onDestroy() {
        monitoring = false
        handler.removeCallbacksAndMessages(null)
        stopAlarm()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
