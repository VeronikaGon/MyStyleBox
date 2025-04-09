package com.hfad.mystylebox

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
import com.hfad.mystylebox.database.AppDatabase
import com.hfad.mystylebox.database.ClothingItemWithTags
import com.hfad.mystylebox.databinding.ActivityFilterBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FilterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFilterBinding
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFilterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "wardrobe_db"
        )
            .allowMainThreadQueries()
            .build()

        val selectedGender = intent.getStringArrayListExtra("selectedGender") ?: emptyList()
        val selectedSeasons = intent.getStringArrayListExtra("selectedSeasons") ?: emptyList()
        val selectedSizes = intent.getStringArrayListExtra("selectedSizes") ?: emptyList()
        val selectedStatuses = intent.getStringArrayListExtra("selectedStatuses") ?: emptyList()
        val selectedTags = intent.getStringArrayListExtra("selectedTags") ?: emptyList()

        selectedGender.forEach {
            when (it) {
                "Женский" -> binding.cbWoman.isChecked = true
                "Мужской" -> binding.cbMan.isChecked = true
                "Универсально" -> binding.cbUniversal.isChecked = true
            }
        }

        selectedSeasons.forEach {
            when (it) {
                "Лето" -> binding.cbSummer.isChecked = true
                "Осень" -> binding.cbAutumn.isChecked = true
                "Зима" -> binding.cbWinter.isChecked = true
                "Весна" -> binding.cbSpring.isChecked = true
                "Без сезона" -> binding.cbNotSeasons.isChecked = true
            }
        }

        selectedSizes.forEach {
            when (it) {
                "XXS" -> binding.cbxxs.isChecked = true
                "XS" -> binding.cbxs.isChecked = true
                "S" -> binding.cbs.isChecked = true
                "M" -> binding.cbm.isChecked = true
                "L" -> binding.cbl.isChecked = true
                "XL" -> binding.cbxl.isChecked = true
                "XXL" -> binding.cbxxl.isChecked = true
                "XXXL" -> binding.cbxxxl.isChecked = true
                "33" -> binding.cb33.isChecked = true
                "34" -> binding.cb34.isChecked = true
                "35" -> binding.cb35.isChecked = true
                "36" -> binding.cb36.isChecked = true
                "37" -> binding.cb37.isChecked = true
                "38" -> binding.cb38.isChecked = true
                "39" -> binding.cb39.isChecked = true
                "40" -> binding.cb40.isChecked = true
                "41" -> binding.cb41.isChecked = true
                "42" -> binding.cb42.isChecked = true
                "43" -> binding.cb43.isChecked = true
                "44" -> binding.cb44.isChecked = true
                "45" -> binding.cb45.isChecked = true
                "46" -> binding.cb46.isChecked = true
                "47" -> binding.cb47.isChecked = true
                "Без размера" -> binding.cbNotSize.isChecked = true
            }
        }

        selectedStatuses.forEach {
            when (it) {
                "Активное использование" -> binding.cbactiveuse.isChecked = true
                "Нуждается в ремонте" -> binding.cbneedremont.isChecked = true
                "Резерв/Ожидание" -> binding.cbwaiting.isChecked = true
                "На продажу" -> binding.cbsale.isChecked = true
            }
        }

        loadTags()

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
                putStringArrayListExtra("selectedGender", arrayListOf())
                putStringArrayListExtra("selectedSeasons", arrayListOf())
                putStringArrayListExtra("selectedSizes", arrayListOf())
                putStringArrayListExtra("selectedStatuses", arrayListOf())
                putStringArrayListExtra("selectedTags", arrayListOf())
            })
            finish()
        }

        binding.blockSeason.setOnClickListener { toggleVisibility(binding.seasonContainer, binding.ivSeasonArrow) }
        binding.blockSize.setOnClickListener { toggleVisibility(binding.sizeContainer, binding.ivSizeArrow) }
        binding.blockStatus.setOnClickListener { toggleVisibility(binding.statusContainer, binding.ivStatusArrow) }
        binding.blockTags.setOnClickListener { toggleVisibility(binding.tagsContainer, binding.ivTagsArrow)}
        binding.blockGender.setOnClickListener { toggleVisibility(binding.genderContainer, binding.ivGenderArrow) }

        addCheckBoxListeners()

        binding.genderContainer.visibility = View.GONE
        binding.ivGenderArrow.setImageResource(R.drawable.ic_arrow_drop_down)
        binding.seasonContainer.visibility = View.GONE
        binding.ivSeasonArrow.setImageResource(R.drawable.ic_arrow_drop_down)
        binding.sizeContainer.visibility = View.GONE
        binding.ivSizeArrow.setImageResource(R.drawable.ic_arrow_drop_down)
        binding.statusContainer.visibility = View.GONE
        binding.ivStatusArrow.setImageResource(R.drawable.ic_arrow_drop_down)
        binding.tagsContainer.visibility = View.GONE
        binding.ivTagsArrow.setImageResource(R.drawable.ic_arrow_drop_down)

        binding.btnApply.visibility = View.VISIBLE
        binding.btnApply.setOnClickListener {
            applyFilters()
        }
    }
    // Метод загрузки тегов из БД и добавления чекбокса "Без тегов"
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
    // Добавляем слушателей для всех чекбоксов в контейнерах сезонов, размеров и статусов
    private fun addCheckBoxListeners() {
        val genderBoxes = listOf(binding.cbWoman, binding.cbMan, binding.cbUniversal)
        val seasonBoxes = listOf(binding.cbSummer, binding.cbAutumn, binding.cbWinter, binding.cbSpring,binding.cbNotSeasons )
        val sizeBoxes = listOf(
            binding.cbxxs, binding.cbxs, binding.cbs, binding.cbm, binding.cbl,
            binding.cbxl, binding.cbxxl, binding.cbxxxl, binding.cb33, binding.cb34,
            binding.cb35, binding.cb36, binding.cb37, binding.cb38, binding.cb39,
            binding.cb40, binding.cb41, binding.cb42, binding.cb43, binding.cb44,
            binding.cb45, binding.cb46, binding.cb47,binding.cbNotSize
        )
        val statusBoxes = listOf(binding.cbactiveuse, binding.cbneedremont, binding.cbwaiting, binding.cbsale)
        val allBoxes = genderBoxes + seasonBoxes + sizeBoxes + statusBoxes
        allBoxes.forEach { checkBox ->
            checkBox.setOnCheckedChangeListener { _, _ -> updateApplyButtonState() }
        }
    }
    private fun applyFilters() {
        val seasons = mutableListOf<String>().apply {
            if (binding.cbSummer.isChecked) add("Лето")
            if (binding.cbAutumn.isChecked) add("Осень")
            if (binding.cbWinter.isChecked) add("Зима")
            if (binding.cbSpring.isChecked) add("Весна")
            if (binding.cbNotSeasons.isChecked) add("Без сезона")
        }
        val sizes = mutableListOf<String>().apply {
            if (binding.cbxxs.isChecked) add("XXS")
            if (binding.cbxs.isChecked) add("XS")
            if (binding.cbs.isChecked) add("S")
            if (binding.cbm.isChecked) add("M")
            if (binding.cbl.isChecked) add("L")
            if (binding.cbxl.isChecked) add("XL")
            if (binding.cbxxl.isChecked) add("XXL")
            if (binding.cbxxxl.isChecked) add("XXXL")
            if (binding.cb33.isChecked) add("33")
            if (binding.cb34.isChecked) add("34")
            if (binding.cb35.isChecked) add("35")
            if (binding.cb36.isChecked) add("36")
            if (binding.cb37.isChecked) add("37")
            if (binding.cb38.isChecked) add("38")
            if (binding.cb39.isChecked) add("39")
            if (binding.cb40.isChecked) add("40")
            if (binding.cb41.isChecked) add("41")
            if (binding.cb42.isChecked) add("42")
            if (binding.cb43.isChecked) add("43")
            if (binding.cb44.isChecked) add("44")
            if (binding.cb45.isChecked) add("45")
            if (binding.cb46.isChecked) add("46")
            if (binding.cb47.isChecked) add("47")
            if (binding.cbNotSize.isChecked) add("Без размера")
        }
        val statuses = mutableListOf<String>().apply {
            if (binding.cbactiveuse.isChecked) add("Активное использование")
            if (binding.cbneedremont.isChecked) add("Нуждается в ремонте")
            if (binding.cbwaiting.isChecked) add("Резерв/Ожидание")
            if (binding.cbsale.isChecked) add("На продажу")
        }
        val tags = mutableListOf<String>().apply {
            for (i in 0 until binding.tagsContainer.childCount) {
                val view = binding.tagsContainer.getChildAt(i)
                if (view is CheckBox && view.isChecked) add(view.text.toString())
            }
        }
        val genders = mutableListOf<String>().apply {
            if (binding.cbWoman.isChecked) add("Женский")
            if (binding.cbMan.isChecked) add("Мужской")
            if (binding.cbUniversal.isChecked) add("Универсально")
        }
        val genderMatchFunc: (ClothingItemWithTags) -> Boolean = { itemWithTags ->
            val item = itemWithTags.clothingItem
            if (genders.isEmpty()) {
                true
            } else {
                genders.any { gender -> gender.equals(item.gender, ignoreCase = true) }
            }
        }
        CoroutineScope(Dispatchers.IO).launch {
            val allItems: List<ClothingItemWithTags> = db.clothingItemDao().getClothingItemsWithTags()
            val filteredItems = allItems.filter { itemWithTags: ClothingItemWithTags ->
                val item = itemWithTags.clothingItem
                val sizeMatch = if (sizes.isEmpty()) { true } else { sizes.any { size -> if (size == "Без размера") { item.size.isNullOrBlank() } else { size.equals(item.size, ignoreCase = true) } } }
                val statusMatch = statuses.isEmpty() || statuses.any { stat -> stat.equals(item.status, ignoreCase = true) }
                val seasonList = item.seasons?.map { it.trim().lowercase() } ?: emptyList()
                val seasonMatch = if (seasons.isEmpty()) { true } else { seasons.any { season -> if (season == "Без сезона") { seasonList.isEmpty() } else { seasonList.contains(season.trim().lowercase()) } }  }
                val itemTagNames = itemWithTags.tags?.map { tag -> tag.name.trim().lowercase() } ?: emptyList()
                val tagMatch = if (tags.contains("Без тегов")) { itemTagNames.isEmpty() } else { tags.isEmpty() || tags.map { it.trim().lowercase() }.any { tag -> itemTagNames.contains(tag) } }
                sizeMatch && statusMatch && seasonMatch && tagMatch && genderMatchFunc(itemWithTags)
            }
            withContext(Dispatchers.Main) {
                if (filteredItems.isEmpty()) {
                    Toast.makeText(this@FilterActivity, "Ничего не найдено", Toast.LENGTH_SHORT).show()
                } else {
                    val resultIntent = Intent().apply {
                        putStringArrayListExtra("selectedGender", ArrayList(genders))
                        putStringArrayListExtra("selectedSeasons", ArrayList(seasons))
                        putStringArrayListExtra("selectedSizes", ArrayList(sizes))
                        putStringArrayListExtra("selectedStatuses", ArrayList(statuses))
                        putStringArrayListExtra("selectedTags", ArrayList(tags))
                    }
                    setResult(Activity.RESULT_OK, resultIntent)
                    finish()
                }
            }
        }
    }
    private fun updateApplyButtonState() {
    }
    // Сброс всех выбранных фильтров
    private fun resetAllSelections() {
        resetCheckBoxes(binding.genderContainer)
        resetCheckBoxes(binding.seasonContainer)
        resetCheckBoxes(binding.sizeContainer)
        resetCheckBoxes(binding.statusContainer)
        resetCheckBoxes(binding.tagsContainer)
    }
    // Сбрасываем состояние всех чекбоксов в переданном контейнере
    private fun resetCheckBoxes(container: ViewGroup) {
        for (i in 0 until container.childCount) {
            val view = container.getChildAt(i)
            if (view is CheckBox) {
                view.isChecked = false
            }
        }
    }
    // Переключение видимости блока фильтров с изменением иконки стрелки
    private fun toggleVisibility(container: View, arrow: ImageView) {
        if (container.visibility == View.VISIBLE) {
            container.visibility = View.GONE
            arrow.setImageResource(R.drawable.ic_arrow_drop_down)
        } else {
            container.visibility = View.VISIBLE
            arrow.setImageResource(R.drawable.ic_arrow_drop_up)
        }
    }
}