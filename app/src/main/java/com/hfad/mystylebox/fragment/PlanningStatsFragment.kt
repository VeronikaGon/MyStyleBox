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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class PlanningStatsFragment : Fragment(R.layout.fragment_plan_stats), SecondFragment.StatsUpdatable {
    private lateinit var mostAdapter: OutfitAdapter
    private lateinit var leastAdapter: OutfitAdapter
    private lateinit var scrollContent: View
    private lateinit var tvEmptyPlanning: TextView

    private val db by lazy { AppDatabase.getInstance(requireContext()) }
    private val dailyPlanDao by lazy { db.dailyPlanDao() }
    private val outfitDao by lazy { db.outfitDao() }
    private val outfitClothingDao by lazy { db.outfitClothingItemDao() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        scrollContent     = view.findViewById(R.id.scrollContent)
        tvEmptyPlanning   = view.findViewById(R.id.tvEmptyPlan)
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
        mostAdapter  = OutfitAdapter(emptyList(), R.layout.item_grayborder)
        leastAdapter = OutfitAdapter(emptyList(), R.layout.item_grayborder)
        rvMost.adapter  = mostAdapter
        rvLeast.adapter = leastAdapter
        updateStats()

        val barChart = view.findViewById<BarChart>(R.id.barChart)
        val pieChart = view.findViewById<PieChart>(R.id.pieChart)

        viewLifecycleOwner.lifecycleScope.launch {
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
            setDrawValues(true)
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float) = value.toInt().toString()
            }
            setColors(colors)
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
    override fun updateStats() {
        val root = view ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            val allPlans = withContext(Dispatchers.IO) { dailyPlanDao.getAllDailyPlans() }

            if (allPlans.isEmpty()) {
                scrollContent.visibility   = View.GONE
                tvEmptyPlanning.visibility = View.VISIBLE
                return@launch
            }

            scrollContent.visibility   = View.VISIBLE
            tvEmptyPlanning.visibility = View.GONE

            val tvTotal   = root.findViewById<TextView>(R.id.tvtotaldaysplanned)
            val tvUsually = root.findViewById<TextView>(R.id.tvusuallyinsetofthings)
            val tvInRow   = root.findViewById<TextView>(R.id.tvplanneddaysinrow)
            val barChart  = root.findViewById<BarChart>(R.id.barChart)
            val pieChart  = root.findViewById<PieChart>(R.id.pieChart)
            val tvMostLabel  = root.findViewById<TextView>(R.id.tvMostLabel)
            val tvLeastLabel = root.findViewById<TextView>(R.id.tvLeastLabel)
            val tvFavHeader  = root.findViewById<TextView>(R.id.tvfavoritedaytoplanning)
            val tvFavValue   = root.findViewById<TextView>(R.id.tvfavoritedaytoplan)

            val distinctDates = allPlans.map { it.planDate }.distinct().size
            tvTotal.text      = distinctDates.toString()

            val totalItems = allPlans.sumOf { plan ->
                outfitDao.getClothingItemsForOutfit(plan.outfitId).size
            }
            tvUsually.text = if (allPlans.isEmpty()) "0" else (totalItems / allPlans.size).toString()

            val maxStreak = computeMaxStreak(allPlans)
            tvInRow.text  = maxStreak.toString()
            val monthlyCounts = computeMonthlyPlanCounts(allPlans)
            setupBarChart(barChart, monthlyCounts)
            val weekCounts = dailyPlanDao.getCountByWeekday()
            val maxCnt     = weekCounts.maxOfOrNull { it.cnt } ?: 0
            val favDays    = weekCounts.filter { it.cnt == maxCnt }
                .map { it.weekday.toWeekdayName() }
            tvFavValue.text = favDays.joinToString(",\n")
            val usageAll = dailyPlanDao.getMostFrequent(Int.MAX_VALUE)
            val maxUse   = usageAll.maxOfOrNull { it.cnt } ?: 0
            val minUse   = usageAll.minOfOrNull { it.cnt } ?: 0
            val mostIds  = usageAll.filter { it.cnt == maxUse }.map { it.outfitId }
            val leastIds = usageAll.filter { it.cnt == minUse }.map { it.outfitId }
            val mostList  = outfitDao.getOutfitsByIds(mostIds)
            val leastList = outfitDao.getOutfitsByIds(leastIds)

            tvMostLabel.text  = if (mostList.size > 1)
                "Самые часто надеваемые комплекты:"
            else
                "Самый часто надеваемый комплект:"
            tvLeastLabel.text = if (leastList.size > 1)
                "Самые редко используемые комплекты:"
            else
                "Самый редко используемый комплект:"
            mostAdapter.updateData(mostList)
            leastAdapter.updateData(leastList)
            val usedPercent = computeUsedPercent(allPlans)
            setupPieChart(pieChart, usedPercent)
        }
    }

    private fun computeMaxStreak(plans: List<com.hfad.mystylebox.database.entity.DailyPlan>): Int {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dates = plans.mapNotNull { runCatching { fmt.parse(it.planDate) }.getOrNull() }
            .distinct().sorted()
        var maxStreak = 1; var curr = 1
        for (i in 1 until dates.size) {
            val prev = dates[i - 1]; val cur = dates[i]
            Calendar.getInstance().apply { time = prev }.apply { add(Calendar.DAY_OF_YEAR, 1) }
                .takeIf { it.time == cur }?.let { curr++ } ?: run { curr = 1 }
            if (curr > maxStreak) maxStreak = curr
        }
        return maxStreak
    }

    private fun computeMonthlyPlanCounts(plans: List<com.hfad.mystylebox.database.entity.DailyPlan>): List<Int> {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val monthMap = mutableMapOf<Int, MutableSet<Int>>()
        plans.forEach { plan ->
            fmt.parse(plan.planDate)?.let { date ->
                val m = Calendar.getInstance().apply { time = date }.get(Calendar.MONTH)
                monthMap.getOrPut(m) { mutableSetOf() }.add(plan.outfitId.toInt())
            }
        }
        return (0..11).map { monthMap[it]?.size ?: 0 }
    }

    private fun computeUsedPercent(plans: List<com.hfad.mystylebox.database.entity.DailyPlan>): Int {
        val totalOut = outfitDao.getAllOutfits().size
        val usedCount = plans.map { it.outfitId }.toSet().size
        return if (totalOut == 0) 0 else usedCount * 100 / totalOut
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