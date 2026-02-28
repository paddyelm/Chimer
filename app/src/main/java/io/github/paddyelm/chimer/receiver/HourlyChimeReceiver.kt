package io.github.paddyelm.chimer.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.core.content.getSystemService
import io.github.paddyelm.chimer.util.ChimeScheduler

class HourlyChimeReceiver: BroadcastReceiver() {

    companion object {
        private const val TAG = "HourlyChimeReceiver";
    }

    override fun onReceive(context: Context, intent: Intent) {

        val vibrator = context.getSystemService<Vibrator>();

        if (vibrator != null && vibrator.hasVibrator()) {

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val effect = VibrationEffect.createWaveform(longArrayOf(0, 50, 100, 50), -1);
                    vibrator.vibrate(effect);
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(longArrayOf(0, 50, 100, 50), -1);
                }
            } catch (e: Exception) {
                Log.e(TAG, "Vibration failed, attempting fallback sound", e);
                fallBackChime();
            }

        } else {
            fallBackChime();
        }

        ChimeScheduler.scheduleNextChime(context)

    }

    /**
     * Play a chime using the ToneGenerator.
     */
    private fun fallBackChime() {

        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 200);
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play fallback chime tone", e);
        }

    }

}