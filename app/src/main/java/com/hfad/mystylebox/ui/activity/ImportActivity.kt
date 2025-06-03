package com.hfad.mystylebox.ui.activity

import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.hfad.mystylebox.R
import com.hfad.mystylebox.database.AppDatabase
import com.hfad.mystylebox.database.entity.Category
import com.hfad.mystylebox.database.entity.ClothingItem
import com.hfad.mystylebox.database.entity.ClothingItemTag
import com.hfad.mystylebox.database.entity.DailyPlan
import com.hfad.mystylebox.database.entity.Outfit
import com.hfad.mystylebox.database.entity.OutfitClothingItem
import com.hfad.mystylebox.database.entity.OutfitTag
import com.hfad.mystylebox.database.entity.Subcategory
import com.hfad.mystylebox.database.entity.Tag
import com.hfad.mystylebox.database.entity.WishListItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.io.StringWriter
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class ImportActivity : AppCompatActivity() {
    private lateinit var btnBack: ImageButton
    private lateinit var btnSelectFile: Button
    private lateinit var tvFileName: TextView
    private lateinit var btnImport: Button

    private var selectedZipUri: Uri? = null
    private lateinit var database: AppDatabase

    private val openDocumentLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri != null) {
                // ACTION_OPEN_DOCUMENT: можно взять persistable permission
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                selectedZipUri = uri
                tvFileName.text = getFileNameFromUri(uri)
                btnImport.isEnabled = true
                Log.d("IMPORT_DEBUG", "Выбран URI = $uri")
            } else {
                Toast.makeText(this, "Файл не выбран", Toast.LENGTH_SHORT).show()
            }
        }

    companion object {
        private const val REQUEST_CODE_FALLBACK = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_import)

        database = AppDatabase.getInstance(this)

        btnBack = findViewById(R.id.imageButton2)
        btnSelectFile = findViewById(R.id.btnSelectFile)
        tvFileName = findViewById(R.id.tvFileName)
        btnImport = findViewById(R.id.buttonImport)

        btnBack.setOnClickListener {
            onBackPressed()
        }

        btnImport.isEnabled = false

        btnSelectFile.setOnClickListener {
            val mimeTypes = arrayOf("application/zip", "application/octet-stream")

            // Сперва пробуем ACTION_OPEN_DOCUMENT (SAF)
            val intentSAF = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"  // задаём "*/*", чтобы попасть в любой файловый менеджер
                putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
            }

            if (intentSAF.resolveActivity(packageManager) != null) {
                openDocumentLauncher.launch(mimeTypes)
            } else {
                // Фоллбэк на ACTION_GET_CONTENT
                val intentGet = Intent(Intent.ACTION_GET_CONTENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "*/*"
                    putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
                }
                if (intentGet.resolveActivity(packageManager) != null) {
                    startActivityForResult(intentGet, REQUEST_CODE_FALLBACK)
                } else {
                    Toast.makeText(
                        this,
                        "Для выбора ZIP-файла необходим файловый менеджер, способный читать ZIP-архивы",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        btnImport.setOnClickListener {
            val zipUri = selectedZipUri
            if (zipUri == null) {
                Toast.makeText(this, "Сначала выберите файл", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            Log.d("IMPORT_DEBUG", "Запуск performImport с URI = $zipUri")
            performImport(zipUri)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_FALLBACK && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                // Для ACTION_GET_CONTENT НЕ вызываем takePersistableUriPermission!
                selectedZipUri = uri
                tvFileName.text = getFileNameFromUri(uri)
                btnImport.isEnabled = true
                Log.d("IMPORT_DEBUG", "Переход из фоллака: выбран URI = $uri")
            }
        }
    }

    private fun getFileNameFromUri(uri: Uri): String {
        var result: String? = null
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) {
                    result = cursor.getString(idx)
                }
            }
        }
        return result ?: uri.path?.substringAfterLast('/') ?: "unknown.zip"
    }

    private fun performImport(zipUri: Uri) {
    }
}