package com.hfad.mystylebox.fragment

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.RadioButton
import android.widget.TextView
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class CategoryCount(val categoryName: String, val count: Int)
data class FilterParams(val onlyThings: Boolean)

class StatsRepository(context: Context) {
    private val dao = AppDatabase.getInstance(context).clothingItemDao()

    fun totalItems(): Flow<Int> =
        dao.getTotalItemsCountFlow()

    fun byCategory(): Flow<List<CategoryCount>> =
        dao.getItemCountByCategoryFlow()
}


class WardrobeStatsFragment : Fragment(R.layout.fragment_wardrobe_stats) {

    private lateinit var rbThings: RadioButton
    private lateinit var rbOutfits: RadioButton
    private lateinit var pieChart: PieChart
    private lateinit var tvBottom: TextView
    private lateinit var tvBottom1: TextView
    private lateinit var tvBottom2: TextView

    private val repo: StatsRepository by lazy { StatsRepository(requireContext()) }

    private var onlyThings = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rbThings  = view.findViewById(R.id.rbThings)
        rbOutfits = view.findViewById(R.id.rbOutfits)
        pieChart  = view.findViewById(R.id.pieChart)
        tvBottom  = view.findViewById(R.id.tvBottom)
        tvBottom1  = view.findViewById(R.id.tvBottom1)
        tvBottom2  = view.findViewById(R.id.tvBottom2)

        rbThings.setOnClickListener {
            onlyThings = true
            rbThings.isChecked = true
            rbOutfits.isChecked = false
        }
        rbOutfits.setOnClickListener {
            onlyThings = false
            rbThings.isChecked = false
            rbOutfits.isChecked = true
        }

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            combine(
                repo.totalItems(),
                repo.byCategory()
            ) { total, dist -> total to dist }
                .collect { (total, dist) ->
                    updateUi(total, dist)
                }
        }
    }

    private fun updateUi(total: Int, byCategory: List<CategoryCount>) {
        val entries = byCategory.map { PieEntry(it.count.toFloat(), it.categoryName) }
        val dataSet = PieDataSet(entries, "").apply {
            colors = listOf(
                ContextCompat.getColor(requireContext(), R.color.pie1),
                ContextCompat.getColor(requireContext(), R.color.pie2),
                ContextCompat.getColor(requireContext(), R.color.pie3),
                ContextCompat.getColor(requireContext(), R.color.pie4),
                ContextCompat.getColor(requireContext(), R.color.pie5),
                ContextCompat.getColor(requireContext(), R.color.pie6),
                ContextCompat.getColor(requireContext(), R.color.pie7),
                ContextCompat.getColor(requireContext(), R.color.pie8),
                ContextCompat.getColor(requireContext(), R.color.pie9),
                ContextCompat.getColor(requireContext(), R.color.pie10),
                ContextCompat.getColor(requireContext(), R.color.pie11),
                ContextCompat.getColor(requireContext(), R.color.pie12)
            )
            sliceSpace = 2f
        }

        pieChart.apply {
            data = PieData(dataSet)
            isDrawHoleEnabled = true
            holeRadius = 60f
            setHoleColor(Color.WHITE)
            setCenterText("${total}\nвещей")
            setCenterTextSize(16f)
            description.isEnabled = false
            legend.isEnabled = false
            invalidate()
        }

        val percent = StringBuilder()
        val name = StringBuilder()
        val count = StringBuilder()
        byCategory.forEach {
            percent.append("${(it.count.toFloat()/total.toFloat()).toFloat()*100}%\n")
            name.append("${it.categoryName}\n")
            count.append("${it.count}\n")

        }
        tvBottom.text = percent.toString().trim()
        tvBottom1.text = name.toString().trim()
        tvBottom2.text = count.toString().trim()
    }
}