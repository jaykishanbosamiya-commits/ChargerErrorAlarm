package com.example.chargererroralarm

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private lateinit var delayText: TextView
    private lateinit var delayBar: SeekBar
    private lateinit var enabledSwitch: Switch
    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        delayText = findViewById(R.id.delayText)
        delayBar = findViewById(R.id.delayBar)
        enabledSwitch = findViewById(R.id.enabledSwitch)
        statusText = findViewById(R.id.statusText)

        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val delay = prefs.getInt("delay_seconds", 5)
        enabledSwitch.isChecked = prefs.getBoolean("enabled", true)
        delayBar.progress = delay - 1
        updateDelayText(delay)

        delayBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                val seconds = p + 1
                updateDelayText(seconds)
                prefs.edit().putInt("delay_seconds", seconds).apply()
            }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })

        enabledSwitch.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("enabled", checked).apply()
            if (!checked) {
                stopService(Intent(this, ChargerMonitorService::class.java))
                statusText.text = "Monitoring: OFF"
            }
        }

        findViewById<Button>(R.id.startButton).setOnClickListener {
            if (!enabledSwitch.isChecked) enabledSwitch.isChecked = true
            ContextCompat.startForegroundService(this, Intent(this, ChargerMonitorService::class.java))
            statusText.text = "Monitoring: STARTING..."
        }

        findViewById<Button>(R.id.stopButton).setOnClickListener {
            stopService(Intent(this, ChargerMonitorService::class.java))
            statusText.text = "Monitoring: OFF"
        }

        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
        }
    }

    private fun updateDelayText(seconds: Int) {
        delayText.text = "Alarm delay: $seconds second${if (seconds == 1) "" else "s"}"
    }
}
