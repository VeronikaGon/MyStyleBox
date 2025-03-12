package com.hfad.mystylebox

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import android.graphics.Color
import android.widget.EditText
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.hfad.mystylebox.adapter.ClothingAdapter
import com.hfad.mystylebox.database.AppDatabase

class SearchClothingActivity : AppCompatActivity() {

    private lateinit var searchView: SearchView
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ClothingAdapter
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search_clothing)

        searchView = findViewById(R.id.searchView)
        searchView.isIconified = false

        val searchText = searchView.findViewById<EditText>(androidx.appcompat.R.id.search_src_text)
        searchText.setTextColor(Color.BLACK) // Цвет вводимого текста
        searchText.setHintTextColor(Color.GRAY) // Цвет hint
        searchView.setBackgroundColor(Color.WHITE) // Фон SearchView

        recyclerView = findViewById(R.id.recyclerViewSearch)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Инициализация базы данных Room (работа в основном потоке для простоты)
        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "wardrobe_db"
        )
            .allowMainThreadQueries()
            .build()

        val itemsWithCategories = db.clothingItemDao().getAllItemsWithCategories()

        // Создаем список для адаптера
        val categorySubcategoryList = itemsWithCategories.map {
            Pair(it.categoryName, it.subcategoryName)
        }

        // Передаем полные данные в адаптер
        adapter = ClothingAdapter(itemsWithCategories, R.layout.item_clothing1)
        recyclerView.adapter = adapter

        setupSearchView()
        setupItemClick()
    }

    private fun setupSearchView() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let {
                    val trimmedQuery = it.trim()
                    if (trimmedQuery.isNotEmpty()) {
                        val queryPattern = "%$trimmedQuery%"
                        var results = db.clothingItemDao().searchByNameWithCategories(queryPattern)
                        if (results.isEmpty()) {
                            results = db.clothingItemDao().searchByDescriptionWithCategories(queryPattern)
                        }
                        adapter.updateData(results) // Теперь совпадает тип
                    }
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                val trimmedQuery = newText?.trim() ?: ""
                if (trimmedQuery.isNotEmpty()) {
                    val queryPattern = "%$trimmedQuery%"
                    var results = db.clothingItemDao().searchByNameWithCategories(queryPattern)
                    if (results.isEmpty()) {
                        results = db.clothingItemDao().searchByDescriptionWithCategories(queryPattern)
                    }
                    adapter.updateData(results)
                } else {
                    adapter.updateData(emptyList())
                }
                return true
            }
        })
    }

    // Исправление putExtra
    private fun setupItemClick() {
        adapter.onItemClick = { itemWithCategory ->
            val intent = Intent(this, EditclothesActivity::class.java).apply {
                // Передаем базовый объект ClothingItem
                putExtra("clothing_item", itemWithCategory.clothingItem)
                putExtra("image_uri", itemWithCategory.clothingItem.imagePath)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(intent)
        }
    }
}