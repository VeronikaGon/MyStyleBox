package com.hfad.mystylebox.notifications
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.hfad.mystylebox.R
import android.Manifest

object NotificationHelper {

    const val CHANNEL_ID = "mystylebox_channel"
    const val CHANNEL_NAME = "MyStyleBox Уведомления"
    const val CHANNEL_DESC = "Канал уведомлений для напоминаний MyStyleBox"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = CHANNEL_DESC
                enableLights(true)
                lightColor = Color.MAGENTA
                enableVibration(true)
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    fun buildNotification(
        context: Context,
        title: String,
        message: String,
        notificationId: Int
    ): Notification {
        val bigTextStyle = NotificationCompat.BigTextStyle()
            .bigText(message)
            .setBigContentTitle(title)
            .setSummaryText("MyStyleBox")

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.logo)
            .setContentTitle(title)
            .setContentText(
                if (message.length > 40) {
                    message.take(40) + "…"
                } else {
                    message
                }
            )
            .setStyle(bigTextStyle)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setColor(context.getColor(R.color.pink))
            .build()
    }

    fun showNotification(context: Context, notificationId: Int, notification: Notification) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPostNotif = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPostNotif) {
                return
            }
        }
        nm.notify(notificationId, notification)
    }
}