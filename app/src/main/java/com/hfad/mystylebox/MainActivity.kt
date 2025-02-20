package com.hfad.mystylebox

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.Manifest
import androidx.core.content.ContextCompat

import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.room.Room
import com.hfad.mystylebox.database.AppDatabase
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private var photoUri: Uri? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = GridLayoutManager(this, 2)

        val selectPhotoButton = findViewById<Button>(R.id.selectPhotoButton)
        selectPhotoButton.setOnClickListener {
            showImagePickerDialog()
        }
    }

    // Метод, который обновляет RecyclerView
    override fun onResume() {
        super.onResume()
        loadClothingItems()
    }
    // Метод, который получает данные из базы
    private fun loadClothingItems() {
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "clothing_db"
        )
            .allowMainThreadQueries()
            .build()

        val loadedItems = db.clothingItemDao().getAllItems()  // Получаем список из базы
        recyclerView.adapter = ClothingAdapter(loadedItems)
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
                val view = convertView ?: LayoutInflater.from(this@MainActivity)
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
        photoUri = FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            photoFile
        )
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
        val storageDir = getExternalFilesDir(null)
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
        val intent = Intent(this, CategorySelectionActivity::class.java)
        intent.putExtra("image_uri", imageUri.toString())  // Передаём строковый путь к изображению
        startActivity(intent)
    }
}
