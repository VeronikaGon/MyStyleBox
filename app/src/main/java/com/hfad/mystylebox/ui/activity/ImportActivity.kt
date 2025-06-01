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
import com.hfad.mystylebox.database.entity.Subcategory
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

    // Ссылки на View из разметки
    private lateinit var btnBack: ImageButton
    private lateinit var btnSelectFile: Button
    private lateinit var tvFileName: TextView
    private lateinit var btnImport: Button

    // Храним URI выбранного ZIP (null, пока не выбрано)
    private var selectedZipUri: Uri? = null

    // DAO базы
    private lateinit var database: AppDatabase

    // Регистрация контракта для выбора ZIP-файла из «проводника»
    private val openDocumentLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri != null) {
                // Пользователь выбрал файл – проверим, что это ZIP
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )

                // Сохраняем URI и показываем название файла
                selectedZipUri = uri
                tvFileName.text = getFileNameFromUri(uri)
                btnImport.isEnabled = true
            } else {
                // Пользователь отменил выбор
                Toast.makeText(this, "Файл не выбран", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_import)

        // Инициализируем ссылку на БД
        database = AppDatabase.getInstance(this)

        // Находим View по ID
        btnBack = findViewById(R.id.imageButton2)
        btnSelectFile = findViewById(R.id.btnSelectFile)
        tvFileName = findViewById(R.id.tvFileName)
        btnImport = findViewById(R.id.buttonImport)

        // Обработчик «назад»
        btnBack.setOnClickListener {
            onBackPressed()
        }

        // Обработчик кнопки «Выбрать файл»
        btnSelectFile.setOnClickListener {
            // Фильтруем только ZIP: берем MIME type "application/zip"
            openDocumentLauncher.launch(arrayOf("application/zip", "application/octet-stream"))
            // Обратите внимание: некоторые ZIP-файлы могут возвращать mime "application/octet-stream"
        }

        // Обработчик кнопки «Импорт»
        btnImport.setOnClickListener {
            val zipUri = selectedZipUri
            if (zipUri == null) {
                Toast.makeText(this, "Сначала выберите файл", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            performImport(zipUri)
        }
    }

    /**
     * Получаем «человекочитаемое» имя файла по Uri:
     * читаем через OpenableColumns.DISPLAY_NAME.
     */
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

    /**
     * Основная функция: берет URI выбранного ZIP-а и начинает разбор:
     * 1. Извлекает backup_data.json → парсит JSON с помощью Gson
     * 2. Чистит базы данных (удаляет старые записи) и вставляет новые
     * 3. Извлекает все файлы из папки photos/ в ZIP → в каталог filesDir/photos/
     * 4. Выдаёт пользователю сообщение об успехе/ошибке
     */
    private fun performImport(zipUri: Uri) {
        // Показываем неиндетерминированный ProgressDialog
        val progressDialog = ProgressDialog(this).apply {
            setTitle("Импорт резервной копии")
            setMessage("Пожалуйста, подождите…")
            setCancelable(false)
            isIndeterminate = true
            show()
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1) Открываем ZipInputStream из Uri
                contentResolver.openInputStream(zipUri).use { inputStream ->
                    if (inputStream == null) throw IOException("Не удалось открыть выбранный файл")

                    ZipInputStream(BufferedInputStream(inputStream)).use { zis ->
                        var entry: ZipEntry? = zis.nextEntry

                        // Для JSON: временно сохраним строку, когда найдём entry с именем backup_data.json
                        var jsonData: String? = null

                        // 1.a) Находим backup_data.json и читаем его в строку
                        while (entry != null) {
                            if (!entry.isDirectory && entry.name == "backup_data.json") {
                                // Считываем весь JSON в одну строку
                                val writer = StringWriter()
                                val buffer = CharArray(1024)
                                InputStreamReader(zis, "UTF-8").use { reader ->
                                    var n: Int
                                    while (reader.read(buffer).also { n = it } != -1) {
                                        writer.write(buffer, 0, n)
                                    }
                                }
                                jsonData = writer.toString()
                                break
                            }
                            // Если не тот файл, переходим к следующему entry
                            entry = zis.nextEntry
                        }

                        if (jsonData == null) {
                            throw IOException("В ZIP не найден файл backup_data.json")
                        }

                        // Парсим JSON: получаем List<Category> и List<Subcategory>
                        val gson = Gson()
                        val backupMapType = object : com.google.gson.reflect.TypeToken<Map<String, Any>>() {}.type
                        @Suppress("UNCHECKED_CAST")
                        val backupMap = gson.fromJson<Map<String, Any>>(jsonData, backupMapType)

                        // В backupMap["categories"] лежит список Map<..., ...>, конвертируем его в список Category
                        val categoriesJson = gson.toJsonTree(backupMap["categories"])
                        val subcategoriesJson = gson.toJsonTree(backupMap["subcategories"])

                        val categoriesList: List<Category> = gson.fromJson(categoriesJson, object : com.google.gson.reflect.TypeToken<List<Category>>() {}.type)
                        val subcategoriesList: List<Subcategory> = gson.fromJson(subcategoriesJson, object : com.google.gson.reflect.TypeToken<List<Subcategory>>() {}.type)

                        // 2) Очищаем старые данные (можно вызвать categoryDao().deleteAll() и subcategoryDao().deleteAll())
                        database.categoryDao().apply {
                            // Если у вас нет метода deleteAll(), можно использовать:
                            // database.clearAllTables()
                            deleteAllCategories()
                        }
                        database.subcategoryDao().apply {
                            deleteAllSubcategories()
                        }

                        // 3) Вставляем из JSON (предполагаем, что поля id в JSON или не важны)
                        categoriesList.forEach { cat ->
                            // Если в JSON есть поле id, но вы хотите, чтобы база сама присвоила новый id,
                            // создайте новый объект Category( name = cat.name )
                            // Предположим, что в entity Category у вас 2 поля: val id: Int, val name: String
                            database.categoryDao().insert(Category(cat.name))
                        }
                        // После вставки категорий нам нужно узнать их новые ID, чтобы привязать subcategories
                        val allCatsAfterInsert = database.categoryDao().getAllCategories() // список уже с новыми id

                        // Сопоставим по имени: oldName → newId
                        val nameToNewIdMap = allCatsAfterInsert.associateBy({ it.name }, { it.id.toInt() })

                        subcategoriesList.forEach { sub ->
                            // Ищем новый categoryId по имени категории
                            val parentName = categoriesList.find { it.id == sub.categoryId }?.name
                            val newCatId = if (parentName != null) {
                                nameToNewIdMap[parentName] ?: sub.categoryId.toInt()
                            } else {
                                // Если не нашли по имени, на всякий случай поставим старое значение,
                                // которое тоже может сработать, если в новой базе id совпадут.
                                sub.categoryId.toInt()
                            }
                            database.subcategoryDao().insert(Subcategory(newCatId, sub.name))
                        }
                    }
                }

                // 4) Ещё раз открываем ZipInputStream, чтобы извлечь папку photos/
                contentResolver.openInputStream(zipUri).use { inputStream2 ->
                    if (inputStream2 == null) throw IOException("Не удалось открыть ZIP для фото")
                    ZipInputStream(BufferedInputStream(inputStream2)).use { zis2 ->
                        var entry2: ZipEntry? = zis2.nextEntry

                        // Основная «рабочая» папка, куда копировать все файлы
                        val photosRoot = File(filesDir, "photos")

                        while (entry2 != null) {
                            if (!entry2.isDirectory && entry2.name.startsWith("photos/")) {
                                // Получаем путь внутри photos/, например: photos/subfolder/IMG_123.jpg
                                val relativePath = entry2.name.removePrefix("photos/")
                                val outFile = File(photosRoot, relativePath)

                                // Создаём все родительские папки
                                outFile.parentFile?.let { parentDir ->
                                    if (!parentDir.exists()) parentDir.mkdirs()
                                }

                                // Записываем содержимое entry в файл outFile
                                FileOutputStream(outFile).use { fos ->
                                    val buffer = ByteArray(4096)
                                    var count: Int
                                    while (zis2.read(buffer).also { count = it } != -1) {
                                        fos.write(buffer, 0, count)
                                    }
                                    fos.flush()
                                }
                            }
                            entry2 = zis2.nextEntry
                        }
                    }
                }

                // 5) Всё прошло успешно: возвращаемся в UI-поток и оповещаем пользователя
                runOnUiThread {
                    progressDialog.dismiss()
                    AlertDialog.Builder(this@ImportActivity)
                        .setTitle("Импорт завершён")
                        .setMessage("Данные успешно восстановлены из резервной копии.")
                        .setPositiveButton("ОК") { dialog, _ ->
                            dialog.dismiss()
                            finish() // Можно закрыть экран
                        }
                        .show()
                }

            } catch (e: Exception) {
                Log.e("ImportError", "Ошибка при импорте базы", e)
                runOnUiThread {
                    progressDialog.dismiss()
                    AlertDialog.Builder(this@ImportActivity)
                        .setTitle("Ошибка импорта")
                        .setMessage("Не удалось выполнить импорт:\n${e.localizedMessage}")
                        .setPositiveButton("ОК", null)
                        .show()
                }
            }
        }
    }
}