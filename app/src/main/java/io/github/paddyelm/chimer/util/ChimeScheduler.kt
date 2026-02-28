package io.github.paddyelm.chimer.util

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.icu.util.Calendar
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.content.getSystemService
import io.github.paddyelm.chimer.receiver.HourlyChimeReceiver


/**
 * Utility object responsible for managing the scheduling and cancellation of hourly chime alarms.
 *
 * This scheduler utilizes the [AlarmManager] to trigger a broadcast at the start of every hour.
 * It handles the calculation of the next occurrence, ensures exact timing even when the device
 * is idle (using [AlarmManager.setExactAndAllowWhileIdle]), and manages the necessary
 * permissions for exact alarms on Android 12 (API 31) and above.
 */
object ChimeScheduler {

    private const val TAG = "ChimeScheduler";
    private const val REQUEST_CODE = 1001;


    /**
     * Schedules the next hourly chime alarm.
     *
     * This function calculates the time for the next full hour (e.g., if it is currently 1:45 PM,
     * it schedules for 2:00 PM) and sets an exact alarm using [AlarmManager].
     *
     * For devices running Android 12 (API 31) and above, it checks for the
     * `SCHEDULE_EXACT_ALARM` permission before attempting to schedule the alarm.
     *
     * @param context The application or activity context used to access system services and
     * create the [PendingIntent].
     */
    fun scheduleNextChime(context: Context) {

        val alarmManager = retrieveAlarmManager(context) ?: return;

        // Check if we can schedule exact alarms (Required for Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.w(TAG, "Exact alarm permission not granted.")
                openExactAlarmPermissionSettings(context);
                return;
            }
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, HourlyChimeReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        );

        val calendar = Calendar.getInstance().apply {
            add(Calendar.HOUR_OF_DAY, 1);
            set(Calendar.MINUTE, 0);
            set(Calendar.SECOND, 0);
            set(Calendar.MILLISECOND, 0);
        };

        try {
            // RTC_WAKEUP ensures the watch wakes up from ambient mode to vibrate
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException: Cannot schedule exact alarm", e);
            openExactAlarmPermissionSettings(context);
            return;
        }

    }

    /**
     * Cancels any previously scheduled hourly chime alarm.
     *
     * This function retrieves the [AlarmManager] and attempts to cancel the [PendingIntent]
     * associated with the [HourlyChimeReceiver]. This effectively stops the scheduled
     * chime from triggering until [scheduleNextChime] is called again.
     *
     * @param context The application or activity context used to access system services and
     * recreate the [PendingIntent] to be cancelled.
     */
    fun cancelChime(context: Context) {
        val alarmManager = retrieveAlarmManager(context) ?: return;
        val pendingIntent = PendingIntent.getBroadcast(
          context,
            REQUEST_CODE,
            Intent(context, HourlyChimeReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        );

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
        }
    }

    private fun openExactAlarmPermissionSettings(context: Context) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Toast.makeText(
                context,
                "Please enable 'Alarms & Reminders' for Chimer to work.",
                Toast.LENGTH_LONG
            ).show()

            val intent =
                Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = android.net.Uri.fromParts("package", context.packageName, null);

                    if (context !is android.app.Activity) {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    }
                }

            context.startActivity(intent);
        } else {
            Log.i(TAG, "Android version < 12: Exact alarms are pre-granted.");
        }
    }


    /**
     * Retrieves the [AlarmManager] system service from the provided [context].
     *
     * If the service is unavailable, an error is logged, a toast message is displayed
     * to the user, and the function returns `null`.
     *
     * @param context The context used to access system services.
     * @return The [AlarmManager] instance, or `null` if the service could not be retrieved.
     */
    private fun retrieveAlarmManager (context: Context): AlarmManager? {
        val alarmManager = context.getSystemService<AlarmManager>();

        if (alarmManager == null) {
            Log.e(TAG, "Could not get AlarmManager system service. Chime cannot be scheduled.");

            Toast.makeText(
                context,
                "Error: Alarm System is unavailable on this device.",
                Toast.LENGTH_LONG
            ).show();
        }

        return alarmManager;
    }

}