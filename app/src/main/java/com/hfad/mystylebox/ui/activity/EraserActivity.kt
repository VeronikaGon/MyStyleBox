package com.hfad.mystylebox.ui.activity

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.hfad.mystylebox.EraserView
import com.hfad.mystylebox.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EraserActivity : AppCompatActivity() {

    private lateinit var eraserView: EraserView
    private var imageUri: Uri? = null
    private lateinit var sizeButton10: ImageButton
    private lateinit var sizeButton20: ImageButton
    private lateinit var sizeButton30: ImageButton
    private lateinit var sizeButton40: ImageButton
    private lateinit var sizeButton50: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_eraser)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            title = "Ластик"
            setDisplayHomeAsUpEnabled(true)
        }

        val uriString = intent.getStringExtra("result_image_uri") ?: intent.getStringExtra("imageUri")
        if (uriString.isNullOrEmpty()) {
            Toast.makeText(this, "Не удалось получить URI изображения", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        val imageUri = Uri.parse(uriString)

        eraserView = findViewById(R.id.eraserView)
        eraserView.isEraserEnabled = false

        eraserView.multiTouchListener = {
            clearEraserSelection()
        }

        Glide.with(this)
            .asBitmap()
            .load(imageUri)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(bitmap: Bitmap, transition: Transition<in Bitmap>?) {
                    val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
                    eraserView.setBitmap(mutableBitmap)
                }
                override fun onLoadCleared(placeholder: Drawable?) {}
            })

        sizeButton10 = findViewById(R.id.size10)
        sizeButton20 = findViewById(R.id.size20)
        sizeButton30 = findViewById(R.id.size30)
        sizeButton40 = findViewById(R.id.size40)
        sizeButton50 = findViewById(R.id.size50)

        sizeButton10.setOnClickListener {
            eraserView.setEraserSize(10f * resources.displayMetrics.density)
            eraserView.isEraserEnabled = true
            highlightSizeButton(it as ImageButton)
            Toast.makeText(this, "Ластик 10 dp", Toast.LENGTH_SHORT).show()
        }
        sizeButton20.setOnClickListener {
            eraserView.setEraserSize(20f * resources.displayMetrics.density)
            eraserView.isEraserEnabled = true
            highlightSizeButton(it as ImageButton)
            Toast.makeText(this, "Ластик 20 dp", Toast.LENGTH_SHORT).show()
        }
        sizeButton30.setOnClickListener {
            eraserView.setEraserSize(30f * resources.displayMetrics.density)
            eraserView.isEraserEnabled = true
            highlightSizeButton(it as ImageButton)
            Toast.makeText(this, "Ластик 30 dp", Toast.LENGTH_SHORT).show()
        }
        sizeButton40.setOnClickListener {
            eraserView.setEraserSize(40f * resources.displayMetrics.density)
            eraserView.isEraserEnabled = true
            highlightSizeButton(it as ImageButton)
            Toast.makeText(this, "Ластик 40 dp", Toast.LENGTH_SHORT).show()
        }
        sizeButton50.setOnClickListener {
            eraserView.setEraserSize(50f * resources.displayMetrics.density)
            eraserView.isEraserEnabled = true
            highlightSizeButton(it as ImageButton)
            Toast.makeText(this, "Ластик 50 dp", Toast.LENGTH_SHORT).show()
        }

        findViewById<ImageButton>(R.id.undoButton).setOnClickListener {
            eraserView.undo()
        }
        findViewById<ImageButton>(R.id.redoButton).setOnClickListener {
            eraserView.redo()
        }

        eraserView.setOnLongClickListener {
            eraserView.isEraserEnabled = !eraserView.isEraserEnabled
            Toast.makeText(
                this,
                if (eraserView.isEraserEnabled) "Режим ластика включен" else "Режим ластика выключен",
                Toast.LENGTH_SHORT
            ).show()
            true
        }
    }
    // Переопределяем нажатие системной кнопки "Назад"
    override fun onBackPressed() {
        AlertDialog.Builder(this)
            .setTitle("Внимание")
            .setMessage("Данные не будут сохранены. Вы действительно хотите выйти?")
            .setPositiveButton("Выйти") { _, _ ->
                super.onBackPressed()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
    // Сброс выбора размера ластика: снимаем подсветку и отключаем режим стирания
    private fun clearEraserSelection() {
        val sizeButtons = listOf(sizeButton10, sizeButton20, sizeButton30, sizeButton40, sizeButton50)
        sizeButtons.forEach { it.isSelected = false }
        eraserView.isEraserEnabled = false
    }

    // Подсветка выбранной кнопки и сброс выделения у остальных
    private fun highlightSizeButton(selectedButton: ImageButton) {
        val sizeButtons = listOf(sizeButton10, sizeButton20, sizeButton30, sizeButton40, sizeButton50)
        sizeButtons.forEach { it.isSelected = false }
        selectedButton.isSelected = true
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.eraser_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            android.R.id.home -> {  onBackPressed(); true }
            R.id.action_save -> { saveEditedImage(); true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // Сохранение отредактированного изображения и передача результата в следующую активность
    private fun saveEditedImage() {
        val bitmapToSave = eraserView.getBitmap()
        if (bitmapToSave == null) {
            Toast.makeText(this, "Нет изображения для сохранения", Toast.LENGTH_SHORT).show()
            return
        }
        CoroutineScope(Dispatchers.IO).launch {
            val file = createImageFile()
            FileOutputStream(file).use { out ->
                bitmapToSave.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            val resultUri = Uri.fromFile(file)
            withContext(Dispatchers.Main) {
                // Передача результата в EditImageActivity без создания новой активности
                val resultIntent = Intent().apply {
                    putExtra("result_image_uri", resultUri.toString())
                }
                setResult(RESULT_OK, resultIntent)
                finish()
            }
        }
    }

    // Изменим расширение файла на .png
    private fun createImageFile(): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("ERASED_${timestamp}_", ".png", storageDir)
    }
}