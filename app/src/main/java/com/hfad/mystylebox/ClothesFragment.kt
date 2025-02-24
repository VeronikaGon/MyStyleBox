package com.hfad.mystylebox

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.hfad.mystylebox.database.AppDatabase
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.Manifest
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import android.app.AlertDialog
import android.widget.SearchView
import com.google.android.material.tabs.TabLayout

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class ClothesFragment : Fragment() {
    private var param1: String? = null
    private var param2: String? = null
    private lateinit var recyclerView: RecyclerView
    private var photoUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_clothes, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tabLayout = view.findViewById<TabLayout>(R.id.tabLayout)
        tabLayout.addTab(tabLayout.newTab().setText("Все"))
        tabLayout.addTab(tabLayout.newTab().setText("Верх"))
        tabLayout.addTab(tabLayout.newTab().setText("Низ"))
        tabLayout.addTab(tabLayout.newTab().setText("Платья"))
        tabLayout.addTab(tabLayout.newTab().setText("Обувь"))
        tabLayout.addTab(tabLayout.newTab().setText("Аксессуары"))
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                val selectedCategory = tab.text.toString()
                (recyclerView.adapter as? ClothingAdapter)?.filterByCategory(selectedCategory)
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
        for (i in 0 until tabLayout.tabCount) {
            val tab = (tabLayout.getChildAt(0) as ViewGroup).getChildAt(i)
            val layoutParams = tab.layoutParams as ViewGroup.MarginLayoutParams
            layoutParams.setMargins(4, 0, 4, 0)
            tab.requestLayout()
        }
        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = GridLayoutManager(context, 2)
        loadClothingItems()
        val selectPhotoButton = view.findViewById<Button>(R.id.selectPhotoButton)
        selectPhotoButton.setOnClickListener {
            showImagePickerDialog()
        }
    }

    // Метод, который получает данные из базы
    private fun loadClothingItems() {
        val db = Room.databaseBuilder(
            requireContext(),
            AppDatabase::class.java,
            "wardrobe_db"
        )
            .allowMainThreadQueries()
            .build()
        val loadedItems = db.clothingItemDao().getAllItems()
        // Получаем список подкатегорий из базы
        val subcategories = db.subcategoryDao().getAllSubcategories()
        val categoryMap = mapOf(
            1 to "Верх",
            2 to "Низ",
            3 to "Платья",
            4 to "Обувь",
            5 to "Аксессуары"
        )
        // Формируем мапу: для каждой подкатегории определяем название категории через categoryMap
        val subcategoryToCategoryMap = subcategories.associate { it.id to (categoryMap[it.categoryId] ?: "Неизвестно") }

        val adapter = ClothingAdapter(loadedItems, subcategoryToCategoryMap)
        recyclerView.adapter = adapter

        adapter.onItemClick = { clothingItem ->
            val intent = Intent(requireContext(), EditclothesActivity::class.java)
            intent.putExtra("image_uri", clothingItem.imagePath)
            intent.putExtra("clothing_item", clothingItem)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(intent)
        }
    }
    // Метод, который обновляет RecyclerView
    override fun onResume() {
        super.onResume()
        loadClothingItems()
    }
    // Диалог выбора действия с использованием dialog_item.xml для отображения пунктов
    private fun showImagePickerDialog() {
        val options = arrayOf("Выбрать из галереи", "Сфотографировать", "Выбрать из файлов")
        val icons = arrayOf(R.drawable.gallery, R.drawable.camera, R.drawable.file)

        val adapter = object : BaseAdapter() {
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
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Выберите действие")
            .setAdapter(adapter) { _, which ->
                when (which) {
                    0 -> openGallery()
                    1 -> openCamera()
                    2 -> openFiles()
                }
            }
            .show()
    }

    private fun openGallery() {
//            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        val intent = Intent(Intent.ACTION_PICK).apply {
            setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/jpeg", "image/png"))
        }
        galleryLauncher.launch(intent)
    }
    private fun openCamera() {
        val photoFile = createImageFile()
        photoUri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.fileprovider", photoFile)
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
        }
        cameraLauncher.launch(intent)
    }
    private fun openFiles() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
        }
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
            startCategorySelectionActivity(selectedImageUri)
        }
    }
    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            startCategorySelectionActivity(photoUri)
        }
    }
    private val fileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val selectedImageUri: Uri? = result.data?.data
            startCategorySelectionActivity(selectedImageUri)
        }
    }
    private fun startCategorySelectionActivity(imageUri: Uri?) {
        if (imageUri == null) return
        val intent = Intent(requireContext(), CategorySelectionActivity::class.java)
        intent.putExtra("image_uri", imageUri.toString())  // Передаём строковый путь к изображению
        startActivity(intent)
    }

}