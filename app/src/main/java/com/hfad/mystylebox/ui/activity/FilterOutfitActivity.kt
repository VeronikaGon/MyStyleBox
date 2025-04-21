    package com.hfad.mystylebox.ui.activity

    import android.app.Activity
    import android.content.Intent
    import android.os.Bundle
    import android.util.TypedValue
    import android.view.Gravity
    import android.view.View
    import android.view.ViewGroup
    import android.widget.CheckBox
    import android.widget.ImageView
    import android.widget.Toast
    import androidx.appcompat.app.AppCompatActivity
    import androidx.room.Room
    import com.hfad.mystylebox.R
    import com.hfad.mystylebox.database.AppDatabase
    import com.hfad.mystylebox.database.OutfitWithTags
    import com.hfad.mystylebox.databinding.ActivityFilterOutfitBinding
    import kotlinx.coroutines.CoroutineScope
    import kotlinx.coroutines.Dispatchers
    import kotlinx.coroutines.launch
    import kotlinx.coroutines.withContext

    class FilterOutfitActivity : AppCompatActivity() {
        private lateinit var binding: ActivityFilterOutfitBinding
        private lateinit var db: AppDatabase

        private val rangeHeat = Pair(35, 100)         // "Жара 35°C" – от 35 и выше
        private val rangeHot = Pair(27, 34)             // "Жарко 27 ... 34°C"
        private val rangeWarm = Pair(20, 26)            // "Тепло 20 ... 26°C"
        private val rangeCool = Pair(10, 19)            // "Прохладно 10 ... 19°C"
        private val rangeCold = Pair(-5, 9)             // "Холодно -5 ... 9°C"
        private val rangeFrost = Pair(-50, -6)          // "Мороз  -6°C" (условно от -50 до -6)
        private val NOT_TEMPERATURE = -99

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            binding = ActivityFilterOutfitBinding.inflate(layoutInflater)
            setContentView(binding.root)

            db = Room.databaseBuilder(
                applicationContext,
                AppDatabase::class.java,
                "wardrobe_db"
            )
                .allowMainThreadQueries()
                .build()

            // Восстанавливаем ранее выбранные сезоны и теги, если таковые переданы
            val selectedSeasons = intent.getStringArrayListExtra("selectedSeasons") ?: emptyList()
            val selectedTags = intent.getStringArrayListExtra("selectedTags") ?: emptyList()
            val selectedTemperature = intent.getIntExtra("selectedTemperature", Int.MIN_VALUE)
            val selectedNotTemperature = intent.getBooleanExtra("selectedNotTemperature", false)

            // Отмечаем чекбоксы сезонов по переданным значениям
            selectedSeasons.forEach {
                when (it) {
                    "Лето" -> binding.cbSummer.isChecked = true
                    "Осень" -> binding.cbAutumn.isChecked = true
                    "Зима" -> binding.cbWinter.isChecked = true
                    "Весна" -> binding.cbSpring.isChecked = true
                    "Без сезона" -> binding.cbNotSeasons.isChecked = true
                }
            }

            if (selectedNotTemperature)
                binding.cbNotTemperature.isChecked = true

            if (selectedTemperature != Int.MIN_VALUE) {
                if (selectedTemperature in rangeHeat.first..rangeHeat.second) binding.cbHeat.isChecked = true
                if (selectedTemperature in rangeHot.first..rangeHot.second) binding.cbHot.isChecked = true
                if (selectedTemperature in rangeWarm.first..rangeWarm.second) binding.cbWarm.isChecked = true
                if (selectedTemperature in rangeCool.first..rangeCool.second) binding.cbCool.isChecked = true
                if (selectedTemperature in rangeCold.first..rangeCold.second) binding.cbCold.isChecked = true
                if (selectedTemperature in rangeFrost.first..rangeFrost.second) binding.cbFrost.isChecked = true
            }
            // Загружаем теги из базы данных и создаём чекбоксы динамически
            loadTags()

            // Восстанавливаем выбранные теги по наименованию
            selectedTags.forEach { tag ->
                (0 until binding.tagsContainer.childCount)
                    .map { binding.tagsContainer.getChildAt(it) }
                    .filterIsInstance<CheckBox>()
                    .firstOrNull { it.text.toString() == tag }
                    ?.isChecked = true
            }

            binding.btnReset.setOnClickListener {
                resetAllSelections()
                setResult(Activity.RESULT_OK, Intent().apply {
                    putStringArrayListExtra("selectedSeasons", arrayListOf())
                    putStringArrayListExtra("selectedTags", arrayListOf())
                })
                finish()
            }

            // Слушатели для переключения видимости блоков фильтров
            binding.blockSeason.setOnClickListener { toggleVisibility(binding.seasonContainer, binding.ivSeasonArrow) }
            binding.blockTemperature.setOnClickListener { toggleVisibility(binding.temperatureContainer, binding.ivTemperatureArrow) }
            binding.blockTags.setOnClickListener { toggleVisibility(binding.tagsContainer, binding.ivTagsArrow) }

            // По умолчанию скрываем контейнеры фильтров
            binding.seasonContainer.visibility = View.GONE
            binding.ivSeasonArrow.setImageResource(R.drawable.ic_arrow_drop_down)
            binding.temperatureContainer.visibility = View.GONE
            binding.ivTemperatureArrow.setImageResource(R.drawable.ic_arrow_drop_down)
            binding.tagsContainer.visibility = View.GONE
            binding.ivTagsArrow.setImageResource(R.drawable.ic_arrow_drop_down)

            addCheckBoxListeners()

            binding.btnApply.setOnClickListener {
                applyFilters()
            }
        }

        // Загрузка тегов из базы данных и динамическое добавление чекбоксов
        private fun loadTags() {
            val tags = db.tagDao().getAllTags()
            binding.tagsContainer.removeAllViews()

            tags.forEach { tag ->
                val checkBox = CheckBox(this).apply {
                    text = tag.name
                    setBackgroundResource(R.drawable.checkbox_background)
                    setButtonDrawable(null)
                    setPadding(16, 16, 16, 16)
                    gravity = Gravity.CENTER
                }
                val layoutParams = ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, 8f, resources.displayMetrics
                    ).toInt()
                    marginEnd = TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, 8f, resources.displayMetrics
                    ).toInt()
                }
                checkBox.layoutParams = layoutParams
                checkBox.setOnCheckedChangeListener { _, _ -> updateApplyButtonState() }
                binding.tagsContainer.addView(checkBox)
            }
            // Добавляем чекбокс "Без тегов"
            val noTagCheckBox = CheckBox(this).apply {
                text = "Без тегов"
                setBackgroundResource(R.drawable.checkbox_background)
                setButtonDrawable(null)
                setPadding(16, 16, 16, 16)
                gravity = Gravity.CENTER
                setOnCheckedChangeListener { _, _ -> updateApplyButtonState() }
            }
            val noTagLayoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 8f, resources.displayMetrics
                ).toInt()
                marginEnd = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 8f, resources.displayMetrics
                ).toInt()
            }
            noTagCheckBox.layoutParams = noTagLayoutParams
            binding.tagsContainer.addView(noTagCheckBox)
        }

        // Добавление слушателей для чекбоксов в контейнерах сезонов и температурных диапазонов
        private fun addCheckBoxListeners() {
            val seasonBoxes = listOf(
                binding.cbSummer, binding.cbAutumn,
                binding.cbWinter, binding.cbSpring, binding.cbNotSeasons
            )
            val temperatureBoxes = listOf(
                binding.cbHeat, binding.cbHot, binding.cbWarm,
                binding.cbCool, binding.cbCold, binding.cbFrost
            )
            (seasonBoxes + temperatureBoxes).forEach { checkBox ->
                checkBox.setOnCheckedChangeListener { _, _ -> updateApplyButtonState() }
            }
        }

        // Основной метод фильтрации: по сезонам, температуре и тегам.
        private fun applyFilters() {
            val seasons = mutableListOf<String>().apply {
                if (binding.cbSummer.isChecked) add("Лето")
                if (binding.cbAutumn.isChecked) add("Осень")
                if (binding.cbWinter.isChecked) add("Зима")
                if (binding.cbSpring.isChecked) add("Весна")
                if (binding.cbNotSeasons.isChecked) add("Без сезона")
            }
            val selectedTempRanges = mutableListOf<Pair<Int, Int>>().apply {
                if (!binding.cbNotTemperature.isChecked) {
                    if (binding.cbHeat.isChecked) add(rangeHeat)
                    if (binding.cbHot.isChecked) add(rangeHot)
                    if (binding.cbWarm.isChecked) add(rangeWarm)
                    if (binding.cbCool.isChecked) add(rangeCool)
                    if (binding.cbCold.isChecked) add(rangeCold)
                    if (binding.cbFrost.isChecked) add(rangeFrost)
                }
            }
            val selectedTags = mutableListOf<String>().apply {
                for (i in 0 until binding.tagsContainer.childCount) {
                    val view = binding.tagsContainer.getChildAt(i)
                    if (view is CheckBox && view.isChecked)
                        add(view.text.toString())
                }
            }

            CoroutineScope(Dispatchers.IO).launch {
                val allOutfitsWithTags: List<OutfitWithTags> = db.outfitDao().getAllOutfitsWithTags()
                val filteredOutfits = allOutfitsWithTags.filter { outfitWithTags ->
                    val outfit = outfitWithTags.outfit

                    val outfitSeasons = outfit.seasons?.map { it.trim().lowercase() } ?: emptyList()
                    val seasonMatch = if (seasons.isEmpty()) true
                    else seasons.any { season ->
                        if (season == "Без сезона") outfitSeasons.isEmpty()
                        else outfitSeasons.contains(season.trim().lowercase())
                    }

                    val temperatureMatch = when {
                        binding.cbNotTemperature.isChecked ->
                            outfit.minTemp == NOT_TEMPERATURE && outfit.maxTemp == NOT_TEMPERATURE
                        selectedTempRanges.isEmpty() -> true
                        else -> selectedTempRanges.any { range ->
                            rangesIntersect(outfit.minTemp, outfit.maxTemp, range.first, range.second)
                        }
                    }
                    val outfitTagNames = outfitWithTags.tags.map { it.name.trim().lowercase() }
                    val tagMatch = when {
                        selectedTags.contains("Без тегов") -> outfitTagNames.isEmpty()
                        selectedTags.isNotEmpty() -> selectedTags
                            .filter { it != "Без тегов" }
                            .map { it.trim().lowercase() }
                            .all { tag -> outfitTagNames.contains(tag) }
                        else -> true
                    }

                    seasonMatch && temperatureMatch && tagMatch
                }
                withContext(Dispatchers.Main) {
                    if (filteredOutfits.isEmpty()) {
                        Toast.makeText(this@FilterOutfitActivity, "Ничего не найдено", Toast.LENGTH_SHORT).show()
                    } else {
                        val resultIntent = Intent().apply {
                            putStringArrayListExtra("selectedSeasons", ArrayList(seasons))
                            putStringArrayListExtra("selectedTags", ArrayList(selectedTags))
                        }
                        if (selectedTempRanges.isNotEmpty()) {
                            val tempValue = (selectedTempRanges.first().first + selectedTempRanges.first().second) / 2
                            resultIntent.putExtra("selectedTemperature", tempValue)
                        }
                        if (binding.cbNotTemperature.isChecked) {
                            resultIntent.putExtra("selectedNotTemperature", true)
                        }
                        setResult(Activity.RESULT_OK, resultIntent)
                        finish()
                    }
                }
            }
        }

        // Вспомогательная функция для проверки пересечения двух диапазонов температур.
        private fun rangesIntersect(min1: Int, max1: Int, min2: Int, max2: Int): Boolean {
            return max1 >= min2 && max2 >= min1
        }

        // Метод для обновления состояния кнопки "Применить" – можно реализовать по необходимости.
        private fun updateApplyButtonState() {
            // Например, можно сделать кнопку активной при изменении состояния хотя бы одного чекбокса.
        }

        // Сбрасываем все выбранные фильтры.
        private fun resetAllSelections() {
            resetCheckBoxes(binding.seasonContainer)
            resetCheckBoxes(binding.temperatureContainer)
            resetCheckBoxes(binding.tagsContainer)
        }

        // Сброс состояния всех чекбоксов в переданном контейнере.
        private fun resetCheckBoxes(container: ViewGroup) {
            for (i in 0 until container.childCount) {
                val view = container.getChildAt(i)
                if (view is CheckBox) {
                    view.isChecked = false
                }
            }
        }

        // Переключение видимости контейнера фильтров с изменением иконки стрелки.
        private fun toggleVisibility(container: View, arrow: ImageView) {
            if (container.visibility == View.VISIBLE) {
                container.visibility = View.GONE
                when (arrow.id) {
                    binding.ivSeasonArrow.id -> arrow.setImageResource(R.drawable.ic_arrow_drop_down)
                    binding.ivTemperatureArrow.id -> arrow.setImageResource(R.drawable.ic_arrow_drop_down)
                    binding.ivTagsArrow.id -> arrow.setImageResource(R.drawable.ic_arrow_drop_down)
                }
            } else {
                container.visibility = View.VISIBLE
                when (arrow.id) {
                    binding.ivSeasonArrow.id -> arrow.setImageResource(R.drawable.ic_arrow_drop_up)
                    binding.ivTemperatureArrow.id -> arrow.setImageResource(R.drawable.ic_arrow_drop_up)
                    binding.ivTagsArrow.id -> arrow.setImageResource(R.drawable.ic_arrow_drop_up)
                }
            }
        }
    }