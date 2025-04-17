package com.hfad.mystylebox.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.hfad.mystylebox.R
import org.threeten.bp.LocalDate
import org.threeten.bp.YearMonth

class MonthsAdapter(
    private val months: List<YearMonth>,
    private val today: LocalDate
) : RecyclerView.Adapter<MonthsAdapter.MonthViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MonthViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_month, parent, false)
        return MonthViewHolder(view)
    }

    override fun getItemCount() = months.size

    override fun onBindViewHolder(holder: MonthViewHolder, position: Int) {
        holder.bind(months[position], today)
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

        fun bind(month: YearMonth, today: LocalDate) {
            val monthName = MONTH_NAMES[month.monthValue - 1]
            tvHeader.text = "$monthName ${month.year} г."
            tvHeader.setTextColor(
                if (month.monthValue == today.monthValue && month.year == today.year)
                    Color.BLACK else Color.GRAY
            )

            glDays.removeAllViews()

            listOf("Пн","Вт","Ср","Чт","Пт","Сб","Вс").forEach { day ->
                glDays.addView(TextView(itemView.context).apply {
                    text = day
                    setTextColor(Color.DKGRAY)
                    textSize = 14f
                    layoutParams = GridLayout.LayoutParams().apply {
                        width = 0
                        columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    }
                    textAlignment = TextView.TEXT_ALIGNMENT_CENTER
                })
            }

            val offset = (month.atDay(1).dayOfWeek.value + 6) % 7
            repeat(offset) {
                glDays.addView(TextView(itemView.context))
            }

            for (day in 1..month.lengthOfMonth()) {
                val date = month.atDay(day)
                glDays.addView(TextView(itemView.context).apply {
                    text = day.toString()
                    textSize = 16f
                    setTextColor(if (date == today) Color.BLUE else Color.BLACK)
                    layoutParams = GridLayout.LayoutParams().apply {
                        width = 0
                        height = ViewGroup.LayoutParams.WRAP_CONTENT
                        columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                        setMargins(0, 8, 0, 8)
                    }
                    textAlignment = TextView.TEXT_ALIGNMENT_CENTER
                })
            }

            val totalCells = 7 + offset + month.lengthOfMonth()
            glDays.rowCount = (totalCells + 6) / 7
        }
    }
}