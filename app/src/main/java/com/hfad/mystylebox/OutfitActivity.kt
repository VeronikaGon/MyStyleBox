package com.hfad.mystylebox

import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class OutfitActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_outfit)

        val clothingImageView = findViewById<ImageView>(R.id.clothingImageView)
        val imagePath = intent.getStringExtra("imagePath")
        if (!imagePath.isNullOrEmpty()) {
            val bitmap = BitmapFactory.decodeFile(imagePath)
            clothingImageView.setImageBitmap(bitmap)
        } else {
            // Обработка случая, когда путь не передан или ошибка при загрузке изображения
        }
    }
}