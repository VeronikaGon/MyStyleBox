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

    // Объявляем View для дней недели и других элементов интерфейса
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
    private lateinit var tvCurrentMonth: TextView

    // Кнопки для переключения недель и кнопка для перехода к выбору комплекта
    private lateinit var btnPrevWeek: ImageButton
    private lateinit var btnNextWeek: ImageButton
    private lateinit var btnCalendarAddOutfit: ImageButton

    // Контейнеры для планирования комплекта
    private lateinit var llAdd: LinearLayout
    private lateinit var hsOutfit: HorizontalScrollView

    private lateinit var recyclerViewOutfits: RecyclerView
    private lateinit var outfitAdapter: OutfitAdapter

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
        val view = inflater.inflate(R.layout.fragment_first, container, false)

        currentDate = LocalDate.now()
        selectedDate = currentDate
        currentWeekStart = getStartOfWeek(selectedDate!!)

        // Находим элементы по id
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
        btnCalendarAddOutfit = view.findViewById(R.id.btnCalendarAddOutfit)
        llAdd = view.findViewById(R.id.lladd)
        hsOutfit = view.findViewById(R.id.hsoutfit)
        btnPrevWeek = view.findViewById(R.id.btnPrevDay)
        btnNextWeek = view.findViewById(R.id.btnNextDay)
        tvCurrentMonth.text = getMonthName(selectedDate!!.month.value)

        recyclerViewOutfits = view.findViewById(R.id.recyclerViewOutfits)

        // Инициализируем RecyclerView с горизонтальным менеджером компоновки
        recyclerViewOutfits.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        // Инициализируем адаптер. В конструкторе передаем пустой список и id layout для элемента (например, R.layout.item_clothing)
        outfitAdapter = OutfitAdapter(emptyList(), R.layout.item_clothing)
        recyclerViewOutfits.adapter = outfitAdapter

        // Находим кнопки для переключения недель (например, btnPrevDay и btnNextDay)
        btnPrevWeek = view.findViewById(R.id.btnPrevDay)
        btnNextWeek = view.findViewById(R.id.btnNextDay)

        // Обновляем название месяца по выбранной дате
        tvCurrentMonth.text = getMonthName(selectedDate!!.month.value)
        llMonday.setOnClickListener { onDayClicked(currentWeekStart!!) }
        llTuesday.setOnClickListener { onDayClicked(currentWeekStart!!.plusDays(1)) }
        llWednesday.setOnClickListener { onDayClicked(currentWeekStart!!.plusDays(2)) }
        llThursday.setOnClickListener { onDayClicked(currentWeekStart!!.plusDays(3)) }
        llFriday.setOnClickListener { onDayClicked(currentWeekStart!!.plusDays(4)) }
        llSaturday.setOnClickListener { onDayClicked(currentWeekStart!!.plusDays(5)) }
        llSunday.setOnClickListener { onDayClicked(currentWeekStart!!.plusDays(6)) }

        // Обработчики для переключения недель
        btnPrevWeek.setOnClickListener {
            currentWeekStart = currentWeekStart!!.minusWeeks(1)
            updateWeekView()
        }
        btnNextWeek.setOnClickListener {
            currentWeekStart = currentWeekStart!!.plusWeeks(1)
            updateWeekView()
        }

        // Обработчик для перехода к выбору комплекта
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
     * - Расчитывает даты для каждого дня недели
     * - Обновляет текстовое представление дня (число месяца)
     * - Обновляет фон контейнеров в зависимости от выбранного и текущей даты
     * - Обновляет название месяца
     */
    private fun updateWeekView() {
        // Вычисляем даты от понедельника до воскресенья
        val monday = currentWeekStart!!
        val tuesday = monday.plusDays(1)
        val wednesday = monday.plusDays(2)
        val thursday = monday.plusDays(3)
        val friday = monday.plusDays(4)
        val saturday = monday.plusDays(5)
        val sunday = monday.plusDays(6)

        // Обновляем текст номеров дня (число месяца)
        tvMondaynumber.text = monday.dayOfMonth.toString()
        tvTuesdaynumber.text = tuesday.dayOfMonth.toString()
        tvWednesdaynumber.text = wednesday.dayOfMonth.toString()
        tvThursdaynumber.text = thursday.dayOfMonth.toString()
        tvFridaynumber.text = friday.dayOfMonth.toString()
        tvSaturdaynumber.text = saturday.dayOfMonth.toString()
        tvSundaynumber.text = sunday.dayOfMonth.toString()

        // Обновляем фон для каждого контейнера в зависимости от выбранной даты
        updateDayBackground(llMonday, monday)
        updateDayBackground(llTuesday, tuesday)
        updateDayBackground(llWednesday, wednesday)
        updateDayBackground(llThursday, thursday)
        updateDayBackground(llFriday, friday)
        updateDayBackground(llSaturday, saturday)
        updateDayBackground(llSunday, sunday)

        // Обновляем название месяца (берем месяц выбранной даты)
        tvCurrentMonth.text = getMonthName(selectedDate!!.month.value)

        // Перепроверяем, запланирован ли комплект для выбранной даты (асинхронный запрос к БД)
        checkOutfitPlanForDate(selectedDate!!)
    }

    /**
     * Обработчик клика по дню.
     * Устанавливает clicked day как выбранную дату и обновляет интерфейс.
     */
    private fun onDayClicked(date: LocalDate) {
        selectedDate = date
        updateWeekView()
    }
    /**
     * Метод для асинхронной проверки, запланирован ли комплект для заданной даты.
     * После получения результата обновляет видимость контейнеров.
     */
    private fun checkOutfitPlanForDate(date: LocalDate) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getInstance(requireContext())
            // Пример запроса: получаем все планы для данной даты
            val dailyPlans = db.dailyPlanDao().getDailyPlansForDate(date.toString())
            withContext(Dispatchers.Main) {
                if (dailyPlans.isNotEmpty()) {
                    llAdd.visibility = View.GONE
                    hsOutfit.visibility = View.VISIBLE
                } else {
                    llAdd.visibility = View.VISIBLE
                    hsOutfit.visibility = View.GONE
                }
            }
        }
    }
    /**
     * Метод для сохранения выбранных комплектов в базу данных.
     * Для каждого выбранного id создается запись в таблице daily_plan с соответствующей датой.
     */
    private fun saveDailyPlans(date: LocalDate, selectedIds: List<Int>) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getInstance(requireContext())
            selectedIds.forEach { outfitId ->
                val dailyPlan = DailyPlan(date.toString(),outfitId)
                db.dailyPlanDao().insert(dailyPlan)
            }
            // После вставки записей обновляем UI на основном потоке
            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), "Комплект(ы) запланированы!", Toast.LENGTH_SHORT).show()
                // Обновляем вид, чтобы отобразить изменения для выбранной даты.
                checkOutfitPlanForDate(date)
            }
        }
    }
    /**
     * Обновляет фон контейнера для дня:
     * - Если дата равна выбранной (selectedDate) — ставим активный фон.
     * - Если дата равна текущей (currentDate) — ставим фон для сегодняшнего дня.
     * - Иначе убираем фон.
     */
    private fun updateDayBackground(container: LinearLayout, date: LocalDate) {
        when {
            selectedDate == date -> {
                container.setBackgroundResource(R.drawable.item_background_active)
            }
            date == currentDate -> {
                container.setBackgroundResource(R.drawable.item_active_today)
            }
            else -> {
                container.setBackgroundResource(0)
            }
        }
    }

    /**
     * Проверяет, запланирован ли комплект для заданной даты.
     * Здесь можно реализовать запрос в базу данных.
     * Для демонстрации вернем true, если число месяца равно 11.
     */
    private fun isOutfitPlannedForDate(date: LocalDate): Boolean {
        return date.dayOfMonth == 11
    }

    /**
     * Возвращает название месяца по его номеру.
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
     * В ISO неделя начинается с понедельника.
     */
    private fun getStartOfWeek(date: LocalDate): LocalDate {
        return date.minusDays((date.dayOfWeek.value - 1).toLong())
    }
}