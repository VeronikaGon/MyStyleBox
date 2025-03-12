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
import android.content.Intent
import android.widget.ImageButton
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.bumptech.glide.Glide
import com.hfad.mystylebox.database.AppDatabase
import com.hfad.mystylebox.database.ClothingItemDao
import com.hfad.mystylebox.database.SubcategoryDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditclothesActivity : AppCompatActivity() {
    private val REQUEST_READ_STORAGE = 101
    private lateinit var subcategoryDao: SubcategoryDao
    private lateinit var clothingItemDao: ClothingItemDao
    private var currentClothingItem: ClothingItem? = null
    private val editResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val updatedItem = result.data?.getParcelableExtra<ClothingItem>("updated_item")
            if (updatedItem != null) {
                // Обновляем UI предварительного просмотра с обновлёнными данными
                updatePreviewUI(updatedItem)
            }
        }
    }
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
        // Инициализация отступов с учётом системных окон
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val clothingItem = intent.getParcelableExtra<ClothingItem>("clothing_item")
        if (clothingItem != null) {
            currentClothingItem = clothingItem
        } else {
            Toast.makeText(this, "Данные не получены", Toast.LENGTH_SHORT).show()
        }

        // Инициализация остальных элементов
        val llStoimost = findViewById<LinearLayout>(R.id.llstoimost)
        val llSeason = findViewById<LinearLayout>(R.id.llseason)
        val llSize = findViewById<LinearLayout>(R.id.llsize)
        val llBrend = findViewById<LinearLayout>(R.id.llbrend)
        val llNotes = findViewById<LinearLayout>(R.id.llNotes)
        val textViewStoimost = findViewById<TextView>(R.id.textviewstoimost)
        val sizeTextView = findViewById<TextView>(R.id.textviewsize)
        val nameTextView = findViewById<TextView>(R.id.textviewname)
        val brendTextView = findViewById<TextView>(R.id.textviewbrend)
        val subcategoryTextView = findViewById<TextView>(R.id.textviewcategory)
        val notesTextView = findViewById<TextView>(R.id.textViewNotes)
        val statusTextView = findViewById<TextView>(R.id.status)
        val genderTextView = findViewById<TextView>(R.id.textviewgender)
        val imageView = findViewById<ImageView>(R.id.clothingImageView)
        val checkboxSummer = findViewById<CheckBox>(R.id.cbSummer)
        val checkboxSpring = findViewById<CheckBox>(R.id.cbSpring)
        val checkboxAutumn = findViewById<CheckBox>(R.id.cbAutumn)
        val checkboxWinter = findViewById<CheckBox>(R.id.cbWinter)

        // Инициализация кнопки удаления
        val deleteButton = findViewById<ImageButton>(R.id.buttondelete)
        deleteButton.setOnClickListener {
            clothingItem?.let { item ->
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Подтверждение удаления")
                    .setMessage("Вы точно хотите удалить вещь?")
                    .setPositiveButton("Да") { dialog, _ ->
                        lifecycleScope.launch(Dispatchers.IO) {
                            clothingItemDao.delete(item)
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@EditclothesActivity, "Вещь удалена", Toast.LENGTH_SHORT).show()
                                finish()
                            }
                        }
                        dialog.dismiss()
                    }
                    .setNegativeButton("Нет") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
            }
        }
        val Buttonedit = findViewById<ImageButton>(R.id.buttonedit)
        Buttonedit.setOnClickListener {
            if (clothingItem != null) {
                navigateToClothesActivity()
            }
        }
        val shareButton = findViewById<ImageButton>(R.id.buttonshareit)
        shareButton.setOnClickListener {
            val imageUriString = intent.getStringExtra("image_uri")
            if (!imageUriString.isNullOrEmpty()) {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/*"
                    putExtra(Intent.EXTRA_STREAM, Uri.parse(imageUriString))
                    putExtra(Intent.EXTRA_TEXT, "Посмотрите на эту вещь!")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(shareIntent, "Поделиться изображением"))
            } else {
                Toast.makeText(this, "Изображение не найдено", Toast.LENGTH_SHORT).show()
            }
        }
        // Далее – обновление остальных полей (имя, статус, стоимость, размеры, бренды, сезоны, подкатегория, и т.д.)
        clothingItem?.let { item ->
            genderTextView.text = item.gender
            nameTextView.text = item.name
            statusTextView.text = item.status
            if (item.cost == 0f) {
                llStoimost.visibility = View.GONE
            } else {
                llStoimost.visibility = View.VISIBLE
                textViewStoimost.text = item.cost.toString()
            }
            if (item.size.isNullOrEmpty()) {
                llSize.visibility = View.GONE
            } else {
                llSize.visibility = View.VISIBLE
                sizeTextView.text = item.size
            }
            if (item.brend.isNullOrEmpty()) {
                llBrend.visibility = View.GONE
            } else {
                llBrend.visibility = View.VISIBLE
                brendTextView.text = item.brend
            }
            if (item.notes.isNullOrEmpty()) {
                llNotes.visibility = View.GONE
            } else {
                llNotes.visibility = View.VISIBLE
                notesTextView.text = item.notes
            }
            // Обработка сезонов
            if (item.seasons.isNullOrEmpty()) {
                llSeason.visibility = View.GONE
            } else {
                llSeason.visibility = View.VISIBLE
                checkboxSummer.isChecked = item.seasons.contains("Лето")
                checkboxSpring.isChecked = item.seasons.contains("Весна")
                checkboxAutumn.isChecked = item.seasons.contains("Осень")
                checkboxWinter.isChecked = item.seasons.contains("Зима")
            }
            // Получение названия подкатегории в фоновом потоке
            lifecycleScope.launch {
                val subcategoryName = withContext(Dispatchers.IO) {
                    subcategoryDao.getSubcategoryNameById(item.subcategoryId)
                }
                subcategoryTextView.text = subcategoryName
            }

            // Установка изображения
            val imageUriString = intent.getStringExtra("image_uri")
            if (!imageUriString.isNullOrEmpty()) {
                try {
                    val uri = Uri.parse(imageUriString)
                    Glide.with(this)
                        .load(uri)
                        .into(imageView)
                } catch (e: Exception) {
                    Toast.makeText(this, "Некорректный URI изображения", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Изображение не найдено", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            Toast.makeText(this, "Данные не получены", Toast.LENGTH_SHORT).show()
        }
    }

    //Обновление после редактирования
    private fun updatePreviewUI(item: ClothingItem) {
            findViewById<TextView>(R.id.status).text = item.status
            findViewById<TextView>(R.id.textviewsize).text = item.size
            findViewById<TextView>(R.id.textviewbrend).text = item.brend
            findViewById<TextView>(R.id.textViewNotes).text = item.notes
            findViewById<CheckBox>(R.id.cbSummer).isChecked = item.seasons.contains("Лето")
            findViewById<CheckBox>(R.id.cbSpring).isChecked = item.seasons.contains("Весна")
            findViewById<CheckBox>(R.id.cbAutumn).isChecked = item.seasons.contains("Осень")
            findViewById<CheckBox>(R.id.cbWinter).isChecked = item.seasons.contains("Зима")
            findViewById<TextView>(R.id.textviewstoimost).text = item.cost.toString()
        findViewById<TextView>(R.id.textviewgender).text = item.gender
        findViewById<TextView>(R.id.textviewname).text = item.name
        currentClothingItem = item
    }

    private fun navigateToClothesActivity() {
        currentClothingItem?.let { item ->
            val intent = Intent(this, ClothesActivity::class.java).apply {
                putExtra("clothingItem", item)
            }
            editResultLauncher.launch(intent)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
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