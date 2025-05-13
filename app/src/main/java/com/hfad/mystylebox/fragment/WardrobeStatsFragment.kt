package com.hfad.mystylebox.fragment

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.compose.material3.DrawerValue
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.hfad.mystylebox.R
import com.hfad.mystylebox.database.AppDatabase
import com.hfad.mystylebox.database.entity.CategoryCount
import com.hfad.mystylebox.database.entity.ClothingItem
import com.hfad.mystylebox.database.entity.ClothingItemFull
import com.hfad.mystylebox.database.entity.SeasonCount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StatsRepository(context: Context) {
    private val dao = AppDatabase.getInstance(context).clothingItemDao()

    fun allItemsFull(): Flow<List<ClothingItemFull>> =
        dao.getAllItemsFullFlow()
}

class WardrobeStatsFragment : Fragment(R.layout.fragment_wardrobe_stats) {

    private lateinit var rbByCategory: RadioButton
    private lateinit var rbBySeason:   RadioButton
    private lateinit var rbByStatus:   RadioButton
    private lateinit var rbByTegi:     RadioButton
    private lateinit var rbBySize:     RadioButton
    private var selectedFilterId: Int = R.id.rbByCategory

    private lateinit var rbThings: RadioButton
    private lateinit var rbOutfits: RadioButton
    private lateinit var pieChart: PieChart
    private lateinit var llScales: LinearLayout

    private val repo: StatsRepository by lazy { StatsRepository(requireContext()) }
    private var onlyThings = true
    private var lastItems: List<ClothingItemFull> = emptyList()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rbThings    = view.findViewById(R.id.rbThings)
        rbOutfits   = view.findViewById(R.id.rbOutfits)
        pieChart    = view.findViewById(R.id.pieChart)
        llScales    = view.findViewById(R.id.llScales)

        rbByCategory = view.findViewById(R.id.rbByCategory)
        rbBySeason   = view.findViewById(R.id.rbBySeason)
        rbByStatus   = view.findViewById(R.id.rbByStatus)
        rbByTegi     = view.findViewById(R.id.rbByTegi)
        rbBySize     = view.findViewById(R.id.rbBySize)

        val filterButtons = listOf(rbByCategory, rbBySeason, rbByStatus, rbByTegi, rbBySize)
        filterButtons.forEach { rb ->
            rb.setOnClickListener {
                filterButtons.forEach { it.isChecked = false }
                rb.isChecked = true
                selectedFilterId = rb.id
                renderCurrent()
            }
        }


        rbThings.setOnClickListener {
            onlyThings = true
            rbThings.isChecked = true
            rbOutfits.isChecked = false
            renderCurrent()
        }
        rbOutfits.setOnClickListener {
            onlyThings = false
            rbThings.isChecked = false
            rbOutfits.isChecked = true
            renderCurrent()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repo.allItemsFull().collect {
                lastItems = it
                renderCurrent()
            }
        }
    }
    private fun renderCurrent() {
        val filtered = lastItems.filter { true }
        val total = filtered.size

        // 2) Составляем пары (ключ, count)
        val pairs: List<Pair<String, Int>> = when (selectedFilterId) {
            R.id.rbByCategory ->
                filtered.groupingBy { it.categoryName }
                    .eachCount()
                    .toList()

            R.id.rbBySeason -> {
                val raw = filtered.flatMap { item ->
                    val seasons = item.clothingItem.seasons
                    if (seasons.isNullOrEmpty()) {
                        listOf("Без сезона")
                    } else {
                        seasons
                    }
                }
                raw.groupingBy { it }
                    .eachCount()
                    .toList()
            }

            R.id.rbByStatus ->
                filtered.groupingBy { it.clothingItem.status.ifBlank { "Без статуса" } }
                    .eachCount()
                    .toList()

            R.id.rbBySize ->
                filtered.groupingBy { it.clothingItem.size.ifBlank { "Без размера" } }
                    .eachCount()
                    .toList()

            R.id.rbByTegi ->
                filtered
                    .flatMap { item ->
                        val names = item.tags.map { it.name }
                        if (names.isEmpty()) listOf("Без тега") else names
                    }
                    .groupingBy { it }
                    .eachCount()
                    .toList()

            else -> emptyList()
        }

        if (selectedFilterId == R.id.rbBySeason) {
            updateUiBySeason(total, pairs.map { SeasonCount(it.first, it.second) })
        } else {
            val listCounts = pairs.map { CategoryCount(it.first, it.second) }
            val center = when (selectedFilterId) {
                R.id.rbByCategory -> "$total\nвещей"
                else               -> "$total\nвещей"
            }
            updateUiByCategoryLike(total, listCounts, center)
        }
    }

    private fun updateUiBySeason(total: Int, bySeason: List<SeasonCount>) {
        val mutable = bySeason.toMutableList()
        val without = total - bySeason.sumOf { it.count }
        if (without > 0) mutable.add(SeasonCount("Без сезона", without))

        val order = listOf("Зима","Весна","Лето","Осень","Без сезона")
        val ordered = order.mapNotNull { name -> mutable.find { it.season == name } }

        // цветовая карта
        val colorMap = mapOf(
            "Зима" to R.color.piewinter,
            "Весна" to R.color.piespring,
            "Лето" to R.color.piesummer,
            "Осень" to R.color.pieautumn,
            "Без сезона" to R.color.pienotseasons
        )
        val colors = ordered.map { ContextCompat.getColor(requireContext(), colorMap[it.season]!!) }

        val entries = ordered.map { PieEntry(it.count.toFloat(), it.season) }
        val set = PieDataSet(entries,"").apply {
            setDrawValues(false)
            this.colors = colors
        }

        pieChart.apply {
            data = PieData(set)
            setDrawEntryLabels(false)
            isDrawHoleEnabled = true
            holeRadius = 60f
            setHoleColor(Color.WHITE)
            setCenterText("$total\nвещей")
            setCenterTextSize(16f)
            description.isEnabled = false
            legend.isEnabled = false
            invalidate()
        }

        llScales.removeAllViews()
        ordered.forEachIndexed { idx, sc ->
            val percent = if (total > 0) sc.count * 100 / total else 0
            val row = layoutInflater.inflate(R.layout.item_scale, llScales, false)
            row.findViewById<TextView>(R.id.tvPercent).text = "$percent%"
            row.findViewById<TextView>(R.id.tvName).text = sc.season
            row.findViewById<TextView>(R.id.tvCount).text = sc.count.toString()

            val pb = row.findViewById<ProgressBar>(R.id.progressBar)
            pb.max = 100; pb.progress = percent
            pb.progressTintList = ColorStateList.valueOf(colors[idx])

            llScales.addView(row)
        }
    }

    private fun updateUiByCategoryLike(
        total: Int,
        items: List<CategoryCount>,
        centerText: String
    ) {
        // Цвета для сегментов (переиспользуем палитру)
        val colors = listOf(
            R.color.pie1, R.color.pie2, R.color.pie3, R.color.pie4,
            R.color.pie5, R.color.pie6, R.color.pie7, R.color.pie8,
            R.color.pie9, R.color.pie10, R.color.pie11, R.color.pie12
        ).map { ContextCompat.getColor(requireContext(), it) }

        // Данные для PieChart
        val entries = items.map { PieEntry(it.count.toFloat(), it.categoryName) }
        val set = PieDataSet(entries, "").apply {
            setDrawValues(false)
            this.colors = colors
        }

        pieChart.apply {
            data = PieData(set)
            setDrawEntryLabels(false)
            isDrawHoleEnabled = true
            holeRadius = 60f
            setHoleColor(Color.WHITE)
            setCenterText(centerText)
            setCenterTextSize(16f)
            description.isEnabled = false
            legend.isEnabled = false
            invalidate()
        }

        // Генерация шкал под диаграммой
        llScales.removeAllViews()
        items.forEachIndexed { idx, cat ->
            val percent = if (total > 0) cat.count * 100 / total else 0
            val row = layoutInflater.inflate(R.layout.item_scale, llScales, false)
            row.findViewById<TextView>(R.id.tvPercent).text = "$percent%"
            row.findViewById<TextView>(R.id.tvName).text    = cat.categoryName
            row.findViewById<TextView>(R.id.tvCount).text   = cat.count.toString()

            val pb = row.findViewById<ProgressBar>(R.id.progressBar)
            pb.max = 100
            pb.progress = percent
            pb.progressTintList = ColorStateList.valueOf(colors[idx % colors.size])

            llScales.addView(row)
        }
    }

}