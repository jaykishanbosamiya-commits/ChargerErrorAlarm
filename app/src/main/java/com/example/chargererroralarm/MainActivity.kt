package com.example.chargererroralarm

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        delayText = findViewById(R.id.delayText)
        delayBar = findViewById(R.id.delayBar)
        enabledSwitch = findViewById(R.id.enabledSwitch)

        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val savedDelay = prefs.getInt("delay_seconds", 5)
        val enabled = prefs.getBoolean("enabled", true)

        delayBar.progress = savedDelay - 1
        enabledSwitch.isChecked = enabled
        updateDelayText(savedDelay)

        delayBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                val seconds = progress + 1
                updateDelayText(seconds)
                prefs.edit().putInt("delay_seconds", seconds).apply()
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        enabledSwitch.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("enabled", checked).apply()
        }

        findViewById<Button>(R.id.startButton).setOnClickListener {
            startMonitor()
        }

        findViewById<Button>(R.id.stopButton).setOnClickListener {
            stopService(Intent(this, ChargerMonitorService::class.java))
        }

        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                100
            )
        }
    }

    private fun updateDelayText(seconds: Int) {
        delayText.text = "Alarm delay: $seconds second${if (seconds == 1) "" else "s"}"
    }

    private fun startMonitor() {
        val intent = Intent(this, ChargerMonitorService::class.java)
        ContextCompat.startForegroundService(this, intent)
    }
}
