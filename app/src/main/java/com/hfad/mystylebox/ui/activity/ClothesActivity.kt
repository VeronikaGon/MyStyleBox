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
import com.hfad.mystylebox.database.entity.Tag
import com.hfad.mystylebox.ui.bottomsheet.ImageOptionsBottomSheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ClothesActivity : AppCompatActivity(), ImageOptionsBottomSheet.ImageOptionsListener {

    private lateinit var db: AppDatabase
    private lateinit var clothingItemDao: ClothingItemDao
    private lateinit var subcategoryDao: SubcategoryDao
    private lateinit var clothingItemTagDao: ClothingItemTagDao

    private lateinit var flexboxTags: FlexboxLayout
    private var selectedTagIds: MutableSet<Int> = mutableSetOf()

    private lateinit var clothingImageView: ImageView
    private lateinit var clothingNameEditText: EditText
    private lateinit var clothingBrendEditText: EditText
    private lateinit var clothingCostEditText: EditText
    private lateinit var clothingNotesEditText: EditText
    private lateinit var saveButton: Button
    private lateinit var categoryField: TextView
    private lateinit var textviewTitle: TextView
    private lateinit var cbSummer: CheckBox
    private lateinit var cbSpring: CheckBox
    private lateinit var cbWinter: CheckBox
    private lateinit var cbAutumn: CheckBox

    private var imagePath: String? = null
    private var subcategory: String? = null
    private var selectedSubcategoryId: Int = -1
    private var isReselection: Boolean = false
    private var isInEditMode: Boolean = false
    private var photoUri: Uri? = null

    private lateinit var categoryResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var tagEditingLauncher: ActivityResultLauncher<Intent>
    private lateinit var editImageActivityLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.clothes)

        // Инициализация БД и DAO
        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "wardrobe_db"
        ).allowMainThreadQueries().build()
        clothingItemDao = db.clothingItemDao()
        subcategoryDao = db.subcategoryDao()
        clothingItemTagDao = db.clothingItemTagDao()

        flexboxTags = findViewById(R.id.Tags)
        clothingImageView = findViewById(R.id.clothingImageView)
        clothingNameEditText = findViewById(R.id.enterName)
        clothingBrendEditText = findViewById(R.id.enterBrend)
        clothingCostEditText = findViewById(R.id.enterStoimost)
        clothingNotesEditText = findViewById(R.id.enterNotes)
        cbSummer = findViewById(R.id.cbSummer)
        cbSpring = findViewById(R.id.cbSpring)
        cbWinter = findViewById(R.id.cbWinter)
        cbAutumn = findViewById(R.id.cbAutumn)
        saveButton = findViewById(R.id.ButtonSAVE)
        textviewTitle = findViewById(R.id.textviewtitle)
        categoryField = findViewById(R.id.categoryField)
        subcategory = intent.getStringExtra("subcategory")

        if (!subcategory.isNullOrEmpty() && clothingNameEditText.text.isEmpty()) {
            categoryField.text = subcategory
            clothingNameEditText.setText(subcategory)
        }

        intent.getParcelableExtra<ClothingItem>("clothingItem")?.let { item ->
            lifecycleScope.launch(Dispatchers.IO) {
                val savedTagIds = clothingItemTagDao
                    .getTagsForClothingItem(item.id)
                    .map { it.id }
                    .toSet()
                withContext(Dispatchers.Main) {
                    selectedTagIds.clear()
                    selectedTagIds.addAll(savedTagIds)
                }
            }
        }

        tagEditingLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val returnedTags = result.data
                    ?.getParcelableArrayListExtra<Tag>("selected_tags")
                if (returnedTags != null) {
                    selectedTagIds.clear()
                    returnedTags.forEach { tag -> selectedTagIds.add(tag.id) }
                }
                loadTagsFromDatabase()
            }
        }
        loadTagsFromDatabase()

        findViewById<LinearLayout>(R.id.llTegi).setOnClickListener {
            val intent = Intent(this, TagEditingActivity::class.java)
            tagEditingLauncher.launch(intent)
        }
        findViewById<ImageButton>(R.id.imageButton).setOnClickListener {
            val intent = Intent(this, TagEditingActivity::class.java)
            tagEditingLauncher.launch(intent)
        }

        categoryResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val returnedSubcategory = result.data?.getStringExtra("subcategory")
                selectedSubcategoryId = result.data?.getIntExtra("selected_subcategory_id", -1) ?: -1
                categoryField.text = returnedSubcategory ?: "Не выбрано"
                if (clothingNameEditText.text.isEmpty()) {
                    clothingNameEditText.setText(returnedSubcategory)
                }
            }
        }
        editImageActivityLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val editedImageUriString = result.data?.getStringExtra("image_uri")
                if (!editedImageUriString.isNullOrEmpty()) {
                    val editedImageUri = Uri.parse(editedImageUriString)
                    Glide.with(this).load(editedImageUri).into(clothingImageView)
                    imagePath = editedImageUri.toString()
                } else {
                    Toast.makeText(this, "Ошибка получения результата редактирования", Toast.LENGTH_SHORT).show()
                }
            }
        }

        val editImageButton = findViewById<ImageButton>(R.id.editimage)
        editImageButton.setOnClickListener {
            val bottomSheet = ImageOptionsBottomSheet()
            bottomSheet.show(supportFragmentManager, "ImageOptionsBottomSheet")
        }

        categoryField.setOnClickListener {
            val intent = Intent(this, CategorySelectionActivity::class.java).apply {
                putExtra("image_uri", imagePath.toString())
                putExtra("name", clothingNameEditText.text.toString())
                putExtra("brend", clothingBrendEditText.text.toString())
                putExtra("cost", clothingCostEditText.text.toString())
                putExtra("notes", clothingNotesEditText.text.toString())
                putExtra("size", getSelectedSize())
                putExtra("status", getSelectedStatus())
                putExtra("gender", getSelectedGender())
                putExtra("is_reselection", true)
            }
            categoryResultLauncher.launch(intent)
        }

        // Восстанавливаем состояние, если оно было сохранено
        savedInstanceState?.let {
            clothingNameEditText.setText(it.getString("name"))
            clothingBrendEditText.setText(it.getString("brend"))
            clothingCostEditText.setText(it.getString("cost"))
            clothingNotesEditText.setText(it.getString("notes"))
            categoryField.text = it.getString("subcategory") ?: "Не выбрано"
            imagePath = it.getString("image_path")
        }

        // Получаем данные, переданные через Intent
        subcategory = intent.getStringExtra("subcategory")
        selectedSubcategoryId = intent.getIntExtra("selected_subcategory_id", -1)
        isReselection = intent.getBooleanExtra("is_reselection", false)
        imagePath = intent.getStringExtra("image_path")
        imagePath?.let { path ->
            clothingImageView.setImageURI(Uri.parse(path))
        }
        subcategory?.let {
            categoryField.text = it
        }

        // Обработка выбора размера через FlexboxLayout
        val flexboxLayout = findViewById<FlexboxLayout>(R.id.flexboxLayout)
        for (i in 0 until flexboxLayout.childCount) {
            val child = flexboxLayout.getChildAt(i)
            if (child is RadioButton) {
                child.setOnClickListener {
                    // Снимаем выбор со всех RadioButton
                    for (j in 0 until flexboxLayout.childCount) {
                        (flexboxLayout.getChildAt(j) as? RadioButton)?.isChecked = false
                    }
                    child.isChecked = true
                }
            }
        }

        // Если активность запущена для редактирования, заполняем поля данными существующего элемента
        val editingItem = intent.getParcelableExtra<ClothingItem>("clothingItem")
        imagePath?.let { path ->
            clothingImageView.setImageURI(Uri.parse(path))
        }
        if (editingItem != null) {
            populateFields(editingItem)
            textviewTitle.text = "Редактирование вещи"
            saveButton.text = "Обновить"
            isInEditMode = true
            saveButton.setOnClickListener { updateClothingItem(editingItem)
            }
        } else {
            saveButton.setOnClickListener { saveClothingItem() }
        }
    }

    override fun onEditOptionSelected() {
        startEditActivityForChangePhoto()
    }
    override fun onChangePhotoOptionSelected() {
        showImagePickerDialog()
    }
    // Реализация интерфейса ImageOptionsListener
    private fun startEditActivityForChangePhoto() {
        val intent = Intent(this, EditImageActivity::class.java).apply {
            putExtra("imageUri", imagePath)
            putExtra("origin", "clothes")
        }
        editImageActivityLauncher.launch(intent)
    }
    private fun startEditActivity(imageUri: Uri?) {
        if (imageUri == null) return
        val intent = Intent(this, EditImageActivity::class.java).apply {
            putExtra("imageUri", imageUri.toString())
            putExtra("origin", "clothes")
        }
        editImageActivityLauncher.launch(intent)
    }

    // Метод для выбора изображения (галерея, камера, файлы)
    private fun showImagePickerDialog() {
        val options = arrayOf("Выбрать из галереи", "Сфотографировать", "Выбрать из файлов")
        val icons = arrayOf(R.drawable.gallery, R.drawable.ic_camera, R.drawable.ic_file)
        val adapterDialog = object : BaseAdapter() {
            override fun getCount() = options.size
            override fun getItem(position: Int) = options[position]
            override fun getItemId(position: Int) = position.toLong()
            override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
                val view = convertView ?: LayoutInflater.from(this@ClothesActivity)
                    .inflate(R.layout.dialog_item, parent, false)
                val iconView = view.findViewById<ImageView>(R.id.icon)
                val textView = view.findViewById<TextView>(R.id.text)
                iconView.setImageResource(icons[position])
                textView.text = options[position]
                return view
            }
        }
        android.app.AlertDialog.Builder(this)
            .setTitle("Выберите действие")
            .setAdapter(adapterDialog) { _, which ->
                when (which) {
                    0 -> openGallery()
                    1 -> openCamera()
                    2 -> openFiles()
                }
            }
            .show()
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK).apply {
            setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/jpeg", "image/png"))
        }
        galleryLauncher.launch(intent)
    }

    private fun openCamera() {
        val photoFile = createImageFile()
        photoUri = FileProvider.getUriForFile(
            this, "${this.packageName}.fileprovider", photoFile
        )
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
        }
        cameraLauncher.launch(intent)
    }
    private fun openFiles() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" }
        fileLauncher.launch(intent)
    }
    private fun createImageFile(): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = this.getExternalFilesDir(null)
        return File.createTempFile("JPEG_${timestamp}_", ".jpg", storageDir)
    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val selectedImageUri: Uri? = result.data?.data
            startEditActivity(selectedImageUri)
        }
    }
    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            startEditActivity(photoUri)
        }
    }
    private val fileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val selectedImageUri: Uri? = result.data?.data
            startEditActivity(selectedImageUri)
        }
    }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("name", clothingNameEditText.text.toString())
        outState.putString("brend", clothingBrendEditText.text.toString())
        outState.putString("cost", clothingCostEditText.text.toString())
        outState.putString("notes", clothingNotesEditText.text.toString())
        outState.putString("subcategory", categoryField.text.toString())
        outState.putString("image_path", imagePath)
        outState.putIntegerArrayList("selected_tags_ids", ArrayList(selectedTagIds))
    }

    override fun onBackPressed() {
        if (clothingNameEditText.text.toString().isNotEmpty() ||
            clothingBrendEditText.text.toString().isNotEmpty() ||
            clothingCostEditText.text.toString().isNotEmpty() ||
            clothingNotesEditText.text.toString().isNotEmpty()
        ) {
            if (isInEditMode) {
                AlertDialog.Builder(this)
                    .setTitle("Выход")
                    .setMessage("Вы точно хотите выйти? Данные не будут обновлены.")
                    .setPositiveButton("Да") { _: DialogInterface, _: Int -> finish() }
                    .setNegativeButton("Нет", null)
                    .show()
            }
            else {
                AlertDialog.Builder(this)
                    .setTitle("Выход")
                    .setMessage("Вы точно хотите выйти? Данная вещь не будет сохранена.")
                    .setPositiveButton("Да") { _: DialogInterface, _: Int -> finish() }
                    .setNegativeButton("Нет", null)
                    .show()
            }
        } else {
            super.onBackPressed()
        }
    }

    // Метод для заполнения полей при редактировании
    private fun populateFields(item: ClothingItem) {
        clothingNameEditText.setText(item.name)
        clothingBrendEditText.setText(item.brend)
        clothingCostEditText.setText(item.cost.toString())
        clothingNotesEditText.setText(item.notes)
        cbSummer.isChecked = item.seasons?.contains("Лето") == true
        cbSpring.isChecked = item.seasons?.contains("Весна") == true
        cbWinter.isChecked = item.seasons?.contains("Зима") == true
        cbAutumn.isChecked = item.seasons?.contains("Осень") == true
        setSelectedSize(item.size)
        setSelectedStatus(item.status)
        setSelectedGender(item.gender)
        if (imagePath.isNullOrEmpty() && item.imagePath.isNotEmpty()) {
            imagePath = item.imagePath
            clothingImageView.setImageURI(Uri.parse(imagePath))
        }
        lifecycleScope.launch {
            val subcatName = withContext(Dispatchers.IO) { subcategoryDao.getSubcategoryNameById(item.subcategoryId) }
            categoryField.text = subcatName
        }
        selectedSubcategoryId = item.subcategoryId
        lifecycleScope.launch(Dispatchers.IO) {
            val savedTagIds = clothingItemTagDao.getTagsForClothingItem(item.id).map { it.id }.toSet()
            selectedTagIds.clear()
            selectedTagIds.addAll(savedTagIds)
            val allTags = db.tagDao().getAllTags()
            withContext(Dispatchers.Main) { displayTagsAsCheckboxes(allTags, selectedTagIds) }
        }
    }
    // Метод для обновления существующего элемента
    private fun updateClothingItem(oldItem: ClothingItem) {
        val name = clothingNameEditText.text.toString().trim()
        if (name.isEmpty()) {
            Toast.makeText(this, "Введите название", Toast.LENGTH_SHORT).show()
            return
        }
        val brend = clothingBrendEditText.text.toString().trim()
        val costInput = clothingCostEditText.text.toString().trim()
        val cost = if (costInput.isEmpty()) 0.0f else {
            if (!validateCostInput(costInput)) {
                Toast.makeText(this, "Введите корректное значение для стоимости", Toast.LENGTH_SHORT).show()
                return
            }
            costInput.replace(',', '.').toFloatOrNull() ?: 0.0f
        }
        val notes = clothingNotesEditText.text.toString().trim()
        val seasons = mutableListOf<String>()
        if (cbSummer.isChecked) seasons.add(cbSummer.text.toString())
        if (cbSpring.isChecked) seasons.add(cbSpring.text.toString())
        if (cbWinter.isChecked) seasons.add(cbWinter.text.toString())
        if (cbAutumn.isChecked) seasons.add(cbAutumn.text.toString())
        val updatedItem = ClothingItem(
            name,
            selectedSubcategoryId,
            brend,
            getSelectedGender(),
            imagePath ?: "",
            seasons,
            cost,
            getSelectedStatus(),
            getSelectedSize(),
            notes
        )
        updatedItem.id = oldItem.id

        lifecycleScope.launch(Dispatchers.IO) {
            clothingItemDao.update(updatedItem)
            clothingItemTagDao.deleteTagsForClothingItem(updatedItem.id)
            selectedTagIds.forEach { tagId ->
                clothingItemTagDao.insert(ClothingItemTag(updatedItem.id, tagId))
            }
            withContext(Dispatchers.Main) {
                Toast.makeText(this@ClothesActivity, "Вещь обновлена", Toast.LENGTH_SHORT).show()
                val resultIntent = Intent().apply { putExtra("updated_item", updatedItem) }
                setResult(RESULT_OK, resultIntent)
                finish()
            }
        }
    }
    // Загрузка тегов из БД и отображение их в виде CheckBox
    private fun loadTagsFromDatabase() {
        lifecycleScope.launch(Dispatchers.IO) {
            val allTags = db.tagDao().getAllTags()
            withContext(Dispatchers.Main) { displayTagsAsCheckboxes(allTags, selectedTagIds) }
        }
    }
    // Получение выбранного размера через FlexboxLayout
    private fun getSelectedSize(): String {
        val flexboxLayout = findViewById<FlexboxLayout>(R.id.flexboxLayout)
        return (0 until flexboxLayout.childCount)
            .map { flexboxLayout.getChildAt(it) }
            .filterIsInstance<RadioButton>()
            .firstOrNull { it.isChecked }
            ?.text?.toString() ?: ""
    }
    // Получение выбранного статуса
    private fun getSelectedStatus(): String {
        val statusRadioGroup = findViewById<RadioGroup>(R.id.radioGroupStatus)
        return findViewById<RadioButton>(statusRadioGroup.checkedRadioButtonId)?.text?.toString() ?: ""
    }
    // Получение выбранного гендера
    private fun getSelectedGender(): String {
        val genderRadioGroup = findViewById<RadioGroup>(R.id.radioGroupGender)
        return findViewById<RadioButton>(genderRadioGroup.checkedRadioButtonId)?.text?.toString() ?: "Универсально"
    }
    // Установка выбранного размера
    private fun setSelectedSize(size: String) {
        val flexboxLayout = findViewById<FlexboxLayout>(R.id.flexboxLayout)
        for (i in 0 until flexboxLayout.childCount) {
            val child = flexboxLayout.getChildAt(i)
            if (child is RadioButton && child.text.toString() == size) {
                child.isChecked = true
                break
            }
        }
    }
    // Установка выбранного статуса
    private fun setSelectedStatus(status: String) {
        val statusRadioGroup = findViewById<RadioGroup>(R.id.radioGroupStatus)
        for (i in 0 until statusRadioGroup.childCount) {
            val child = statusRadioGroup.getChildAt(i)
            if (child is RadioButton && child.text.toString() == status) {
                child.isChecked = true
                break
            }
        }
    }
    // Установка выбранного гендера
    private fun setSelectedGender(gender: String) {
        val genderRadioGroup = findViewById<RadioGroup>(R.id.radioGroupGender)
        for (i in 0 until genderRadioGroup.childCount) {
            val child = genderRadioGroup.getChildAt(i)
            if (child is RadioButton && child.text.toString() == gender) {
                child.isChecked = true
                break
            }
        }
    }
    // Отображение тегов в виде CheckBox с возможностью выбора/снятия выбора
    private fun displayTagsAsCheckboxes(allTags: List<Tag>, preselectedTagIds: Set<Int> = emptySet()) {
        flexboxTags.removeAllViews()
        allTags.forEach { tag ->
            val checkBox = CheckBox(this).apply {
                text = tag.name
                id = tag.id
                setBackgroundResource(R.drawable.checkbox_background)
                buttonDrawable = null
                setPadding(16, 5, 16, 5)
                isChecked = tag.id in preselectedTagIds
            }
            checkBox.setOnClickListener {
                if (checkBox.isChecked) {
                    selectedTagIds.add(tag.id)
                } else {
                    selectedTagIds.remove(tag.id)
                }
            }
            val params = FlexboxLayout.LayoutParams(
                FlexboxLayout.LayoutParams.WRAP_CONTENT,
                FlexboxLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(5, 5, 5, 5) }
            flexboxTags.addView(checkBox, params)
        }
    }
    // Валидация ввода стоимости
    private fun validateCostInput(costString: String): Boolean {
        val regex = Regex("^\\d+([.,]\\d{1,2})?\$")
        return regex.matches(costString.trim())
    }
    // Метод для сохранения новой вещи
    private fun saveClothingItem() {
        val brend = clothingBrendEditText.text.toString().trim().ifEmpty { "" }
        val name = clothingNameEditText.text.toString().trim()
        if (name.isEmpty()) {
            Toast.makeText(this, "Введите название", Toast.LENGTH_SHORT).show()
            return
        }
        val costInput = clothingCostEditText.text.toString().trim()
        val cost = if (costInput.isEmpty()) 0.0f else {
            if (!validateCostInput(costInput)) {
                Toast.makeText(this, "Введите корректное значение для стоимости", Toast.LENGTH_SHORT).show()
                return
            }
            costInput.replace(',', '.').toFloatOrNull() ?: 0.0f
        }
        val notes = clothingNotesEditText.text.toString().trim().ifEmpty { "" }
        val status = getSelectedStatus()
        val size = getSelectedSize()
        val gender = getSelectedGender()

        if (selectedSubcategoryId == -1) {
            Toast.makeText(this, "Выберите подкатегорию", Toast.LENGTH_SHORT).show()
            return
        }
        val seasons = mutableListOf<String>()
        if (cbSummer.isChecked) seasons.add(cbSummer.text.toString())
        if (cbSpring.isChecked) seasons.add(cbSpring.text.toString())
        if (cbWinter.isChecked) seasons.add(cbWinter.text.toString())
        if (cbAutumn.isChecked) seasons.add(cbAutumn.text.toString())
        if (imagePath.isNullOrEmpty()) {
            Toast.makeText(this, "Выберите фотографию", Toast.LENGTH_SHORT).show()
            return
        }

        val item = ClothingItem(name, selectedSubcategoryId, brend, gender, imagePath!!, seasons, cost, status, size, notes)
        val newItemId = clothingItemDao.insert(item)
        selectedTagIds.forEach { tagId ->
            clothingItemTagDao.insert(ClothingItemTag(newItemId.toInt(), tagId))
        }
        Toast.makeText(this, "Вещь сохранена", Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        val saved = savedInstanceState.getIntegerArrayList("selected_tags_ids")
        if (saved != null) {
            selectedTagIds.clear()
            selectedTagIds.addAll(saved)
        }
    }

    override fun onResume() {
        super.onResume()
        loadTagsFromDatabase()
    }
}