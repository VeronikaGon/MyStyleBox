package com.hfad.mystylebox.fragment

import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
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
import com.hfad.mystylebox.database.dao.WishListItemDao
import com.hfad.mystylebox.database.entity.WishListItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WishlistStatsFragment : Fragment(R.layout.fragment_wishlist_stats), SecondFragment.StatsUpdatable {

    private lateinit var pieChart: PieChart
    private lateinit var llScales: LinearLayout
    private lateinit var tvMostExpensive: TextView
    private lateinit var tvCheapest: TextView
    private lateinit var tvWishlistCost: TextView
    private lateinit var tvAverageWishlistCost: TextView
    private lateinit var scrollContent: View
    private lateinit var tvEmptyWishlist: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        scrollContent      = view.findViewById(R.id.scrollContent)
        tvEmptyWishlist    = view.findViewById(R.id.tvEmptyWishlist)
        pieChart        = view.findViewById(R.id.pieChart)
        llScales        = view.findViewById(R.id.llScales)
        tvMostExpensive = view.findViewById(R.id.tvmostexpensiveitem)
        tvCheapest      = view.findViewById(R.id.tvmostCheapestitem)
        tvWishlistCost = view.findViewById(R.id.tvwishlistcost)
        tvAverageWishlistCost      = view.findViewById(R.id.tvaveragewishlistcost)
        updateStats()
        lifecycleScope.launch {
            val dao      = AppDatabase.getInstance(requireContext()).wishListItemDao()
            val allItems = withContext(Dispatchers.IO) { dao.getAll() }

            val totalCost    = dao.getAll().sumOf { it.price }
            val averageCost  = dao.getAll().map { it.price }.average()
            val mostExp      = dao.getAll().maxByOrNull { it.price }?.price ?: 0.0
            val cheapest     = dao.getAll().minByOrNull { it.price }?.price ?: 0.0

            tvWishlistCost.text       = totalCost.toInt().toString()
            tvAverageWishlistCost.text= averageCost.toInt().toString()
            tvMostExpensive.text      = mostExp.toInt().toString()
            tvCheapest.text           = cheapest.toInt().toString()

            val statsByCategory = withContext(Dispatchers.IO) {
                dao.getCountByCategory()
            }
            if (statsByCategory.isNotEmpty()) {
                setupPieChart(statsByCategory)
                setupScales(statsByCategory)
            }
        }
    }
    override fun updateStats() {
        val root = view ?: return
        lifecycleScope.launch {
            val dao      = AppDatabase.getInstance(requireContext()).wishListItemDao()
            val items    = withContext(Dispatchers.IO) { dao.getAll() }
            val stats    = withContext(Dispatchers.IO) { dao.getCountByCategory() }

            if (items.isEmpty()) {
                scrollContent.visibility   = View.GONE
                tvEmptyWishlist.visibility = View.VISIBLE
                return@launch
            } else {
                scrollContent.visibility   = View.VISIBLE
                tvEmptyWishlist.visibility = View.GONE
            }
            renderWishlist(root, items, stats)
        }
    }

    private fun renderWishlist(
        root: View,
        items: List<WishListItem>,
        stats: List<WishListItemDao.CategoryCount>
    ) {
        val totalCost = items.sumOf { it.price.toInt() }
        val avgCost   = if (items.isEmpty()) 0 else items.map { it.price.toInt() }.average().toInt()
        val maxPrice  = items.maxOfOrNull { it.price.toInt() } ?: 0
        val minPrice  = items.minOfOrNull { it.price.toInt() } ?: 0

        root.findViewById<TextView>(R.id.tvwishlistcost).text       = totalCost.toString()
        root.findViewById<TextView>(R.id.tvaveragewishlistcost).text= avgCost.toString()
        root.findViewById<TextView>(R.id.tvmostexpensiveitem).text = maxPrice.toString()
        root.findViewById<TextView>(R.id.tvmostCheapestitem).text  = minPrice.toString()

        val entries = stats.map { PieEntry(it.wishCount.toFloat(), it.categoryName) }
        val colors  = stats.mapIndexed { idx, _ -> ContextCompat.getColor(requireContext(), R.color.pie1 + idx % 10) }
        val pie     = root.findViewById<PieChart>(R.id.pieChart)
        pie.data = PieData(PieDataSet(entries, "").apply { this.colors = colors; setDrawValues(false) })
        pie.centerText = "${stats.sumOf { it.wishCount }}\nжел. вещей"
        pie.invalidate()

        val ll = root.findViewById<LinearLayout>(R.id.llScales)
        ll.removeAllViews()
        val totalItems = stats.sumOf { it.wishCount }
        stats.forEachIndexed { idx, st ->
            val pct = if (totalItems > 0) st.wishCount * 100 / totalItems else 0
            val row = layoutInflater.inflate(R.layout.item_scale, ll, false)
            row.findViewById<TextView>(R.id.tvName).text    = st.categoryName
            row.findViewById<TextView>(R.id.tvCount).text   = st.wishCount.toString()
            row.findViewById<TextView>(R.id.tvPercent).text = "${pct}%"
            val pb = row.findViewById<ProgressBar>(R.id.progressBar)
            pb.max = 100; pb.progress = pct
            pb.progressTintList = ColorStateList.valueOf(colors[idx])
            ll.addView(row)
        }
    }

private fun setupPieChart(data: List<WishListItemDao.CategoryCount>) {
        val entries = data.map { PieEntry(it.wishCount.toFloat(), it.categoryName) }
        val colors  = getCategoryColors(requireContext())

        PieDataSet(entries, "").apply {
            this.colors = colors
        }.let { set ->
            pieChart.data = PieData(set).apply { setDrawValues(false) }
        }

        pieChart.apply {
            setDrawEntryLabels(false)
            description.isEnabled   = false
            isRotationEnabled       = false
            legend.isEnabled        = false
            holeRadius = 60f
            setCenterText("${data.sumOf { it.wishCount }}\nжел. вещей")
            setCenterTextSize(16f)
            animateY(500)
            invalidate()
        }
    }

    private fun setupScales(data: List<WishListItemDao.CategoryCount>) {
        llScales.removeAllViews()
        val total  = data.sumOf { it.wishCount }.toFloat()
        val colors = getCategoryColors(requireContext())

        data.forEachIndexed { i, stat ->
            val pct = if (total > 0) (stat.wishCount / total * 100).toInt() else 0
            val row = layoutInflater.inflate(R.layout.item_scale, llScales, false)
            row.findViewById<TextView>(R.id.tvPercent).text = "$pct%"
            row.findViewById<TextView>(R.id.tvName).text    = stat.categoryName
            row.findViewById<TextView>(R.id.tvCount).text   = stat.wishCount.toString()
            val pb = row.findViewById<ProgressBar>(R.id.progressBar)
            pb.max = 100; pb.progress = pct
            pb.progressTintList = ColorStateList.valueOf(colors[i % colors.size])
            llScales.addView(row)
        }
    }
    private fun getCategoryColors(context: Context): List<Int> =
        listOf(
            R.color.pie1, R.color.pie2, R.color.pie3,
            R.color.pie4, R.color.pie5, R.color.pie6,
            R.color.pie7, R.color.pie8, R.color.pie9
        ).map { ContextCompat.getColor(context, it) }
}