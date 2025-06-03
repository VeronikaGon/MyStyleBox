package com.hfad.mystylebox.ui.activity

import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.hfad.mystylebox.R
import com.hfad.mystylebox.adapter.MonthsAdapter
import com.hfad.mystylebox.database.AppDatabase
import com.hfad.mystylebox.database.dao.DailyPlanDao
import com.hfad.mystylebox.ui.bottomsheet.DayDetailsBottomSheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.threeten.bp.LocalDate
import org.threeten.bp.YearMonth

class CalendarActivity : AppCompatActivity() {

    private lateinit var recycler: RecyclerView
    private lateinit var dao: DailyPlanDao
    private lateinit var btnBackCustom: ImageButton
    private lateinit var btnToday: ImageButton
    private lateinit var monthsList: List<YearMonth>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendar)

        btnBackCustom = findViewById(R.id.btnBackCustom)
        btnBackCustom.setOnClickListener {
            onBackPressed()
        }

        btnToday = findViewById(R.id.btnToday)
        btnToday.setOnClickListener {
            scrollToToday()
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

    private fun scrollToToday() {
        if (!::monthsList.isInitialized || monthsList.isEmpty()) return

        val todayIndex = monthsList.indexOf(YearMonth.now()).coerceAtLeast(0)
        recycler.scrollToPosition(todayIndex)
    }

    private fun loadCalendar() {
        lifecycleScope.launch(Dispatchers.IO) {
            val planImages = dao.getAllPlanImages()
            val dateImageMap = planImages
                .groupBy { it.date }
                .mapValues { entry -> entry.value.map { it.path } }

            val months = generateMonths()
            monthsList = months
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

    private fun generateMonths(): List<YearMonth> {
        val db = AppDatabase.getInstance(this)
        val plans = db.dailyPlanDao().getAllDailyPlans()

        if (plans.isEmpty()) {
            val result = mutableListOf<YearMonth>()
            var ym = YearMonth.now()
            for (i in 0..3) {
                result.add(ym.plusMonths(i.toLong()))
            }
            return result
        }

        val existingYears = plans
            .map { LocalDate.parse(it.planDate).year }
            .distinct()
            .sorted()

        val result = mutableListOf<YearMonth>()
        for (year in existingYears) {
            for (month in 1..12) {
                result.add(YearMonth.of(year, month))
            }
        }

        val maxExisting = result.maxOrNull() ?: YearMonth.now()
        var next = maxExisting.plusMonths(1)
        for (i in 0 until 3) {
            result.add(next)
            next = next.plusMonths(1)
        }
        return result
    }
}