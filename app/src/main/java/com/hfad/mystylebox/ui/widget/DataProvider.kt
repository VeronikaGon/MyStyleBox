package com.hfad.mystylebox.ui.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.hfad.mystylebox.database.AppDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DataProvider {

    data class WidgetOutfit(
        val uriItem1: Uri,
        val uriItem2: Uri,
        val uriItem3: Uri,
        val uriItem4: Uri,
        val caption: String
    )
    fun notifyWidgetDataChanged(context: Context) {
        val mgr = AppWidgetManager.getInstance(context)
        val ids = mgr.getAppWidgetIds(ComponentName(context, MyWidget::class.java))

        val prefs = context.getSharedPreferences("com.hfad.mystylebox.PREFS", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        ids.forEach { id ->
            editor.remove("current_index_$id")
        }
        editor.apply()

        val updateIntent = Intent(context, MyWidget::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        }
        context.sendBroadcast(updateIntent)
    }

    fun getPlannedOutfits(context: Context): List<WidgetOutfit> {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            .format(Date())

        val db = AppDatabase.getInstance(context)
        val outfits = db.dailyPlanDao().getOutfitsByDate(todayStr)

        return outfits.map { outfit ->
            val uri = Uri.parse("file://${outfit.imagePath}")

            WidgetOutfit(
                uriItem1 = uri,
                uriItem2 = uri,
                uriItem3 = uri,
                uriItem4 = uri,
                caption  = outfit.name
            )
        }
    }
}