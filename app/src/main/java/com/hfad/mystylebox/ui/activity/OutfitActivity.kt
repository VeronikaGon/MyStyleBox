package com.hfad.mystylebox.ui.activity

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.slider.RangeSlider
import com.hfad.mystylebox.MainActivity
import com.hfad.mystylebox.R
import com.hfad.mystylebox.database.AppDatabase
import com.hfad.mystylebox.database.dao.ClothingItemDao
import com.hfad.mystylebox.database.entity.Outfit
import com.hfad.mystylebox.database.entity.OutfitClothingItem
import com.hfad.mystylebox.database.dao.OutfitDao
import com.hfad.mystylebox.database.entity.OutfitTag
import com.hfad.mystylebox.database.dao.OutfitTagDao
import com.hfad.mystylebox.database.entity.Tag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OutfitActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_CODE_EDIT_IMAGE = 2001
        private const val REQUEST_CODE_EDIT_TAGS  = 2002
        private const val REQUEST_CODE_EDIT_BOARD = 3001
    }

    private lateinit var db: AppDatabase
    private lateinit var outfitDao: OutfitDao
    private lateinit var outfitTagDao: OutfitTagDao
    private lateinit var clothingItemDao: ClothingItemDao

    private lateinit var outfitNameEditText: EditText
    private lateinit var outfitDescriptionEditText: EditText
    private lateinit var btnSave: Button
    private lateinit var editImageButton: ImageButton
    private lateinit var outfitImageView: ImageView
    private var isFromBoard: Boolean = false

    private var ImagePath: String? = null
    private var isInEditMode: Boolean = false
    private var currentOutfit: Outfit? = null
    private var isUpdatingFromCode: Boolean = false
    private var isCheckboxUpdating: Boolean = false
    private lateinit var cbWeather: CheckBox
    private lateinit var cbHeat: CheckBox
    private lateinit var cbHot: CheckBox
    private lateinit var cbWarm: CheckBox
    private lateinit var cbCool: CheckBox
    private lateinit var cbCold: CheckBox
    private lateinit var cbFrost: CheckBox
    private lateinit var cbSummer: CheckBox
    private lateinit var cbSpring: CheckBox
    private lateinit var cbWinter: CheckBox
    private lateinit var cbAutumn: CheckBox
    private lateinit var tvTemp: TextView
    private lateinit var rsWeather: RangeSlider
    private lateinit var fbWeatherChecks:FlexboxLayout
    private lateinit var saveButton: Button
    private lateinit var flexboxTags: FlexboxLayout
    private var selectedTagIds: MutableSet<Int> = mutableSetOf()
    private lateinit var tagEditingLauncher: ActivityResultLauncher<Intent>
    private var selectedTags: List<Tag> = listOf()
    private var selectedClothingItemIds: MutableList<Int> = mutableListOf()
    private lateinit var LL:LinearLayout
    private var selectedClothingImagePaths: MutableList<String> = mutableListOf()
    private var savedItemStatesJson: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_outfit)

        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "wardrobe_db"
        ).allowMainThreadQueries().build()
        outfitDao      = db.outfitDao()
        outfitTagDao   = db.outfitTagDao()
        clothingItemDao= db.clothingItemDao()

        flexboxTags = findViewById(R.id.Tags)
        outfitNameEditText = findViewById(R.id.enterName)
        outfitDescriptionEditText = findViewById(R.id.enterNotes)
        tvTemp = findViewById(R.id.tvTemp)
        rsWeather = findViewById(R.id.rsweather)
        btnSave = findViewById(R.id.ButtonSAVE)
        cbSummer = findViewById(R.id.cbSummer)
        cbSpring = findViewById(R.id.cbSpring)
        cbWinter = findViewById(R.id.cbWinter)
        cbAutumn = findViewById(R.id.cbAutumn)
        cbWeather = findViewById(R.id.cbweather)
        cbHeat = findViewById(R.id.cbHeat)
        cbHot = findViewById(R.id.cbHot)
        cbWarm = findViewById(R.id.cbWarm)
        cbCool = findViewById(R.id.cbCool)
        cbCold = findViewById(R.id.cbCold)
        cbFrost = findViewById(R.id.cbFrost)
        tvTemp = findViewById(R.id.tvTemp)
        rsWeather = findViewById(R.id.rsweather)
        fbWeatherChecks = findViewById(R.id.fbWeatherChecks)
        cbSummer = findViewById(R.id.cbSummer)
        cbSpring = findViewById(R.id.cbSpring)
        cbWinter = findViewById(R.id.cbWinter)
        cbAutumn = findViewById(R.id.cbAutumn)
        saveButton = findViewById(R.id.ButtonSAVE)
        outfitImageView           = findViewById(R.id.outfitImageView)
        editImageButton = findViewById(R.id.editimage)
        LL = findViewById(R.id.ll)

        isFromBoard = intent.getBooleanExtra("fromBoard", false)
        currentOutfit = intent.getParcelableExtra("outfit")

        if (savedInstanceState == null) {
            if (currentOutfit != null) {
                isInEditMode = true
                populateFields(currentOutfit!!)
                btnSave.text = "Обновить"
            }
            else if (isFromBoard) {
                btnSave.text = "Сохранить"
                outfitNameEditText.setText("Комплект")
                val imagePath = intent.getStringExtra("imagePath")
                val ids       = intent.getIntegerArrayListExtra("selected_item_ids")
                val paths     = intent.getStringArrayListExtra("selected_image_paths")
                val statesJson = intent.getStringExtra("item_states_json")

                if (!imagePath.isNullOrEmpty()) {
                    ImagePath = imagePath
                    outfitImageView.setImageBitmap(BitmapFactory.decodeFile(imagePath))
                }
                if (ids != null && paths != null) {
                    selectedClothingItemIds.clear()
                    selectedClothingItemIds.addAll(ids)
                    selectedClothingImagePaths.clear()
                    selectedClothingImagePaths.addAll(paths)
                    savedItemStatesJson = statesJson
                }
            }
            else {
                btnSave.text = "Сохранить"
                savedItemStatesJson = null
            }
        }

        tagEditingLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val returnedTags = result.data?.getParcelableArrayListExtra<Tag>("selected_tags")
                if (returnedTags != null) {
                    selectedTagIds.clear()
                    selectedTags = returnedTags
                    returnedTags.forEach { tag -> selectedTagIds.add(tag.id) }
                    loadTagsFromDatabase()
                }
            }
        }

        editImageButton.setOnClickListener {
            if (selectedClothingImagePaths.isNotEmpty()) {
                val intent = Intent(this, BoardActivity::class.java).apply {
                    if (!savedItemStatesJson.isNullOrEmpty()) {
                        putExtra("item_states_json", savedItemStatesJson)
                    }
                    putStringArrayListExtra("selected_image_paths", ArrayList(selectedClothingImagePaths))
                    putIntegerArrayListExtra("selected_item_ids", ArrayList(selectedClothingItemIds))
                    putExtra("launchedFromForm", true)
                }
                startActivityForResult(intent, REQUEST_CODE_EDIT_BOARD)
            } else {
                Toast.makeText(this, "Сначала соберите комплект через доску", Toast.LENGTH_SHORT).show()
            }
        }

        if (isInEditMode && currentOutfit != null
            && currentOutfit!!.minTemp != -99 && currentOutfit!!.maxTemp != -99
        ) {
            isUpdatingFromCode = true
            rsWeather.setValues(
                currentOutfit!!.minTemp.toFloat(),
                currentOutfit!!.maxTemp.toFloat()
            )
            isUpdatingFromCode = false
            cbWeather.isChecked = true
            setWeatherVisibility(true)
            tvTemp.text = "${currentOutfit!!.minTemp} ... ${currentOutfit!!.maxTemp}°C"
        } else {
            isUpdatingFromCode = true
            rsWeather.setValues(-50f, 50f)
            isUpdatingFromCode = false
            cbWeather.isChecked = false
            setWeatherVisibility(false)
            tvTemp.text = "${rsWeather.values.first().toInt()} ... ${rsWeather.values.last().toInt()}°C"
        }

        btnSave.setOnClickListener {
            if (outfitNameEditText.text.toString().trim().isEmpty()) {
                Toast.makeText(this, "Введите название комплекта", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            saveOrUpdateOutfit()
        }

        cbWeather.buttonTintList = ColorStateList.valueOf(Color.parseColor("#FFB5A7"))
        cbWeather.setOnCheckedChangeListener { _, isChecked ->
            setWeatherVisibility(isChecked)
            if (isChecked) {
                val curMin = rsWeather.values.first().toInt()
                val curMax = rsWeather.values.last().toInt()
                updateWeatherCheckboxes(curMin, curMax)
            }
        }

        tvTemp.text = "${rsWeather.values.first().toInt()} ... ${rsWeather.values.last().toInt()}°C"
        rsWeather.addOnChangeListener { slider, _, fromUser ->
            if (isUpdatingFromCode) return@addOnChangeListener
            if (fromUser) {
                val sliderValues = slider.values.map { it.toInt() }
                tvTemp.text = "${sliderValues.first()} ... ${sliderValues.last()}°C"
                updateWeatherCheckboxes(sliderValues.first(), sliderValues.last())
            }
        }

        findViewById<ImageButton>(R.id.btnplus).setOnClickListener {
            if (!isUpdatingFromCode) {
                val lower = rsWeather.values[0]
                val upper = rsWeather.values[1]
                if (upper < rsWeather.valueTo) {
                    val newUpper = upper + 1
                    isUpdatingFromCode = true
                    rsWeather.setValues(lower, newUpper)
                    isUpdatingFromCode = false
                    tvTemp.text = "${lower.toInt()} ... ${newUpper.toInt()}°C"
                    updateWeatherCheckboxes(lower.toInt(), newUpper.toInt())
                }
            }
        }
        findViewById<ImageButton>(R.id.btnminus).setOnClickListener {
            if (!isUpdatingFromCode) {
                val lower = rsWeather.values[0]
                val upper = rsWeather.values[1]
                if (lower > rsWeather.valueFrom) {
                    val newLower = lower - 1
                    isUpdatingFromCode = true
                    rsWeather.setValues(newLower, upper)
                    isUpdatingFromCode = false
                    tvTemp.text = "${newLower.toInt()} ... ${upper.toInt()}°C"
                    updateWeatherCheckboxes(newLower.toInt(), upper.toInt())
                }
            }
        }

        // 11. Слушатели для чекбоксов температуры
        val checkBoxListener = CompoundButton.OnCheckedChangeListener { _, isChecked ->
            if (isCheckboxUpdating) return@OnCheckedChangeListener
            val totalSelected = countSelectedRanges()
            if (!isChecked && totalSelected == 0) {
                Toast.makeText(this, "Должен быть выбран хотя бы один диапазон погоды", Toast.LENGTH_SHORT).show()
                return@OnCheckedChangeListener
            }
            adjustSliderRange()
        }
        cbHeat.setOnCheckedChangeListener(checkBoxListener)
        cbHot.setOnCheckedChangeListener(checkBoxListener)
        cbWarm.setOnCheckedChangeListener(checkBoxListener)
        cbCool.setOnCheckedChangeListener(checkBoxListener)
        cbCold.setOnCheckedChangeListener(checkBoxListener)
        cbFrost.setOnCheckedChangeListener(checkBoxListener)

        val initMin = rsWeather.values.first().toInt()
        val initMax = rsWeather.values.last().toInt()
        updateWeatherCheckboxes(initMin, initMax)
        loadTagsFromDatabase()
    }

    // Метод для загрузки тегов из базы данных и выделения тех, которые связаны с текущим комплектом
    private fun loadTagsFromDatabase() {
        lifecycleScope.launch(Dispatchers.IO) {
            val allTags = db.tagDao().getAllTags()
            val preselectedIds = currentOutfit?.let { outfit ->
                outfitTagDao.getTagsForOutfit(outfit.id).map { it.id }.toSet()
            } ?: emptySet()
            selectedTagIds.clear()
            selectedTagIds.addAll(preselectedIds)
            withContext(Dispatchers.Main) {
                displayTagsAsCheckboxes(allTags, selectedTagIds)
            }
        }
    }

    // Обработка результата от BoardActivity (редактирование картинки)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_EDIT_BOARD && resultCode == Activity.RESULT_OK) {
            val newImagePath = data?.getStringExtra("imagePath")
            val newIds = data?.getIntegerArrayListExtra("selected_item_ids")
            val newPaths = data?.getStringArrayListExtra("selected_image_paths")
            val newStatesJson = data?.getStringExtra("item_states_json")

            if (!newImagePath.isNullOrEmpty()) {
                ImagePath = newImagePath
                outfitImageView.setImageBitmap(BitmapFactory.decodeFile(newImagePath))
            }
            if (newIds != null && newPaths != null) {
                selectedClothingItemIds.clear()
                selectedClothingItemIds.addAll(newIds)
                selectedClothingImagePaths.clear()
                selectedClothingImagePaths.addAll(newPaths)
                savedItemStatesJson = newStatesJson
            }
        }
    }


    // Метод для отображения тегов в виде CheckBox внутри FlexboxLayout
    private fun displayTagsAsCheckboxes(allTags: List<Tag>, preselectedTagIds: Set<Int> = emptySet()) {
        flexboxTags.removeAllViews()
        allTags.forEach { tag ->
            val checkBox = CheckBox(this).apply {
                text = tag.name
                id = tag.id
                setBackgroundResource(R.drawable.checkbox_background)
                setButtonDrawable(null)
                setPadding(16, 5, 16, 5)
                isChecked = tag.id in preselectedTagIds
            }
            checkBox.setOnClickListener {
                if (checkBox.isChecked) selectedTagIds.add(tag.id)
                else selectedTagIds.remove(tag.id)
            }
            val params = FlexboxLayout.LayoutParams(
                FlexboxLayout.LayoutParams.WRAP_CONTENT,
                FlexboxLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(5, 5, 5, 5) }
            flexboxTags.addView(checkBox, params)
        }
    }

    // Метод для заполнения полей при редактировании комплекта
    private fun populateFields(outfit: Outfit) {
        outfitNameEditText.setText(outfit.name)
        outfitDescriptionEditText.setText(outfit.description)
        tvTemp.text = "${outfit.minTemp} ... ${outfit.maxTemp}°C"
        if (outfit.minTemp == -99 || outfit.maxTemp == -99) {
            isUpdatingFromCode = true
            rsWeather.setValues(-50f, 50f)
            isUpdatingFromCode = false
            cbWeather.isChecked = false
            setWeatherVisibility(false)
        } else {
            isUpdatingFromCode = true
            rsWeather.setValues(outfit.minTemp.toFloat(), outfit.maxTemp.toFloat())
            isUpdatingFromCode = false
            cbWeather.isChecked = true
            setWeatherVisibility(true)
        }

        outfit.seasons?.let { seasons ->
            cbSummer.isChecked = seasons.contains("Лето")
            cbSpring.isChecked = seasons.contains("Весна")
            cbWinter.isChecked = seasons.contains("Зима")
            cbAutumn.isChecked = seasons.contains("Осень")
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val savedTagIds = outfitTagDao.getTagsForOutfit(outfit.id).map { it.id }.toSet()
            selectedTagIds.clear()
            selectedTagIds.addAll(savedTagIds)
            val allTags = db.tagDao().getAllTags()
            withContext(Dispatchers.Main) {
                displayTagsAsCheckboxes(allTags, selectedTagIds)
            }
        }
    }

    // Метод для сохранения или обновления комплекта
    private fun saveOrUpdateOutfit() {
        val name = outfitNameEditText.text.toString().trim()
        if (name.isEmpty()) {
            Toast.makeText(this, "Введите название комплекта", Toast.LENGTH_SHORT).show()
            return
        }
        val description = outfitDescriptionEditText.text.toString().trim()
        val seasons = mutableListOf<String>()
        if (cbSummer.isChecked) seasons.add("Лето")
        if (cbSpring.isChecked) seasons.add("Весна")
        if (cbWinter.isChecked) seasons.add("Зима")
        if (cbAutumn.isChecked) seasons.add("Осень")
        val minTemp = if (cbWeather.isChecked) rsWeather.values.first().toInt() else -99
        val maxTemp = if (cbWeather.isChecked) rsWeather.values.last().toInt() else -99

        val outfit = if (isInEditMode && currentOutfit != null) {
            currentOutfit!!.apply {
                this.name = name
                this.description = description
                this.seasons = seasons
                this.minTemp = minTemp
                this.maxTemp = maxTemp
                this.imagePath = ImagePath ?: ""
            }
        } else {
            Outfit().apply {
                this.name = name
                this.description = description
                this.seasons = seasons
                this.minTemp = minTemp
                this.maxTemp = maxTemp
                this.imagePath = ImagePath ?: ""
            }
        }

        if (isInEditMode) {
            outfitDao.update(outfit)
            outfitTagDao.deleteTagsForOutfit(outfit.id)
            selectedTagIds.forEach { tagId ->
                outfitTagDao.insert(OutfitTag(outfit.id, tagId))
            }
            lifecycleScope.launch(Dispatchers.Main) {
                Toast.makeText(this@OutfitActivity, "Комплект обновлён", Toast.LENGTH_SHORT).show()
                val resultIntent = Intent().apply {
                    putExtra("updated_item", outfit)
                }
                setResult(RESULT_OK, resultIntent)
                finish()
            }
        } else {
            val newId = outfitDao.insertOutfit(outfit)
            Toast.makeText(this, "Комплект сохранён", Toast.LENGTH_SHORT).show()

            try {
                selectedClothingItemIds.distinct().forEach { clothingItemId ->
                    db.outfitClothingItemDao().insert(
                        OutfitClothingItem(clothingItemId, newId.toInt())
                    )
                }
            } catch (e: Exception) {
                Log.e("OutfitActivity", "Ошибка при вставке outfitClothingItem: ${e.message}")
                Toast.makeText(this, "Ошибка сохранения связей комплекта", Toast.LENGTH_SHORT).show()
            }

            try {
                selectedTagIds.forEach { tagId ->
                    outfitTagDao.insert(
                        OutfitTag(newId.toInt(), tagId)
                    )
                }
            } catch (e: Exception) {
                Log.e("OutfitActivity", "Ошибка при вставке outfitTag: ${e.message}")
                Toast.makeText(this, "Ошибка сохранения тегов", Toast.LENGTH_SHORT).show()
            }

            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("openFragment", "outfits")
            }
            startActivity(intent)
            finish()
        }
    }

    // Считаем количество выбранных диапазонов (для проверки «не останется пусто»)
    private fun countSelectedRanges(): Int {
        var count = 0
        if (cbFrost.isChecked) count++
        if (cbCold.isChecked) count++
        if (cbCool.isChecked) count++
        if (cbWarm.isChecked) count++
        if (cbHot.isChecked) count++
        if (cbHeat.isChecked) count++
        return count
    }

    // Метод для обновления состояния чекбоксов, связанных с диапазонами погоды
    private fun updateWeatherCheckboxes(sliderMin: Int, sliderMax: Int) {
        fun rangesOverlap(aMin: Int, aMax: Int, bMin: Int, bMax: Int): Boolean {
            return (aMin <= bMax) && (aMax >= bMin)
        }

        if (sliderMin == -50 && sliderMax == 50) {
            isCheckboxUpdating = true
            cbFrost.isChecked = true
            cbCold.isChecked  = true
            cbCool.isChecked  = true
            cbWarm.isChecked  = true
            cbHot.isChecked   = true
            cbHeat.isChecked  = true
            isCheckboxUpdating = false
            return
        }

        isCheckboxUpdating = true
        cbFrost.isChecked = rangesOverlap(sliderMin, sliderMax, -50, -6)
        cbCold.isChecked  = rangesOverlap(sliderMin, sliderMax, -5, 9)
        cbCool.isChecked  = rangesOverlap(sliderMin, sliderMax, 10, 19)
        cbWarm.isChecked  = rangesOverlap(sliderMin, sliderMax, 20, 26)
        cbHot.isChecked   = rangesOverlap(sliderMin, sliderMax, 27, 34)
        cbHeat.isChecked  = rangesOverlap(sliderMin, sliderMax, 35, 50)
        isCheckboxUpdating = false
    }

    // Метод для управления видимостью слайдера и температуры
    private fun setWeatherVisibility(visible: Boolean) {
        val visibility = if (visible) View.VISIBLE else View.GONE
        tvTemp.visibility = visibility
        rsWeather.visibility = visibility
        fbWeatherChecks.visibility = visibility
        LL.visibility = visibility
    }

    private fun adjustSliderRange() {
        val oldSliderMin = rsWeather.values.first().toInt()
        val oldSliderMax = rsWeather.values.last().toInt()

        val intervals = mutableListOf<Pair<Int, Int>>()

        val staticRanges = listOf(
            cbFrost to (-50 to -6),
            cbCold  to (-5 to 9),
            cbCool  to (10 to 19),
            cbWarm  to (20 to 26),
            cbHot   to (27 to 34),
            cbHeat  to (35 to 50)
        )

        for ((checkBox, staticPair) in staticRanges) {
            if (checkBox.isChecked) {
                val (sMin, sMax) = staticPair
                if (sMax < oldSliderMin || sMin > oldSliderMax) {
                    intervals.add(sMin to sMax)
                } else {
                    val partMin = maxOf(sMin, oldSliderMin)
                    val partMax = minOf(sMax, oldSliderMax)
                    intervals.add(partMin to partMax)
                }
            }
        }

        if (intervals.isEmpty()) {
            val currentMin = oldSliderMin
            val currentMax = oldSliderMax
            isUpdatingFromCode = true
            rsWeather.setValues(currentMin.toFloat(), currentMax.toFloat())
            isUpdatingFromCode = false
            tvTemp.text = "$currentMin ... $currentMax°C"
            return
        }

        val newMin = intervals.minOf { it.first }
        val newMax = intervals.maxOf { it.second }

        isUpdatingFromCode = true
        rsWeather.setValues(newMin.toFloat(), newMax.toFloat())
        isUpdatingFromCode = false

        tvTemp.text = "$newMin ... $newMax°C"

        updateWeatherCheckboxes(newMin, newMax)
    }

    // Обработка нажатия кнопки "Назад" с подтверждением выхода
    @Suppress("MissingSuperCall")
    override fun onBackPressed() {
        AlertDialog.Builder(this)
            .setTitle("Внимание")
            .setMessage("Вы уверены, что хотите выйти? Все несохранённые изменения будут потеряны.")
            .setPositiveButton("Да") { dialog, which ->
                val intent = Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    putExtra("openFragment", "outfits")
                }
                startActivity(intent)
            }
            .setNegativeButton("Нет", null)
            .show()
    }
}