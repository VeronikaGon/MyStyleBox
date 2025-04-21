package com.hfad.mystylebox.ui.bottomsheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.threeten.bp.LocalDate
import com.hfad.mystylebox.R
import org.threeten.bp.format.DateTimeFormatter
import java.util.Locale

class BottomSheetScheduleFragment : BottomSheetDialogFragment() {

    interface Callback {
        fun onSchedule(date: LocalDate)
        fun onScheduleMore(date: LocalDate)
        fun onRemoveOne(date: LocalDate)
    }

    companion object {
        private const val ARG_DATE = "arg_date"
        private const val ARG_OUTFIT_NAME = "arg_outfit_name" // null если пустой день

        /**
         * @param date         — дата в формате LocalDate
         * @param outfitName   — название комплекта, если показываем для конкретного элемента;
         *                      null, если ничего не запланировано и показываем просто дату
         */
        fun newInstance(date: LocalDate, outfitName: String?): BottomSheetScheduleFragment {
            val fmtDate = date.toString() // ISO
            val fragment = BottomSheetScheduleFragment()
            fragment.arguments = Bundle().apply {
                putString(ARG_DATE, fmtDate)
                outfitName?.let { putString(ARG_OUTFIT_NAME, it) }
            }
            return fragment
        }
    }

    private lateinit var date: LocalDate
    private var outfitName: String? = null
    private var callback: Callback? = null

    /** Fluent-setter для колбэка */
    fun setCallback(cb: Callback) = apply { this.callback = cb }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            date = LocalDate.parse(it.getString(ARG_DATE))
            outfitName = it.getString(ARG_OUTFIT_NAME)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.bottom_sheet_schedule, container, false).also { view ->
        // 1) Заголовок
        val tvDate = view.findViewById<TextView>(R.id.tvSheetDate)
        tvDate.text = buildHeaderText()

        val btnPrimary = view.findViewById<Button>(R.id.btnPrimary)
        val btnRemove  = view.findViewById<Button>(R.id.btnRemove)
        val btnCancel  = view.findViewById<Button>(R.id.btnCancel)

        // 2) Если outfitName != null — у нас уже был запланирован хотя бы 1 комплект
        if (outfitName != null) {
            btnPrimary.text     = getString(R.string.sheet_schedule_more)  // "Запланировать ещё"
            btnRemove.visibility = View.VISIBLE                            // показываем "Убрать комплект"
        } else {
            btnPrimary.text     = getString(R.string.sheet_schedule)       // "Запланировать комплект"
            btnRemove.visibility = View.GONE
        }

        btnPrimary.setOnClickListener {
            if (outfitName != null) callback?.onScheduleMore(date)
            else                  callback?.onSchedule(date)
            dismiss()
        }
        btnRemove.setOnClickListener {
            callback?.onRemoveOne(date)
            dismiss()
        }
        btnCancel.setOnClickListener { dismiss() }
    }

    /**
     * Собирает строку заголовка:
     * «Сегодня, d MMMM yyyy г.» / «Завтра, …» / «d MMMM yyyy г.»
     * + « — названиеКомплекта» если outfitName != null
     */
    private fun buildHeaderText(): String {
        val formattedDate = formatFullDate(date)
        val prefix = when {
            date.isEqual(LocalDate.now())            -> "Сегодня, $formattedDate"
            date.isEqual(LocalDate.now().plusDays(1)) -> "Завтра, $formattedDate"
            else -> formattedDate
        }
        return if (outfitName != null) "$prefix — $outfitName" else prefix
    }

    /** DateTimeFormatter ThreeTenBP */
    private fun formatFullDate(d: LocalDate): String {
        val fmt = DateTimeFormatter.ofPattern("d MMMM yyyy 'г.'", Locale("ru"))
        return d.format(fmt)
    }
}