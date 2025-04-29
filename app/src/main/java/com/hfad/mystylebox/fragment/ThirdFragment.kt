package com.hfad.mystylebox.fragment
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RadioButton
import androidx.appcompat.widget.SearchView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
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

    private var allItems = listOf<WishListItem>()
    private var filtered = listOf<WishListItem>()
    private var categories = listOf<Category>()
    private var subcategories = listOf<Subcategory>()
    private var subcats     = listOf<Subcategory>()
    private var displayedCategories: List<Category> = emptyList()
    private var photoUri: Uri? = null
    private lateinit var galleryLauncher: ActivityResultLauncher<Intent>
    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    private lateinit var fileLauncher: ActivityResultLauncher<Intent>
    private lateinit var editLauncher: ActivityResultLauncher<Intent>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_third, container, false)

    override fun onViewCreated(view: View, saved: Bundle?) {
        super.onViewCreated(view, saved)

        searchView = view.findViewById(R.id.searchView)
        rbGrid     = view.findViewById(R.id.rbGrid)
        rbList     = view.findViewById(R.id.rbList)
        tabLayout  = view.findViewById(R.id.tabLayout)
        rv         = view.findViewById(R.id.recyclerView)

        rv.layoutManager = GridLayoutManager(context, 2)
        adapter = WishListAdapter(emptyList(), R.layout.item_clothing, onClick = { /* ваш existing click */ },
            onLongClick = { item -> showItemBottomSheet(item) })
        rv.adapter = adapter

        searchView.setOnQueryTextListener(object: SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(q: String?) = false
            override fun onQueryTextChange(q: String?): Boolean {
                applyAllFilters(q ?: "")
                return true
            }
        })

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
            adapter.layoutRes = R.layout.item_clothing1
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
        galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val selectedImageUri: Uri? = result.data?.data
                startEditActivity(selectedImageUri)
            }
        }
        cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                startEditActivity(photoUri)
            }
        }
        fileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val selectedImageUri: Uri? = result.data?.data
                startEditActivity(selectedImageUri)
            }
        }
        editLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val editedImageUriString = result.data?.getStringExtra("result_image_uri")
                if (!editedImageUriString.isNullOrEmpty()) {
                    val editedImageUri = Uri.parse(editedImageUriString)
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
            CoroutineScope(Dispatchers.IO).launch {
                Room.databaseBuilder(requireContext(), AppDatabase::class.java, "wardrobe_db")
                    .build().wishListItemDao().delete(item)
                allItems = allItems.filterNot { it.id == item.id }
                withContext(Dispatchers.Main) { applyFilters() }
            }
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
                    applyFilters()
                    Toast.makeText(requireContext(),
                        "Перенесено в гардероб", Toast.LENGTH_SHORT).show()
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
                tabLayout.removeAllTabs()
                tabLayout.addTab(tabLayout.newTab().setText("Все"))
                displayedCategories.forEach { tabLayout.addTab(tabLayout.newTab().setText(it.name)) }

                tabLayout.addOnTabSelectedListener(object: TabLayout.OnTabSelectedListener {
                    override fun onTabSelected(tab: TabLayout.Tab) {
                        val catId = if (tab.position == 0) null
                        else displayedCategories[tab.position - 1].id
                        applyFilters(categoryId = catId)
                    }
                    override fun onTabUnselected(tab: TabLayout.Tab)=Unit
                    override fun onTabReselected(tab: TabLayout.Tab)=Unit
                })

                applyFilters()
            }
        }
    }

    private fun applyFilters(
        text: String = searchView.query.toString(),
        categoryId: Int? = null
    ) {
        var tmp = allItems.filter { it.name.contains(text, ignoreCase = true) }
        if (categoryId != null) {
            tmp = tmp.filter { item ->
                subcats.find { it.id == item.subcategoryId }?.categoryId == categoryId
            }
        }
        adapter.updateData(tmp)
    }

    /** Основная фильтрация: текст + категория */
    private fun applyAllFilters(text: String, category: String = "Все") {
        // 1) текстовый поиск
        var tmp = allItems.filter {
            it.name.contains(text, ignoreCase = true)
        }
        // 2) фильтр по категории (ищем id в списке subcategories)
        if (category != "Все") {
            val id = subcategories.find { it.name == category }?.id ?: -1
            tmp = tmp.filter { it.subcategoryId == id }
        }
        filtered = tmp
        adapter.updateData(filtered)
    }

    private fun showImagePickerDialog() {
        val options = arrayOf("Выбрать из галереи", "Сфотографировать", "Выбрать из файлов")
        val icons = arrayOf(R.drawable.gallery, R.drawable.ic_camera, R.drawable.ic_file)
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
                    1 -> openCamera()
                    2 -> openFiles()
                }
            }
            .show()
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

    /** Запускает EditImageActivity, передаёт URI и флаг-источник */
    private fun startEditActivity(imageUri: Uri?) {
        if (imageUri == null) return
        val intent = Intent(requireContext(), EditImageActivity::class.java).apply {
            putExtra("imageUri", imageUri.toString())
            putExtra("source", "ThirdFragment")
        }
        editLauncher.launch(intent)
    }
    override fun onResume() {
        super.onResume()
        loadDataAndSetupTabs()
    }
    /** Вставляем в БД и обновляем список */
    private fun insertNewItem(imageUrl: String) {
        val newItem = WishListItem(imageUrl, "Новая вещь", 0.0, "", "", subcategories.firstOrNull()?.id ?: 1, "")
        CoroutineScope(Dispatchers.IO).launch {
            val db = Room.databaseBuilder(
                requireContext(),
                AppDatabase::class.java,
                "wardrobe_db"
            ).build()
            db.wishListItemDao().insert(newItem)
            allItems = db.wishListItemDao().getAll()
            withContext(Dispatchers.Main) {
                applyAllFilters(searchView.query.toString())
            }
        }
    }
}