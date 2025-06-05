package com.hfad.mystylebox.fragment

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hfad.mystylebox.ui.activity.CalendarActivity
import com.hfad.mystylebox.ui.widget.DataProvider
import com.hfad.mystylebox.ui.activity.OutfitSelectionActivity
import com.hfad.mystylebox.R
import com.hfad.mystylebox.adapter.OutfitAdapter
import com.hfad.mystylebox.adapter.PlannedOutfitActionAdapter
import com.hfad.mystylebox.database.AppDatabase
import com.hfad.mystylebox.database.entity.DailyPlan
import com.hfad.mystylebox.ui.bottomsheet.BottomSheetScheduleFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
import java.util.Locale

class FirstFragment : Fragment() {

    private lateinit var currentDate: LocalDate
    private var selectedDate: LocalDate? = null
    private var currentWeekStart: LocalDate? = null

    private lateinit var llMonday: LinearLayout;    private lateinit var tvMondaynumber: TextView
    private lateinit var llTuesday: LinearLayout;   private lateinit var tvTuesdaynumber: TextView
    private lateinit var llWednesday: LinearLayout; private lateinit var tvWednesdaynumber: TextView
    private lateinit var llThursday: LinearLayout;  private lateinit var tvThursdaynumber: TextView
    private lateinit var llFriday: LinearLayout;    private lateinit var tvFridaynumber: TextView
    private lateinit var llSaturday: LinearLayout;  private lateinit var tvSaturdaynumber: TextView
    private lateinit var llSunday: LinearLayout;    private lateinit var tvSundaynumber: TextView

    private lateinit var tvDescription : TextView
    private lateinit var tvTitle: TextView
    private lateinit var tvCurrentMonth: TextView

    private lateinit var btnPrevWeek: ImageButton
    private lateinit var btnNextWeek: ImageButton
    private lateinit var btnCalendarAddOutfit: ImageButton
    private lateinit var btnCalendarMonth: ImageButton

    private lateinit var llAdd: LinearLayout
    private lateinit var recyclerViewOutfits: RecyclerView
    private lateinit var outfitAdapter: OutfitAdapter

    private val outfitSelectionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val ids = result.data
                    ?.getStringArrayListExtra("EXTRA_SELECTED_IDS")
                    ?.mapNotNull { it.toIntOrNull() }
                if (ids != null && selectedDate != null) saveDailyPlans(selectedDate!!, ids)
            }
        }

    private val calendarLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                updateWeekView()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_first, container, false)

        currentDate = LocalDate.now()
        selectedDate = currentDate
        currentWeekStart = getStartOfWeek(selectedDate!!)

        llMonday = view.findViewById(R.id.llMonday); tvMondaynumber = view.findViewById(R.id.tvMondaynumber)
        llTuesday = view.findViewById(R.id.llTuesday); tvTuesdaynumber = view.findViewById(R.id.tvTuesdaynumber)
        llWednesday = view.findViewById(R.id.llWednesday); tvWednesdaynumber = view.findViewById(R.id.tvWednesdaynumber)
        llThursday = view.findViewById(R.id.llThursday); tvThursdaynumber = view.findViewById(R.id.tvThursdaynumber)
        llFriday = view.findViewById(R.id.llFriday); tvFridaynumber = view.findViewById(R.id.tvFridaynumber)
        llSaturday = view.findViewById(R.id.llSaturday); tvSaturdaynumber = view.findViewById(R.id.tvSaturdaynumber)
        llSunday = view.findViewById(R.id.llSunday); tvSundaynumber = view.findViewById(R.id.tvSundaynumber)
        tvCurrentMonth = view.findViewById(R.id.tvCurrentMonth)
        tvTitle = view.findViewById(R.id.tvTitle)

        tvDescription = view.findViewById(R.id.tvdesription)
        btnCalendarAddOutfit = view.findViewById(R.id.btnCalendarAddOutfit)
        btnCalendarMonth = view.findViewById(R.id.btnCalendarMonth)
        llAdd = view.findViewById(R.id.lladd)
        recyclerViewOutfits = view.findViewById(R.id.recyclerViewOutfits)
        recyclerViewOutfits.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        outfitAdapter = OutfitAdapter(emptyList(), R.layout.item_outfit)
        recyclerViewOutfits.adapter = outfitAdapter

        btnPrevWeek = view.findViewById(R.id.btnPrevDay)
        btnNextWeek = view.findViewById(R.id.btnNextDay)

        tvCurrentMonth.text = getMonthName(selectedDate!!.month.value)
        updateTitle()

        listOf(
            llMonday to 0, llTuesday to 1, llWednesday to 2,
            llThursday to 3, llFriday to 4, llSaturday to 5, llSunday to 6
        ).forEach { (ll, idx) ->
            ll.setOnClickListener {
                selectedDate = currentWeekStart!!.plusDays(idx.toLong())
                updateWeekView()
            }
            ll.setOnLongClickListener {
                val date = currentWeekStart!!.plusDays(idx.toLong())
                CoroutineScope(Dispatchers.IO).launch {
                    val exists = hasAnyOutfits()
                    withContext(Dispatchers.Main) {
                        if (!exists) {
                            Toast.makeText(
                                requireContext(),
                                "Сначала добавьте хотя бы один комплект",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            selectedDate = date
                            updateWeekView()
                            BottomSheetScheduleFragment
                                .newInstance(date, outfitName = null)
                                .setCallback(object : BottomSheetScheduleFragment.Callback {
                                    override fun onSchedule(d: LocalDate) {
                                        outfitSelectionLauncher.launch(
                                            Intent(requireContext(), OutfitSelectionActivity::class.java)
                                                .putExtra("EXTRA_SELECTED_DATE", d.toString())
                                        )
                                    }
                                    override fun onScheduleMore(d: LocalDate) = Unit
                                    override fun onRemoveOne(d: LocalDate) = Unit
                                })
                                .show(parentFragmentManager, "sheet_empty")
                        }
                    }
                }
                true
            }
        }

        btnPrevWeek.setOnClickListener {
            currentWeekStart = currentWeekStart!!.minusWeeks(1)
            updateWeekView()
        }
        btnNextWeek.setOnClickListener {
            currentWeekStart = currentWeekStart!!.plusWeeks(1)
            updateWeekView()
        }

        btnCalendarAddOutfit.setOnLongClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                val exists = hasAnyOutfits()
                withContext(Dispatchers.Main) {
                    if (!exists) {
                        Toast.makeText(
                            requireContext(),
                            "Сначала добавьте хотя бы один комплект",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        BottomSheetScheduleFragment
                            .newInstance(selectedDate!!, outfitName = null)
                            .setCallback(object : BottomSheetScheduleFragment.Callback {
                                override fun onSchedule(d: LocalDate) {
                                    outfitSelectionLauncher.launch(
                                        Intent(requireContext(), OutfitSelectionActivity::class.java)
                                            .putExtra("EXTRA_SELECTED_DATE", d.toString())
                                    )
                                }
                                override fun onScheduleMore(d: LocalDate) = Unit
                                override fun onRemoveOne(d: LocalDate) = Unit
                            })
                            .show(parentFragmentManager, "sheet_empty")
                    }
                }
            }
            true
        }

        btnCalendarAddOutfit.setOnClickListener{ CoroutineScope(Dispatchers.IO).launch {
            val exists = hasAnyOutfits()
            withContext(Dispatchers.Main) {
                if (!exists) {
                    Toast.makeText(
                        requireContext(),
                        "Сначала добавьте хотя бы один комплект",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                else {
                    BottomSheetScheduleFragment
                        .newInstance(selectedDate!!, outfitName = null)
                        .setCallback(object : BottomSheetScheduleFragment.Callback {
                            override fun onSchedule(d: LocalDate) {
                                outfitSelectionLauncher.launch(
                                    Intent(requireContext(), OutfitSelectionActivity::class.java)
                                        .putExtra("EXTRA_SELECTED_DATE", d.toString())
                                )
                            }
                            override fun onScheduleMore(d: LocalDate) = Unit
                            override fun onRemoveOne(d: LocalDate) = Unit
                        })
                        .show(parentFragmentManager, "sheet_empty")
                    true
                }
            }
        }
        }

        btnCalendarMonth.setOnClickListener {
            val intent = Intent(requireContext(), CalendarActivity::class.java)
            calendarLauncher.launch(intent)
        }
        updateWeekView()
        return view
    }
    //проверка есть ли комплекты
    private suspend fun hasAnyOutfits(): Boolean {
        val db = AppDatabase.getInstance(requireContext())
        return db.outfitDao().getCount() > 0
    }

    //метод обновления отображения недели
    private fun updateWeekView() {
        val days = List(7) { currentWeekStart!!.plusDays(it.toLong()) }

        tvMondaynumber   .text = days[0].dayOfMonth.toString()
        tvTuesdaynumber  .text = days[1].dayOfMonth.toString()
        tvWednesdaynumber.text = days[2].dayOfMonth.toString()
        tvThursdaynumber .text = days[3].dayOfMonth.toString()
        tvFridaynumber   .text = days[4].dayOfMonth.toString()
        tvSaturdaynumber .text = days[5].dayOfMonth.toString()
        tvSundaynumber   .text = days[6].dayOfMonth.toString()

        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getInstance(requireContext())
            val planned = days.filter {
                db.dailyPlanDao().getDailyPlansForDate(it.toString()).isNotEmpty()
            }.toSet()
            withContext(Dispatchers.Main) {
                updateDayAppearance(llMonday, days[0], planned)
                updateDayAppearance(llTuesday, days[1], planned)
                updateDayAppearance(llWednesday, days[2], planned)
                updateDayAppearance(llThursday, days[3], planned)
                updateDayAppearance(llFriday, days[4], planned)
                updateDayAppearance(llSaturday, days[5], planned)
                updateDayAppearance(llSunday, days[6], planned)

                tvCurrentMonth.text = getMonthName(selectedDate!!.monthValue)
                tvTitle.text = getFormattedDay(selectedDate!!)

                if (planned.contains(selectedDate!!)) {
                    llAdd.visibility = View.GONE
                    recyclerViewOutfits.visibility = View.VISIBLE
                    loadPlannedOutfits(selectedDate!!)
                } else {
                    llAdd.visibility = View.VISIBLE
                    recyclerViewOutfits.visibility = View.GONE
                    // Динамический текст внизу:
                    tvDescription.text = when {
                        selectedDate!!.isEqual(currentDate) ->
                            "У вас нет запланированных комплектов на сегодня.\nЗапланируйте свой день, чтобы повысить продуктивность!"

                        selectedDate!!.isEqual(currentDate.plusDays(1)) ->
                            "У вас нет запланированных комплектов на завтра.\nЗапланируйте свой день, чтобы повысить продуктивность!"

                        else ->
                            "У вас нет запланированных комплектов на ${formatFullDate(selectedDate!!)}\nЗапланируйте свой день, чтобы повысить продуктивность!"
                    }
                }
            }
        }
    }
    private fun updateDayAppearance(
        container: LinearLayout,
        date: LocalDate,
        planned: Set<LocalDate>
    ) {
        val tvLabel  = container.getChildAt(0) as TextView
        val tvNumber = container.getChildAt(1) as TextView

        container.findViewWithTag<ImageView>("planned_icon")
            ?.let { container.removeView(it) }
        when {
            date == selectedDate -> {
                tvLabel.visibility  = View.VISIBLE
                tvNumber.visibility = View.VISIBLE
                container.setBackgroundResource(R.drawable.item_background_active)
            }
            planned.contains(date) -> {
                tvLabel.visibility  = View.GONE
                tvNumber.visibility = View.GONE
                val bg = if (date == currentDate)
                    R.drawable.item_active_today
                else
                    R.drawable.item_planned
                container.setBackgroundResource(bg)
                val iv = ImageView(requireContext()).apply {
                    tag = "planned_icon"
                    setImageResource(R.drawable.ic_check_20)
                    layoutParams = LinearLayout.LayoutParams(
                        WRAP_CONTENT, WRAP_CONTENT
                    ).also { it.gravity = Gravity.CENTER }
                }
                container.addView(iv)
            }
            date == currentDate -> {
                tvLabel.visibility  = View.VISIBLE
                tvNumber.visibility = View.VISIBLE
                container.setBackgroundResource(R.drawable.item_active_today)
            }
            else -> {
                tvLabel.visibility  = View.VISIBLE
                tvNumber.visibility = View.VISIBLE
                container.background = null
            }
        }
    }

    // При клике по контейнеру дня устанавливается выбранная дата, после чего обновляется весь UI.
    private fun onDayClicked(date: LocalDate) {
        selectedDate = date
        updateWeekView()
    }

    //Обновляет заголовок (tvTitle) с отформатированным текстом для выбранного дня.
    private fun updateTitle() {
        tvTitle.text = getFormattedDay(selectedDate!!)
    }
    /** Возвращает строку вида "25 апреля 2025 г." */
    private fun formatFullDate(date: LocalDate): String {
        val formatter = DateTimeFormatter.ofPattern("d MMMM yyyy 'г.'", Locale("ru"))
        return date.format(formatter)
    }

    /**
     * Форматирует дату для отображения в заголовке (tvTitle).
     * Если дата равна вчера, сегодня или завтра – возвращаются надписи "Вчера", "Сегодня", "Завтра".
     * Иначе возвращается формат "число + сокращённое название месяца" (например, "13 апр.")
     */
    private fun getFormattedDay(date: LocalDate): String {
        return when {
            date.isEqual(currentDate.minusDays(1)) -> "Вчера"
            date.isEqual(currentDate) -> "Сегодня"
            date.isEqual(currentDate.plusDays(1)) -> "Завтра"
            else -> "${date.dayOfMonth} ${getAbbreviatedMonthName(date.month.value)}"
        }
    }

    /**
     * Возвращает сокращённое название месяца для форматирования дней.
     */
    private fun getAbbreviatedMonthName(month: Int): String {
        return when (month) {
            1 -> "янв."
            2 -> "февр."
            3 -> "мар."
            4 -> "апр."
            5 -> "мая"
            6 -> "июня"
            7 -> "июля"
            8 -> "авг."
            9 -> "сен."
            10 -> "окт."
            11 -> "нояб."
            12 -> "дек."
            else -> ""
        }
    }

    /**
     * Проверяет, для выбранной даты есть запланированные комплекты.
     * Если да – скрываем llAdd, показываем RecyclerView и загружаем данные.
     * Если нет – показываем llAdd, скрываем RecyclerView и очищаем адаптер.
     */
    private fun checkOutfitPlanForDate(date: LocalDate) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getInstance(requireContext())
            val dailyPlans = db.dailyPlanDao().getDailyPlansForDate(date.toString())
            withContext(Dispatchers.Main) {
                if (dailyPlans.isNotEmpty()) {
                    llAdd.visibility = View.GONE
                    recyclerViewOutfits.visibility = View.VISIBLE
                    loadPlannedOutfits(date)
                } else {
                    llAdd.visibility = View.VISIBLE
                    recyclerViewOutfits.visibility = View.GONE
                    outfitAdapter.updateData(emptyList())
                }
            }
        }
    }

    /**
     * Загружает детали запланированных комплектов для выбранной даты.
     * Предполагается, что в OutfitDao есть метод getOutfitsByIds().
     */
    private fun loadPlannedOutfits(date: LocalDate) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getInstance(requireContext())
            val ids = db.dailyPlanDao().getDailyPlansForDate(date.toString()).map { it.outfitId }
            val outfits = db.outfitDao().getOutfitsByIds(ids)
            withContext(Main) {
                recyclerViewOutfits.adapter = PlannedOutfitActionAdapter(
                    outfits,
                    onDeleteRequested = { outfit ->
                        AlertDialog.Builder(requireContext())
                            .setTitle("Убрать комплект?")
                            .setMessage("Точно убрать этот комплект из планирования?")
                            .setPositiveButton("Да") { _, _ ->
                                CoroutineScope(Dispatchers.IO).launch {
                                    db.dailyPlanDao()
                                        .deleteByDateAndOutfitId(date.toString(), outfit.id.toLong())
                                    withContext(Main) {
                                        updateWeekView()
                                        DataProvider.notifyWidgetDataChanged(requireContext())
                                    }
                                }
                            }
                            .setNegativeButton("Нет", null)
                            .show()
                    },
                    onItemLongClick = { outfit ->
                        BottomSheetScheduleFragment
                            .newInstance(selectedDate!!, outfitName = outfit.name)
                            .setCallback(object: BottomSheetScheduleFragment.Callback {
                                override fun onSchedule(d: LocalDate) {
                                    outfitSelectionLauncher.launch(
                                        Intent(requireContext(), OutfitSelectionActivity::class.java)
                                            .putExtra("EXTRA_SELECTED_DATE", d.toString())
                                    )
                                }
                                override fun onScheduleMore(d: LocalDate) {
                                    onSchedule(d)
                                }
                                override fun onRemoveOne(d: LocalDate) {
                                    CoroutineScope(Dispatchers.IO).launch {
                                        db.dailyPlanDao()
                                            .deleteByDateAndOutfitId(d.toString(), outfit.id.toLong())
                                        withContext(Main) {
                                            updateWeekView()
                                            DataProvider.notifyWidgetDataChanged(requireContext())
                                        }
                                    }
                                }
                            })
                            .show(parentFragmentManager, "sheet_item")
                    }
                )
            }
        }
    }

    //Сохраняет выбранные комплекты для указанной даты.
    private fun saveDailyPlans(date: LocalDate, selectedIds: List<Int>) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getInstance(requireContext())
            selectedIds.forEach { outfitId ->
                val dailyPlan = DailyPlan(date.toString(),outfitId)
                db.dailyPlanDao().insert(dailyPlan)
            }
            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), "Комплект(ы) запланированы!", Toast.LENGTH_SHORT).show()
                checkOutfitPlanForDate(date)
                DataProvider.notifyWidgetDataChanged(requireContext())
            }
        }
    }

    /**
     * Обновляет фон контейнера для дня:
     * - Если дата совпадает с выбранной (selectedDate) – фон item_background_active.xml.
     * - Если дата равна сегодняшней (currentDate), а не выбрана – фон item_active_today.xml.
     * - Иначе сбрасываем фон (null).
     */
    private fun updateDayBackground(container: LinearLayout, date: LocalDate) {
        when {
            selectedDate == date -> container.setBackgroundResource(R.drawable.item_background_active)
            date.isEqual(currentDate) -> container.setBackgroundResource(R.drawable.item_active_today)
            else -> container.setBackgroundResource(0)
        }
    }

    /**
     * Возвращает полное название месяца по его номеру.
     */
    private fun getMonthName(month: Int): String {
        return when (month) {
            1 -> "Январь"
            2 -> "Февраль"
            3 -> "Март"
            4 -> "Апрель"
            5 -> "Май"
            6 -> "Июнь"
            7 -> "Июль"
            8 -> "Август"
            9 -> "Сентябрь"
            10 -> "Октябрь"
            11 -> "Ноябрь"
            12 -> "Декабрь"
            else -> ""
        }
    }

    /**
     * Вычисляет дату начала недели (понедельник) для данной даты.
     * По стандарту ISO неделя начинается с понедельника.
     */
    private fun getStartOfWeek(date: LocalDate): LocalDate {
        return date.minusDays((date.dayOfWeek.value - 1).toLong())
    }
}