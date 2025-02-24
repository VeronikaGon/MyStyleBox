package com.hfad.mystylebox

import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.hfad.mystylebox.database.ClothingItem
import android.Manifest
import android.content.ContentResolver
import android.graphics.BitmapFactory
import android.widget.Button
import android.widget.ImageButton
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.bumptech.glide.Glide
import com.hfad.mystylebox.database.AppDatabase
import com.hfad.mystylebox.database.Category
import com.hfad.mystylebox.database.Subcategory
import com.hfad.mystylebox.database.ClothingItemDao
import com.hfad.mystylebox.database.SubcategoryDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditclothesActivity : AppCompatActivity() {
    private val REQUEST_READ_STORAGE = 101
    private lateinit var subcategoryDao: SubcategoryDao
    private lateinit var clothingItemDao: ClothingItemDao
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Инициализация базы данных и DAO
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "wardrobe_db"
        ).build()
        subcategoryDao = db.subcategoryDao()
        clothingItemDao = db.clothingItemDao()
        enableEdgeToEdge()
        setContentView(R.layout.activity_editclothes)

        // Выбор разрешения в зависимости от версии Android
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_MEDIA_IMAGES),
                    REQUEST_READ_STORAGE
                )
            } else {
                setupUI()
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    REQUEST_READ_STORAGE
                )
            } else {
                setupUI()
            }
        }

    }
    private fun setupUI() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val clothingItem = intent.getParcelableExtra<ClothingItem>("clothing_item")

        // Инициализация элементов
        val llStoimost = findViewById<LinearLayout>(R.id.llstoimost)
        val llSeason = findViewById<LinearLayout>(R.id.llseason)
        val llSize = findViewById<LinearLayout>(R.id.llsize)
        val llBrend = findViewById<LinearLayout>(R.id.llbrend)
        val llNotes = findViewById<LinearLayout>(R.id.llNotes)
        val llTegi = findViewById<LinearLayout>(R.id.llTegi)
        val textViewStoimost = findViewById<TextView>(R.id.textviewstoimost)
        val Size = findViewById<TextView>(R.id.textviewsize)
        val Name = findViewById<TextView>(R.id.textviewname)
        val Brend = findViewById<TextView>(R.id.textviewbrend)
        val Subcategory = findViewById<TextView>(R.id.textviewcategory)
        val Notes = findViewById<TextView>(R.id.textViewNotes)
        val Status = findViewById<TextView>(R.id.status)
        val Image = findViewById<ImageView>(R.id.clothingImageView)
        val checkboxSummer = findViewById<CheckBox>(R.id.cbSummer)
        val checkboxSpring = findViewById<CheckBox>(R.id.cbSpring)
        val checkboxAutumn = findViewById<CheckBox>(R.id.cbAutumn)
        val checkboxWinter = findViewById<CheckBox>(R.id.cbWinter)
        val deleteButton = findViewById<ImageButton>(R.id.buttondelete)
        deleteButton.setOnClickListener {
            clothingItem?.let { item ->
                // Запускаем операцию удаления в фоновом потоке
                lifecycleScope.launch(Dispatchers.IO) {
                    clothingItemDao.delete(item)
                    // После удаления переключаемся на основной поток для завершения активности
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@EditclothesActivity, "Вещь удалена", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            }

            val imageUriString = intent.getStringExtra("image_uri")
            if (!imageUriString.isNullOrEmpty()) {
                try {
                    val uri = Uri.parse(imageUriString)
                    Glide.with(this)
                        .load(uri)
                        .into(Image)
                } catch (e: Exception) {
                    Toast.makeText(this, "Некорректный URI изображения", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Изображение не найдено", Toast.LENGTH_SHORT).show()
            }
            val subcategoryId = clothingItem?.subcategoryId
            if (subcategoryId != null) {
                lifecycleScope.launch {
                    val subcategoryName = withContext(Dispatchers.IO) {
                        subcategoryDao.getSubcategoryNameById(subcategoryId)
                    }
                    Subcategory.text = subcategoryName
                }
            }
            Name.text = clothingItem?.name.toString()
            Status.text = clothingItem?.status.toString()
            if (clothingItem?.cost == 0f) {
                llStoimost.visibility = View.GONE
            } else {
                llStoimost.visibility = View.VISIBLE
                textViewStoimost.text = clothingItem?.cost.toString()
            }
            // Проверка наличия сезона/сезоннов
            clothingItem?.seasons?.let { seasons ->
                if (seasons.isEmpty()) {
                    llSeason.visibility = View.GONE
                } else {
                    llSeason.visibility = View.VISIBLE
                    checkboxSummer.isChecked = seasons.contains("Лето")
                    checkboxSpring.isChecked = seasons.contains("Весна")
                    checkboxAutumn.isChecked = seasons.contains("Осень")
                    checkboxWinter.isChecked = seasons.contains("Зима")
                }
            } ?: run {
                llSeason.visibility = View.GONE
            }
            // Проверка наличия размера
            if (clothingItem?.size.isNullOrEmpty()) {
                llSize.visibility = View.GONE
            } else {
                llSize.visibility = View.VISIBLE
                Size.text = clothingItem?.size
            }
            // Проверка наличия бренда
            if (clothingItem?.brend.isNullOrEmpty()) {
                llBrend.visibility = View.GONE
            } else {
                llBrend.visibility = View.VISIBLE
                Brend.text = clothingItem?.brend
            }
            // Проверка наличия заметок
            if (clothingItem?.brend.isNullOrEmpty()) {
                llNotes.visibility = View.GONE
            } else {
                llNotes.visibility = View.VISIBLE
                Notes.text = clothingItem?.notes
            }
        }
    }
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_READ_STORAGE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupUI()
            } else {
                Toast.makeText(this, "Нет доступа к внешнему хранилищу", Toast.LENGTH_SHORT).show()
            }
        }
    }
}