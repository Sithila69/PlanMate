package com.planmate

import android.app.KeyguardManager
import android.content.Context
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Bundle
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class AlarmActivity : AppCompatActivity() {
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var vibrator: Vibrator
    private lateinit var wakeLock: PowerManager.WakeLock

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarm)

        // Keep the screen on and bright
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD)

        // Acquire wake lock
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyApp::AlarmWakeLock")
        wakeLock.acquire(10 * 60 * 1000L /*10 minutes*/)

        // Get task title from intent
        val taskTitle = intent.getStringExtra("taskTitle") ?: "Reminder"
        findViewById<TextView>(R.id.taskTitleTextView).text = taskTitle

        // Set up stop button
        findViewById<Button>(R.id.stopAlarmButton).setOnClickListener {
            stopAlarmAndFinish()
        }

        // Start alarm sound and vibration
        startAlarm()
    }

    private fun startAlarm() {
        // Play alarm sound
        val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        mediaPlayer = MediaPlayer.create(this, alarmUri).apply {
            isLooping = true
            start()
        }

        // Vibrate
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        val vibrationPattern = longArrayOf(0, 1000, 1000)
        vibrator.vibrate(VibrationEffect.createWaveform(vibrationPattern, 0))
    }

    private fun stopAlarmAndFinish() {
        mediaPlayer.stop()
        mediaPlayer.release()
        vibrator.cancel()
        if (wakeLock.isHeld) {
            wakeLock.release()
        }
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::mediaPlayer.isInitialized) {
            mediaPlayer.release()
        }
        if (::vibrator.isInitialized) {
            vibrator.cancel()
        }
        if (::wakeLock.isInitialized && wakeLock.isHeld) {
            wakeLock.release()
        }
    }
}
