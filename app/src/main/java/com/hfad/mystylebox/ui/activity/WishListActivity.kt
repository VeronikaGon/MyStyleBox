package com.hfad.mystylebox.ui.activity

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.children
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.bumptech.glide.Glide
import com.google.android.flexbox.FlexboxLayout
import com.hfad.mystylebox.R
import com.hfad.mystylebox.database.AppDatabase
import com.hfad.mystylebox.database.entity.ClothingItem
import com.hfad.mystylebox.database.dao.ClothingItemDao
import com.hfad.mystylebox.database.entity.ClothingItemTag
import com.hfad.mystylebox.database.dao.ClothingItemTagDao
import com.hfad.mystylebox.database.dao.SubcategoryDao
import com.hfad.mystylebox.database.dao.WishListItemDao
import com.hfad.mystylebox.database.entity.Tag
import com.hfad.mystylebox.database.entity.WishListItem
import com.hfad.mystylebox.ui.bottomsheet.ImageOptionsBottomSheet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WishListActivity : AppCompatActivity(), ImageOptionsBottomSheet.ImageOptionsListener {
    private lateinit var db: AppDatabase
    private var editingItem: WishListItem? = null
    private var isInEditMode = false
    private var imagePath: String? = null
    private lateinit var subcatDao: SubcategoryDao
    private lateinit var dao: WishListItemDao
    private lateinit var ivPhoto: ImageView
    private lateinit var btnEditPhoto: ImageButton
    private lateinit var etName: EditText
    private lateinit var etPrice: EditText
    private lateinit var etNotes: EditText
    private lateinit var flexSize: FlexboxLayout
    private lateinit var rgGender: RadioGroup
    private lateinit var tvCategory: TextView
    private lateinit var tvTitle: TextView
    private lateinit var btnSave: Button

    private lateinit var editImageLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.wishlist)

        db = AppDatabase.getInstance(this)
        subcatDao = db.subcategoryDao()
        dao = db.wishListItemDao()

        ivPhoto = findViewById(R.id.clothingImageView)
        btnEditPhoto = findViewById(R.id.editimage)
        etName = findViewById(R.id.enterName)
        etPrice = findViewById(R.id.enterStoimost)
        etNotes = findViewById(R.id.enterNotes)
        flexSize = findViewById(R.id.flexboxLayout)
        rgGender = findViewById(R.id.radioGroupGender)
        tvCategory = findViewById(R.id.categoryField)
        tvTitle = findViewById(R.id.textviewtitle)
        btnSave = findViewById(R.id.ButtonSAVE)

        editingItem = intent.getParcelableExtra("wishItem")
        isInEditMode = editingItem != null
        val subcatId = intent.getIntExtra("subcategory_id", -1)
        if (isInEditMode) {
            tvTitle.text = "Редактировать вещь в списке желаний"
            btnSave.text = "Обновить"
            populateFields(editingItem!!)
        }
        tvCategory.text = subcatDao.getSubcategoryNameById(subcatId)
        val imageUriString = intent.getStringExtra("image_uri").orEmpty()
        imagePath = imageUriString
        findViewById<ImageView>(R.id.clothingImageView).let {
            Glide.with(this).load(imageUriString).into(it)
        }

        editImageLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                val newUri = result.data
                    ?.getStringExtra("image_uri")
                    ?: return@registerForActivityResult
                imagePath = newUri
                Glide.with(this).load(newUri).into(ivPhoto)
            }
        }
        btnEditPhoto.setOnClickListener {
            val intent = Intent(this, EditImageActivity::class.java)
                .putExtra("imageUri", imagePath)
                .putExtra("origin", "wishlist")
            editImageLauncher.launch(intent)
        }
        setupSizeSelection()

        findViewById<Button>(R.id.ButtonSAVE).setOnClickListener {
            val name = etName.text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(this, "Введите название", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val priceText = etPrice.text.toString().trim()
            if (priceText.isEmpty()) {
                Toast.makeText(this, "Введите стоимость", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val price = priceText.toDoubleOrNull()
            if (price == null) {
                Toast.makeText(this, "Некорректная стоимость", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val notes = etNotes.text.toString().trim()
            val size = flexSize.children
                .filterIsInstance<RadioButton>()
                .firstOrNull { it.isChecked }?.text?.toString() ?: ""
            val gender = rgGender.checkedRadioButtonId.takeIf { it != -1 }
                ?.let { findViewById<RadioButton>(it).text.toString() }
                ?: "Универсально"

            val wish = WishListItem(
                imagePath.orEmpty(),
                name, price, notes,
                size, subcatId, gender
            )
            lifecycleScope.launch(Dispatchers.IO) {
                if (isInEditMode) {
                    wish.id = editingItem!!.id
                    dao.update(wish)
                } else {
                    dao.insert(wish)
                }
                withContext(Dispatchers.Main) {
                    setResult(Activity.RESULT_OK)
                    finish()
                }
            }
        }
    }

    override fun onEditOptionSelected() = startEditImage()
    override fun onChangePhotoOptionSelected() = startEditImage()
    private fun startEditImage() {
        startEditActivityForChangePhoto()
    }

    private fun startEditActivityForChangePhoto() {
        val intent = Intent(this, EditImageActivity::class.java)
            .putExtra("imageUri", imagePath)
        editImageLauncher.launch(intent)
    }

    /** Запрет вылета без сохранения */
    override fun onBackPressed() {
        if (hasUnsavedChanges()) {
            AlertDialog.Builder(this)
                .setTitle("Выйти без сохранения?")
                .setMessage(
                    if (isInEditMode) "Изменения не сохранятся"
                    else "Созданная вещь не будет сохранена"
                )
                .setPositiveButton("Выйти") { _, _ -> super.onBackPressed() }
                .setNegativeButton("Отмена", null)
                .show()
        } else {
            super.onBackPressed()
        }
    }

    private fun hasUnsavedChanges(): Boolean {
        // если хоть одно поле изменилось относительно editingItem
        val orig = editingItem
        if (orig == null) {
            return etName.text.isNotBlank()
                    || etPrice.text.isNotBlank()
                    || etNotes.text.isNotBlank()
        }
        return etName.text.toString() != orig.name
                || etPrice.text.toString() != orig.price.toString()
                || etNotes.text.toString() != orig.notes
                || flexSize.children
            .filterIsInstance<RadioButton>()
            .firstOrNull { it.isChecked }?.text != orig.size
                || findViewById<RadioButton>(rgGender.checkedRadioButtonId).text != orig.gender
                || imagePath != orig.imagePath
    }

    private fun setupSizeSelection() {
        val flexbox = findViewById<FlexboxLayout>(R.id.flexboxLayout)
        for (i in 0 until flexbox.childCount) {
            (flexbox.getChildAt(i) as? RadioButton)?.apply {
                setOnClickListener {
                    for (j in 0 until flexbox.childCount) {
                        (flexbox.getChildAt(j) as? RadioButton)?.isChecked = false
                    }
                    isChecked = true
                }
            }
        }
    }

    // Метод для заполнения полей при редактировании
    private fun populateFields(item: WishListItem) {
        imagePath = item.imagePath
        Glide.with(this).load(item.imagePath).into(ivPhoto)
        etName.setText(item.name)
        etPrice.setText(item.price.toString())
        etNotes.setText(item.notes)
        setSelectedSize(item.size)
        setSelectedGender(item.gender)
    }

    // Установка выбранного размера
    private fun setSelectedSize(size: String) {
        flexSize.children.filterIsInstance<RadioButton>()
            .firstOrNull { it.text == size }?.isChecked = true
    }

    // Установка выбранного гендера
    private fun setSelectedGender(g: String) {
        (0 until rgGender.childCount)
            .map { rgGender.getChildAt(it) }
            .filterIsInstance<RadioButton>()
            .firstOrNull { it.text == g }
            ?.isChecked = true
    }
}