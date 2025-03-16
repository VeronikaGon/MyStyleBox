package com.hfad.mystylebox

import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.hfad.mystylebox.database.AppDatabase
import com.hfad.mystylebox.database.Category
import androidx.appcompat.widget.SearchView
import com.hfad.mystylebox.adapter.CategoryPagerAdapter
import com.hfad.mystylebox.adapter.SearchSubcategoryPagerAdapter
import com.hfad.mystylebox.database.Subcategory

class CategorySelectionActivity : AppCompatActivity() {

    private var imageUri: Uri? = null
    private lateinit var categories: List<Category>
    private lateinit var db: AppDatabase
    private lateinit var allSubcategories: List<Subcategory>

    private var selectedTabIndex: Int = 0

    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var searchView: SearchView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category_selection)

        db = AppDatabase.getInstance(this)
        searchView = findViewById(R.id.searchView)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                filterCategories(newText)
                return true
            }
        })

        val uriString = intent.getStringExtra("image_uri")
        if (!uriString.isNullOrEmpty()) {
            imageUri = Uri.parse(uriString)
        }

        categories = db.categoryDao().getAllCategories()
        allSubcategories = db.subcategoryDao().getAllSubcategories()

        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)

        val defaultAdapter = CategoryPagerAdapter(this, categories, imageUri)
        viewPager.adapter = defaultAdapter
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = categories[position].name
        }.attach()
        tabLayout.getTabAt(0)?.view?.setBackgroundColor(Color.parseColor("#FCD5CE"))
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                updateTabBackgrounds()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {
                updateTabBackgrounds()
            }
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    // Обновление фона вкладок
    private fun updateTabBackgrounds() {
        for (i in 0 until tabLayout.tabCount) {
            val tab = tabLayout.getTabAt(i)
            val tabView = (tabLayout.getChildAt(0) as? android.view.ViewGroup)?.getChildAt(i)
            if (tab?.isSelected == true) {
                tabView?.setBackgroundColor(Color.parseColor("#FCD5CE"))
            } else {
                tabView?.setBackgroundColor(Color.parseColor("#F8EDEB"))
            }
        }
    }

    private fun filterCategories(query: String?) {
        if (query.isNullOrEmpty()) {
            val defaultAdapter = CategoryPagerAdapter(this, categories, imageUri)
            viewPager.adapter = defaultAdapter
            tabLayout.removeAllTabs()
            TabLayoutMediator(tabLayout, viewPager) { tab, position ->
                tab.text = categories[position].name
            }.attach()
            viewPager.setCurrentItem(selectedTabIndex, false)
        } else {
            selectedTabIndex = tabLayout.selectedTabPosition
            val filteredSubcategories = allSubcategories.filter { subcategory ->
                subcategory.name.contains(query, ignoreCase = true)
            }
            if (filteredSubcategories.isEmpty()) {
                val emptyAdapter = SearchSubcategoryPagerAdapter(this, listOf(), emptyMap(), imageUri)
                viewPager.adapter = emptyAdapter
                tabLayout.removeAllTabs()
                TabLayoutMediator(tabLayout, viewPager) { tab, _ ->
                    tab.text = "Ничего не найдено"
                }.attach()
                return
            }
            val categoryIdToName = categories.associate { it.id to it.name }
            val filteredSubcategoriesByCategory: Map<String, List<Subcategory>> =
                filteredSubcategories.groupBy { subcategory ->
                    categoryIdToName[subcategory.categoryId] ?: "Без категории"
                }
            val filteredCategories: List<String> = filteredSubcategoriesByCategory.keys.toList().sorted()
               val searchAdapter = SearchSubcategoryPagerAdapter(this, filteredCategories, filteredSubcategoriesByCategory, imageUri)
            viewPager.adapter = searchAdapter
            tabLayout.removeAllTabs()
            TabLayoutMediator(tabLayout, viewPager) { tab, position ->
                tab.text = filteredCategories[position]
            }.attach()
            viewPager.setCurrentItem(0, false)
        }
    }

    // Метод, вызываемый из фрагментов при выборе подкатегории
    fun onSubcategorySelected(subcategory: String, selectedSubcategoryId: Int) {
        val intent = android.content.Intent(this, ClothesActivity::class.java).apply {
            putExtra("subcategory", subcategory)
            putExtra("selected_subcategory_id", selectedSubcategoryId)
            putExtra("image_path", imageUri.toString())
        }
        startActivity(intent)
        finish()
    }
}