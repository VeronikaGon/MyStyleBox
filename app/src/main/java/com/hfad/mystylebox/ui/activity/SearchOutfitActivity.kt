package com.hfad.mystylebox.ui.activity

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.SearchView
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import com.hfad.mystylebox.R
import com.hfad.mystylebox.adapter.OutfitAdapter
import com.hfad.mystylebox.database.AppDatabase
import com.hfad.mystylebox.database.entity.Outfit
import com.hfad.mystylebox.databinding.ActivitySearchOutfitBinding

class SearchOutfitActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySearchOutfitBinding
    private lateinit var searchView: SearchView
    private lateinit var recyclerView: androidx.recyclerview.widget.RecyclerView
    private lateinit var adapter: OutfitAdapter
    private lateinit var db: AppDatabase
    private lateinit var allOutfits: List<Outfit>
    private lateinit var tvEmpty: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchOutfitBinding.inflate(layoutInflater)
        setContentView(binding.root)

        searchView = binding.searchView
        searchView.isIconified = false
        tvEmpty      = binding.tvEmpty

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

        allOutfits = db.outfitDao().getAllOutfits()
        adapter = OutfitAdapter(allOutfits, R.layout.item_list)
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

    private fun performSearch(queryStr: String?) {
        val trimmed = queryStr?.trim().orEmpty()

        if (trimmed.isEmpty()) {
            adapter.updateData(allOutfits)
            tvEmpty.visibility = View.GONE
        } else {
            val pattern = "%$trimmed%"
            var results = db.outfitDao().searchByName(pattern)
            if (results.isEmpty()) {
                results = db.outfitDao().searchByDescription(pattern)
            }
            adapter.updateData(results)
            tvEmpty.visibility = if (results.isEmpty()) View.VISIBLE else View.GONE
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