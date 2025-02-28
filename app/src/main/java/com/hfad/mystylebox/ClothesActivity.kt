package com.hfad.mystylebox

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.google.android.flexbox.FlexboxLayout
import com.hfad.mystylebox.database.AppDatabase
import com.hfad.mystylebox.database.ClothingItem
import com.hfad.mystylebox.database.ClothingItemDao
import com.hfad.mystylebox.database.SubcategoryDao
import com.hfad.mystylebox.database.Tag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ClothesActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var clothingItemDao: ClothingItemDao
    private lateinit var subcategoryDao: SubcategoryDao

    private lateinit var clothingImageView: ImageView
    private lateinit var clothingNameEditText: EditText
    private lateinit var clothingBrendEditText: EditText
    private lateinit var clothingCostEditText: EditText
    private lateinit var clothingNotesEditText: EditText
    private lateinit var saveButton: Button
    private lateinit var categoryField: TextView
    private lateinit var textviewTitle: TextView
    private lateinit var cbSummer: CheckBox
    private lateinit var cbSpring: CheckBox
    private lateinit var cbWinter: CheckBox
    private lateinit var cbAutumn: CheckBox
    private var imagePath: String? = null
    private var subcategory: String? = null
    private var selectedSubcategoryId: Int = -1
    private lateinit var categoryResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var tagEditingLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.clothes)
        clothingImageView = findViewById(R.id.clothingImageView)
        clothingNameEditText = findViewById(R.id.enterName)
        clothingBrendEditText = findViewById(R.id.enterBrend)
        clothingCostEditText = findViewById(R.id.enterStoimost)
        clothingNotesEditText = findViewById(R.id.enterNotes)
        clothingNameEditText = findViewById(R.id.enterName)
        cbSummer = findViewById<CheckBox>(R.id.cbSummer)
        cbSpring = findViewById<CheckBox>(R.id.cbSpring)
        cbWinter = findViewById<CheckBox>(R.id.cbWinter)
        cbAutumn = findViewById<CheckBox>(R.id.cbAutumn)
        saveButton = findViewById(R.id.ButtonSAVE)
        textviewTitle = findViewById(R.id.textviewtitle)
        categoryField = findViewById(R.id.categoryField)

        tagEditingLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val selectedTags = result.data?.getParcelableArrayListExtra<Tag>("selected_tags")
                // Отобразите выбранные теги в интерфейсе ClothesActivity
                // Например, в FlexboxLayout или TextView
            }
        }
        val llTegi = findViewById<LinearLayout>(R.id.llTegi)
        llTegi.setOnClickListener {
            val intent = Intent(this, TagEditingActivity::class.java)
            tagEditingLauncher.launch(intent)
        }
        val tagEditButton = findViewById<ImageButton>(R.id.imageButton)
        tagEditButton.setOnClickListener {
            val intent = Intent(this, TagEditingActivity::class.java)
            startActivity(intent)
        }
        tagEditButton.setOnClickListener {
            val intent = Intent(this, TagEditingActivity::class.java)
            tagEditingLauncher.launch(intent)
        }

        categoryResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val subcategory = result.data?.getStringExtra("subcategory")
                selectedSubcategoryId = result.data?.getIntExtra("selected_subcategory_id", -1) ?: -1
                categoryField.text = subcategory ?: "Не выбрано"
                clothingNameEditText.setText(result.data?.getStringExtra("name") ?: "")
                clothingBrendEditText.setText(result.data?.getStringExtra("brend") ?: "")
                clothingCostEditText.setText(result.data?.getStringExtra("cost") ?: "")
                clothingNotesEditText.setText(result.data?.getStringExtra("notes") ?: "")
                setSelectedSize(result.data?.getStringExtra("size") ?: "")
                setSelectedStatus(result.data?.getStringExtra("status") ?: "")
                setSelectedGender(result.data?.getStringExtra("gender") ?: "Универсально")
            }
        }
        categoryField.setOnClickListener {
            val intent = Intent(this, CategorySelectionActivity::class.java).apply {
                putExtra("image_uri", imagePath.toString())
                putExtra("name", clothingNameEditText.text.toString())
                putExtra("brend", clothingBrendEditText.text.toString())
                putExtra("cost", clothingCostEditText.text.toString())
                putExtra("notes", clothingNotesEditText.text.toString())
                putExtra("size", getSelectedSize())
                putExtra("status", getSelectedStatus())
                putExtra("gender", getSelectedGender())
            }
            categoryResultLauncher.launch(intent)
        }
        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "wardrobe_db"
        )
            .allowMainThreadQueries()
            .build()
        clothingItemDao = db.clothingItemDao()
        subcategoryDao = db.subcategoryDao()

        subcategory = intent.getStringExtra("subcategory")
        selectedSubcategoryId = intent.getIntExtra("selected_subcategory_id", -1)
        imagePath = intent.getStringExtra("image_path")
        if (!imagePath.isNullOrEmpty()) {
            clothingImageView.setImageURI(Uri.parse(imagePath))
        }
        if (subcategory != null) {
            categoryField.text = subcategory
        }

        val flexboxLayout = findViewById<FlexboxLayout>(R.id.flexboxLayout)
        for (i in 0 until flexboxLayout.childCount) {
            val child = flexboxLayout.getChildAt(i)
            if (child is RadioButton) {
                child.setOnClickListener {
                    for (j in 0 until flexboxLayout.childCount) {
                        (flexboxLayout.getChildAt(j) as? RadioButton)?.isChecked = false
                    }
                    child.isChecked = true
                }
            }
        }
        // Проверяем, запущена ли активность в режиме редактирования
        val editingItem = intent.getParcelableExtra<ClothingItem>("clothingItem")
        imagePath = intent.getStringExtra("image_path")
        if (!imagePath.isNullOrEmpty()) {
            clothingImageView.setImageURI(Uri.parse(imagePath))
        }
        if (editingItem != null) {
            populateFields(editingItem)
            textviewTitle.text = "Редактирование вещи"
            saveButton.text = "Обновить"
            saveButton.setOnClickListener { updateClothingItem(editingItem) }
        } else {
            saveButton.setOnClickListener { saveClothingItem() }
        }
    }
    //установка значений для редактирования
    private fun populateFields(item: ClothingItem) {
        clothingNameEditText.setText(item.name)
        clothingBrendEditText.setText(item.brend)
        clothingCostEditText.setText(item.cost.toString())
        clothingNotesEditText.setText(item.notes)
        // Установка сезонов
        cbSummer.isChecked = item.seasons?.contains("Лето") == true
        cbSpring.isChecked = item.seasons?.contains("Весна") == true
        cbWinter.isChecked = item.seasons?.contains("Зима") == true
        cbAutumn.isChecked = item.seasons?.contains("Осень") == true
        // Установка размера (предполагается, что flexboxLayout присутствует)
        setSelectedSize(item.size)
        setSelectedStatus(item.status)
        setSelectedGender(item.gender)
        // Установка изображения (если imagePath не передан в Intent, можно использовать значение из объекта)
        if (imagePath.isNullOrEmpty() && item.imagePath.isNotEmpty()) {
            imagePath = item.imagePath
            clothingImageView.setImageURI(Uri.parse(imagePath))
        }

        // Установка категории – если необходимо, можно загрузить название подкатегории:
        lifecycleScope.launch {
            val subcatName = withContext(Dispatchers.IO) {
                subcategoryDao.getSubcategoryNameById(item.subcategoryId)
            }
            categoryField.text = subcatName
        }
        selectedSubcategoryId = item.subcategoryId
    }
    private fun updateClothingItem(oldItem: ClothingItem) {
        val name = clothingNameEditText.text.toString().trim()
        if (name.isEmpty()) {
            Toast.makeText(this, "Введите название", Toast.LENGTH_SHORT).show()
            return
        }
        val brend = clothingBrendEditText.text.toString().trim()
        val cost = clothingCostEditText.text.toString().trim().toFloatOrNull() ?: 0.0f
        val notes = clothingNotesEditText.text.toString().trim()
        val seasons = mutableListOf<String>()
        if (cbSummer.isChecked) seasons.add(cbSummer.text.toString())
        if (cbSpring.isChecked) seasons.add(cbSpring.text.toString())
        if (cbWinter.isChecked) seasons.add(cbWinter.text.toString())
        if (cbAutumn.isChecked) seasons.add(cbAutumn.text.toString())
        val updatedItem = ClothingItem(
            name,
            selectedSubcategoryId,
            brend,
            getSelectedGender(),
            imagePath ?: "",
            seasons,
            cost,
            getSelectedStatus(),
            getSelectedSize(),
            notes
        )
        updatedItem.id = oldItem.id
        lifecycleScope.launch(Dispatchers.IO) {
            clothingItemDao.update(updatedItem)
            withContext(Dispatchers.Main) {
                Toast.makeText(this@ClothesActivity, "Вещь обновлена", Toast.LENGTH_SHORT).show()
                val resultIntent = Intent().apply {
                    putExtra("updated_item", updatedItem)
                }
                setResult(RESULT_OK, resultIntent)
                finish()
            }
        }
    }
    //метод для получения размера вещи
    private fun getSelectedSize(): String {
        val flexboxLayout = findViewById<FlexboxLayout>(R.id.flexboxLayout)
        return (0 until flexboxLayout.childCount)
            .map { flexboxLayout.getChildAt(it) }
            .filterIsInstance<RadioButton>()
            .firstOrNull { it.isChecked }
            ?.text
            ?.toString()
            ?: ""
    }
    //метод для получения статуса вещи
    private fun getSelectedStatus(): String {
        val statusRadioGroup = findViewById<RadioGroup>(R.id.radioGroupStatus)
        return findViewById<RadioButton>(statusRadioGroup.checkedRadioButtonId)?.text?.toString() ?: ""
    }
    //метод для получения пола для конкретной вещи
    private fun getSelectedGender(): String {
        val genderRadioGroup = findViewById<RadioGroup>(R.id.radioGroupGender)
        return findViewById<RadioButton>(genderRadioGroup.checkedRadioButtonId)?.text?.toString() ?: "Универсально"
    }
    //метод для задавания размера вещи
    private fun setSelectedSize(size: String) {
        val flexboxLayout = findViewById<FlexboxLayout>(R.id.flexboxLayout)
        for (i in 0 until flexboxLayout.childCount) {
            val child = flexboxLayout.getChildAt(i)
            if (child is RadioButton && child.text.toString() == size) {
                child.isChecked = true
                break
            }
        }
    }
    //метод для задавания статуса вещи
    private fun setSelectedStatus(status: String) {
        val statusRadioGroup = findViewById<RadioGroup>(R.id.radioGroupStatus)
        for (i in 0 until statusRadioGroup.childCount) {
            val child = statusRadioGroup.getChildAt(i)
            if (child is RadioButton && child.text.toString() == status) {
                child.isChecked = true
                break
            }
        }
    }
    //метод для задавания пола для конкретной вещи
    private fun setSelectedGender(gender: String) {
        val genderRadioGroup = findViewById<RadioGroup>(R.id.radioGroupGender)
        for (i in 0 until genderRadioGroup.childCount) {
            val child = genderRadioGroup.getChildAt(i)
            if (child is RadioButton && child.text.toString() == gender) {
                child.isChecked = true
                break
            }
        }
    }

    //метод сохранение вещи в базе
    private fun saveClothingItem() {
        val brend = clothingBrendEditText.text.toString().trim().ifEmpty { "" }
        val name = clothingNameEditText.text.toString().trim()
        if (name.isEmpty()) {
            Toast.makeText(this, "Введите название ", Toast.LENGTH_SHORT).show()
            return
        }
        val cost = clothingCostEditText.text.toString().trim().toFloatOrNull() ?: 0.0f
        val notes = clothingNotesEditText.text.toString().trim().ifEmpty { "" }
        val status = getSelectedStatus()
        val size = getSelectedSize()
        val gender = getSelectedGender()
        val seasons = mutableListOf<String>()
        if (selectedSubcategoryId == -1) {
            Log.d("ClothesActivity", "Selected Subcategory ID: $selectedSubcategoryId")
            Toast.makeText(this, "Выберите подкатегорию", Toast.LENGTH_SHORT).show()
            return
        }
        if (cbSummer.isChecked) seasons.add(cbSummer.text.toString())
        if (cbSpring.isChecked) seasons.add(cbSpring.text.toString())
        if (cbWinter.isChecked) seasons.add(cbWinter.text.toString())
        if (cbAutumn.isChecked) seasons.add(cbAutumn.text.toString())
        val item = ClothingItem(name, selectedSubcategoryId, brend, gender, imagePath!!, seasons,cost,status, size,notes)
        db.clothingItemDao().insert(item)
        Toast.makeText(this, "Вещь сохранена", Toast.LENGTH_SHORT).show()
        finish()
    }
}
