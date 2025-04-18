package com.hfad.mystylebox.adapter

import android.graphics.Color
import android.net.Uri
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.View.TEXT_ALIGNMENT_CENTER
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.room.ColumnInfo.Companion.UNDEFINED
import com.bumptech.glide.Glide
import com.hfad.mystylebox.R
import com.hfad.mystylebox.database.DailyPlanDao
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
            val badgeSize = (20 * dp).toInt()

            for (day in 1..month.lengthOfMonth()) {
                val date = month.atDay(day)
                val paths = dateImageMap[date.toString()] ?: emptyList()

                val cellView = if (paths.isEmpty()) {
                    TextView(itemView.context).apply {
                        text = day.toString()
                        textSize = 16f
                        setTextColor(if (date == today) Color.BLUE else Color.BLACK)
                        gravity = Gravity.CENTER
                        layoutParams = GridLayout.LayoutParams().apply {
                            width = 0
                            height = cellSize
                            columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f)
                        }
                    }
                } else {
                    FrameLayout(itemView.context).apply {
                        layoutParams = GridLayout.LayoutParams().apply {
                            width = 0
                            height = cellSize
                            columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f)
                        }
                        addView(ImageView(context).apply {
                            scaleType = ImageView.ScaleType.CENTER_CROP
                            layoutParams = FrameLayout.LayoutParams(cellSize, cellSize)
                            Glide.with(this).load(File(paths[0])).into(this)
                        })
                        if (paths.size >= 2) {
                            addView(TextView(context).apply {
                                text = paths.size.toString()
                                textSize = 10f
                                gravity = Gravity.CENTER
                                setBackgroundResource(R.drawable.badge_background)
                                setTextColor(Color.BLACK)
                                layoutParams = FrameLayout.LayoutParams(
                                    badgeSize, badgeSize,
                                    Gravity.END or Gravity.TOP
                                ).apply {
                                    setMargins(0, -5, -5, 0)
                                }
                            })
                        }
                    }
                }
                cellView.setOnClickListener { onDateClick(date) }
                glDays.addView(cellView)
            }

            val totalCells = 7 + offset + month.lengthOfMonth()
            glDays.rowCount = (totalCells + 6) / 7
        }
    }
}