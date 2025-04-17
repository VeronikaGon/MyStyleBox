package com.hfad.mystylebox

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hfad.mystylebox.adapter.MonthsAdapter
import com.hfad.mystylebox.database.AppDatabase
import org.threeten.bp.LocalDate
import org.threeten.bp.YearMonth
import org.threeten.bp.format.TextStyle
import java.util.*

class CalendarActivity : AppCompatActivity() {

    companion object {
        private val MONTH_NAMES = arrayOf(
            "Январь", "Февраль", "Март", "Апрель",
            "Май", "Июнь", "Июль", "Август",
            "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendar)

        // Находим Toolbar и назначаем его ActionBar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            title = "Календарь комплектов"
            setDisplayHomeAsUpEnabled(true)
        }

        val recycler = findViewById<RecyclerView>(R.id.rvCalendarMonths)
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = MonthsAdapter(generateMonths(), LocalDate.now())

    }

    /**
     * Генерируем список YearMonth для всех годов, в которых есть записи в daily_plan.
     * Для каждого года берём все 12 месяцев.
     */
    private fun generateMonths(): List<YearMonth> {
        val db = AppDatabase.getInstance(this)
        val plans = db.dailyPlanDao().getAllDailyPlans()
        if (plans.isEmpty()) return listOf(YearMonth.now())

        // Собираем уникальные годы
        val years = plans.map { LocalDate.parse(it.planDate).year }
            .distinct()
            .sorted()

        // Для каждого года генерируем 12 месяцев
        val result = mutableListOf<YearMonth>()
        for (year in years) {
            for (month in 1..12) {
                result.add(YearMonth.of(year, month))
            }
        }
        return result
    }
}