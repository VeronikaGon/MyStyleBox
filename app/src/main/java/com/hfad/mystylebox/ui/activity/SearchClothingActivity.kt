package com.hfad.mystylebox.ui.activity

import android.os.Bundle
import androidx.appcompat.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import android.graphics.Color
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import com.hfad.mystylebox.R
import com.hfad.mystylebox.adapter.ClothingAdapter
import com.hfad.mystylebox.database.AppDatabase
import com.hfad.mystylebox.database.entity.ClothingItemFull
import com.hfad.mystylebox.databinding.ActivitySearchClothingBinding

class SearchClothingActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySearchClothingBinding
    private lateinit var searchView: SearchView
    private lateinit var recyclerView: androidx.recyclerview.widget.RecyclerView
    private lateinit var adapter: ClothingAdapter
    private lateinit var db: AppDatabase
    private lateinit var allItems: List<ClothingItemFull>
    private lateinit var tvEmpty: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchClothingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        searchView = binding.searchView
        searchView.isIconified = false
        tvEmpty    = binding.root.findViewById(R.id.tvEmpty)

        val searchText = searchView.findViewById<EditText>(androidx.appcompat.R.id.search_src_text)
        searchText.setTextColor(Color.BLACK)
        searchText.setHintTextColor(Color.GRAY)
        searchView.setBackgroundColor(Color.WHITE)

        recyclerView = binding.recyclerViewSearch
        recyclerView.layoutManager = LinearLayoutManager(this)

        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "wardrobe_db"
        )
            .allowMainThreadQueries()
            .build()

        allItems = db.clothingItemDao().getAllItemsFull()

        adapter = ClothingAdapter(allItems , R.layout.item_list)
        recyclerView.adapter = adapter

        setupSearchView()
        setupItemClick()
    }

    private fun setupSearchView() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                performSearch(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                performSearch(newText)
                return true
            }
        })
    }

    private fun performSearch(text: String?) {
        val trimmed = text?.trim().orEmpty()

        if (trimmed.isEmpty()) {
            adapter.updateData(allItems)
            tvEmpty.visibility = View.GONE
        } else {
            val pattern = "%$trimmed%"
            var results = db.clothingItemDao().searchByNameWithFull(pattern)
            if (results.isEmpty()) {
                results = db.clothingItemDao().searchByDescriptionWithFull(pattern)
            }
            adapter.updateData(results)

            tvEmpty.visibility = if (results.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun setupItemClick() {
        adapter.onItemClick = { itemFull ->
            val intent = android.content.Intent(this, EditclothesActivity::class.java).apply {
                putExtra("clothing_item", itemFull.clothingItem)
                putExtra("image_uri", itemFull.clothingItem.imagePath)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(intent)
        }
    }
}