package com.hfad.mystylebox.ui.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import com.hfad.mystylebox.MainActivity
import com.hfad.mystylebox.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MyWidget : AppWidgetProvider() {

    companion object {
        private const val TAG = "MyWidget"
        private const val ACTION_PREV = "com.hfad.mystylebox.ACTION_PREV"
        private const val ACTION_NEXT = "com.hfad.mystylebox.ACTION_NEXT"
        private const val ACTION_AUTO_UPDATE = "com.hfad.mystylebox.ACTION_AUTO_UPDATE"
        private const val PREFS_NAME = "com.hfad.mystylebox.PREFS"
        private const val KEY_INDEX = "current_index_"
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        val mgr = AppWidgetManager.getInstance(context)
        val ids = mgr.getAppWidgetIds(ComponentName(context, MyWidget::class.java))
        if (ids.isNotEmpty()) onUpdate(context, mgr, ids)
        scheduleNextUpdate(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        // Отменяем авто‑апдейт
        val mgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, MyWidget::class.java).apply { action = ACTION_AUTO_UPDATE },
            PendingIntent.FLAG_IMMUTABLE
        )
        mgr.cancel(pi)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Log.d(TAG, "onUpdate widgets: ${'$'}{appWidgetIds.contentToString()}")
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        for (id in appWidgetIds) {
            val idx = prefs.getInt("$KEY_INDEX$id", 0)
            updateWidget(context, appWidgetManager, id, idx)
        }
        scheduleNextUpdate(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val mgr = AppWidgetManager.getInstance(context)
        when (intent.action) {
            ACTION_PREV, ACTION_NEXT -> {
                val wid = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
                if (wid == -1) return
                val outfits = DataProvider.getPlannedOutfits(context)
                if (outfits.isEmpty()) {
                    updateWidget(context, mgr, wid, 0)
                    return
                }
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                var idx = prefs.getInt("$KEY_INDEX$wid", 0)
                idx = when (intent.action) {
                    ACTION_NEXT -> minOf(idx + 1, outfits.lastIndex)
                    ACTION_PREV -> maxOf(idx - 1, 0)
                    else -> idx
                }
                prefs.edit().putInt("$KEY_INDEX$wid", idx).apply()
                updateWidget(context, mgr, wid, idx)
            }

            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_BOOT_COMPLETED -> {
                val ids = mgr.getAppWidgetIds(ComponentName(context, MyWidget::class.java))
                if (ids.isNotEmpty()) onUpdate(context, mgr, ids)
            }

            ACTION_AUTO_UPDATE -> {
                val ids = mgr.getAppWidgetIds(ComponentName(context, MyWidget::class.java))
                if (ids.isNotEmpty()) onUpdate(context, mgr, ids)
            }
        }
    }

    private fun updateWidget(
        context: Context,
        mgr: AppWidgetManager,
        widgetId: Int,
        currentIndex: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_layout)
        val clickIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val clickPI = PendingIntent.getActivity(
            context,
            widgetId,
            clickIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.iv_main, clickPI)
        views.setOnClickPendingIntent(R.id.tv_caption, clickPI)

        val today = SimpleDateFormat("d MMMM yyyy 'г.'", Locale("ru")).format(Date())
        views.setTextViewText(R.id.tv_date, "Планирование на сегодня")
        views.setTextViewText(R.id.tv_date, "Сегодня, $today")

        val outfits = DataProvider.getPlannedOutfits(context)
        val cnt = outfits.size

        if (cnt == 0) {
            views.setViewVisibility(R.id.iv_main, View.VISIBLE)
            views.setImageViewResource(R.id.iv_main, R.drawable.ic_calendaradd)

            views.setViewVisibility(R.id.tv_count, View.GONE)
            views.setViewVisibility(R.id.tv_caption, View.GONE)
            views.setViewVisibility(R.id.btn_prev, View.GONE)
            views.setViewVisibility(R.id.btn_next, View.GONE)
            views.setViewVisibility(R.id.tv_empty, View.VISIBLE)

        } else {
            views.setViewVisibility(R.id.tv_empty, View.GONE)
            views.setViewVisibility(R.id.tv_count, View.VISIBLE)
            val idx = currentIndex.coerceIn(0, outfits.lastIndex)
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putInt("$KEY_INDEX$widgetId", idx)
                .apply()

            val verb = if (cnt == 1) "Запланирован" else "Запланировано"

            val noun = when {
                cnt % 10 == 1 && cnt % 100 != 11 -> "комплект"
                cnt in 2..4                       -> "комплекта"
                else                              -> "комплектов"
            }
            views.setTextViewText(R.id.tv_count, "$verb $cnt $noun")
            views.setViewVisibility(R.id.btn_prev, if (currentIndex > 0) View.VISIBLE else View.GONE)
            views.setViewVisibility(R.id.btn_next, if (currentIndex < cnt - 1) View.VISIBLE else View.GONE)
            listOf(
                ACTION_PREV to R.id.btn_prev,
                ACTION_NEXT to R.id.btn_next
            ).forEachIndexed { i, (action, viewId) ->
                val pi = PendingIntent.getBroadcast(
                    context,
                    widgetId * 10 + i,
                    Intent(context, MyWidget::class.java).apply {
                        this.action = action
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                    },
                    PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(viewId, pi)
            }

            val outfit = outfits[idx]
            views.setViewVisibility(R.id.iv_main, View.VISIBLE)
            outfit.uriItem1.path?.let { path ->
                decodeSampledBitmapFromFile(path, 200, 200)?.let { bmp ->
                    views.setImageViewBitmap(R.id.iv_main, bmp)
                }
            }

            views.setViewVisibility(R.id.tv_caption, View.VISIBLE)
            views.setTextViewText(R.id.tv_caption, outfit.caption)
        }

        mgr.updateAppWidget(widgetId, views)
    }

    private fun scheduleNextUpdate(context: Context) {
        val mgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val next = Calendar.getInstance().apply {
            add(Calendar.DATE, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 5)
        }.timeInMillis

        val pi = PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, MyWidget::class.java).apply { action = ACTION_AUTO_UPDATE },
            PendingIntent.FLAG_IMMUTABLE
        )
        mgr.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + 60_000,       // через минуту
            5 * 60_000,                                // каждые 5 минут
            pi
        )
    }

    // Вспомогательные методы для загрузки Bitmap...
    private fun decodeSampledBitmapFromFile(
        filePath: String,
        reqWidth: Int,
        reqHeight: Int
    ): Bitmap? {
        // Получаем размеры
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(filePath, options)
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
        options.inJustDecodeBounds = false
        return BitmapFactory.decodeFile(filePath, options)
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfH = height / 2
            val halfW = width / 2
            while (halfH / inSampleSize >= reqHeight && halfW / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}