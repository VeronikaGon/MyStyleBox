package com.hfad.mystylebox.ui.activity

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.widget.SearchView
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import com.hfad.mystylebox.R
import com.hfad.mystylebox.adapter.OutfitAdapter
import com.hfad.mystylebox.database.AppDatabase
import com.hfad.mystylebox.database.Outfit
import com.hfad.mystylebox.databinding.ActivitySearchOutfitBinding

class SearchOutfitActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySearchOutfitBinding
    private lateinit var searchView: SearchView
    private lateinit var recyclerView: androidx.recyclerview.widget.RecyclerView
    private lateinit var adapter: OutfitAdapter
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchOutfitBinding.inflate(layoutInflater)
        setContentView(binding.root)

        searchView = binding.searchView
        searchView.isIconified = false

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

        val allOutfits: List<Outfit> = db.outfitDao().getAllOutfits()

        adapter = OutfitAdapter(allOutfits, R.layout.item_clothing1)
        recyclerView.adapter = adapter

        setupSearchView()
        setupItemClick()
    }

    private fun setupSearchView() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let {
                    performSearch(it)
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrBlank()) {
                    adapter.updateData(emptyList())
                } else {
                    performSearch(newText)
                }
                return true
            }
        })
    }

    private fun performSearch(queryStr: String) {
        val trimmedQuery = queryStr.trim()
        if (trimmedQuery.isNotEmpty()) {
            val queryPattern = "%$trimmedQuery%"
            var results: List<Outfit> =
                db.outfitDao().searchByName(queryPattern)
            if (results.isEmpty()) {
                results = db.outfitDao().searchByDescription(queryPattern)
            }
            adapter.updateData(results)
        } else {
            adapter.updateData(emptyList())
        }
    }

    private fun setupItemClick() {
        adapter.onItemClick = { outfit ->
            val intent = android.content.Intent(this, EditoutfitActivity::class.java).apply {
                putExtra("outfit", outfit)
                putExtra("image_uri", outfit.imagePath)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(intent)
        }
    }
}