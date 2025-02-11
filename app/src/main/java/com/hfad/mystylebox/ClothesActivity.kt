package com.hfad.mystylebox

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.room.Room
import com.hfad.mystylebox.database.AppDatabase
import com.hfad.mystylebox.database.ClothingItem

class ClothesActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var clothingImageView: ImageView
    private lateinit var clothingNameEditText: EditText
    private lateinit var saveButton: Button
    private var imagePath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.clothes)

        // Привязываем элементы из макета
        clothingImageView = findViewById(R.id.clothingImageView)
        clothingNameEditText = findViewById(R.id.enterName)
        saveButton = findViewById(R.id.ButtonSAVE)

        // Инициализируем базу данных (используйте тот же способ, как и в MainActivity)
        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "clothing_db"
        )
            .allowMainThreadQueries() // не рекомендуется в продакшене
            .build()

        // Получаем путь к изображению, переданный из MainActivity
        imagePath = intent.getStringExtra("image_path")
        if (!imagePath.isNullOrEmpty()) {
            // Отображаем изображение в ImageView
            clothingImageView.setImageURI(Uri.parse(imagePath))
        }

        // Обработка нажатия на кнопку "Сохранить"
        saveButton.setOnClickListener {
            saveClothingItem()
        }
    }
    private fun saveClothingItem() {
        // Получаем название вещи из EditText
        val name = clothingNameEditText.text.toString().trim()

        // Если название пустое или не передано изображение, показываем сообщение об ошибке
        if (name.isEmpty() || imagePath.isNullOrEmpty()) {
            Toast.makeText(this, "Введите название и выберите фото", Toast.LENGTH_SHORT).show()
            return
        }
        val category = "Одежда"
        val gender = "Универсально"
        val seasons = listOf<String>() // или можно собрать выбранные сезоны
        // Создаем объект вещи (убедитесь, что конструктор в ClothingItem соответствует передаваемым параметрам)
        val item = ClothingItem(name, category, gender, imagePath!!, seasons)
        // Сохраняем вещь в базу данных
        db.clothingItemDao().insert(item)
        Toast.makeText(this, "Вещь сохранена", Toast.LENGTH_SHORT).show()
        // Возвращаемся в MainActivity (или закрываем текущую активность)
        finish()
    }
}
