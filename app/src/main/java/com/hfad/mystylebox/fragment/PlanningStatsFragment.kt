package com.hfad.mystylebox.fragment

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
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
import com.hfad.mystylebox.database.entity.Outfit
import kotlinx.coroutines.launch

class PlanningStatsFragment: Fragment(R.layout.fragment_plan_stats) {
    private lateinit var mostAdapter: OutfitAdapter
    private lateinit var leastAdapter: OutfitAdapter

    private val db by lazy { AppDatabase.getInstance(requireContext()) }
    private val dailyPlanDao by lazy { db.dailyPlanDao() }
    private val outfitDao by lazy { db.outfitDao() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1) TextView-ы
        val tvFavDay = view.findViewById<TextView>(R.id.tvfavoritedaytoplan)
        val tvTotal = view.findViewById<TextView>(R.id.tvtotaldaysplanned)
        val tvUsually = view.findViewById<TextView>(R.id.tvusuallyinsetofthings)
        val tvInRow = view.findViewById<TextView>(R.id.tvplanneddaysinrow)

        // 2) RecyclerView-ы
        val rvMost = view.findViewById<RecyclerView>(R.id.rvMostFrequent)
        val rvLeast = view.findViewById<RecyclerView>(R.id.rvLeastFrequent)
        rvMost.layoutManager = LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
        rvLeast.layoutManager =
            LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
        mostAdapter = OutfitAdapter(emptyList(), R.layout.item_outfit)
        leastAdapter = OutfitAdapter(emptyList(), R.layout.item_outfit)
        rvMost.adapter = mostAdapter
        rvLeast.adapter = leastAdapter

        // 3) Графики
        val barChart = view.findViewById<BarChart>(R.id.barChart)
        val pieChart = view.findViewById<PieChart>(R.id.pieChart)

        // 4) Асинхронно читаем из БД
        viewLifecycleOwner.lifecycleScope.launch {
            // 4.1) Сколько уникальных дат запланировано
            val allPlans = dailyPlanDao.getAllDailyPlans()
            val distinctDates = allPlans.map { it.planDate }.distinct().size
            tvTotal.text = distinctDates.toString()

            // 4.2) По дням недели и любимый день
            val weekCounts = dailyPlanDao.getCountByWeekday()
            val maxCnt = weekCounts.maxOfOrNull { it.cnt } ?: 0
            val favDays = weekCounts.filter { it.cnt == maxCnt }
                .map { it.weekday.toWeekdayName() }
            tvFavDay.text = if (favDays.size > 1)
                "Самые часто планируемые дни: ${favDays.joinToString(", ")}"
            else
                "Самый часто планируемый день: ${favDays.firstOrNull() ?: "-"}"

            // 4.3) Самые частые и самые редкие комплекты
            val usageAll = dailyPlanDao.getMostFrequent(Int.MAX_VALUE)
            val maxUse = usageAll.maxOfOrNull { it.cnt } ?: 0
            val minUse = usageAll.minOfOrNull { it.cnt } ?: 0

            val mostIds = usageAll.filter { it.cnt == maxUse }.map { it.outfitId }
            val leastIds = usageAll.filter { it.cnt == minUse }.map { it.outfitId }

            val mostList = outfitDao.getOutfitsByIds(mostIds)
            val leastList = outfitDao.getOutfitsByIds(leastIds)

            // меняем заголовки при нескольких элементов
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

            // 4.4) По месяцам
            val rawMonths = dailyPlanDao.getCountByMonth()
            val monthMap = rawMonths.associate { it.month.toInt() to it.cnt }
            val months = (1..12).map { monthMap[it] ?: 0 }
            setupBarChart(barChart, months)

            // 4.5) Процент использования гардероба
            val totalOutfits = outfitDao.getAllOutfits().size
            val usedPercent = if (totalOutfits == 0) 0
            else mostIds.union(leastIds).size * 100 / totalOutfits
            // в PieChart мы не хотим шкалы — сделаем прозрачные подписи
            setupPieChart(pieChart, usedPercent)
        }
    }

    private fun String.toWeekdayName(): String = when (this) {
        "0" -> "воскресенье"; "1" -> "понедельник"; "2" -> "вторник"
        "3" -> "среда"; "4" -> "четверг"; "5" -> "пятница"
        "6" -> "суббота"; else -> ""
    }

    private fun setupBarChart(barChart: BarChart, values: List<Int>) {
        val entries = values.mapIndexed { i, v -> BarEntry(i.toFloat(), v.toFloat()) }
        val color = ContextCompat.getColor(requireContext(), R.color.pie3)
        val ds = BarDataSet(entries, "").apply {
            setColor(color)
            setDrawValues(true)
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String =
                    value.toInt().toString()
            }
        }

        // Показываем все 12 меток и отключаем лишнюю сетку
        barChart.apply {
            data = BarData(ds).apply { barWidth = 0.6f }
            description.isEnabled = false
            axisRight.isEnabled = false
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                labelCount = 12
                setDrawGridLines(false)
                valueFormatter = IndexAxisValueFormatter(
                    listOf(
                        "янв",
                        "фев",
                        "мар",
                        "апр",
                        "май",
                        "июн",
                        "июл",
                        "авг",
                        "сен",
                        "окт",
                        "ноя",
                        "дек"
                    )
                )
            }
            axisLeft.setDrawGridLines(false)
            invalidate()
        }
    }

    private fun setupPieChart(pieChart: PieChart, percent: Int) {
        val entries = listOf(PieEntry(percent.toFloat()), PieEntry((100 - percent).toFloat()))
        val usedColor = ContextCompat.getColor(requireContext(), R.color.pie3)
        val otherColor = Color.TRANSPARENT // второй сектор невидим

        val ds = PieDataSet(entries, "").apply {
            setColors(listOf(usedColor, otherColor))
            setDrawValues(false)                // убираем подписи значений
        }

        pieChart.apply {
            data = PieData(ds)
            isDrawHoleEnabled = true
            holeRadius = 80f
            setUsePercentValues(true)
            setEntryLabelTextSize(16f)          // текст процентов
            setEntryLabelColor(Color.BLACK)
            legend.isEnabled = false
            description.isEnabled = false
            invalidate()
        }
    }
}