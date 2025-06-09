package com.hfad.mystylebox.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.threeten.bp.LocalDate

class StaleOutfitsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val oneMonthAgo = LocalDate.now().minusMonths(1).toString()

        val db = com.hfad.mystylebox.database.AppDatabase.getInstance(context)
        CoroutineScope(Dispatchers.IO).launch {
            val staleOutfits = db.outfitDao().getStaleOutfitsSince(oneMonthAgo)
            if (staleOutfits.isNotEmpty()) {
                val outfit = staleOutfits.first()
                NotificationHelper.createNotificationChannel(context)
                val title = "Напоминание о давно не используемом комплекте"
                val message = "Вы не надевали комплект «${outfit.name}» более месяца. Не пора ли снова его использовать?"
                val notification = NotificationHelper.buildNotification(context, title, message, 1003)
                NotificationHelper.showNotification(context, 1003, notification)
            }
        }
    }
}