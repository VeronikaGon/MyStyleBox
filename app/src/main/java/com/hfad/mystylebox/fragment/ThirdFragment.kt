package com.hfad.mystylebox.fragment

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import androidx.appcompat.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.tabs.TabLayout
import com.hfad.mystylebox.R
import com.hfad.mystylebox.adapter.WishListAdapter
import com.hfad.mystylebox.database.AppDatabase
import com.hfad.mystylebox.database.entity.Category
import com.hfad.mystylebox.database.entity.ClothingItem
import com.hfad.mystylebox.database.entity.Subcategory
import com.hfad.mystylebox.database.entity.WishListItem
import com.hfad.mystylebox.ui.activity.EditImageActivity
import com.hfad.mystylebox.ui.activity.FilterWishlistActivity
import com.hfad.mystylebox.ui.activity.WishListActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ThirdFragment : Fragment() {

    private lateinit var searchView: SearchView
    private lateinit var rbGrid: RadioButton
    private lateinit var rbList: RadioButton
    private lateinit var tabLayout: TabLayout
    private lateinit var rv: RecyclerView
    private lateinit var adapter: WishListAdapter
    private lateinit var imageFilter: ImageButton
    private lateinit var emptyTextView: TextView
    private lateinit var selectPhotoButton: ImageButton

    private var allItems = listOf<WishListItem>()
    private var categories = listOf<Category>()
    private var subcats = listOf<Subcategory>()
    private var displayedCategories: List<Category> = emptyList()
    private var photoUri: Uri? = null

    private lateinit var galleryLauncher: ActivityResultLauncher<Intent>
    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    private lateinit var fileLauncher: ActivityResultLauncher<Intent>
    private lateinit var editLauncher: ActivityResultLauncher<Intent>

    private var currentFilters: Bundle = Bundle()

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            openCamera()
        } else {
            Toast.makeText(requireContext(), "Нужно разрешение на камеру", Toast.LENGTH_SHORT).show()
        }
    }
    private val filterLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val genders = result.data?.getStringArrayListExtra("selectedGender") ?: arrayListOf()
            val sizes   = result.data?.getStringArrayListExtra("selectedSize")   ?: arrayListOf()
            val counts  = result.data?.getStringArrayListExtra("selectedCount")  ?: arrayListOf()

            currentFilters = Bundle().apply {
                putStringArrayList("gender", ArrayList(genders))
                putStringArrayList("size",   ArrayList(sizes))
                putStringArrayList("count",  ArrayList(counts))
            }
            val hasFilters = genders.isNotEmpty() || sizes.isNotEmpty() || counts.isNotEmpty()

            imageFilter.setColorFilter(
                if (hasFilters) ContextCompat.getColor(requireContext(), R.color.pink)
                else ContextCompat.getColor(requireContext(), R.color.black)
            )
            applyCombinedFilters()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_third, container, false)

    override fun onViewCreated(view: View, saved: Bundle?) {
        super.onViewCreated(view, saved)

        searchView = view.findViewById(R.id.searchView)
        rbGrid = view.findViewById(R.id.rbGrid)
        rbList = view.findViewById(R.id.rbList)
        tabLayout = view.findViewById(R.id.tabLayout)
        rv = view.findViewById(R.id.recyclerView)
        imageFilter = view.findViewById(R.id.imageFilter)
        emptyTextView = view.findViewById(R.id.emptyTextView)
        selectPhotoButton = view.findViewById(R.id.selectPhotoButton)
        val toggleGroup    = view.findViewById<LinearLayout>(R.id.toggleGroup)
        val llSearch       = view.findViewById<LinearLayout>(R.id.llsearch)
        val lp             = llSearch.layoutParams as LinearLayout.LayoutParams

        rv.layoutManager = GridLayoutManager(context, 2)
        adapter = WishListAdapter(emptyList(), R.layout.item_clothing,
            onClick = { item -> showItemBottomSheet(item) },
            onLongClick = { item -> showItemBottomSheet(item) }
        )
        rv.adapter = adapter

        val closeButton: ImageView =
            searchView.findViewById(androidx.appcompat.R.id.search_close_btn)
        closeButton.setOnClickListener {
            searchView.setQuery("", false)
            searchView.clearFocus()
        }

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(q: String?) = false
            override fun onQueryTextChange(q: String?): Boolean {
                val text = q.orEmpty()

                if (text.isEmpty()) {
                    imageFilter.visibility = View.VISIBLE
                    toggleGroup.visibility = View.VISIBLE
                    lp.weight = 1.0F
                    llSearch.layoutParams = lp
                    searchView.clearFocus()
                    applyCombinedFilters(text = "", categoryId = null)
                } else {
                    imageFilter.visibility = View.GONE
                    toggleGroup.visibility = View.GONE
                    lp.weight = 0f
                    llSearch.layoutParams = lp
                    applyCombinedFilters(
                        text = q.orEmpty(),
                        categoryId = tabLayout.selectedTabPosition.takeIf { it != 0 }
                            ?.let { displayedCategories[it - 1].id })
                }
               return true
            }
        })
        searchView.setOnCloseListener {
            searchView.clearFocus()
            true
        }

        // Grid/List переключение
        fun selectGrid() {
            rbGrid.setBackgroundResource(R.drawable.edittext_background)
            rbList.setBackgroundResource(0)
            rv.layoutManager = GridLayoutManager(context, 2)
            adapter.layoutRes = R.layout.item_clothing
            adapter.notifyDataSetChanged()
        }

        fun selectList() {
            rbList.setBackgroundResource(R.drawable.edittext_background)
            rbGrid.setBackgroundResource(0)
            rv.layoutManager = LinearLayoutManager(context)
            adapter.layoutRes = R.layout.item_wishlist
            adapter.notifyDataSetChanged()
        }

        rbGrid.setOnClickListener {
            if (!rbGrid.isChecked) rbGrid.isChecked = true
            rbList.isChecked = false
            selectGrid()
        }

        rbList.setOnClickListener {
            if (!rbList.isChecked) rbList.isChecked = true
            rbGrid.isChecked = false
            selectList()
        }

        rbGrid.isChecked = true
        selectGrid()

        imageFilter.setOnClickListener {
            if (allItems.size <= 4) {
                val needed = 4 - allItems.size
                val word = when {
                    needed % 10 == 1 && needed % 100 != 11 -> "желанную вещь"
                    needed % 10 in 2..4 && needed % 100 !in 12..14 -> "желанные вещи"
                    else -> "желанных вещей"
                }
                Toast.makeText(
                    requireContext(),
                    "Добавьте ещё $needed $word для фильтрации",
                    Toast.LENGTH_SHORT
                ).show()
                Toast.makeText(
                    requireContext(),
                    "Добавьте ещё желанных вещей для фильтрации",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                val intent = Intent(requireContext(), FilterWishlistActivity::class.java).apply {
                    putStringArrayListExtra(
                        "selectedGender",
                        currentFilters.getStringArrayList("gender")
                    )
                    putStringArrayListExtra(
                        "selectedSize",
                        currentFilters.getStringArrayList("size")
                    )
                    putStringArrayListExtra(
                        "selectedCount",
                        currentFilters.getStringArrayList("count")
                    )
                }
                filterLauncher.launch(intent)
            }
        }

        galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                val selectedImageUri: Uri? = result.data?.data
                startEditActivity(selectedImageUri)
            }
        }

        cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                startEditActivity(photoUri)
            }
        }

        fileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                val selectedImageUri: Uri? = result.data?.data
                startEditActivity(selectedImageUri)
            }
        }

        editLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                val editedImageUriString = result.data?.getStringExtra("result_image_uri")
                if (!editedImageUriString.isNullOrEmpty()) {
                    Uri.parse(editedImageUriString)
                }
            }
        }

        view.findViewById<ImageButton>(R.id.selectPhotoButton)
            .setOnClickListener { showImagePickerDialog() }

        loadDataAndSetupTabs()
    }

    private fun showItemBottomSheet(item: WishListItem) {
        val bsView = layoutInflater.inflate(R.layout.bottom_sheet_wishlist_item, null)
        val dialog = BottomSheetDialog(requireContext())
        dialog.setContentView(bsView)

        bsView.findViewById<TextView>(R.id.tvItemName).text = item.name
        bsView.findViewById<TextView>(R.id.btnEdit).setOnClickListener {
            dialog.dismiss()
            Intent(requireContext(), WishListActivity::class.java).also { intent ->
                intent.putExtra("wishItem", item)
                intent.putExtra("image_uri", item.imagePath)
                intent.putExtra("subcategory_id", item.subcategoryId)
                startActivity(intent)
            }
        }
        bsView.findViewById<TextView>(R.id.btnDelete).setOnClickListener {
            dialog.dismiss()
            AlertDialog.Builder(requireContext())
                .setTitle("Удалить вещь?")
                .setMessage("Вы действительно хотите удалить «${item.name}» из списка желаний?")
                .setPositiveButton("Удалить") { _, _ ->
                    CoroutineScope(Dispatchers.IO).launch {
                        Room.databaseBuilder(requireContext(), AppDatabase::class.java, "wardrobe_db")
                            .build().wishListItemDao().delete(item)
                        allItems = allItems.filterNot { it.id == item.id }
                        withContext(Dispatchers.Main) {
                            updateEmptyOrLimitedMode()
                        }
                    }
                }
                .setNegativeButton("Отмена", null)
                .show()
        }
        bsView.findViewById<TextView>(R.id.btnCancel).setOnClickListener {
            dialog.dismiss()
        }
        bsView.findViewById<Button>(R.id.btnAddClothing).setOnClickListener {
            dialog.dismiss()
            val clothingItem = ClothingItem(item.name.toString(),item.subcategoryId,"",  item.gender.toString(), item.imagePath.toString(), mutableListOf(),item.price.toFloat() ,"Активное использование","",item.notes)
            CoroutineScope(Dispatchers.IO).launch {
                val db = Room.databaseBuilder(requireContext(),
                    AppDatabase::class.java,"wardrobe_db").build()
                db.clothingItemDao().insert(clothingItem)
                db.wishListItemDao().delete(item)
                allItems = allItems.filterNot { it.id == item.id }
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(),
                        "Перенесено в гардероб", Toast.LENGTH_SHORT).show()
                    updateEmptyOrLimitedMode()
                }
            }
        }

        dialog.show()
    }

    private fun loadDataAndSetupTabs() {
        CoroutineScope(Dispatchers.IO).launch {
            val db = Room.databaseBuilder(
                requireContext(), AppDatabase::class.java, "wardrobe_db"
            ).build()

            allItems   = db.wishListItemDao().getAll()
            subcats    = db.subcategoryDao().getAllSubcategories()
            categories = db.categoryDao().getAllCategories()

            val usedCategoryIds = allItems
                .mapNotNull { item ->
                    subcats.find { it.id == item.subcategoryId }?.categoryId
                }.distinct()

            displayedCategories = categories.filter { it.id in usedCategoryIds }

            withContext(Dispatchers.Main) {
                updateEmptyOrLimitedMode()
            }
        }
    }

    private fun applyCombinedFilters(
        text: String = searchView.query.toString(),
        categoryId: Int? = null
    ) {
        // 1) Собираем фильтры
        val genders = currentFilters.getStringArrayList("gender") ?: arrayListOf()
        val sizes   = currentFilters.getStringArrayList("size")   ?: arrayListOf()
        val counts  = currentFilters.getStringArrayList("count")  ?: arrayListOf()

        // 2) По тексту + категории
        val filteredByTextCat = allItems
            .filter { it.name.contains(text, ignoreCase = true) }
            .filter { item ->
                categoryId == null ||
                        subcats.find { it.id == item.subcategoryId }?.categoryId == categoryId
            }

        // 3) По полу
        val filteredByGender = filteredByTextCat.filter {
            genders.isEmpty() || genders.contains(it.gender)
        }

        // 4) По размеру, учитывая null/"" как "Без размера"
        val filteredBySize = filteredByGender.filter { item ->
            if (sizes.isEmpty()) true
            else sizes.any { sizeText ->
                if (sizeText == "Без размера") item.size.isNullOrBlank()
                else sizeText.equals(item.size, ignoreCase = true)
            }
        }

        // 5) По цене
        val finalFiltered = filteredBySize.filter { item ->
            if (counts.isEmpty()) true
            else counts.any { rangeText ->
                val (low, high) = Companion.priceRanges[rangeText] ?: (0 to Int.MAX_VALUE)
                item.price.toInt() in low..high
            }
        }

        // 6) **Обновляем адаптер** — без этого RecyclerView останется пустым!
        adapter.updateData(finalFiltered)
    }

    private fun updateEmptyOrLimitedMode() {
        when {
            allItems.isEmpty() -> {
                emptyTextView.visibility = View.VISIBLE
                rv.visibility = View.GONE
                tabLayout.visibility = View.GONE
                searchView.visibility = View.GONE
                imageFilter.visibility = View.VISIBLE
                selectPhotoButton.visibility = View.VISIBLE
                adapter.updateData(emptyList())
            }

            allItems.size <= 4 -> {
                emptyTextView.visibility = View.GONE
                tabLayout.visibility = View.VISIBLE
                rv.visibility = View.VISIBLE
                setupTabs()
                searchView.visibility = View.VISIBLE
                imageFilter.visibility = View.VISIBLE
                selectPhotoButton.visibility = View.VISIBLE
                imageFilter.setColorFilter(ContextCompat.getColor(requireContext(), R.color.black))
                adapter.updateData(allItems)
            }

            else -> {
                emptyTextView.visibility = View.GONE
                rv.visibility = View.VISIBLE
                setupTabs()
                searchView.visibility = View.VISIBLE
                imageFilter.visibility = View.VISIBLE
                selectPhotoButton.visibility = View.VISIBLE

                applyCombinedFilters()
            }
        }
    }

    private fun setupTabs() {
        tabLayout.removeAllTabs()

        if (allItems.isNotEmpty()) {
            tabLayout.addTab(tabLayout.newTab().setText("Все"))
        }

        displayedCategories.forEach { cat ->
            tabLayout.addTab(tabLayout.newTab().setText(cat.name))
        }

        tabLayout.post {
            setTabMargins()
        }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                val categoryId: Int? = if (tab.position == 0 && allItems.isNotEmpty()) {
                    null
                } else {
                    val index = if (allItems.isNotEmpty()) tab.position - 1 else tab.position
                    if (index in displayedCategories.indices) displayedCategories[index].id else null
                }
                applyCombinedFilters(categoryId = categoryId)
            }
            override fun onTabUnselected(tab: TabLayout.Tab) = Unit
            override fun onTabReselected(tab: TabLayout.Tab) = Unit
        })
    }

    private fun setTabMargins() {
        val marginPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            4f,
            resources.displayMetrics
        ).toInt()

        val tabStrip = (tabLayout.getChildAt(0) as? ViewGroup) ?: return

        for (i in 0 until tabStrip.childCount) {
            val tabView = tabStrip.getChildAt(i)
            val params = tabView.layoutParams as ViewGroup.MarginLayoutParams
            params.setMargins(marginPx, 0, marginPx, 0)
            tabView.requestLayout()
        }
    }

    companion object {
        val priceRanges = mapOf(
            "до 1000" to (0 to 1000),
            "от 1000 до 3000" to (1000 to 3000),
            "от 3000 до 5000" to (3000 to 5000),
            "от 5000 до 10000" to (5000 to 10000),
            "от 10000 до 40000" to (10000 to 40000),
            "больше 40000" to (40001 to Int.MAX_VALUE)
        )
    }

    private fun showImagePickerDialog() {
        val options = arrayOf("Выбрать из галереи", "Сфотографировать", "Выбрать из файлов")
        val icons = arrayOf(R.drawable.ic_gallery, R.drawable.ic_camera, R.drawable.ic_file)
        val adapterDialog = object : BaseAdapter() {
            override fun getCount() = options.size
            override fun getItem(position: Int) = options[position]
            override fun getItemId(position: Int) = position.toLong()
            override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
                val view = convertView ?: LayoutInflater.from(requireContext())
                    .inflate(R.layout.dialog_item, parent, false)
                view.findViewById<ImageView>(R.id.icon).setImageResource(icons[position])
                view.findViewById<TextView>(R.id.text).text = options[position]
                return view
            }
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Выберите действие")
            .setAdapter(adapterDialog) { _, which ->
                when (which) {
                    0 -> openGallery()
                    1 -> checkCameraPermissionAndOpen()
                    2 -> openFiles()
                }
            }
            .show()
    }

    // Метод проверки разрешения
    private fun checkCameraPermissionAndOpen() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                openCamera()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                AlertDialog.Builder(requireContext())
                    .setTitle("Требуется доступ к камере")
                    .setMessage("Для фотографирования одежды нужно разрешение на камеру.")
                    .setPositiveButton("Разрешить") { _, _ ->
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                    .setNegativeButton("Отмена", null)
                    .show()
            }
            else -> {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK).apply {
            data = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            type = "image/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/jpeg", "image/png"))
        }
        galleryLauncher.launch(intent)
    }

    private fun openCamera() {
        val photoFile = createImageFile()
        photoUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            photoFile
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

    //Запускает EditImageActivity, передаёт URI и флаг-источник
    private fun startEditActivity(imageUri: Uri?) {
        if (imageUri == null) return
        Intent(requireContext(), EditImageActivity::class.java).apply {
            putExtra("imageUri", imageUri.toString())
            putExtra("source", "ThirdFragment")
        }.also { editLauncher.launch(it) }
    }

    override fun onResume() {
        super.onResume()
        loadDataAndSetupTabs()
    }
}