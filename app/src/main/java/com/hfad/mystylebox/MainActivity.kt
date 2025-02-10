package com.hfad.mystylebox

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.room.Room
import com.hfad.mystylebox.database.AppDatabase
import com.hfad.mystylebox.database.ClothingItem
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private val READ_STORAGE_PERMISSION_CODE = 100
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ClothingAdapter
    private var photoUri: Uri? = null

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val selectedImageUri: Uri? = result.data?.data
            startClothesActivity(selectedImageUri)
        }
    }
    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val selectedImageUri: Uri? = result.data?.data
                startClothesActivity(selectedImageUri)
            }
        }
    private val fileLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val selectedImageUri: Uri? = result.data?.data
                startClothesActivity(selectedImageUri)
            }
        }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)


        val selectPhotoButton = findViewById<Button>(R.id.selectPhotoButton)
        selectPhotoButton.setOnClickListener {
            showImagePickerDialog()
        }
    }
    override fun onResume() {
        super.onResume()
        loadClothingItems() // Метод, который получает данные из базы и обновляет RecyclerView
    }
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
        // Задаём варианты и соответствующие иконки (укажите свои drawable)
        val options = arrayOf("Выбрать из галереи", "Сфотографировать", "Выбрать из файлов")
        val icons = arrayOf(R.drawable.gallery, R.drawable.camera, R.drawable.file)

        // Создаём адаптер для диалога, который будет использовать layout dialog_item.xml
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

        // Построение диалога с адаптером
        AlertDialog.Builder(this)
            .setTitle("Выберите действие")
            .setAdapter(adapter) { dialog, which ->
                when (which) {
                    0 -> openGallery()   // Выбран вариант "Выбрать из галереи"
                    1 -> openCamera()
                    2 -> openFiles()
                }
            }
            .show()
    }

    private fun openGallery() {
        if (!checkStoragePermission()) {
            // Запрашиваем разрешение, если его нет
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                READ_STORAGE_PERMISSION_CODE
            )
        } else {
            // Если разрешение предоставлено, открываем галерею
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            galleryLauncher.launch(intent)
        }
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == READ_STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Разрешение получено – запускаем галерею
                openGallery()
            } else {
                // Если разрешение не предоставлено, можно показать сообщение
                Toast.makeText(this, "Разрешение на чтение хранилища необходимо", Toast.LENGTH_SHORT).show()
            }
        }
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
    // Запускаем ClothesActivity и передаём выбранный URI изображения в виде строки
    private fun startClothesActivity(imageUri: Uri?) {
        val intent = Intent(this, ClothesActivity::class.java)
        intent.putExtra("image_path", imageUri.toString())
        startActivity(intent)
    }
    //проверка предоставлено ли разрешение
    private fun checkStoragePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

}
