package com.hfad.mystylebox.adapter

import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.hfad.mystylebox.R
import org.threeten.bp.LocalDate
import org.threeten.bp.YearMonth
import java.io.File

class MonthsAdapter(
    private val months: List<YearMonth>,
    private val today: LocalDate,
    private val dateImageMap: Map<String, List<String>>,
    private val onDateClick: (LocalDate) -> Unit
) : RecyclerView.Adapter<MonthsAdapter.MonthViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MonthViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_month, parent, false)
        return MonthViewHolder(view)
    }

    override fun getItemCount() = months.size

    override fun onBindViewHolder(holder: MonthViewHolder, position: Int) {
        holder.bind(months[position], today, dateImageMap, onDateClick)
    }

    class MonthViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        companion object {
            private val MONTH_NAMES = arrayOf(
                "Январь","Февраль","Март","Апрель",
                "Май","Июнь","Июль","Август",
                "Сентябрь","Октябрь","Ноябрь","Декабрь"
            )
        }

        private val tvHeader: TextView = itemView.findViewById(R.id.tvMonthHeader)
        private val glDays: GridLayout = itemView.findViewById(R.id.glDays)

        fun bind(
            month: YearMonth,
            today: LocalDate,
            dateImageMap: Map<String, List<String>>,
            onDateClick: (LocalDate) -> Unit
        ) {

            val monthName = MONTH_NAMES[month.monthValue - 1]
            tvHeader.text = "$monthName ${month.year} г."
            tvHeader.setTextColor(
                if (month.monthValue == today.monthValue && month.year == today.year)
                    Color.BLACK else Color.GRAY
            )

            glDays.removeAllViews()
            glDays.columnCount = 7
            glDays.alignmentMode = GridLayout.ALIGN_MARGINS
            glDays.useDefaultMargins = true

            listOf("Пн","Вт","Ср","Чт","Пт","Сб","Вс").forEach { day ->
                glDays.addView(TextView(itemView.context).apply {
                    text = day
                    setTextColor(Color.DKGRAY)
                    textSize = 14f
                    layoutParams = GridLayout.LayoutParams().apply {
                        width = 0
                        height = ViewGroup.LayoutParams.WRAP_CONTENT
                        columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f)
                    }
                    textAlignment = TextView.TEXT_ALIGNMENT_CENTER
                })
            }

            val offset = (month.atDay(1).dayOfWeek.value + 6) % 7
            repeat(offset) {
                glDays.addView(View(itemView.context).apply {
                    layoutParams = GridLayout.LayoutParams().apply {
                        width = 0
                        height = 0
                        columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f)
                    }
                })
            }

            val dp = itemView.context.resources.displayMetrics.density
            val cellSize = (48 * dp).toInt()

            for (day in 1..month.lengthOfMonth()) {
                val date = month.atDay(day)
                val paths = dateImageMap[date.toString()] ?: emptyList()
                val cellView = LayoutInflater.from(itemView.context)
                    .inflate(R.layout.item_month_cell, glDays, false)

                val container = cellView.findViewById<FrameLayout>(R.id.dayCellContainer)
                val tvDayNumber = cellView.findViewById<TextView>(R.id.tvDayNumber)
                val tvBadgeCount = cellView.findViewById<TextView>(R.id.tvBadgeCount)
                val ivOutfit = cellView.findViewById<ImageView>(R.id.ivOutfit)

                tvDayNumber.text = day.toString()

                if (paths.isNotEmpty()) {
                    ivOutfit.visibility = View.VISIBLE
                    Glide.with(ivOutfit.context)
                        .load( File(paths[0]) )
                        .into(ivOutfit)

                    tvDayNumber.visibility = View.GONE

                    if (paths.size >= 2) {
                        tvBadgeCount.visibility = View.VISIBLE
                        tvBadgeCount.text = paths.size.toString()
                    } else {
                        tvBadgeCount.visibility = View.GONE
                    }

                } else {
                    tvDayNumber.visibility  = View.VISIBLE
                    ivOutfit.visibility = View.GONE
                    tvBadgeCount.visibility = View.GONE
                    tvDayNumber.setTextColor(Color.BLACK)
                }

                if (date == today) {
                    container.setBackgroundResource(R.drawable.item_background_active)
                } else {
                    container.setBackgroundResource(0)
                }

                cellView.setOnClickListener { onDateClick(date) }

                glDays.addView(cellView, GridLayout.LayoutParams().apply {
                    width = 0
                    height = cellSize
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f)
                })
            }

            val totalCells = 7 + offset + month.lengthOfMonth()
            glDays.rowCount = (totalCells + 6) / 7
        }
    }
}