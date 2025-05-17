package com.hfad.mystylebox.fragment

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.hfad.mystylebox.R
import com.hfad.mystylebox.adapter.OutfitAdapter
import com.hfad.mystylebox.database.AppDatabase
import kotlinx.coroutines.launch
import com.github.mikephil.charting.data.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class PlanningStatsFragment: Fragment(R.layout.fragment_plan_stats) {
    private lateinit var mostAdapter: OutfitAdapter
    private lateinit var leastAdapter: OutfitAdapter

    private val db by lazy { AppDatabase.getInstance(requireContext()) }
    private val dailyPlanDao by lazy { db.dailyPlanDao() }
    private val outfitDao by lazy { db.outfitDao() }
    private val outfitClothingDao by lazy { db.outfitClothingItemDao() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1) TextView-ы
        val tvFavDay = view.findViewById<TextView>(R.id.tvfavoritedaytoplan)
        val tvTotal = view.findViewById<TextView>(R.id.tvtotaldaysplanned)
        val tvUsually = view.findViewById<TextView>(R.id.tvusuallyinsetofthings)
        val tvInRow = view.findViewById<TextView>(R.id.tvplanneddaysinrow)
        val tvFavHeader  = view.findViewById<TextView>(R.id.tvfavoritedaytoplanning)
        val tvFavValue   = view.findViewById<TextView>(R.id.tvfavoritedaytoplan)

        val rvMost  = view.findViewById<RecyclerView>(R.id.rvMostFrequent)
        val rvLeast = view.findViewById<RecyclerView>(R.id.rvLeastFrequent)
        rvMost.layoutManager  = LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
        rvLeast.layoutManager = LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
        mostAdapter  = OutfitAdapter(emptyList(), R.layout.item_outfit)
        leastAdapter = OutfitAdapter(emptyList(), R.layout.item_outfit)
        rvMost.adapter  = mostAdapter
        rvLeast.adapter = leastAdapter

        // Графики
        val barChart = view.findViewById<BarChart>(R.id.barChart)
        val pieChart = view.findViewById<PieChart>(R.id.pieChart)

        viewLifecycleOwner.lifecycleScope.launch {
            // 1) tvTotal: уникальные даты
            val allPlans      = dailyPlanDao.getAllDailyPlans()
            val distinctDates = allPlans.map { it.planDate }.distinct().size
            tvTotal.text     = distinctDates.toString()

            val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)

            val thisYearPlans = allPlans.filter { plan ->
                runCatching {
                    fmt.parse(plan.planDate)?.let { date ->
                        Calendar.getInstance().apply { time = date }
                            .get(Calendar.YEAR) == currentYear
                    }
                }.getOrNull() == true
            }

            val monthCountMap = mutableMapOf<Int, Int>()
            thisYearPlans.forEach { plan ->
                fmt.parse(plan.planDate)?.let { date ->
                    val month = Calendar.getInstance().apply { time = date }
                        .get(Calendar.MONTH) + 1  // январь = 0
                    monthCountMap[month] = (monthCountMap[month] ?: 0) + 1
                }
            }

            val months = (1..12).map { monthCountMap[it] ?: 0 }

            setupBarChart(barChart, months)

            val weekCounts = dailyPlanDao.getCountByWeekday()
            val maxCnt     = weekCounts.maxOfOrNull { it.cnt } ?: 0
            val favDays    = weekCounts.filter { it.cnt == maxCnt }
                .map { it.weekday.toWeekdayName() }
            tvFavValue.text = favDays.joinToString(",\n")

            val usageAll   = dailyPlanDao.getMostFrequent(Int.MAX_VALUE)
            val maxUse     = usageAll.maxOfOrNull { it.cnt } ?: 0
            val minUse     = usageAll.minOfOrNull { it.cnt } ?: 0

            val mostIds    = usageAll.filter { it.cnt == maxUse }.map { it.outfitId }
            val leastIds   = usageAll.filter { it.cnt == minUse }.map { it.outfitId }

            val mostList   = outfitDao.getOutfitsByIds(mostIds)
            val leastList  = outfitDao.getOutfitsByIds(leastIds)

            view.findViewById<TextView>(R.id.tvfavoritedaytoplanning).apply {
                text = if (favDays.size > 1)
                    "Самые часто планируемые дни:"
                else
                    "Самый часто планируемый день:"
            }
            tvFavHeader.text = "Самые часто планируемые дни"
            view.findViewById<TextView>(R.id.tvMostLabel).apply {
                text = if (mostList.size > 1)
                    "Самые часто надеваемые комплекты:"
                else
                    "Самый часто надеваемый комплект:"
            }
            view.findViewById<TextView>(R.id.tvLeastLabel).apply {
                text = if (leastList.size > 1)
                    "Самые редко используемые комплекты:"
                else
                    "Самый редко используемый комплект:"
            }

            mostAdapter.updateData(mostList)
            leastAdapter.updateData(leastList)

            val rawMonths = dailyPlanDao.getCountByMonth()
            val monthMap  = rawMonths.associate { it.month.toInt() to it.cnt }
            setupBarChart(barChart, months)

            val totalItems = allPlans.sumOfPlanItems()
            val avgItems   = if (allPlans.isEmpty()) 0 else totalItems / allPlans.size
            tvUsually.text = avgItems.toString()

            val dates = allPlans.mapNotNull {
                runCatching { fmt.parse(it.planDate) }.getOrNull()
            }.distinct().sorted()
            var maxStreak = 1
            var currStreak = 1
            for (i in 1 until dates.size) {
                val prev = dates[i - 1]
                val cur  = dates[i]
                val cal = Calendar.getInstance().apply { time = prev }
                cal.add(Calendar.DAY_OF_YEAR, 1)
                if (cal.time == cur) {
                    currStreak++
                    if (currStreak > maxStreak) maxStreak = currStreak
                } else {
                    currStreak = 1
                }
            }
            tvInRow.text = maxStreak.toString()

            // 6) PieChart
            val totalOutfits = outfitDao.getAllOutfits().size
            val usedCount    = mostIds.union(leastIds).size
            val usedPercent  = if (totalOutfits == 0) 0 else usedCount * 100 / totalOutfits
            setupPieChart(pieChart, usedPercent)

            for (plan in thisYearPlans) {
                fmt.parse(plan.planDate)?.let { date ->
                    val cal = Calendar.getInstance().apply { time = date }
                    val month = cal.get(Calendar.MONTH) + 1
                    monthCountMap[month] = (monthCountMap[month] ?: 0) + 1
                }
            }
            setupBarChart(barChart, months)
        }
    }

    private fun List<com.hfad.mystylebox.database.entity.DailyPlan>.sumOfPlanItems(): Int {
        var sum = 0
        for (plan in this) {
            sum += db.outfitDao().getClothingItemsForOutfit(plan.outfitId).size
        }
        return sum
    }

    private fun String.toWeekdayName(): String = when (this) {
        "0" -> "воскресенье"; "1" -> "понедельник"; "2" -> "вторник"
        "3" -> "среда"; "4" -> "четверг"; "5" -> "пятница"
        "6" -> "суббота"; else -> ""
    }

    private fun setupBarChart(barChart: BarChart, values: List<Int>) {
        val entries = values.mapIndexed { i, v -> BarEntry(i.toFloat(), v.toFloat()) }
        val pink = ContextCompat.getColor(requireContext(), R.color.pie3)
        val gray = ContextCompat.getColor(requireContext(), android.R.color.darker_gray)

        val colors = values.map { v -> if (v == 0) gray else pink }
        val ds = BarDataSet(entries, "").apply {
            setColors(colors)
            setDrawValues(true)
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String =
                    value.toInt().toString()
            }
        }

        barChart.apply {
            data = BarData(ds).apply { barWidth = 0.2f }
            description.isEnabled = false
            legend.isEnabled      = false
            axisRight.isEnabled    = false
            setFitBars(true)
            xAxis.axisMinimum = -0.2f
            xAxis.axisMaximum = values.size - 0.2f
            xAxis.apply {
                position       = XAxis.XAxisPosition.BOTTOM
                granularity    = 1f
                labelCount     = 10
                setDrawGridLines(false)
                setCenterAxisLabels(false)
                valueFormatter = IndexAxisValueFormatter(
                    listOf("янв","фев","мар","апр","май","июн","июл","авг","сен","окт","ноя","дек")
                )
            }
            axisLeft.setDrawGridLines(false)
            invalidate()
        }
    }

    private fun setupPieChart(pieChart: PieChart, percent: Int) {
        val entries   = listOf(PieEntry(percent.toFloat()), PieEntry((100 - percent).toFloat()))
        val usedColor = ContextCompat.getColor(requireContext(), R.color.pie3)
        val grayColor = ContextCompat.getColor(requireContext(), android.R.color.darker_gray)

        val ds = PieDataSet(entries, "").apply {
            setColors(listOf(usedColor, grayColor))
            setDrawValues(false)
        }

        pieChart.apply {
            data                  = PieData(ds)
            isDrawHoleEnabled     = true
            holeRadius            = 80f
            setUsePercentValues(true)
            setEntryLabelTextSize(16f)
            setEntryLabelColor(Color.BLACK)
            legend.isEnabled      = false
            description.isEnabled = false
            centerText = "$percent%"
            setCenterTextColor(Color.BLACK)
            setCenterTextSize(16f)
            invalidate()
        }
    }
}