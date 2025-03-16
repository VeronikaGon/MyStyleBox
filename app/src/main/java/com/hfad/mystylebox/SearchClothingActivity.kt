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
import com.hfad.mystylebox.database.ClothingItemFull
import com.hfad.mystylebox.database.ClothingItemWithTags
import com.hfad.mystylebox.databinding.ActivitySearchClothingBinding

class SearchClothingActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySearchClothingBinding
    private lateinit var searchView: SearchView
    private lateinit var recyclerView: androidx.recyclerview.widget.RecyclerView
    private lateinit var adapter: ClothingAdapter
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchClothingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        searchView = binding.searchView
        searchView.isIconified = false

        // Настройка внешнего вида SearchView
        val searchText = searchView.findViewById<EditText>(androidx.appcompat.R.id.search_src_text)
        searchText.setTextColor(Color.BLACK)
        searchText.setHintTextColor(Color.GRAY)
        searchView.setBackgroundColor(Color.WHITE)

        recyclerView = binding.recyclerViewSearch
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Инициализация базы данных Room (для простоты работаем в основном потоке)
        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "wardrobe_db"
        )
            .allowMainThreadQueries()
            .build()

        // Загружаем все вещи с полной информацией (включая категорию, подкатегорию и теги)
        val itemsFull: List<ClothingItemFull> = db.clothingItemDao().getAllItemsFull()

        // Инициализируем адаптер с типом ClothingItemFull
        adapter = ClothingAdapter(itemsFull, R.layout.item_clothing1)
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
                        var results: List<ClothingItemFull> =
                            db.clothingItemDao().searchByNameWithFull(queryPattern)
                        if (results.isEmpty()) {
                            results = db.clothingItemDao().searchByDescriptionWithFull(queryPattern)
                        }
                        adapter.updateData(results)
                    }
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                val trimmedQuery = newText?.trim() ?: ""
                if (trimmedQuery.isNotEmpty()) {
                    val queryPattern = "%$trimmedQuery%"
                    var results: List<ClothingItemFull> =
                        db.clothingItemDao().searchByNameWithFull(queryPattern)
                    if (results.isEmpty()) {
                        results = db.clothingItemDao().searchByDescriptionWithFull(queryPattern)
                    }
                    adapter.updateData(results)
                } else {
                    adapter.updateData(emptyList())
                }
                return true
            }
        })
    }

    private fun setupItemClick() {
        adapter.onItemClick = { itemFull ->
            val intent = android.content.Intent(this, EditclothesActivity::class.java).apply {
                // Передаем базовый объект ClothingItem
                putExtra("clothing_item", itemFull.clothingItem)
                putExtra("image_uri", itemFull.clothingItem.imagePath)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(intent)
        }
    }
}