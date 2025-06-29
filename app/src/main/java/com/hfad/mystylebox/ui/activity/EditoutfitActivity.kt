package com.hfad.mystylebox.ui.activity

import android.Manifest
import android.content.ContentResolver
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.CheckBox
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.google.android.flexbox.FlexboxLayout
import com.hfad.mystylebox.R
import com.hfad.mystylebox.adapter.ClothingAdapter
import com.hfad.mystylebox.database.AppDatabase
import com.hfad.mystylebox.database.entity.ClothingItemFull
import com.hfad.mystylebox.database.entity.Outfit
import com.hfad.mystylebox.database.dao.OutfitDao
import com.hfad.mystylebox.database.dao.OutfitTagDao
import com.hfad.mystylebox.database.entity.Tag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class EditoutfitActivity : AppCompatActivity() {
    private val REQUEST_READ_STORAGE = 101

    private lateinit var outfitDao: OutfitDao
    private lateinit var outfitTagDao: OutfitTagDao
    private lateinit var flexboxTags: FlexboxLayout
    private lateinit var recyclerViewClothes: RecyclerView
    private var currentOutfit: Outfit? = null
    private var imageUri: Uri? = null

    private val editResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val updatedItem: Outfit? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                result.data?.getParcelableExtra("updated_item", Outfit::class.java)
            else result.data?.getParcelableExtra("updated_item")
            val newUriString = result.data?.getStringExtra("image_uri")
            if (newUriString != null) {
                imageUri = Uri.parse(newUriString)
            }
            if (updatedItem != null) {
                updatePreviewUI(updatedItem)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editoutfits)

        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "wardrobe_db"
        ).build()
        outfitDao = db.outfitDao()
        outfitTagDao = db.outfitTagDao()

        val extras = intent.extras
        if (extras != null) {
            Log.d("EditoutfitActivity", "Получены extras: ${extras.keySet()}")
        } else {
            Log.d("EditoutfitActivity", "Extras отсутствуют")
        }

        currentOutfit = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) intent.getParcelableExtra("outfit", Outfit::class.java) else intent.getParcelableExtra("outfit")
        if (currentOutfit != null) {
            findViewById<TextView>(R.id.textviewname).text = currentOutfit!!.name
        } else {
            Toast.makeText(this, "Данные не получены", Toast.LENGTH_SHORT).show()
        }

        val uriString = intent.getStringExtra("image_uri")
           if (uriString.isNullOrEmpty()) {
                   Log.d("EditoutfitActivity", "image_uri отсутствует в Intent")
                   Toast.makeText(this, "URI отсутствует", Toast.LENGTH_SHORT).show()
               } else {
                   imageUri = Uri.parse(uriString)
                   Log.d("EditoutfitActivity", "imageUri = $imageUri")
               }

        flexboxTags = findViewById(R.id.Tags)
        recyclerViewClothes = findViewById(R.id.recyclerViewClothes)
        recyclerViewClothes.layoutManager = GridLayoutManager(this, 1, GridLayoutManager.HORIZONTAL, false)
        recyclerViewClothes.adapter = ClothingAdapter(emptyList(), R.layout.item_vivod)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
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

    // Метод для настройки UI (вызывается после проверки разрешений)
    private fun setupUI() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val llTemperature = findViewById<LinearLayout>(R.id.lltemperature)
        val textViewTemperature = findViewById<TextView>(R.id.textviewtemperature)
        val nameTextView = findViewById<TextView>(R.id.textviewname)
        val llNotes = findViewById<LinearLayout>(R.id.llNotes)
        val llSeason = findViewById<LinearLayout>(R.id.llseason)
        val scrollviewinfo = findViewById<HorizontalScrollView>(R.id.scrollviewinfo)
        val notesTextView = findViewById<TextView>(R.id.textViewDescription)
        val checkboxSummer = findViewById<CheckBox>(R.id.cbSummer)
        val checkboxSpring = findViewById<CheckBox>(R.id.cbSpring)
        val checkboxAutumn = findViewById<CheckBox>(R.id.cbAutumn)
        val checkboxWinter = findViewById<CheckBox>(R.id.cbWinter)

        val deleteButton = findViewById<ImageButton>(R.id.buttondelete)
        deleteButton.setOnClickListener {
            currentOutfit?.let { item ->
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Подтверждение удаления")
                    .setMessage("Вы точно хотите удалить этот комплект?")
                    .setPositiveButton("Да") { dialog, _ ->
                        lifecycleScope.launch(Dispatchers.IO) {
                            outfitDao.delete(item)
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@EditoutfitActivity, "Комплект удалён", Toast.LENGTH_SHORT).show()
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

        val editButton = findViewById<ImageButton>(R.id.buttonedit)
        editButton.setOnClickListener {
            currentOutfit?.let { item ->
                navigateToOutfitActivity()
            }
        }

        val shareButton = findViewById<ImageButton>(R.id.buttonshareit)
        shareButton.setOnClickListener {
            val uri = imageUri
            if (uri == null) {
                Toast.makeText(this, "Изображение не найдено", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            try {
                val contentUri = when (uri.scheme) {
                    ContentResolver.SCHEME_CONTENT -> {
                        uri
                    }
                    ContentResolver.SCHEME_FILE -> {
                        val file = File(uri.path!!)
                        FileProvider.getUriForFile(
                            this,
                            "$packageName.fileprovider",
                            file
                        )
                    }
                    else -> {
                        val file = File(uri.path ?: uri.toString())
                        FileProvider.getUriForFile(
                            this,
                            "$packageName.fileprovider",
                            file
                        )
                    }
                }

                // 3) Запускаем Intent ACTION_SEND
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/*"
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    putExtra(Intent.EXTRA_TEXT, "Посмотрите на этот комплект!")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(shareIntent, "Поделиться изображением"))

            } catch (e: Exception) {
                Toast.makeText(
                    this,
                    "Ошибка подготовки шаринга: ${e.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()
                Log.e("EDIT_DEBUG", "Ошибка шаринга", e)
            }
        }

        currentOutfit?.let { item ->
            nameTextView.text = item.name
            if (item.description.isNullOrEmpty()) {
                llNotes.visibility = View.GONE
            } else {
                llNotes.visibility = View.VISIBLE
                notesTextView.text = item.description
            }
            if (item.minTemp == -99) {
                llTemperature.visibility = View.GONE
            } else {
                llTemperature.visibility = View.VISIBLE
                textViewTemperature.text = "От +${item.minTemp}°C\nдо +${item.maxTemp}°C"
            }
            if (item.seasons.isNullOrEmpty()) {
                llSeason.visibility = View.GONE
            } else {
                llSeason.visibility = View.VISIBLE
                checkboxSummer.isChecked = item.seasons.contains("Лето")
                checkboxSpring.isChecked = item.seasons.contains("Весна")
                checkboxAutumn.isChecked = item.seasons.contains("Осень")
                checkboxWinter.isChecked = item.seasons.contains("Зима")
            }
            if(llSeason.visibility == View.GONE && llTemperature.visibility == View.GONE)
            {
                scrollviewinfo.visibility = View.GONE
            }
            loadImage()

            lifecycleScope.launch(Dispatchers.IO) {
                val tags = outfitTagDao.getTagsForOutfit(item.id)
                withContext(Dispatchers.Main) {
                    if (tags.isNotEmpty()) {
                        findViewById<LinearLayout>(R.id.llTegi).visibility = View.VISIBLE
                        displayTagsForPreview(tags)
                    } else {
                        findViewById<LinearLayout>(R.id.llTegi).visibility = View.GONE
                    }
                }
            }
            lifecycleScope.launch(Dispatchers.IO) {
                val clothingItems = outfitDao.getClothingItemsForOutfit(item.id)
                val clothingItemsFull = clothingItems.map { clothingItem ->
                    ClothingItemFull(clothingItem = clothingItem)
                }
                withContext(Dispatchers.Main) {
                    (recyclerViewClothes.adapter as ClothingAdapter).updateData(clothingItemsFull)
                }
            }
        } ?: run {
            Toast.makeText(this, "Данные не получены", Toast.LENGTH_SHORT).show()
        }
    }

    // Метод загрузки изображения с проверкой корректности URI и файла
    private fun loadImage() {
        val iv = findViewById<ImageView>(R.id.outfitImageView)
        val uri = imageUri
        if (uri == null) {
            Toast.makeText(this, "URI отсутствует", Toast.LENGTH_SHORT).show()
            return
        }
        when (uri.scheme) {
            ContentResolver.SCHEME_CONTENT -> {
                Glide.with(this)
                    .load(uri)
                    .apply(RequestOptions()
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .skipMemoryCache(true)
                        .format(DecodeFormat.PREFER_ARGB_8888))
                    .into(iv)
            }
            ContentResolver.SCHEME_FILE -> {
                val file = File(uri.path!!)
                if (file.exists()) {
                    Glide.with(this).load(file).into(iv)
                } else {
                    Toast.makeText(this, "Файл не найден", Toast.LENGTH_SHORT).show()
                }
            }
            else -> {
                val file = File(uri.path ?: uri.toString())
                if (file.exists()) {
                    Glide.with(this).load(file).into(iv)
                } else {
                    Glide.with(this).load(uri).into(iv)
                    Toast.makeText(this, "Невозможно проверить файл, пытаюсь загрузить URI", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Метод обновления интерфейса после редактирования комплекта
    private fun updatePreviewUI(item: Outfit) {
        currentOutfit = item

        findViewById<TextView>(R.id.textviewname).text = item.name
        findViewById<TextView>(R.id.textViewDescription).text = item.description

        val checkboxSummer = findViewById<CheckBox>(R.id.cbSummer)
        val checkboxSpring = findViewById<CheckBox>(R.id.cbSpring)
        val checkboxAutumn = findViewById<CheckBox>(R.id.cbAutumn)
        val checkboxWinter = findViewById<CheckBox>(R.id.cbWinter)

        val llSeason = findViewById<LinearLayout>(R.id.llseason)
        if (item.seasons.isNullOrEmpty()) {
            llSeason.visibility = View.GONE
        } else {
            llSeason.visibility = View.VISIBLE
            checkboxSummer.isChecked = item.seasons.contains("Лето")
            checkboxSpring.isChecked = item.seasons.contains("Весна")
            checkboxAutumn.isChecked = item.seasons.contains("Осень")
            checkboxWinter.isChecked = item.seasons.contains("Зима")
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val tags = outfitTagDao.getTagsForOutfit(item.id)
            withContext(Dispatchers.Main) {
                val llTegi = findViewById<LinearLayout>(R.id.llTegi)
                val llTemperature = findViewById<LinearLayout>(R.id.lltemperature)
                val textViewTemperature = findViewById<TextView>(R.id.textviewtemperature)
                if (tags.isNotEmpty()) {
                    llTegi.visibility = View.VISIBLE
                    flexboxTags.visibility = View.VISIBLE
                    displayTagsForPreview(tags)
                } else {
                    llTegi.visibility = View.GONE
                    flexboxTags.visibility = View.GONE
                }
                if (item.minTemp == -99) {
                    llTemperature.visibility = View.GONE
                } else {
                    llTemperature.visibility = View.VISIBLE
                    textViewTemperature.text = "От +${item.minTemp}°C\nдо +${item.maxTemp}°C"
                }
            }
        }

        loadImage()
    }

    // Метод для отображения тегов в контейнере FlexboxLayout
    private fun displayTagsForPreview(tags: List<Tag>) {
        flexboxTags.removeAllViews()
        tags.forEach { tag ->
            val textView = TextView(this).apply {
                text = tag.name
                setPadding(16, 8, 16, 8)
                setBackgroundResource(R.drawable.checkbox_background)
                isClickable = false
            }
            val params = FlexboxLayout.LayoutParams(
                FlexboxLayout.LayoutParams.WRAP_CONTENT,
                FlexboxLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(8, 8, 8, 8) }
            flexboxTags.addView(textView, params)
        }
    }

    // Метод для перехода в активность редактирования комплекта
    private fun navigateToOutfitActivity() {
        currentOutfit?.let { outfit  ->
            val intent = Intent(this, OutfitActivity::class.java).apply {
                putExtra("outfit", outfit)
                imageUri?.let { putExtra("image_uri", it) }
            }
            editResultLauncher.launch(intent)
        }
    }

    // Обработка результата запроса разрешений
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_READ_STORAGE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupUI()
            } else {
                Toast.makeText(this,  "Доступ к фото не предоставлен. Закрытие экрана.",
                    Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }
}