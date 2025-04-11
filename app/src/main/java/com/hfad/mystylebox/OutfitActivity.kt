package com.hfad.mystylebox

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
import com.hfad.mystylebox.database.AppDatabase
import com.hfad.mystylebox.database.Outfit
import com.hfad.mystylebox.database.OutfitClothingItem
import com.hfad.mystylebox.database.OutfitDao
import com.hfad.mystylebox.database.OutfitTag
import com.hfad.mystylebox.database.OutfitTagDao
import com.hfad.mystylebox.database.Tag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OutfitActivity : AppCompatActivity() {
    private lateinit var db: AppDatabase
    private lateinit var outfitDao: OutfitDao
    private lateinit var outfitTagDao: OutfitTagDao

    private lateinit var outfitNameEditText: EditText
    private lateinit var outfitDescriptionEditText: EditText
    private lateinit var btnSave: Button

    private var ImagePath: String? = null
    private var isInEditMode: Boolean = false
    private var currentOutfit: Outfit? = null

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
    private var selectedClothingItemIds: List<Int> = listOf()
    private lateinit var LL:LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_outfit)

        val outfitImageView = findViewById<ImageView>(R.id.outfitImageView)
        ImagePath = intent.getStringExtra("imagePath")
        Log.d("OutfitActivity", "Получен путь к фото: $ImagePath")
        if (!ImagePath.isNullOrEmpty()) {
            val bitmap = BitmapFactory.decodeFile(ImagePath)
            outfitImageView.setImageBitmap(bitmap)
        } else {
            Log.e("OutfitActivity", "Путь к фото пустой")
        }
        flexboxTags = findViewById(R.id.Tags)
        selectedClothingItemIds = intent.getIntegerArrayListExtra("selected_clothing_ids") ?: listOf()
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
        LL = findViewById(R.id.ll)
        val llTegi = findViewById<LinearLayout>(R.id.llTegi)
        llTegi.setOnClickListener {
            val intent = Intent(this, TagEditingActivity::class.java)
            tagEditingLauncher.launch(intent)
        }
        val tagEditButton = findViewById<ImageButton>(R.id.imageButton)
        tagEditButton.setOnClickListener {
            val intent = Intent(this, TagEditingActivity::class.java)
            tagEditingLauncher.launch(intent)
        }
        tagEditingLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val returnedTags = result.data?.getParcelableArrayListExtra<Tag>("selected_tags")
                if (returnedTags != null) {
                    selectedTagIds.clear()
                    selectedTags = returnedTags
                    returnedTags.forEach { tag ->
                        selectedTagIds.add(tag.id)
                    }
                    displayTagsAsCheckboxes(returnedTags, selectedTagIds)
                }
            }
        }
        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "wardrobe_db"
        ).allowMainThreadQueries().build()
        outfitDao = db.outfitDao()
        outfitTagDao = db.outfitTagDao()

        currentOutfit = intent.getParcelableExtra("outfit")
        if (currentOutfit != null) {
            isInEditMode = true
            populateFields(currentOutfit!!)
            btnSave.text = "Обновить"
        } else {
            btnSave.text = "Сохранить"
        }

        if (currentOutfit == null || currentOutfit!!.minTemp == -99 || currentOutfit!!.maxTemp == -99) {
            rsWeather.setValues(-50f, 50f)
            cbWeather.isChecked = false
            setWeatherVisibility(false)
        } else {
            tvTemp.text = "${rsWeather.values.first().toInt()} ... ${rsWeather.values.last().toInt()}°C"
        }

        btnSave.setOnClickListener {
            if (outfitNameEditText.text.toString().trim().isEmpty()) {
                Toast.makeText(this, "Введите название комплекта", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            saveOrUpdateOutfit()
        }

        cbWeather.setOnCheckedChangeListener { _, isChecked ->
            setWeatherVisibility(isChecked)
        }

        val btnPlus = findViewById<ImageButton>(R.id.btnplus)
        val btnMinus = findViewById<ImageButton>(R.id.btnminus)

        btnPlus.setOnClickListener {
            val lower = rsWeather.values[0]
            val upper = rsWeather.values[1]
            if (upper < rsWeather.valueTo) {
                val newUpper = upper + 1
                rsWeather.setValues(lower, newUpper)
                tvTemp.text = "${lower.toInt()} ... ${newUpper.toInt()}°C"
                updateWeatherCheckboxes(lower.toInt(), newUpper.toInt())
            }
        }

        btnMinus.setOnClickListener {
            val lower = rsWeather.values[0]
            val upper = rsWeather.values[1]
            if (lower > rsWeather.valueFrom) {
                val newLower = lower - 1
                rsWeather.setValues(newLower, upper)
                tvTemp.text = "${newLower.toInt()} ... ${upper.toInt()}°C"
                updateWeatherCheckboxes(newLower.toInt(), upper.toInt())
            }
        }
        tvTemp.text = "${rsWeather.values.first().toInt()} ... ${rsWeather.values.last().toInt()}°C"

        rsWeather.addOnChangeListener { slider, value, fromUser ->
            val sliderValues = slider.values.map { it.toInt() }
            tvTemp.text = "${sliderValues.first()} ... ${sliderValues.last()}°C"
            updateWeatherCheckboxes(sliderValues.first(), sliderValues.last())
        }

        cbWeather.buttonTintList = ColorStateList.valueOf(Color.parseColor("#FFB5A7"))
        setWeatherVisibility(cbWeather.isChecked)
        cbWeather.setOnCheckedChangeListener { _, isChecked ->
            setWeatherVisibility(isChecked)
        }

        val checkBoxListener = CompoundButton.OnCheckedChangeListener { button, _ ->
            val sliderMin = rsWeather.values.first().toInt()
            val sliderMax = rsWeather.values.last().toInt()
            if (!button.isChecked && countSelectedRanges() <= 0 && (sliderMax - sliderMin) > 1) {
                Toast.makeText(this, "Должен быть выбран хотя бы один диапазон погоды", Toast.LENGTH_SHORT).show()
                button.isChecked = true
            } else {
                adjustSliderRange()
            }
        }
        cbHeat.setOnCheckedChangeListener(checkBoxListener)
        cbHot.setOnCheckedChangeListener(checkBoxListener)
        cbWarm.setOnCheckedChangeListener(checkBoxListener)
        cbCool.setOnCheckedChangeListener(checkBoxListener)
        cbCold.setOnCheckedChangeListener(checkBoxListener)
        cbFrost .setOnCheckedChangeListener(checkBoxListener)
        updateWeatherCheckboxes(rsWeather.values.first().toInt(), rsWeather.values.last().toInt())
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
                if (checkBox.isChecked) {
                    selectedTagIds.add(tag.id)
                } else {
                    selectedTagIds.remove(tag.id)
                }
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
            rsWeather.setValues(-50f, 50f)
            cbWeather.isChecked = false
            setWeatherVisibility(false)
        } else {
            rsWeather.setValues(outfit.minTemp.toFloat(), outfit.maxTemp.toFloat())
            cbWeather.isChecked = (outfit.minTemp >= -50 && outfit.maxTemp <= 50)
            setWeatherVisibility(cbWeather.isChecked)
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
            withContext(Dispatchers.Main) { displayTagsAsCheckboxes(allTags, selectedTagIds) }
        }
    }
    // Метод для сохранения или обновления комплекта
    private fun saveOrUpdateOutfit() {
        val name = outfitNameEditText.text.toString().trim()
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
            db.outfitTagDao().deleteTagsForOutfit(outfit.id)
            selectedTagIds.forEach { tagId ->
                db.outfitTagDao().insert(OutfitTag(outfit.id, tagId))
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
            val id = outfitDao.insertOutfit(outfit)
            Toast.makeText(this, "Комплект сохранён", Toast.LENGTH_SHORT).show()
            try {
                val uniqueClothingIds = selectedClothingItemIds.distinct()
                uniqueClothingIds.forEach { clothingItemId ->
                    db.outfitClothingItemDao().insert(OutfitClothingItem(clothingItemId, id.toInt()))
                }
            } catch (e: Exception) {
                Log.e("OutfitActivity", "Ошибка при вставке outfitClothingItem: ${e.message}")
                Toast.makeText(this, "Ошибка сохранения связей комплекта", Toast.LENGTH_SHORT).show()
            }
            try {
                selectedTagIds.forEach { tagId ->
                    db.outfitTagDao().insert(OutfitTag(id.toInt(), tagId))
                }
            } catch (e: Exception) {
                Log.e("OutfitActivity", "Ошибка при вставке outfitTag: ${e.message}")
                Toast.makeText(this, "Ошибка сохранения тегов комплекта", Toast.LENGTH_SHORT).show()
            }
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("openFragment", "outfits")
            }
            startActivity(intent)
            finish()
        }
    }

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
        if (sliderMin == -50 && sliderMax == 50) {
            cbFrost.isChecked = true
            cbCold.isChecked = true
            cbCool.isChecked = true
            cbWarm.isChecked = true
            cbHot.isChecked = true
            cbHeat.isChecked = true
            return
        }
        cbFrost.isChecked = (sliderMin <= -7 && sliderMax >= -6)
        cbCold.isChecked  = (sliderMin <= -5 && sliderMax >= 9)
        cbCool.isChecked  = (sliderMin <= 10 && sliderMax >= 19)
        cbWarm.isChecked  = (sliderMin <= 20 && sliderMax >= 26)
        cbHot.isChecked   = (sliderMin <= 27 && sliderMax >= 34)
        cbHeat.isChecked  = (sliderMin <= 35 && sliderMax >= 50)
        if (!cbWarm.isChecked) {
            if (sliderMax < 27) {
                cbHot.isChecked = false
                cbHeat.isChecked = false
            }
        }
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
        val selectedRanges = mutableListOf<Pair<Int, Int>>()
        if (cbFrost.isChecked) selectedRanges.add(Pair(-50, -6))
        if (cbCold.isChecked)  selectedRanges.add(Pair(-5, 9))
        if (cbCool.isChecked)  selectedRanges.add(Pair(10, 19))
        if (cbWarm.isChecked)  selectedRanges.add(Pair(20, 26))
        if (cbHot.isChecked)   selectedRanges.add(Pair(27, 34))
        if (cbHeat.isChecked)  selectedRanges.add(Pair(35, 50))

        if (selectedRanges.isEmpty() && rsWeather.values.first() != rsWeather.values.last()) {
            Toast.makeText(this, "Должен быть выбран хотя бы один диапазон погоды", Toast.LENGTH_SHORT).show()
            cbFrost.isChecked = true
            selectedRanges.add(Pair(-50, -6))
        }

        if (selectedRanges.isEmpty()) {
            val currentMin = rsWeather.values.first().toInt()
            val currentMax = rsWeather.values.last().toInt()
            rsWeather.setValues(currentMin.toFloat(), currentMax.toFloat())
            tvTemp.text = "$currentMin ... $currentMax°C"
            return
        }

        val unionMin = selectedRanges.minOf { it.first }
        val unionMax = selectedRanges.maxOf { it.second }

        rsWeather.setValues(unionMin.toFloat(), unionMax.toFloat())
        tvTemp.text = "$unionMin ... $unionMax°C"
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