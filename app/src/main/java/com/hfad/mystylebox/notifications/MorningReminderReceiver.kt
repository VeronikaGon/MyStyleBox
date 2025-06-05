package com.hfad.mystylebox.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.threeten.bp.LocalDate

class MorningReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        Log.d("MorningReminder", "onReceive запущен!")
        val appDatabase = com.hfad.mystylebox.database.AppDatabase.getInstance(context)
        CoroutineScope(Dispatchers.IO).launch {
            val today = LocalDate.now().toString()
            val plansForToday = appDatabase.dailyPlanDao().getDailyPlansForDate(today)
            if (plansForToday.isEmpty()) {
                NotificationHelper.createNotificationChannel(context)
                val title = "Напоминание: запланируйте комплекты"
                val message = "Доброе утро! Вы ещё не запланировали комплекты на сегодня. Начните свой день с отличного настроения!"
                val notification = NotificationHelper.buildNotification(context, title, message, 1001)
                NotificationHelper.showNotification(context, 1001, notification)
            }
        }
    }
}