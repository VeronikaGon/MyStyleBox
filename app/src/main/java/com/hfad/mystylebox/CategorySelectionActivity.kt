package com.hfad.mystylebox

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import com.google.android.material.tabs.TabLayoutMediator
import com.hfad.mystylebox.database.AppDatabase
import com.hfad.mystylebox.database.Category
import androidx.appcompat.widget.SearchView
import com.hfad.mystylebox.database.Subcategory


class CategorySelectionActivity : AppCompatActivity() {

    private var imageUri: Uri? = null
    private lateinit var categories: List<Category>
    private lateinit var db: AppDatabase


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category_selection)

        db = AppDatabase.getInstance(this)
        val searchView = findViewById<SearchView>(R.id.searchView)
        searchView.isIconified = false
        searchView.queryHint = "Поиск по категориям"
        val searchEditText = searchView.findViewById<EditText>(androidx.appcompat.R.id.search_src_text)
        searchEditText.setPadding(16, 16, 16, 16)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterCategories(newText)
                return true
            }
        })
        // Считываем imageUri из Intent
        val uriString = intent.getStringExtra("image_uri")
        if (!uriString.isNullOrEmpty()) {
            imageUri = Uri.parse(uriString)
        }
        // Создаём адаптер для ViewPager
        val db = AppDatabase.getInstance(this)
        categories = db.categoryDao().getAllCategories()
        // Настраиваем ViewPager2 с адаптером
        val viewPager = findViewById<ViewPager2>(R.id.viewPager)
        val adapter = CategoryPagerAdapter(this, categories, imageUri)
        viewPager.adapter = adapter
        // Настраиваем TabLayout и связываем его с ViewPager2
        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = categories[position].name
        }.attach()
        tabLayout.getTabAt(0)?.view?.setBackgroundColor(Color.parseColor("#FCD5CE"))
        tabLayout.addOnTabSelectedListener(object : OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                tab.view.setBackgroundColor(Color.parseColor("#FCD5CE"))
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {
                tab?.view?.setBackgroundColor(Color.parseColor("#F8EDEB"))
            }
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    // Метод, который будет вызываться из фрагментов при выборе подкатегории
    fun onSubcategorySelected(subcategory: String) {
        val intent = Intent(this, ClothesActivity::class.java)
        intent.putExtra("subcategory", subcategory)
        intent.putExtra("image_path", imageUri.toString())
        startActivity(intent)
    }

    private fun filterCategories(query: String?) {
        val viewPager = findViewById<ViewPager2>(R.id.viewPager)
        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)

        if (query.isNullOrEmpty()) {
            // Отображаем стандартные категории
            val adapter = CategoryPagerAdapter(this, categories, imageUri)
            viewPager.adapter = adapter
            tabLayout.removeAllTabs()
            TabLayoutMediator(tabLayout, viewPager) { tab, position ->
                tab.text = categories[position].name
            }.attach()
        } else {
            // Фильтруем подкатегории по запросу
            val allSubcategories = db.subcategoryDao().getAllSubcategories()
            val filteredList = allSubcategories.filter { subcategory ->
                subcategory.name.contains(query, ignoreCase = true)
            }
            // Отображаем вкладку "Все" с результатами поиска
            val adapter = SubcategoryPagerAdapter(this, filteredList, imageUri)
            viewPager.adapter = adapter
            tabLayout.removeAllTabs()
            TabLayoutMediator(tabLayout, viewPager) { tab, _ ->
                tab.text = "Все"
            }.attach()
        }
    }
}