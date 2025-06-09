package com.hfad.mystylebox

import android.content.Context
import android.Manifest
import android.app.ProgressDialog
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.os.Environment
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import com.hfad.mystylebox.database.AppDatabase
import com.hfad.mystylebox.database.entity.Category
import com.hfad.mystylebox.database.entity.Subcategory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.core.view.GravityCompat
import java.io.BufferedOutputStream
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import com.google.gson.Gson
import com.hfad.mystylebox.notifications.AlarmScheduler
import com.hfad.mystylebox.notifications.NotificationHelper
import com.hfad.mystylebox.ui.activity.AboutActivity
import com.hfad.mystylebox.ui.activity.ImportActivity
import java.io.FileInputStream

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var database: AppDatabase
    private lateinit var bottomNavView: BottomNavigationView
    private lateinit var sharedPrefs: SharedPreferences
    private val requestNotifLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                AlertDialog.Builder(this)
                    .setTitle("Требуется разрешение")
                    .setMessage("Для сохранения архива необходим доступ к хранилищу.")
                    .setPositiveButton("Открыть настройки") { _, _ ->
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:$packageName"))
                        startActivity(intent)
                    }
                    .setNegativeButton("Отмена", null)
                    .show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        setContentView(R.layout.activity_main)
        NotificationHelper.createNotificationChannel(this)
        sharedPrefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)
        navView.setNavigationItemSelectedListener(this)

        val notificationsMenuItem = navView.menu.findItem(R.id.nav_notifications)
        val switchView = notificationsMenuItem.actionView
            ?.findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.switch_notifications)
        if (switchView == null) {
            Log.w("MainActivity", "Не удалось найти SwitchCompat в пункте меню nav_notifications")
        } else {
            val notificationsEnabled = sharedPrefs.getBoolean("notifications_enabled", false)
            switchView.isChecked = notificationsEnabled

            switchView.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val permState = ContextCompat.checkSelfPermission(
                            this, Manifest.permission.POST_NOTIFICATIONS
                        )
                        if (permState != PackageManager.PERMISSION_GRANTED) {
                            if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                                requestNotifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                requestNotifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                showGoToSettingsDialog()
                            }
                            switchView.isChecked = false
                            return@setOnCheckedChangeListener
                        }
                    sharedPrefs.edit().putBoolean("notifications_enabled", true).apply()
                    AlarmScheduler.scheduleMorningReminder(this)
                    AlarmScheduler.scheduleEveningReminder(this)
                    AlarmScheduler.scheduleStaleItemsCheck(this)
                    Toast.makeText(this, "Уведомления включены", Toast.LENGTH_SHORT).show()
                } else {
                    sharedPrefs.edit().putBoolean("notifications_enabled", false).apply()
                    AlarmScheduler.cancelAllReminders(this)
                    Toast.makeText(this, "Уведомления выключены", Toast.LENGTH_SHORT).show()
                  }
                }
            }
        }

        drawerLayout.setDrawerLockMode(
            DrawerLayout.LOCK_MODE_UNLOCKED,
            GravityCompat.START
        )

        navView.itemIconTintList = null

        val navHost = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHost.navController

        bottomNavView = findViewById(R.id.bottom_nav)
        NavigationUI.setupWithNavController(bottomNavView, navController)

        database = AppDatabase.getInstance(this)
        Thread {
            val categories = database.categoryDao().getAllCategories()
            if (categories.isEmpty()) {
                populateInitialData()
            }
        }.start()

        val openFragment = intent.getStringExtra("openFragment")
        if (openFragment == "outfits") {
            bottomNavView.selectedItemId = R.id.outfitsFragment
        }
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        drawerLayout.closeDrawer( GravityCompat.START   )
        when (item.itemId) {
            R.id.nav_importdatabase -> {
                startActivity(Intent(this, ImportActivity::class.java))
                return true
            }
            R.id.nav_downloadarchive -> {
                checkStoragePermissionAnd { exportDatabaseToZip() }
                return true
            }
            R.id.nav_savebackupdatabase -> {
                checkStoragePermissionAnd { backupDatabaseWithPhotos() }
                return true
            }
            R.id.nav_help -> {
                startActivity(Intent(this, AboutActivity::class.java))
                return true
            }
        }
        return false
    }

    private fun checkStoragePermissionAnd(action: () -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            action()
        } else {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED -> {
                    action()
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun exportDatabaseToZip() {
        // 1) Создаём и показываем ProgressDialog
        val progressDialog = ProgressDialog(this).apply {
            setTitle("Создание архива")
            setMessage("Пожалуйста, подождите…")
            setCancelable(false)
            isIndeterminate = true
            show()
        }

        // 2) Запускаем работу в фоне
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val downloadDir = Environment
                    .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val zipFile = File(downloadDir, "mystylebox_archive_${System.currentTimeMillis()}.zip")

                ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->
                    val db = database.openHelper.readableDatabase

                    // Экспорт таблиц
                    db.query("SELECT name FROM sqlite_master WHERE type='table'", arrayOfNulls<Any>(0)).use { cursor ->
                        while (cursor.moveToNext()) {
                            val table = cursor.getString(0)
                            if (table in listOf("android_metadata", "sqlite_sequence", "room_master_table")) continue

                            zos.putNextEntry(ZipEntry("$table.csv"))
                            db.query("SELECT * FROM `$table`", arrayOfNulls<Any>(0)).use { c ->
                                val writer = BufferedWriter(OutputStreamWriter(zos))
                                val cols = c.columnNames
                                writer.write(cols.joinToString(","))
                                writer.newLine()
                                while (c.moveToNext()) {
                                    val row = cols.map { col ->
                                        c.getString(c.getColumnIndexOrThrow(col)).replace("\"", "\"\"")
                                    }
                                    writer.write(row.joinToString(",") { "\"$it\"" })
                                    writer.newLine()
                                }
                                writer.flush()
                            }
                            zos.closeEntry()
                        }
                    }

                    // Добавление фото (как было ранее)
                    val roots = listOfNotNull(
                        File(filesDir, "photos"),
                        getExternalFilesDir("photos")?.let { File(it.absolutePath) },
                        getExternalFilesDir(Environment.DIRECTORY_PICTURES)?.let { File(it.absolutePath) },
                        File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "MyStyleBox")
                    )
                    val seen = mutableSetOf<String>()
                    fun zipRec(dir: File, base: String) {
                        if (!dir.exists()) return
                        dir.listFiles()?.forEach { f ->
                            val entryName = "$base/${f.name}"
                            if (f.isDirectory) zipRec(f, entryName)
                            else if (seen.add(f.absolutePath)) {
                                zos.putNextEntry(ZipEntry(entryName))
                                FileInputStream(f).use { fis -> fis.copyTo(zos) }
                                zos.closeEntry()
                            }
                        }
                    }
                    roots.forEach { zipRec(it, "photos") }
                }

                // 3) Всё готово — назад в UI-поток
                runOnUiThread {
                    progressDialog.dismiss()
                    AlertDialog.Builder(this@MainActivity)
                        .setTitle("Готово")
                        .setMessage("ZIP-архив сохранён в папке Download.")
                        .setPositiveButton("ОК", null)
                        .show()
                }
            } catch (e: Exception) {
                Log.e("ExportZip", "Ошибка при экспорте", e)
                runOnUiThread {
                    progressDialog.dismiss()
                    AlertDialog.Builder(this@MainActivity)
                        .setTitle("Ошибка")
                        .setMessage("Не удалось создать архив:\n${e.localizedMessage}")
                        .setPositiveButton("ОК", null)
                        .show()
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun backupDatabaseWithPhotos() {
        val progressDialog = ProgressDialog(this).apply {
            setTitle("Создание резервной копии")
            setMessage("Пожалуйста, подождите…")
            setCancelable(false)
            isIndeterminate = true
            show()
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val allCategories            = database.categoryDao().getAllCategories()
                val allSubcategories         = database.subcategoryDao().getAllSubcategories()
                val allClothingItems         = database.clothingItemDao().getAllClothingItems()
                val allTags                  = database.tagDao().getAllTags()
                val allClothingItemTags      = database.clothingItemTagDao().getAllClothingItemTags()
                val allOutfits               = database.outfitDao().getAllOutfits()
                val allOutfitClothingItems   = database.outfitClothingItemDao().getAllOutfitClothingItems()
                val allOutfitTags            = database.outfitTagDao().getAllOutfitTags()
                val allDailyPlans            = database.dailyPlanDao().getAllDailyPlans()
                val allWishListItems         = database.wishListItemDao().getAllWishListItems()

                val backupMap = mapOf(
                    "categories"               to allCategories,
                    "subcategories"            to allSubcategories,
                    "clothing_items"           to allClothingItems,
                    "tags"                     to allTags,
                    "clothing_item_tags"       to allClothingItemTags,
                    "outfits"                  to allOutfits,
                    "outfit_clothing_items"    to allOutfitClothingItems,
                    "outfit_tags"              to allOutfitTags,
                    "daily_plans"              to allDailyPlans,
                    "wish_list_items"          to allWishListItems
                )

                val json = Gson().toJson(backupMap)

                val downloadDir = Environment
                    .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val zipFileName = "mystylebox_backup_${System.currentTimeMillis()}.zip"
                val zipFile = File(downloadDir, zipFileName)

                ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->
                    zos.putNextEntry(ZipEntry("backup_data.json"))
                    val writer = BufferedWriter(OutputStreamWriter(zos))
                    writer.write(json)
                    writer.flush()
                    zos.closeEntry()

                    val roots = listOfNotNull(
                        File(filesDir, "photos"),
                        getExternalFilesDir("photos")?.let { File(it.absolutePath) },
                        getExternalFilesDir(Environment.DIRECTORY_PICTURES)?.let { File(it.absolutePath) },
                        File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "MyStyleBox"),
                        getExternalCacheDir()?.let { File(it.absolutePath) }
                    )

                    val seen = mutableSetOf<String>()

                    fun zipRec(dir: File, base: String) {
                        if (!dir.exists()) return
                        dir.listFiles()?.forEach { f ->
                            val entryName = "$base/${f.name}"
                            if (f.isDirectory) {
                                zipRec(f, entryName)
                            } else if (seen.add(f.absolutePath)) {
                                zos.putNextEntry(ZipEntry(entryName))
                                FileInputStream(f).use { fis ->
                                    fis.copyTo(zos)
                                }
                                zos.closeEntry()
                            }
                        }
                    }

                    roots.forEach { rootDir ->
                        zipRec(rootDir, rootDir.name)
                    }
                }

                runOnUiThread {
                    progressDialog.dismiss()
                    AlertDialog.Builder(this@MainActivity)
                        .setTitle("Готово")
                        .setMessage("Резервная копия (JSON + фото) сохранена в папке Download:\n$zipFileName")
                        .setPositiveButton("ОК", null)
                        .show()
                }

            } catch (e: Exception) {
                Log.e("BackupZip", "Ошибка при создании резервной копии", e)
                runOnUiThread {
                    progressDialog.dismiss()
                    AlertDialog.Builder(this@MainActivity)
                        .setTitle("Ошибка")
                        .setMessage("Не удалось сохранить резервную копию:\n${e.localizedMessage}")
                        .setPositiveButton("ОК", null)
                        .show()
                }
            }
        }
    }

    private fun showGoToSettingsDialog() {
        AlertDialog.Builder(this)
            .setTitle("Необходимо разрешение на уведомления")
            .setMessage(
                "Чтобы получать напоминания и уведомления, " +
                        "необходимо разрешить доступ в настройках. " +
                        "Откройте настройки приложения и включите «Разрешить уведомления»."
            )
            .setPositiveButton("Перейти в настройки") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    fun disableMenuItem(id: Int) {
        val item = navView.menu.findItem(id)
        item.isEnabled = false
        val gray = ContextCompat.getColor(this, android.R.color.darker_gray)
        val span = SpannableString(item.title)
        span.setSpan(ForegroundColorSpan(gray), 0, span.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        item.title = span
    }
    override fun onBackPressed() {
        drawerLayout.setDrawerElevation(20f)
        drawerLayout.setScrimColor(Color.TRANSPARENT)
        if (drawerLayout.isDrawerOpen( GravityCompat.START   )) {
            drawerLayout.closeDrawer( GravityCompat.START   )
        } else {
            super.onBackPressed()
        }
    }

    private fun populateInitialData() {
        try {
            val categoryDao = database.categoryDao()
            val subcategoryDao = database.subcategoryDao()
            val topId = categoryDao.insert(Category("Верх"))
            val bottomId = categoryDao.insert(Category("Низ"))
            val dressId = categoryDao.insert(Category("Платья"))
            val shoesId = categoryDao.insert(Category("Обувь"))
            val accessoriesId = categoryDao.insert(Category("Аксессуары"))
            val costumesId = categoryDao.insert(Category("Костюмы"))
            val overallsId = categoryDao.insert(Category("Комбинезоны"))
            val bagsId = categoryDao.insert(Category("Сумки"))
            val outerwearId = categoryDao.insert(Category("Верхняя одежда"))
            val headwearId = categoryDao.insert(Category("Головные уборы"))
            val sportswearId = categoryDao.insert(Category("Спорт"))
            val beachwearId = categoryDao.insert(Category("Пляж"))
            val underwearId = categoryDao.insert(Category("Нижнее белье"))
            listOf(
                "Футболка", "Блузка", "Рубашка",
                "Топ", "Свитер", "Джемпер", "Толстовка", "Худи", "Водолазка", "Майка", "Боди", "Лонгслив", "Поло",
                "Кофта", "Жилет", "Пиджак", "Жакет", "Кардиган", "Туника", "Пуловер", "Свитшот", "Блейзер", "Халат",
                "Футболка домашняя", "Пижама"
            ).forEach { name ->
                subcategoryDao.insert(Subcategory(topId.toInt(), name))
            }
            listOf(
                "Брюки", "Джинсы", "Леггинсы", "Юбка", "Шорты", "Классические штаны", "Велосипедки", "Бриджи", "Слаксы", "Чинос",
                        "Бермуды", "Брюки классические", "Брюки льняные", "Брюки укороченные", "Брюки парадные", "Шорты домашние"
            ).forEach { name ->
                subcategoryDao.insert(Subcategory(bottomId.toInt(), name))
            }
            listOf(
                "Повседневное платье", "Официальное платье", "Вечернее платье", "Коктейльное платье", "Летнее платье", "Трикотажное платье",
                "Сарафан", "Платье макси", "Ромпер", "Курта", "Платье-комбинация", "Платье с запахом", "Платье-футляр", "Платье до колена", "Платье джинсовое"
            ).forEach { name ->
                subcategoryDao.insert(Subcategory(dressId.toInt(), name))
            }
            listOf(
                "Кроссовки", "Кеды", "Туфли", "Ботинки", "Босоножки", "Сандалии", "Сапоги", "Лоферы", "Мюли", "Шлёпанцы", "Ботильоны", "Туфли-лодочки", "Балетки",
                "Сникеры", "Полуботинки", "Полусапоги", "Ботфорты", "Обувь рабочая", "Слиперы", "Мокасины", "Сланцы", "Слипоны", "Дерби", "Тапочки"
            ).forEach { name ->
                subcategoryDao.insert(Subcategory(shoesId.toInt(), name))
            }
            listOf(
                "Очки", "Перчатки", "Серьги", "Ремень", "Колье", "Браслет", "Шарф", "Кольцо", "Очки солнцезащитные", "Часы", "Цепочка, цепь", "Брошь", "Платок, шаль",
                "Пояс", "Подвеска", "Кулон", "Чокер", "Повязка, лента", "Галстук", "Бусы", "Заколка", "Ожерелье", "Запонки", "Галстук-бабочка", "Кашне", "Подтяжки",
                "Портмоне, бумажник", "Другое"
            ).forEach { name ->
                subcategoryDao.insert(Subcategory(accessoriesId.toInt(), name))
            }
            listOf(
                "Костюм брючный", "Комплект с юбкой", "Традиционный наряд", "Комплект из двух вещей", "Костюм деловой", "Костюм юбочный", "Co-ord комплект", "Костюм вязаный",
                "Костюм трикотажный", "Кэтсьют", "Форма школьная", "Твинсет", "Униформа", "Костюм – тройка", "Фрак", "Костюм торжественный", "Костюм карнавальный", "Камербанд"
            ).forEach { name ->
                subcategoryDao.insert(Subcategory(costumesId.toInt(), name))
            }
            listOf(
            "Комбинезон", "Комбинезон летний", "Комбинезон вечерний", "Комбинезон рабочий", "Комбинезон брючный", "Комбинезон джинсовый", "Полукомбинезон", "Комбинезон - юбка",
            "Комбинезон без рукавов", "Комбинезон с открытой спиной", "Комбинезон кожанный", "Комбинезон с капюшоном"
            ).forEach { name ->
                subcategoryDao.insert(Subcategory(overallsId.toInt(), name))
            }
            listOf(
            "Сумка", "Рюкзак", "Сумка кросс боди", "Сумка-тоут", "Клатч", "Борсетка", "Сумка-багет", "Сумка хобо", "Сумка-кошелек", "Шоппер", "Поясная сумка",
            "Сумка через плечо", "Дорожная сумка", "Портфель", "Рюкзак школьный", "Сумка для ноута"
            ).forEach { name ->
                subcategoryDao.insert(Subcategory(bagsId.toInt(), name))
            }
            listOf(
            "Пальто", "Куртка", "Пуховик", "Парка", "Плащ", "Шуба", "Ветровка", "Бомбер", "Куртка кожаная", "Тренчкот(Тренч)", "Косуха", "Накидка", "Дождевик", "Полупальто",
            "Дубленка", "Кейп"
                   ).forEach { name ->
                subcategoryDao.insert(Subcategory(outerwearId.toInt(), name))
            }
            listOf(
            "Шапка", "Кепка", "Панама", "Берет", "Фетровая шляпа", "Шляпа", "Бейсболка", "Бандана", "Косынка", "Хиджаб", "Маска", "Наушники теплые", "Кепи", "Капор",
            "Фуражка", "Шляпа-котелок"
            ).forEach { name ->
                subcategoryDao.insert(Subcategory( headwearId.toInt(), name))
            }
            listOf(
            "Леггинсы спортивные", "Спортивные брюки", "Футболка спортивная", "Майка спортивная", "Олимпийка", "Спортивные костюмы", "Спортивные штаны", "Куртка спортивная",
                "Кроссовки для фитнеса", "Ботинки спортивные", "Лонгслив тренировочный", "Топ спортивный", "Юбка теннисная", "Кроссовки беговые", "Йога-штаны", "Костюм для черлидинга"
            ).forEach { name ->
                subcategoryDao.insert(Subcategory(sportswearId.toInt(), name))
            }
            listOf(
            "Купальник", "Бикини", "Туника пляжная", "Рубашка пляжная", "Шорты пляжная", "Костюм купальный", "Платье пляжное", "Плавки", "Парео", "Майка пляжная", "Шорты для плавания",
                "Танкини", "Монокини"
                   ).forEach { name ->
                subcategoryDao.insert(Subcategory(beachwearId.toInt(), name))
            }
            listOf(
            "Носки", "Трусы", "Майка (нижнее белье)", "Трусы-боксеры", "Колготки", "Колготки капроновые", "Боди", "Носки спортивные", "Гольфы", "Трико", "Кальсоны", "Термо бельё",
            "Трусы-стринги", "Трусы-слипы", "Трусы транки", "Бралетт", "Камисоль", "Боди-водолазка", "Лосины", "Бюстгальтер", "Бюстгальтер спортивный", "Cорочка", "Чулки"
                   ).forEach { name ->
                subcategoryDao.insert(Subcategory(underwearId.toInt(), name))
            }
            getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                .edit().putBoolean("db_populated", true).apply()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}