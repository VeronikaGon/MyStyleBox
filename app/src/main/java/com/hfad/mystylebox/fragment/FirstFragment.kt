package com.hfad.mystylebox.fragment

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hfad.mystylebox.OutfitSelectionActivity
import com.hfad.mystylebox.R
import com.hfad.mystylebox.adapter.OutfitAdapter
import com.hfad.mystylebox.database.AppDatabase
import com.hfad.mystylebox.database.DailyPlan
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.LocalDate
import org.threeten.bp.Month

class FirstFragment : Fragment() {

    // Переменные для работы с датами: текущая дата, выбранная дата и дата начала недели
    private lateinit var currentDate: LocalDate
    private var selectedDate: LocalDate? = null
    private var currentWeekStart: LocalDate? = null

    // View для дней недели и элементов интерфейса
    private lateinit var llMonday: LinearLayout
    private lateinit var tvMondaynumber: TextView
    private lateinit var llTuesday: LinearLayout
    private lateinit var tvTuesdaynumber: TextView
    private lateinit var llWednesday: LinearLayout
    private lateinit var tvWednesdaynumber: TextView
    private lateinit var llThursday: LinearLayout
    private lateinit var tvThursdaynumber: TextView
    private lateinit var llFriday: LinearLayout
    private lateinit var tvFridaynumber: TextView
    private lateinit var llSaturday: LinearLayout
    private lateinit var tvSaturdaynumber: TextView
    private lateinit var llSunday: LinearLayout
    private lateinit var tvSundaynumber: TextView
    // Текстовый заголовок, который показывает отформатированную дату для выбранного дня (например, "Вчера" или "13 апр.")
    private lateinit var tvTitle: TextView
    private lateinit var tvCurrentMonth: TextView

    // Кнопки для переключения недель и перехода к выбору комплекта
    private lateinit var btnPrevWeek: ImageButton
    private lateinit var btnNextWeek: ImageButton
    private lateinit var btnCalendarAddOutfit: ImageButton

    // Контейнер для кнопки добавления (например, если нет запланированного комплекта)
    private lateinit var llAdd: LinearLayout
    // RecyclerView для отображения запланированных комплектов вместо HorizontalScrollView
    private lateinit var recyclerViewOutfits: RecyclerView
    // Адаптер для RecyclerView
    private lateinit var outfitAdapter: OutfitAdapter

    // Регистрируем ActivityResultLauncher для получения результата из OutfitSelectionActivity
    private val outfitSelectionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val selectedIds = result.data?.getIntegerArrayListExtra("selected_outfit_ids")
                if (selectedIds != null && selectedDate != null) {
                    saveDailyPlans(selectedDate!!, selectedIds)
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Раздуваем layout фрагмента (убедитесь, что в fragment_first.xml присутствует RecyclerView (id: recyclerViewOutfits)
        // и TextView для заголовка (id: tvTitle))
        val view = inflater.inflate(R.layout.fragment_first, container, false)

        // Инициализируем даты (текущая, выбранная – по умолчанию сегодня, и начало недели)
        currentDate = LocalDate.now()
        selectedDate = currentDate
        currentWeekStart = getStartOfWeek(selectedDate!!)

        // Находим по id элементы дней недели и остальные View
        llMonday = view.findViewById(R.id.llMonday)
        tvMondaynumber = view.findViewById(R.id.tvMondaynumber)
        llTuesday = view.findViewById(R.id.llTuesday)
        tvTuesdaynumber = view.findViewById(R.id.tvTuesdaynumber)
        llWednesday = view.findViewById(R.id.llWednesday)
        tvWednesdaynumber = view.findViewById(R.id.tvWednesdaynumber)
        llThursday = view.findViewById(R.id.llThursday)
        tvThursdaynumber = view.findViewById(R.id.tvThursdaynumber)
        llFriday = view.findViewById(R.id.llFriday)
        tvFridaynumber = view.findViewById(R.id.tvFridaynumber)
        llSaturday = view.findViewById(R.id.llSaturday)
        tvSaturdaynumber = view.findViewById(R.id.tvSaturdaynumber)
        llSunday = view.findViewById(R.id.llSunday)
        tvSundaynumber = view.findViewById(R.id.tvSundaynumber)
        tvCurrentMonth = view.findViewById(R.id.tvCurrentMonth)
        // Находим TextView для заголовка
        tvTitle = view.findViewById(R.id.tvTitle)

        btnCalendarAddOutfit = view.findViewById(R.id.btnCalendarAddOutfit)
        llAdd = view.findViewById(R.id.lladd)
        recyclerViewOutfits = view.findViewById(R.id.recyclerViewOutfits)

        // Инициализируем RecyclerView с горизонтальным менеджером компоновки и адаптером
        recyclerViewOutfits.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        outfitAdapter = OutfitAdapter(emptyList(), R.layout.item_clothing)
        recyclerViewOutfits.adapter = outfitAdapter

        // Находим кнопки для переключения недель (например, btnPrevDay и btnNextDay)
        btnPrevWeek = view.findViewById(R.id.btnPrevDay)
        btnNextWeek = view.findViewById(R.id.btnNextDay)

        // Обновляем название месяца по выбранной дате (полное название месяца)
        tvCurrentMonth.text = getMonthName(selectedDate!!.month.value)
        // Устанавливаем заголовок по выбранной дате
        updateTitle()

        // Устанавливаем обработчики кликов для контейнеров дней недели
        llMonday.setOnClickListener { onDayClicked(currentWeekStart!!) }
        llTuesday.setOnClickListener { onDayClicked(currentWeekStart!!.plusDays(1)) }
        llWednesday.setOnClickListener { onDayClicked(currentWeekStart!!.plusDays(2)) }
        llThursday.setOnClickListener { onDayClicked(currentWeekStart!!.plusDays(3)) }
        llFriday.setOnClickListener { onDayClicked(currentWeekStart!!.plusDays(4)) }
        llSaturday.setOnClickListener { onDayClicked(currentWeekStart!!.plusDays(5)) }
        llSunday.setOnClickListener { onDayClicked(currentWeekStart!!.plusDays(6)) }

        // Обработчики переключения недель
        btnPrevWeek.setOnClickListener {
            currentWeekStart = currentWeekStart!!.minusWeeks(1)
            updateWeekView()
        }
        btnNextWeek.setOnClickListener {
            currentWeekStart = currentWeekStart!!.plusWeeks(1)
            updateWeekView()
        }

        // Обработчик кнопки для перехода к выбору комплекта
        btnCalendarAddOutfit.setOnClickListener {
            val intent = Intent(requireContext(), OutfitSelectionActivity::class.java)
            outfitSelectionLauncher.launch(intent)
        }

        // Первоначальное заполнение недели
        updateWeekView()

        return view
    }

    /**
     * Обновляет отображение недели:
     * - Вычисляет даты каждого дня недели
     * - Обновляет текстовые значения для каждого дня в формате числовой даты (можете оставить как есть)
     * - Обновляет фон контейнеров для каждого дня
     * - Обновляет название месяца и заголовок (tvTitle) в зависимости от выбранного дня
     * - Загружает запланированные комплекты для выбранной даты
     */
    private fun updateWeekView() {
        val monday = currentWeekStart!!
        val tuesday = monday.plusDays(1)
        val wednesday = monday.plusDays(2)
        val thursday = monday.plusDays(3)
        val friday = monday.plusDays(4)
        val saturday = monday.plusDays(5)
        val sunday = monday.plusDays(6)

        // Можно оставить цифры в этих TextView или же заменить на любой другой формат
        tvMondaynumber.text = monday.dayOfMonth.toString()
        tvTuesdaynumber.text = tuesday.dayOfMonth.toString()
        tvWednesdaynumber.text = wednesday.dayOfMonth.toString()
        tvThursdaynumber.text = thursday.dayOfMonth.toString()
        tvFridaynumber.text = friday.dayOfMonth.toString()
        tvSaturdaynumber.text = saturday.dayOfMonth.toString()
        tvSundaynumber.text = sunday.dayOfMonth.toString()

        updateDayBackground(llMonday, monday)
        updateDayBackground(llTuesday, tuesday)
        updateDayBackground(llWednesday, wednesday)
        updateDayBackground(llThursday, thursday)
        updateDayBackground(llFriday, friday)
        updateDayBackground(llSaturday, saturday)
        updateDayBackground(llSunday, sunday)

        tvCurrentMonth.text = getMonthName(selectedDate!!.month.value)
        // Обновляем заголовок в зависимости от выбранного дня
        updateTitle()

        // Загружаем запланированные комплекты для выбранной даты
        checkOutfitPlanForDate(selectedDate!!)
    }

    /**
     * При клике по контейнеру дня устанавливается выбранная дата, после чего обновляется весь UI.
     */
    private fun onDayClicked(date: LocalDate) {
        selectedDate = date
        updateWeekView()
    }

    /**
     * Обновляет заголовок (tvTitle) с отформатированным текстом для выбранного дня.
     */
    private fun updateTitle() {
        // Используем метод форматирования: если выбранная дата равна сегодняшней, вчера или завтра – возвращает соответствующую строку,
        // иначе возвращает формат "13 апр."
        tvTitle.text = getFormattedDay(selectedDate!!)
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
            val dailyPlans = db.dailyPlanDao().getDailyPlansForDate(date.toString())
            val outfitIds = dailyPlans.map { it.outfitId }
            val outfits = db.outfitDao().getOutfitsByIds(outfitIds)
            withContext(Dispatchers.Main) {
                outfitAdapter.updateData(outfits)
            }
        }
    }

    /**
     * Сохраняет выбранные комплекты (DailyPlan) для указанной даты.
     */
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