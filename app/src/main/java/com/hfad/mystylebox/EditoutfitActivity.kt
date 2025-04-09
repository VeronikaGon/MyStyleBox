package com.hfad.mystylebox

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.google.android.flexbox.FlexboxLayout
import com.hfad.mystylebox.adapter.ClothingAdapter
import com.hfad.mystylebox.database.AppDatabase
import com.hfad.mystylebox.database.ClothingItemFull
import com.hfad.mystylebox.database.Outfit
import com.hfad.mystylebox.database.OutfitDao
import com.hfad.mystylebox.database.OutfitTagDao
import com.hfad.mystylebox.database.Tag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class EditoutfitActivity  : AppCompatActivity() {
    private val REQUEST_READ_STORAGE = 101

    private lateinit var outfitDao: OutfitDao
    private lateinit var outfitTagDao: OutfitTagDao
    private lateinit var flexboxTags: FlexboxLayout
    private lateinit var recyclerViewClothes: RecyclerView
    private var currentOutfit: Outfit? = null

    private val editResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val updatedItem = result.data?.getParcelableExtra<Outfit>("updated_item")
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

        val receivedOutfit = intent.getParcelableExtra<Outfit>("outfit")
        if (receivedOutfit != null) {
            findViewById<TextView>(R.id.textviewname).text = receivedOutfit.name
        } else {
            Toast.makeText(this, "Данные не получены", Toast.LENGTH_SHORT).show()
        }
        flexboxTags = findViewById(R.id.Tags)
        recyclerViewClothes = findViewById(R.id.recyclerViewClothes)
        recyclerViewClothes.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recyclerViewClothes.adapter = ClothingAdapter(emptyList(), R.layout.item_clothing)

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

    // Метод для настройки UI
    private fun setupUI() {
        val llTemperature = findViewById<LinearLayout>(R.id.lltemperature)
        val textViewTemperature = findViewById<TextView>(R.id.textviewtemperature)

        val outfitImageView = findViewById<ImageView>(R.id.outfitImageView)
        val imageUriString = intent.getStringExtra("image_uri")

        Log.d("EditoutfitActivity", "Получен image_uri: $imageUriString")

        if (!imageUriString.isNullOrEmpty()) {
            try {
                val uri = Uri.parse(imageUriString)
                Log.d("EditoutfitActivity", "Попытка загрузить изображение из URI: $uri")
                Glide.with(this)
                    .load(uri)
                    .listener(object : RequestListener<Drawable> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: com.bumptech.glide.request.target.Target<Drawable>,
                            isFirstResource: Boolean
                        ): Boolean {
                            Log.e("EditoutfitActivity", "Ошибка загрузки изображения", e)
                            return false  // Позволяет Glide обработать ошибку стандартным способом
                        }

                        override fun onResourceReady(
                            resource: Drawable,
                            model: Any,
                            target: com.bumptech.glide.request.target.Target<Drawable>,
                            dataSource: com.bumptech.glide.load.DataSource,
                            isFirstResource: Boolean
                        ): Boolean {
                            Log.d("EditoutfitActivity", "Изображение успешно загружено")
                            return false
                        }
                    })
                    .into(outfitImageView)
            } catch (e: Exception) {
                Log.e("EditoutfitActivity", "Исключение при загрузке изображения", e)
                Toast.makeText(this, "Некорректный URI изображения", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Изображение не найдено", Toast.LENGTH_SHORT).show()
        }


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val outfit = intent.getParcelableExtra<Outfit>("outfit")
        if (outfit != null) {
            currentOutfit = outfit
        } else {
            Toast.makeText(this, "Данные не получены", Toast.LENGTH_SHORT).show()
        }

        val nameTextView = findViewById<TextView>(R.id.textviewname)
        val imageView = findViewById<ImageView>(R.id.outfitImageView)
        val llNotes = findViewById<LinearLayout>(R.id.llNotes)
        val llSeason = findViewById<LinearLayout>(R.id.llseason)
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
                    .setMessage("Вы точно хотите удалить этот аутфит?")
                    .setPositiveButton("Да") { dialog, _ ->
                        lifecycleScope.launch(Dispatchers.IO) {
                            outfitDao.delete(item)
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@EditoutfitActivity, "Аутфит удалён", Toast.LENGTH_SHORT).show()
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
            val imageUriString = intent.getStringExtra("image_uri")
            if (!imageUriString.isNullOrEmpty()) {
                try {
                    val file = File(Uri.parse(imageUriString).path!!)
                    val contentUri = androidx.core.content.FileProvider.getUriForFile(
                        this,
                        "$packageName.fileprovider",
                        file
                    )
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "image/*"
                        putExtra(Intent.EXTRA_STREAM, contentUri)
                        putExtra(Intent.EXTRA_TEXT, "Посмотрите на этот аутфит!")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    startActivity(Intent.createChooser(shareIntent, "Поделиться изображением"))
                } catch (e: Exception) {
                    Toast.makeText(this, "Ошибка подготовки изображения: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Изображение не найдено", Toast.LENGTH_SHORT).show()
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

    private fun updatePreviewUI(item: Outfit) {
        currentOutfit = item
        findViewById<TextView>(R.id.textviewname).text = item.name
        findViewById<TextView>(R.id.textViewDescription).text = item.description

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

        val outfitimageView = findViewById<ImageView>(R.id.outfitImageView)
        val imageUriString = intent.getStringExtra("image_uri")
        if (!imageUriString.isNullOrEmpty()) {
            try {
                val uri = Uri.parse(imageUriString)
                Log.d("EditoutfitActivity", "URI изображения: $uri")
                Glide.with(this)
                    .load(uri)
                    .into(outfitimageView)
            } catch (e: Exception) {
                Toast.makeText(this, "Некорректный URI изображения", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Изображение не найдено", Toast.LENGTH_SHORT).show()
        }
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

    // Метод для перехода в активность, где происходит редактирование аутфита
    private fun navigateToOutfitActivity() {
        currentOutfit?.let { item ->
            val intent = Intent(this, OutfitActivity::class.java).apply {
                putExtra("outfit", item)
            }
            editResultLauncher.launch(intent)
        }
    }

    // Обработка результата запроса разрешений
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