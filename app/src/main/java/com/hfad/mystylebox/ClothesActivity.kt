package com.hfad.mystylebox

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.room.Room
import com.google.android.flexbox.FlexboxLayout
import com.hfad.mystylebox.database.AppDatabase
import com.hfad.mystylebox.database.ClothingItem
import com.hfad.mystylebox.database.Subcategory

class ClothesActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var clothingImageView: ImageView
    private lateinit var clothingNameEditText: EditText
    private lateinit var clothingBrendEditText: EditText
    private lateinit var clothingCostEditText: EditText
    private lateinit var clothingNotesEditText: EditText
    private lateinit var categorySpinner: Spinner
    private lateinit var saveButton: Button
    private var imagePath: String? = null
    private var subcategory: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.clothes)
        clothingImageView = findViewById(R.id.clothingImageView)
        clothingNameEditText = findViewById(R.id.enterName)
        clothingBrendEditText = findViewById(R.id.enterBrend)
        clothingCostEditText = findViewById(R.id.enterStoimost)
        clothingNotesEditText = findViewById(R.id.enterNotes)
        categorySpinner = findViewById(R.id.categorySpinner)
        saveButton = findViewById(R.id.ButtonSAVE)

        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "clothing_db"
        )
            .allowMainThreadQueries()
            .build()
        subcategory = intent.getStringExtra("subcategory")
        imagePath = intent.getStringExtra("image_path")
        if (!imagePath.isNullOrEmpty()) {
            // Отображаем изображение в ImageView
            clothingImageView.setImageURI(Uri.parse(imagePath))
        }

        saveButton.setOnClickListener {
            saveClothingItem()
        }
    }
    private fun saveClothingItem() {
        val brend = clothingBrendEditText.text.toString().trim().ifEmpty { "" }
        val name = clothingNameEditText.text.toString().trim()
        if (name.isEmpty()) {
            Toast.makeText(this, "Введите название ", Toast.LENGTH_SHORT).show()
            return
        }
        val cost = clothingCostEditText.text.toString().trim().toFloatOrNull() ?: 0.0f
        val notes = clothingNotesEditText.text.toString().trim().ifEmpty { "" }
        val statusRadioGroup = findViewById<RadioGroup>(R.id.radioGroupStatus)
        val selectedStatusId = statusRadioGroup.checkedRadioButtonId
        val status = if (selectedStatusId != -1) {
            findViewById<RadioButton>(selectedStatusId).text.toString()
        } else {
            Toast.makeText(this, "Выберите статус", Toast.LENGTH_SHORT).show()
            return
        }
        val flexboxLayout = findViewById<FlexboxLayout>(R.id.flexboxLayout)
        val size = (0 until flexboxLayout.childCount)
            .map { flexboxLayout.getChildAt(it) }
            .filterIsInstance<RadioButton>()
            .firstOrNull { it.isChecked }
            ?.text
            ?.toString()
            ?: run {
                Toast.makeText(this, "Выберите размер", Toast.LENGTH_SHORT).show()
                return
            }

        // Получаем DAO для Subcategory
        val subcategoryDao = db.subcategoryDao()

// Проверяем, существует ли подкатегория с id = 1
        val subcategory = subcategoryDao.getSubcategoryById(1)

// Если подкатегория не существует, создаем её
        if (subcategory == null) {
            val newSubcategory = Subcategory(1, "Футболки")
            subcategoryDao.insert(newSubcategory)
        }
        val subcategoryId = 1
        val gender = "Универсально"
        val seasons = listOf<String>()
        val item = ClothingItem(name, subcategoryId, brend, gender, imagePath!!, seasons,cost,status, size,notes)

        db.clothingItemDao().insert(item)
        Toast.makeText(this, "Вещь сохранена", Toast.LENGTH_SHORT).show()
        finish()
    }
}
