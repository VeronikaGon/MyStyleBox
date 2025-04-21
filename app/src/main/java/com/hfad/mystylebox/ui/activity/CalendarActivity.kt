package com.hfad.mystylebox.ui.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.hfad.mystylebox.R
import com.hfad.mystylebox.adapter.MonthsAdapter
import com.hfad.mystylebox.database.AppDatabase
import com.hfad.mystylebox.database.DailyPlanDao
import com.hfad.mystylebox.ui.bottomsheet.DayDetailsBottomSheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.threeten.bp.LocalDate
import org.threeten.bp.YearMonth

class CalendarActivity : AppCompatActivity() {

    private lateinit var recycler: RecyclerView
    private lateinit var dao: DailyPlanDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendar)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            title = "Календарь комплектов"
            setDisplayHomeAsUpEnabled(true)
        }

        recycler = findViewById(R.id.rvCalendarMonths)
        dao = AppDatabase.getInstance(this).dailyPlanDao()

        loadCalendar()

        supportFragmentManager.setFragmentResultListener(
            DayDetailsBottomSheet.RESULT_KEY,
            this
        ) { _, _ ->
            refreshCalendar()
        }
    }
    private fun loadCalendar() {
        lifecycleScope.launch(Dispatchers.IO) {
            val planImages = dao.getAllPlanImages()
            val dateImageMap = planImages
                .groupBy { it.date }
                .mapValues { entry -> entry.value.map { it.path } }

            val months = generateMonths()
            val currentIndex = months.indexOf(YearMonth.now()).coerceAtLeast(0)

            withContext(Dispatchers.Main) {
                recycler.adapter = MonthsAdapter(
                    months,
                    LocalDate.now(),
                    dateImageMap
                ) { date ->
                    DayDetailsBottomSheet.newInstance(date)
                        .show(supportFragmentManager, "day_details")
                }
                recycler.scrollToPosition(currentIndex)
            }
        }
    }
    private fun refreshCalendar() {
        loadCalendar()
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