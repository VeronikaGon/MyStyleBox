package com.hfad.mystylebox

import android.app.Activity
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
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
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
    private lateinit var saveButton: Button
    private lateinit var categoryField: TextView
    private var imagePath: String? = null
    private var subcategory: String? = null
    private var selectedSubcategoryId: Int = -1
    private lateinit var categoryResultLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.clothes)
        clothingImageView = findViewById(R.id.clothingImageView)
        clothingNameEditText = findViewById(R.id.enterName)
        clothingBrendEditText = findViewById(R.id.enterBrend)
        clothingCostEditText = findViewById(R.id.enterStoimost)
        clothingNotesEditText = findViewById(R.id.enterNotes)
        clothingNameEditText = findViewById(R.id.enterName)
        saveButton = findViewById(R.id.ButtonSAVE)
        categoryField = findViewById(R.id.categoryField)

        categoryResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val subcategory = result.data?.getStringExtra("subcategory")
                selectedSubcategoryId = result.data?.getIntExtra("selected_subcategory_id", -1) ?: -1
                categoryField.text = subcategory ?: "Не выбрано"
            }
        }
        categoryField.setOnClickListener {
            val intent = Intent(this, CategorySelectionActivity::class.java)
            intent.putExtra("image_uri", imagePath.toString())
            categoryResultLauncher.launch(intent)
        }
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
        val genderRadioGroup = findViewById<RadioGroup>(R.id.radioGroupGender)
        val selectedGenderId = genderRadioGroup.checkedRadioButtonId
        val gender = if (selectedGenderId != -1) {
            findViewById<RadioButton>(selectedGenderId).text.toString()
        } else {
            "Универсально"
        }
        if (selectedSubcategoryId == -1) {
            Toast.makeText(this, "Выберите подкатегорию", Toast.LENGTH_SHORT).show()
            return
        }

        val seasons = listOf<String>()
        val item = ClothingItem(name, selectedSubcategoryId, brend, gender, imagePath!!, seasons,cost,status, size,notes)

        db.clothingItemDao().insert(item)
        Toast.makeText(this, "Вещь сохранена", Toast.LENGTH_SHORT).show()
        finish()
    }
}
