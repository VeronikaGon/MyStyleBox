package com.hfad.mystylebox.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import java.util.*

object AlarmScheduler {

    private const val MORNING_REQUEST_CODE = 2001
    private const val EVENING_REQUEST_CODE = 2002
    private const val STALE_REQUEST_CODE = 2003

    fun scheduleMorningReminder(context: Context) {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, 7)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        val intent = Intent(context, MorningReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            MORNING_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    fun scheduleEveningReminder(context: Context) {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, 20)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        val intent = Intent(context, EveningReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            EVENING_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    fun scheduleStaleItemsCheck(context: Context) {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        val intent = Intent(context, StaleOutfitsReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            STALE_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    fun cancelAllReminders(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val morningIntent = Intent(context, MorningReminderReceiver::class.java)
        val morningPI = PendingIntent.getBroadcast(
            context,
            MORNING_REQUEST_CODE,
            morningIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        am.cancel(morningPI)

        val eveningIntent = Intent(context, EveningReminderReceiver::class.java)
        val eveningPI = PendingIntent.getBroadcast(
            context,
            EVENING_REQUEST_CODE,
            eveningIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        am.cancel(eveningPI)

        val staleIntent = Intent(context, StaleOutfitsReceiver::class.java)
        val stalePI = PendingIntent.getBroadcast(
            context,
            STALE_REQUEST_CODE,
            staleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        am.cancel(stalePI)
    }
}