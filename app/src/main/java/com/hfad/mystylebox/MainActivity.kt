package com.hfad.mystylebox

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.Gravity
import android.view.MenuItem
import android.view.View
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
import com.hfad.mystylebox.ui.activity.WelcomeActivity

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var database: AppDatabase
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        setContentView(R.layout.activity_main)

        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)
        navView.setNavigationItemSelectedListener(this)

        drawerLayout.setDrawerLockMode(
            DrawerLayout.LOCK_MODE_UNLOCKED,
            Gravity.START
        )

        navView.itemIconTintList = null

        navView.setNavigationItemSelectedListener(this)
        val navHost = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHost.navController
        NavigationUI.setupWithNavController(
            findViewById<BottomNavigationView>(R.id.bottom_nav),
            navController
        )
        val menu = navView.menu
        val deleteItem = menu.findItem(R.id.nav_deleteaccount)

        val redTitle = SpannableString(deleteItem.title)
        redTitle.setSpan(
            ForegroundColorSpan(Color.RED),
            0, redTitle.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        deleteItem.title = redTitle

        disableMenuItem(R.id.nav_synchronization)
        disableMenuItem(R.id.newpassword)
        disableMenuItem(R.id.nav_changeemail)

        database = AppDatabase.getInstance(this)
        Thread {
            val categories = database.categoryDao().getAllCategories()
            if (categories.isEmpty()) {
                populateInitialData()
            }
        }.start()
        val bottomNavView = findViewById<BottomNavigationView>(R.id.bottom_nav)
        NavigationUI.setupWithNavController(bottomNavView, navController)
        val openFragment = intent.getStringExtra("openFragment")
        if (openFragment == "outfits") {
            navController.navigate(R.id.outfitsFragment)
        }
        intent.getStringExtra("openFragment")?.let {
            if (it == "outfits") navController.navigate(R.id.outfitsFragment)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        drawerLayout.closeDrawer(Gravity.START)
        when (item.itemId) {
            R.id.nav_account -> {
                startActivity(Intent(this, WelcomeActivity::class.java))
                return true
            }
            R.id.nav_synchronization -> {
                // переход в настройки
                return true
            }
            R.id.newpassword -> {
                // дизлог
                return true
            }
        }
        return false
    }
    fun disableMenuItem(id: Int) {
        val item = navView.menu.findItem(id)
        item.isEnabled = false
        // сделать заголовок серым
        val gray = ContextCompat.getColor(this, android.R.color.darker_gray)
        val span = SpannableString(item.title)
        span.setSpan(ForegroundColorSpan(gray), 0, span.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        item.title = span
    }
    override fun onBackPressed() {
        drawerLayout.setDrawerElevation(20f)
        drawerLayout.setScrimColor(Color.TRANSPARENT)
        if (drawerLayout.isDrawerOpen(Gravity.START)) {
            drawerLayout.closeDrawer(Gravity.START)
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