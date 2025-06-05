package com.hfad.mystylebox.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.threeten.bp.LocalDate

class EveningReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val appDatabase = com.hfad.mystylebox.database.AppDatabase.getInstance(context)
        CoroutineScope(Dispatchers.IO).launch {
            val tomorrow = LocalDate.now().plusDays(1).toString()
            val plansForTomorrow = appDatabase.dailyPlanDao().getDailyPlansForDate(tomorrow)
            if (plansForTomorrow.isEmpty()) {
                NotificationHelper.createNotificationChannel(context)
                val title = "Вечернее напоминание"
                val message = "Не забудьте запланировать комплекты на завтра — пусть завтрашний день станет ещё лучше!"
                val notification = NotificationHelper.buildNotification(context, title, message, 1002)
                NotificationHelper.showNotification(context, 1002, notification)
            }
        }
    }
}