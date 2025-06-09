package com.hfad.mystylebox.ui.activity

import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
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
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                selectedZipUri = uri
                tvFileName.text = getFileNameFromUri(uri)
                btnImport.isEnabled = true
                Log.d("IMPORT_DEBUG", "Selected URI = $uri")
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

        btnBack       = findViewById(R.id.imageButton2)
        btnSelectFile = findViewById(R.id.btnSelectFile)
        tvFileName    = findViewById(R.id.tvFileName)
        btnImport     = findViewById(R.id.buttonImport)

        btnBack.setOnClickListener { onBackPressed() }
        btnImport.isEnabled = false

        btnSelectFile.setOnClickListener {
            val mimeTypes = arrayOf("application/zip", "application/octet-stream")
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
                putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
            }
            if (intent.resolveActivity(packageManager) != null) {
                openDocumentLauncher.launch(mimeTypes)
            } else {
                startActivityForResult(intent, REQUEST_CODE_FALLBACK)
            }
        }

        btnImport.setOnClickListener {
            selectedZipUri?.let { uri ->
                AlertDialog.Builder(this)
                    .setTitle("Импорт данных")
                    .setMessage("При импорте все существующие данные будут удалены. Продолжить?")
                    .setPositiveButton("Да") { _, _ -> performImport(uri) }
                    .setNegativeButton("Отмена", null)
                    .show()
            } ?: Toast.makeText(this, "Сначала выберите файл", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_FALLBACK && resultCode == RESULT_OK) {
            data?.data?.also { uri ->
                selectedZipUri = uri
                tvFileName.text = getFileNameFromUri(uri)
                btnImport.isEnabled = true
                Log.d("IMPORT_DEBUG", "Fallback URI = $uri")
            }
        }
    }

    private fun getFileNameFromUri(uri: Uri): String {
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) return cursor.getString(idx)
            }
        }
        return uri.lastPathSegment ?: "unknown.zip"
    }

    private fun performImport(zipUri: Uri) {
        val progress = ProgressDialog(this).apply {
            setMessage("Импорт данных... Подождите.")
            isIndeterminate = true
            setCancelable(false)
            show()
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val jsonStr = extractJson(zipUri)
                val picturesDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
                val cacheDir    = externalCacheDir!!

                extractPhotos(zipUri, picturesDir, "Pictures/")
                extractPhotos(zipUri, cacheDir, "cache/")

                val root    = Gson().fromJson(jsonStr, JsonObject::class.java)
                val categories:      List<Category>          = Gson().fromJson(root.getAsJsonArray("categories"),      object: TypeToken<List<Category>>(){}.type)
                val subcats:         List<Subcategory>       = Gson().fromJson(root.getAsJsonArray("subcategories"),   object: TypeToken<List<Subcategory>>(){}.type)
                val tags:            List<Tag>               = Gson().fromJson(root.getAsJsonArray("tags"),           object: TypeToken<List<Tag>>(){}.type)
                val items:           List<ClothingItem>      = Gson().fromJson(root.getAsJsonArray("clothing_items"), object: TypeToken<List<ClothingItem>>(){}.type)
                val ciTags:          List<ClothingItemTag>   = Gson().fromJson(root.getAsJsonArray("clothing_item_tags"), object: TypeToken<List<ClothingItemTag>>(){}.type)
                val outfits:         List<Outfit>            = Gson().fromJson(root.getAsJsonArray("outfits"),       object: TypeToken<List<Outfit>>(){}.type)
                val ocItems:         List<OutfitClothingItem> = Gson().fromJson(root.getAsJsonArray("outfit_clothing_items"), object: TypeToken<List<OutfitClothingItem>>(){}.type)
                val oTags:           List<OutfitTag>         = Gson().fromJson(root.getAsJsonArray("outfit_tags"),    object: TypeToken<List<OutfitTag>>(){}.type)
                val plans:           List<DailyPlan>         = Gson().fromJson(root.getAsJsonArray("daily_plans"),    object: TypeToken<List<DailyPlan>>(){}.type)
                val wish:            List<WishListItem>      = Gson().fromJson(root.getAsJsonArray("wish_list_items"),object: TypeToken<List<WishListItem>>(){}.type)

                fun toContentUri(oldPath: String?, baseDir: File): String? {
                    if (oldPath.isNullOrBlank()) return null
                    val name = oldPath.substringAfterLast('/')
                    val file = File(baseDir, name)
                    return if (file.exists()) {
                        FileProvider.getUriForFile(
                            this@ImportActivity,
                            "${packageName}.fileprovider",
                            file
                        ).toString()
                    } else oldPath
                }

                items.forEach   { it.imagePath = toContentUri(it.imagePath, picturesDir) ?: it.imagePath }
                wish.forEach    { it.imagePath = toContentUri(it.imagePath, picturesDir) ?: it.imagePath }
                outfits.forEach { it.imagePath = toContentUri(it.imagePath, cacheDir)    ?: it.imagePath }

                database.runInTransaction {
                    database.clearAllTables()
                    database.categoryDao().insertAll(categories)
                    database.subcategoryDao().insertAll(subcats)
                    database.tagDao().insertAll(tags)
                    database.clothingItemDao().insertAll(items)
                    database.clothingItemTagDao().insertAll(ciTags)
                    database.outfitDao().insertAll(outfits)
                    database.outfitClothingItemDao().insertAll(ocItems)
                    database.outfitTagDao().insertAll(oTags)
                    database.dailyPlanDao().insertAll(plans)
                    database.wishListItemDao().insertAll(wish)
                }

                runOnUiThread {
                    progress.dismiss()
                    AlertDialog.Builder(this@ImportActivity)
                        .setTitle("Успешно")
                        .setMessage("Импорт завершён!")
                        .setPositiveButton("OK") { d, _ ->
                            d.dismiss()
                            finish()
                        }.show()
                }

            } catch (e: Exception) {
                Log.e("IMPORT_DEBUG", "Import error", e)
                runOnUiThread {
                    progress.dismiss()
                    AlertDialog.Builder(this@ImportActivity)
                        .setTitle("Ошибка")
                        .setMessage("Не удалось импортировать: ${e.localizedMessage}")
                        .setPositiveButton("OK", null)
                        .show()
                }
            }
        }
    }

    @Throws(IOException::class)
    private fun extractJson(zipUri: Uri): String {
        contentResolver.openInputStream(zipUri).use { raw ->
            if (raw == null) throw IOException("Cannot open ZIP for JSON")
            ZipInputStream(BufferedInputStream(raw)).use { zip ->
                var entry: ZipEntry? = zip.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory && entry.name.equals("backup_data.json", true)) {
                        return StringWriter().use { sw ->
                            InputStreamReader(zip, Charsets.UTF_8).use { reader ->
                                val buf = CharArray(4096)
                                var len: Int
                                while (reader.read(buf).also { len = it } > 0) {
                                    sw.write(buf, 0, len)
                                }
                            }
                            sw.toString()
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        }
        throw IOException("backup_data.json not found")
    }

    @Throws(IOException::class)
    private fun extractPhotos(zipUri: Uri, targetDir: File, prefix: String) {
        contentResolver.openInputStream(zipUri).use { raw ->
            if (raw == null) throw IOException("Cannot open ZIP for photos")
            ZipInputStream(BufferedInputStream(raw)).use { zip ->
                var entry: ZipEntry? = zip.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory && entry.name.startsWith(prefix)) {
                        val rel = entry.name.removePrefix(prefix)
                        val outFile = File(targetDir, rel)
                        outFile.parentFile?.mkdirs()
                        FileOutputStream(outFile).use { fos ->
                            zip.copyTo(fos)
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        }
    }
}