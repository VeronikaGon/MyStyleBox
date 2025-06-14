package com.hfad.mystylebox.ui.activity

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
import com.hfad.mystylebox.database.entity.ClothingItem
import android.Manifest
import android.content.Intent
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.bumptech.glide.Glide
import com.google.android.flexbox.FlexboxLayout
import com.hfad.mystylebox.R
import com.hfad.mystylebox.adapter.OutfitAdapter
import com.hfad.mystylebox.database.AppDatabase
import com.hfad.mystylebox.database.dao.ClothingItemDao
import com.hfad.mystylebox.database.entity.ClothingItemFull
import com.hfad.mystylebox.database.dao.ClothingItemTagDao
import com.hfad.mystylebox.database.dao.SubcategoryDao
import com.hfad.mystylebox.database.entity.Tag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class EditclothesActivity : AppCompatActivity() {
    private val REQUEST_READ_STORAGE = 101
    private lateinit var subcategoryDao: SubcategoryDao
    private lateinit var clothingItemDao: ClothingItemDao
    private lateinit var clothingItemTagDao: ClothingItemTagDao
    private lateinit var flexboxTags: FlexboxLayout
    private var currentClothingItem: ClothingItem? = null
    private val selectedItems = mutableSetOf<ClothingItemFull>()
    private lateinit var outfitAdapter: OutfitAdapter

    private val editResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val updatedItem = result.data?.getParcelableExtra<ClothingItem>("updated_item")
            if (updatedItem != null) {
                updatePreviewUI(updatedItem)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "wardrobe_db"
        ).build()
        subcategoryDao = db.subcategoryDao()
        clothingItemDao = db.clothingItemDao()
        clothingItemTagDao = db.clothingItemTagDao()
        enableEdgeToEdge()
        setContentView(R.layout.activity_editclothes)
        flexboxTags = findViewById(R.id.Tags)
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
        val recyclerViewOutfits = findViewById<RecyclerView>(R.id.recyclerViewOutfits)
        outfitAdapter = OutfitAdapter(emptyList(), R.layout.item_vivod)
        recyclerViewOutfits.layoutManager = GridLayoutManager(this, 1, GridLayoutManager.HORIZONTAL, false)
        recyclerViewOutfits.adapter = outfitAdapter

        val buttonAddOutfit = findViewById<Button>(R.id.ButtonAddOutfit)
        buttonAddOutfit.setOnClickListener {
            currentClothingItem?.let { item ->
                val intent = Intent(this, BoardActivity::class.java).apply {
                    putIntegerArrayListExtra("selected_item_ids", arrayListOf(item.id))
                    putStringArrayListExtra("selected_image_paths", arrayListOf(item.imagePath))
                }
                startActivity(intent)
            } ?: run {
                Toast.makeText(this, "Вещь не выбрана", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun updateOutfitsContainer(clothingItemId: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = Room.databaseBuilder(
                applicationContext,
                AppDatabase::class.java, "wardrobe_db"
            ).build()
            val outfitsList = db.outfitDao().getOutfitsForClothingItem(clothingItemId)
            val count = outfitsList.size

            withContext(Dispatchers.Main) {
                val llOutfits = findViewById<LinearLayout>(R.id.lloutfits)
                val tv101 = findViewById<TextView>(R.id.textView101)
                if (count == 0) {
                    llOutfits.visibility = View.GONE
                } else {
                    llOutfits.visibility = View.VISIBLE
                    tv101.text = when (count) {
                        1 -> "1 комплект с вещью"
                        in 2..4 -> "$count комплекта с вещью"
                        else -> "$count комплектов с вещью"
                    }
                }
                outfitAdapter.updateData(outfitsList)
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
            updateOutfitsContainer(clothingItem.id)
        } else {
            Toast.makeText(this, "Данные не получены", Toast.LENGTH_SHORT).show()
        }

        // Инициализация остальных элементов
        val llStoimost = findViewById<LinearLayout>(R.id.llstoimost)
        val llSeason = findViewById<LinearLayout>(R.id.llseason)
        val llSize = findViewById<LinearLayout>(R.id.llsize)
        val llBrend = findViewById<LinearLayout>(R.id.llbrend)
        val llNotes = findViewById<LinearLayout>(R.id.llNotes)
        val llTegi = findViewById<LinearLayout>(R.id.llTegi)
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
                try {
                    val file = File(Uri.parse(imageUriString).path!!)
                    val contentUri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "image/*"
                        putExtra(Intent.EXTRA_STREAM, contentUri)
                        putExtra(Intent.EXTRA_TEXT, "Посмотрите на эту вещь!")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    startActivity(Intent.createChooser(shareIntent, "Поделиться изображением"))
                } catch (e: Exception) {
                    Toast.makeText(this, "Ошибка при подготовке изображения: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Изображение не найдено", Toast.LENGTH_SHORT).show()
            }
        }
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
               lifecycleScope.launch(Dispatchers.IO) {
                   val tags = clothingItemTagDao.getTagsForClothingItem(item.id)
                   withContext(Dispatchers.Main) {
                       if (tags.isNotEmpty()) {
                           llTegi.visibility = View.VISIBLE
                           displayTagsForPreview(tags)
                       } else {
                           llTegi.visibility = View.GONE
                       }
                   }
               }
               val imagePath = item.imagePath
               if (!imagePath.isNullOrEmpty()) {
                   try {
                       val uri = Uri.parse(imagePath)
                       Glide.with(this)
                           .load(uri)          // Glide примет и content://, и file://
                           .into(imageView)
                   } catch (e: Exception) {
                       Toast.makeText(
                           this,
                           "Не удалось загрузить изображение: ${e.message}",
                           Toast.LENGTH_SHORT
                       ).show()
                       Log.e("EDIT_DEBUG", "Ошибка загрузки картинки: ${e.localizedMessage}", e)
                   }
               } else {
                   Toast.makeText(this, "Изображение не найдено", Toast.LENGTH_SHORT).show()
               }
           }
    }
    //Обновление после редактирования
    private fun updatePreviewUI(item: ClothingItem) {
        currentClothingItem = item
        findViewById<TextView>(R.id.status).text = item.status
        findViewById<TextView>(R.id.textviewsize).text = item.size
        findViewById<TextView>(R.id.textviewbrend).text = item.brend
        findViewById<TextView>(R.id.textViewNotes).text = item.notes
        findViewById<TextView>(R.id.textviewstoimost).text = item.cost.toString()
        findViewById<TextView>(R.id.textviewgender).text = item.gender
        findViewById<TextView>(R.id.textviewname).text = item.name

        lifecycleScope.launch {
            val subcategoryName = withContext(Dispatchers.IO) {
                subcategoryDao.getSubcategoryNameById(item.subcategoryId)
            }
            findViewById<TextView>(R.id.textviewcategory).text  = subcategoryName
        }

        val llNotes = findViewById<LinearLayout>(R.id.llNotes)
        llNotes.visibility = if (!item.notes.isNullOrEmpty()) View.VISIBLE else View.GONE

        val llSize = findViewById<LinearLayout>(R.id.llsize)
        llSize.visibility = if (!item.size.isNullOrEmpty()) View.VISIBLE else View.GONE

        val llBrend = findViewById<LinearLayout>(R.id.llbrend)
        llBrend.visibility = if (!item.brend.isNullOrEmpty()) View.VISIBLE else View.GONE

        val llStoimost = findViewById<LinearLayout>(R.id.llstoimost)
        llStoimost.visibility = if (item.cost != 0f) View.VISIBLE else View.GONE

        val llSeason = findViewById<LinearLayout>(R.id.llseason)
        llSeason.visibility = if (!item.seasons.isNullOrEmpty()) View.VISIBLE else View.GONE

        findViewById<CheckBox>(R.id.cbSummer).isChecked = item.seasons.contains("Лето")
        findViewById<CheckBox>(R.id.cbSpring).isChecked = item.seasons.contains("Весна")
        findViewById<CheckBox>(R.id.cbAutumn).isChecked = item.seasons.contains("Осень")
        findViewById<CheckBox>(R.id.cbWinter).isChecked = item.seasons.contains("Зима")

        val imageView = findViewById<ImageView>(R.id.clothingImageView)

        item.imagePath?.takeIf { it.isNotEmpty() }?.let { path ->
            val uri = Uri.parse(path)
            Glide.with(this)
                .load(uri)
                .into(imageView)
        }

        val llTegi = findViewById<LinearLayout>(R.id.llTegi)
        lifecycleScope.launch(Dispatchers.IO) {
            val tags = clothingItemTagDao.getTagsForClothingItem(item.id)
            withContext(Dispatchers.Main) {
                if (tags.isNotEmpty()) {
                    llTegi.visibility = View.VISIBLE
                    flexboxTags.visibility = View.VISIBLE
                    displayTagsForPreview(tags)
                } else {
                    llTegi.visibility = View.GONE
                    flexboxTags.visibility = View.GONE
                }
            }
        }
    }
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
            ).apply {
                setMargins(8, 8, 8, 8)
            }
            flexboxTags.addView(textView, params)
        }
    }
    private fun navigateToClothesActivity() {
        currentClothingItem?.let { item ->
            val intent = Intent(this, ClothesActivity::class.java).apply {
                putExtra("clothingItem", item)
            }
            editResultLauncher.launch(intent)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_READ_STORAGE) {
            if (grantResults.isNotEmpty()
                && grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                setupUI()
            } else {
                Toast.makeText(this,
                    "Доступ к фото не предоставлен. Закрытие экрана.",
                    Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }
}