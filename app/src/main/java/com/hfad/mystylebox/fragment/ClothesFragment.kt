package com.hfad.mystylebox.fragment

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity.RESULT_OK
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.bumptech.glide.Glide
import com.hfad.mystylebox.database.AppDatabase
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.hfad.mystylebox.database.ClothingItem
import com.google.android.material.tabs.TabLayout
import com.hfad.mystylebox.ui.activity.BoardActivity
import com.hfad.mystylebox.ui.activity.EditImageActivity
import com.hfad.mystylebox.ui.activity.EditclothesActivity
import com.hfad.mystylebox.ui.activity.FilterActivity
import com.hfad.mystylebox.ui.bottomsheet.ItemActionsBottomSheet
import com.hfad.mystylebox.R
import com.hfad.mystylebox.ui.activity.SearchClothingActivity
import com.hfad.mystylebox.adapter.ClothingAdapter
import com.hfad.mystylebox.database.ClothingItemFull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ClothesFragment : Fragment() {

    private var currentItems: List<ClothingItemFull> = emptyList()
    private var isFilterActive: Boolean = false
    private var currentFilters: Bundle? = null
    private lateinit var recyclerView: RecyclerView
    private lateinit var imageFilter: ImageButton
    private var photoUri: Uri? = null

    private val filterActivityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data ?: return@registerForActivityResult
            val selectedGender = data.getStringArrayListExtra("selectedGender") ?: arrayListOf()
            val selectedSeasons = data.getStringArrayListExtra("selectedSeasons") ?: arrayListOf()
            val selectedSizes = data.getStringArrayListExtra("selectedSizes") ?: arrayListOf()
            val selectedStatuses = data.getStringArrayListExtra("selectedStatuses") ?: arrayListOf()
            val selectedTags = data.getStringArrayListExtra("selectedTags") ?: arrayListOf()

            currentFilters = Bundle().apply {
                putStringArrayList("genders", selectedGender)
                putStringArrayList("seasons", selectedSeasons)
                putStringArrayList("sizes", selectedSizes)
                putStringArrayList("statuses", selectedStatuses)
                putStringArrayList("tags", selectedTags)
            }

            val hasFilters = listOf(selectedGender, selectedSeasons, selectedSizes, selectedStatuses, selectedTags)
                .any { it.isNotEmpty() }
            imageFilter.setColorFilter(
                if (hasFilters) Color.parseColor("#FFB5A7")
                else Color.parseColor("#000000")
            )
            isFilterActive = true
            updateFilteredItems(selectedGender, selectedSeasons, selectedSizes, selectedStatuses, selectedTags)
        }
    }

    // Фильтрация по размерам, статусу, сезонам и тегам.
    private fun updateFilteredItems(
        selectedGender: List<String>,
        selectedSeasons: List<String>,
        selectedSizes: List<String>,
        selectedStatuses: List<String>,
        selectedTags: List<String>
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = Room.databaseBuilder(
                requireContext(), AppDatabase::class.java, "wardrobe_db"
            ).build()

            val allItems: List<ClothingItemFull> = db.clothingItemDao().getAllItemsFull()
            val filteredItems = allItems.filter { itemFull: ClothingItemFull ->
                val item = itemFull.clothingItem
                val sizeMatch = if (selectedSizes.isEmpty()) { true } else { selectedSizes.any { size ->
                        if (size.equals("Без размера", ignoreCase = true)) {
                            item.size.isNullOrBlank() } else { size.equals(item.size, ignoreCase = true) } }
                }
                val statusMatch = selectedStatuses.isEmpty() || selectedStatuses.any { stat -> stat.equals(item.status, ignoreCase = true) }
                val seasonList = item.seasons?.map { it.trim().lowercase() } ?: emptyList()
                val seasonMatch = if (selectedSeasons.isEmpty()) { true } else { selectedSeasons.any { season -> if (season.equals("Без сезона", ignoreCase = true)) { seasonList.isEmpty() } else { seasonList.contains(season.trim().lowercase()) } } }
                val itemTagNames = itemFull.tags.map { it.name.trim().lowercase() }
                val tagMatch = if (selectedTags.isEmpty()) { true } else { if (selectedTags.any { it.equals("Без тегов", ignoreCase = true) }) { itemTagNames.isEmpty() } else { selectedTags.any { tag -> itemTagNames.contains(tag.trim().lowercase()) } } }
                val genderMatch = if (selectedGender.isEmpty()) { true } else { selectedGender.any { gender -> gender.equals(item.gender, ignoreCase = true) } }

                sizeMatch && statusMatch && seasonMatch && tagMatch && genderMatch
            }
            withContext(Dispatchers.Main) {
                currentItems = filteredItems
                isFilterActive = true
                (recyclerView.adapter as? ClothingAdapter)?.updateData(filteredItems)
            }
        }
    }

    // Загрузка всех вещей (без фильтров) и сохранение в currentItems.
    private fun loadClothingItems() {
        CoroutineScope(Dispatchers.IO).launch {
            val db = Room.databaseBuilder(
                requireContext(), AppDatabase::class.java, "wardrobe_db"
            ).build()
            val items: List<ClothingItemFull> = db.clothingItemDao().getAllItemsFull()
            withContext(Dispatchers.Main) {
                currentItems = items
                isFilterActive = false
                (recyclerView.adapter as? ClothingAdapter)?.updateData(items)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (currentFilters == null || currentFilters?.isEmpty == true) {
            loadClothingItems()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_clothes, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val tabLayout = view.findViewById<TabLayout>(R.id.tabLayout)
        CoroutineScope(Dispatchers.IO).launch {
            val db = Room.databaseBuilder(
                requireContext(), AppDatabase::class.java, "wardrobe_db"
            ).build()
            val allItemsFull: List<ClothingItemFull> = db.clothingItemDao().getAllItemsFull()
            val categories = allItemsFull.map { it.categoryName }.distinct().sorted()
            withContext(Dispatchers.Main) {
                tabLayout.removeAllTabs()
                tabLayout.addTab(tabLayout.newTab().setText("Все"))
                for (cat in categories) {
                    tabLayout.addTab(tabLayout.newTab().setText(cat))
                }
                for (i in 0 until tabLayout.tabCount) {
                    val tab = (tabLayout.getChildAt(0) as ViewGroup).getChildAt(i)
                    val layoutParams = tab.layoutParams as ViewGroup.MarginLayoutParams
                    layoutParams.setMargins(4, 0, 4, 0)
                    tab.requestLayout()
                }
            }
        }

        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = GridLayoutManager(context, 2)
        recyclerView.adapter = ClothingAdapter(emptyList(), R.layout.item_clothing).apply {
            onItemClick = { itemFull ->
                val intent = Intent(requireContext(), EditclothesActivity::class.java).apply {
                    putExtra("clothing_item", itemFull.clothingItem)
                    putExtra("image_uri", itemFull.clothingItem.imagePath)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(intent)
            }
            onItemLongClick = { itemFull ->
                val bottomSheet = ItemActionsBottomSheet.newInstance(
                    itemFull.clothingItem.name,
                    itemFull.clothingItem.imagePath
                )
                bottomSheet.show(parentFragmentManager, bottomSheet.tag)
                bottomSheet.onEditClicked = {
                    val intent = Intent(requireContext(), EditclothesActivity::class.java).apply {
                        putExtra("clothing_item", itemFull.clothingItem)
                        putExtra("image_uri", itemFull.clothingItem.imagePath)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    startActivity(intent)
                }
                bottomSheet.onCreateOutfitClicked = {
                    val intent = Intent(requireContext(), BoardActivity::class.java).apply {
                        putIntegerArrayListExtra("selected_item_ids", arrayListOf(itemFull.clothingItem.id))
                        putStringArrayListExtra("selected_image_paths", arrayListOf(itemFull.clothingItem.imagePath))
                    }
                    startActivity(intent)
                }
                bottomSheet.onDeleteClicked = {
                    AlertDialog.Builder(requireContext())
                        .setTitle("Удалить '${itemFull.clothingItem.name}'")
                        .setPositiveButton("Удалить") { _, _ -> deleteItem(itemFull.clothingItem) }
                        .setNegativeButton("Отмена", null)
                        .show()
                }
            }
        }
        loadClothingItems()

        val selectPhotoButton = view.findViewById<ImageButton>(R.id.selectPhotoButton)
        selectPhotoButton.setOnClickListener { showImagePickerDialog() }
        val imageSearch = view.findViewById<ImageButton>(R.id.imageSearch)
        imageSearch.setOnClickListener { startSearchClothingActivity() }
        imageFilter = view.findViewById(R.id.imageFilter)
        imageFilter.setOnClickListener { startFilterActivity() }

        val tabLayoutView = view.findViewById<TabLayout>(R.id.tabLayout)
        tabLayoutView.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                val selectedCategory = tab.text.toString()
                val filtered = if (selectedCategory.equals("Все", ignoreCase = true))
                    currentItems
                else
                    currentItems.filter { it.categoryName.equals(selectedCategory, ignoreCase = true) }
                (recyclerView.adapter as? ClothingAdapter)?.updateData(filtered)
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    private fun deleteItem(item: ClothingItem) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = Room.databaseBuilder(
                requireContext(), AppDatabase::class.java, "wardrobe_db"
            ).build()
            db.clothingItemDao().delete(item)
            withContext(Dispatchers.Main) { loadClothingItems() }
        }
    }

    // Метод для выбора изображения (галерея, камера, файлы)
    private fun showImagePickerDialog() {
        val options = arrayOf("Выбрать из галереи", "Сфотографировать", "Выбрать из файлов")
        val icons = arrayOf(R.drawable.gallery, R.drawable.ic_camera, R.drawable.file)
        val adapterDialog = object : BaseAdapter() {
            override fun getCount() = options.size
            override fun getItem(position: Int) = options[position]
            override fun getItemId(position: Int) = position.toLong()
            override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
                val view = convertView ?: LayoutInflater.from(requireContext())
                    .inflate(R.layout.dialog_item, parent, false)
                val iconView = view.findViewById<ImageView>(R.id.icon)
                val textView = view.findViewById<TextView>(R.id.text)
                iconView.setImageResource(icons[position])
                textView.text = options[position]
                return view
            }
        }
        AlertDialog.Builder(requireContext())
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
            requireContext(), "${requireContext().packageName}.fileprovider", photoFile
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
        val storageDir = requireContext().getExternalFilesDir(null)
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
    private val editLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val editedImageUriString = result.data?.getStringExtra("result_image_uri")
            if (!editedImageUriString.isNullOrEmpty()) {
                val editedImageUri = Uri.parse(editedImageUriString)
                val imageView = requireView().findViewById<ImageView>(R.id.image)
                Glide.with(this).load(editedImageUri).into(imageView)
            }
        }
    }

    private fun startEditActivity(imageUri: Uri?) {
        if (imageUri == null) return
        val intent = Intent(requireContext(), EditImageActivity::class.java)
        intent.putExtra("imageUri", imageUri.toString())
        editLauncher.launch(intent)
    }

    private fun startSearchClothingActivity() {
        val intent = Intent(requireContext(), SearchClothingActivity::class.java)
        startActivity(intent)
    }

    private fun startFilterActivity() {
        val intent = Intent(requireContext(), FilterActivity::class.java).apply {
            currentFilters?.let {
                putStringArrayListExtra("selectedGender", it.getStringArrayList("genders"))
                putStringArrayListExtra("selectedSeasons", it.getStringArrayList("seasons"))
                putStringArrayListExtra("selectedSizes", it.getStringArrayList("sizes"))
                putStringArrayListExtra("selectedStatuses", it.getStringArrayList("statuses"))
                putStringArrayListExtra("selectedTags", it.getStringArrayList("tags"))
            }
        }
        filterActivityLauncher.launch(intent)
    }
}