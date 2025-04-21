package com.hfad.mystylebox.ui.activity

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.hfad.mystylebox.R
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EditImageActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_CODE_ERASER = 1001
    }

    private lateinit var imageView: ImageView
    private var imageUri: Uri? = null
    private var currentBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editimage)
        imageView = findViewById(R.id.image)
        val rotateLeftButton = findViewById<ImageButton>(R.id.rotateLeftButton)
        val rotateRightButton = findViewById<ImageButton>(R.id.rotateRightButton)
        val cropButton = findViewById<ImageButton>(R.id.cropButton)
        val saveButton = findViewById<Button>(R.id.saveButton)
        val eraseButton = findViewById<ImageButton>(R.id.eraseButton)
        val cancelButton = findViewById<Button>(R.id.cancelButton)

        imageUri = savedInstanceState?.getParcelable("imageUri")
        if (imageUri == null) {
            val uriString = intent.getStringExtra("result_image_uri")
                ?: intent.getStringExtra("imageUri")
            if (uriString.isNullOrEmpty()) {
                Toast.makeText(this, "Не удалось получить URI изображения", Toast.LENGTH_LONG).show()
                finish()
                return
            }
            imageUri = Uri.parse(uriString)
        }

        imageUri?.let { loadImage(it) }

        rotateLeftButton.setOnClickListener { rotateImage(-90) }
        rotateRightButton.setOnClickListener { rotateImage(90) }
        cropButton.setOnClickListener { startFreeStyleCrop() }
        saveButton.setOnClickListener { saveImage() }
        cancelButton.setOnClickListener { showCancelConfirmation() }
        eraseButton.setOnClickListener {
            val intent = Intent(this, EraserActivity::class.java)
            intent.putExtra("imageUri", imageUri.toString())
            startActivityForResult(intent, REQUEST_CODE_ERASER)
        }
    }

    //Загружает изображение по указанному URI с масштабированием.
    private fun loadImage(uri: Uri) {
        Glide.with(this)
            .asBitmap()
            .load(uri)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(bitmap: Bitmap, transition: Transition<in Bitmap>?) {
                    currentBitmap = scaleBitmap(bitmap)
                    imageView.setImageBitmap(currentBitmap)
                }
                override fun onLoadCleared(placeholder: Drawable?) {}
            })
    }

    // Обработка результата из UCrop и EraserActivity.
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            UCrop.REQUEST_CROP -> {
                if (resultCode == RESULT_OK) {
                    data?.let {
                        val resultUri = UCrop.getOutput(data)
                        resultUri?.let { uri ->
                            imageUri = uri
                            loadImage(uri)
                        }
                    }
                } else if (resultCode == UCrop.RESULT_ERROR) {
                    data?.let {
                        val cropError = UCrop.getError(data)
                        Log.e("UCrop", "Ошибка обрезки: $cropError")
                        Toast.makeText(this, "Ошибка обрезки: ${cropError?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            REQUEST_CODE_ERASER -> {
                if (resultCode == RESULT_OK) {
                    val uriString = data?.getStringExtra("result_image_uri")
                        ?: data?.getStringExtra("imageUri")
                    if (!uriString.isNullOrEmpty()) {
                        imageUri = Uri.parse(uriString)
                        loadImage(imageUri!!)
                    } else {
                        Toast.makeText(this, "Не удалось получить результат из EraserActivity", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    // Запускает UCrop в режиме свободного кадрирования.
    private fun startFreeStyleCrop() {
        imageUri?.let { sourceUri ->
            val destinationUri = Uri.fromFile(
                File(cacheDir, "cropped_image_${System.currentTimeMillis()}.png")
            )
            val options = UCrop.Options().apply {
                setFreeStyleCropEnabled(true)
                setHideBottomControls(true)
                setToolbarTitle("Кадрирование")
                setToolbarColor(Color.parseColor("#E8A598"))
                setCompressionFormat(Bitmap.CompressFormat.PNG)
                setCompressionQuality(100)
            }
            UCrop.of(sourceUri, destinationUri)
                .withOptions(options)
                .withMaxResultSize(1080, 1080)
                .start(this)
        } ?: run {
            Toast.makeText(this, "Изображение не выбрано", Toast.LENGTH_SHORT).show()
        }
    }

    // Поворачивает изображение на заданное число градусов.
    private fun rotateImage(degrees: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            currentBitmap?.let { bitmap ->
                val matrix = android.graphics.Matrix().apply { postRotate(degrees.toFloat()) }
                val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                // Обновляем currentBitmap и сохраняем повернутое изображение во временный файл
                currentBitmap = rotatedBitmap
                // Сохраняем повернутое изображение во временный файл и обновляем imageUri
                val newUri = updateImageUriFromBitmap(rotatedBitmap)
                withContext(Dispatchers.Main) {
                    imageView.setImageBitmap(rotatedBitmap)
                    imageUri = newUri
                }
            }
        }
    }
    // Сохраняет переданный Bitmap во временный файл и возвращает его URI.
    private fun updateImageUriFromBitmap(bitmap: Bitmap): Uri {
        val file = createImageFile()
        FileOutputStream(file).use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        }
        return Uri.fromFile(file)
    }
    //Масштабирует изображение так, чтобы его наибольшая сторона была равна targetSize.
    private fun scaleBitmap(bitmap: Bitmap): Bitmap {
        val targetSize = 1080
        val width = bitmap.width
        val height = bitmap.height
        val scale = targetSize.toFloat() / maxOf(width, height)
        return if (scale < 1) {
            Bitmap.createScaledBitmap(bitmap, (width * scale).toInt(), (height * scale).toInt(), true)
        } else {
            bitmap
        }
    }

    //Сохраняет итоговое изображение
    private fun saveImage() {
        try {
            if (currentBitmap == null) {
                Toast.makeText(this, "Нет изображения для сохранения", Toast.LENGTH_SHORT).show()
                return
            }
            CoroutineScope(Dispatchers.IO).launch {
                val file = createImageFile()
                FileOutputStream(file).use { outputStream ->
                    currentBitmap!!.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                }
                val resultUri = Uri.fromFile(file)
                withContext(Dispatchers.Main) {
                    val origin = intent.getStringExtra("origin")
                    if (origin == "clothes") {
                        val resultIntent = Intent()
                        resultIntent.putExtra("image_uri", resultUri.toString())
                        setResult(RESULT_OK, resultIntent)
                        finish()
                    } else {
                        val intent = Intent(this@EditImageActivity, CategorySelectionActivity::class.java)
                        intent.putExtra("image_uri", resultUri.toString())
                        startActivity(intent)
                        finish()
                    }
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Не удалось сохранить изображение: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    //Создает временный файл для сохранения изображения.
    private fun createImageFile(): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("EDITED_${timestamp}_", ".png", storageDir).apply {
            createNewFile()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        imageUri?.let { outState.putParcelable("imageUri", it) }
    }

    override fun onDestroy() {
        currentBitmap?.recycle()
        currentBitmap = null
        super.onDestroy()
    }

    override fun onBackPressed() {
            showCancelConfirmation()
    }
    // Показывает диалог подтверждения при нажатии кнопки "Отмена".
    private fun showCancelConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Отмена")
            .setMessage("Вы точно хотите уйти? Изменения не будут сохранены, и вы перейдете на главное окно.")
            .setPositiveButton("Да") { _, _ ->
                super.onBackPressed()
            }
            .setNegativeButton("Нет", null)
            .show()
    }
}