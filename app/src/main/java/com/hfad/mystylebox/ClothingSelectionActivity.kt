package com.hfad.mystylebox

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import com.hfad.mystylebox.adapter.ClothingSelectionAdapter
import com.hfad.mystylebox.database.AppDatabase
import com.hfad.mystylebox.database.ClothingItemFull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ClothingSelectionActivity : AppCompatActivity() {

    private var fullItems: List<ClothingItemFull> = emptyList()
    private var currentItems: List<ClothingItemFull> = emptyList()
    private var isFilterActive: Boolean = false
    private var currentFilters: Bundle? = null

    private lateinit var recyclerView: RecyclerView
    private lateinit var imageFilter: ImageButton
    private lateinit var selectedCountText: TextView
    private lateinit var clayout: View
    private lateinit var tabLayout: TabLayout
    private lateinit var imageBack: ImageButton
    private lateinit var selectclothingButton: ImageButton

    private val selectedItems = mutableSetOf<ClothingItemFull>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_clothing_selection)

        tabLayout = findViewById(R.id.tabLayout)
        recyclerView = findViewById(R.id.recyclerView)
        imageFilter = findViewById(R.id.imageFilter)
        selectedCountText = findViewById(R.id.SelectedCount)
        clayout = findViewById(R.id.clayout)
        imageBack = findViewById(R.id.imageBack)
        selectclothingButton = findViewById(R.id.selectclothingButton)

        val lockedPaths = intent.getStringArrayListExtra("locked_items") ?: arrayListOf()

        clayout.visibility = View.GONE

        imageBack.setOnClickListener { finish() }

        val fromBoard = intent.getBooleanExtra("fromBoard", false)
        selectclothingButton.setOnClickListener {
            val selectedIds = ArrayList(selectedItems.map { it.clothingItem.id })
            val selectedImagePaths = ArrayList(selectedItems.map { it.clothingItem.imagePath })
            if (fromBoard) {
                val resultIntent = Intent().apply {
                    putIntegerArrayListExtra("selected_item_ids", selectedIds)
                    putStringArrayListExtra("selected_image_paths", selectedImagePaths)
                }
                setResult(RESULT_OK, resultIntent)
                finish()
            } else {
                val intent = Intent(this, BoardActivity::class.java).apply {
                    putIntegerArrayListExtra("selected_item_ids", selectedIds)
                    putStringArrayListExtra("selected_image_paths", selectedImagePaths)
                }
                startActivity(intent)
            }
        }
        imageFilter.setOnClickListener { startFilterActivity() }

        recyclerView.layoutManager = GridLayoutManager(this, 2)
        recyclerView.adapter = ClothingSelectionAdapter(
            emptyList(),
            R.layout.item_clothing_selection,
            object : ClothingSelectionAdapter.OnItemSelectionListener {
                override fun onItemSelectionChanged(item: ClothingItemFull, isSelected: Boolean) {
                    if (lockedPaths.contains(item.clothingItem.imagePath)) return
                    if (isSelected) {
                        selectedItems.add(item)
                    } else {
                        selectedItems.remove(item)
                    }
                    updateSelectionCount()
                }
            },
            selectedItems,
            lockedPaths.toSet()
        )
        loadAllClothingItems()
    }

    // Обновление счетчика выбранных элементов и цвета/видимости контейнера clayout
    private fun updateSelectionCount() {
        selectedCountText.text = selectedItems.size.toString()
        if (selectedItems.isEmpty()) {
            clayout.visibility = View.GONE
        } else {
            clayout.visibility = View.VISIBLE
        }
    }

    // Загрузка полного списка вещей из БД
    private fun loadAllClothingItems() {
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getInstance(this@ClothingSelectionActivity)
            fullItems = db.clothingItemDao().getAllItemsFull()
            currentItems = fullItems
            withContext(Dispatchers.Main) {
                (recyclerView.adapter as? ClothingSelectionAdapter)?.updateData(currentItems)
                buildTabs()
                updateSelectionCount()
                isFilterActive = false
            }
        }
    }

    private fun buildTabs() {
        tabLayout.removeAllTabs()
        tabLayout.addTab(tabLayout.newTab().setText("Все"))
        val categories = fullItems.map { it.categoryName }.distinct().sorted()
        for (cat in categories) {
            tabLayout.addTab(tabLayout.newTab().setText(cat))
        }
        for (i in 0 until tabLayout.tabCount) {
            val tab = (tabLayout.getChildAt(0) as ViewGroup).getChildAt(i)
            val layoutParams = tab.layoutParams as ViewGroup.MarginLayoutParams
            layoutParams.setMargins(4, 0, 4, 0)
            tab.requestLayout()
        }
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                val selectedCategory = tab.text.toString()
                val listToFilter = if (isFilterActive) currentItems else fullItems
                val filtered = if (selectedCategory.equals("Все", ignoreCase = true))
                    listToFilter
                else
                    listToFilter.filter { it.categoryName.equals(selectedCategory, ignoreCase = true) }
                (recyclerView.adapter as? ClothingSelectionAdapter)?.updateData(filtered)
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    // Фильтрация по критериям (размер, статус, сезоны, теги)
    private fun updateFilteredItems(
        selectedSeasons: List<String>,
        selectedSizes: List<String>,
        selectedStatuses: List<String>,
        selectedTags: List<String>
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            if (selectedSeasons.isEmpty() && selectedSizes.isEmpty() &&
                selectedStatuses.isEmpty() && selectedTags.isEmpty()
            ) {
                withContext(Dispatchers.Main) {
                    (recyclerView.adapter as? ClothingSelectionAdapter)?.updateData(fullItems)
                    isFilterActive = false
                }
                return@launch
            }
            val filteredItems = fullItems.filter { itemFull ->
                val item = itemFull.clothingItem
                val sizeMatch = selectedSizes.isEmpty() ||
                        selectedSizes.any { size -> size.equals(item.size, ignoreCase = true) }
                val statusMatch = selectedStatuses.isEmpty() ||
                        selectedStatuses.any { stat -> stat.equals(item.status, ignoreCase = true) }
                val seasonList = item.seasons?.map { it.trim().lowercase() } ?: emptyList()
                val seasonMatch = selectedSeasons.isEmpty() ||
                        selectedSeasons.any { sel -> seasonList.contains(sel.trim().lowercase()) }
                val itemTagNames = itemFull.tags.map { it.name.trim().lowercase() }
                val tagMatch = selectedTags.isEmpty() ||
                        selectedTags.any { sel -> itemTagNames.contains(sel.trim().lowercase()) }
                sizeMatch && statusMatch && seasonMatch && tagMatch
            }
            withContext(Dispatchers.Main) {
                currentItems = filteredItems
                isFilterActive = true
                (recyclerView.adapter as? ClothingSelectionAdapter)?.updateData(filteredItems)
                updateSelectionCount()
            }
        }
    }

    // Убираем вызов loadAllClothingItems() из onResume, чтобы не сбрасывать фильтрацию
    override fun onResume() {
        super.onResume()
        if (!isFilterActive && currentFilters == null) {
            loadAllClothingItems()
        }
    }

    // Запуск FilterActivity для получения фильтров
    private val filterActivityLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data ?: return@registerForActivityResult
            val selectedSeasons = data.getStringArrayListExtra("selectedSeasons") ?: arrayListOf()
            val selectedSizes = data.getStringArrayListExtra("selectedSizes") ?: arrayListOf()
            val selectedStatuses = data.getStringArrayListExtra("selectedStatuses") ?: arrayListOf()
            val selectedTags = data.getStringArrayListExtra("selectedTags") ?: arrayListOf()

            currentFilters = Bundle().apply {
                putStringArrayList("seasons", selectedSeasons)
                putStringArrayList("sizes", selectedSizes)
                putStringArrayList("statuses", selectedStatuses)
                putStringArrayList("tags", selectedTags)
            }

            val hasFilters = listOf(selectedSeasons, selectedSizes, selectedStatuses, selectedTags)
                .any { it.isNotEmpty() }
            imageFilter.setColorFilter(
                if (hasFilters) Color.parseColor("#FFB5A7")
                else Color.parseColor("#000000")
            )
            updateFilteredItems(selectedSeasons, selectedSizes, selectedStatuses, selectedTags)
        }
    }

    private fun startFilterActivity() {
        val intent = Intent(this, FilterActivity::class.java).apply {
            currentFilters?.let {
                putStringArrayListExtra("selectedSeasons", it.getStringArrayList("seasons"))
                putStringArrayListExtra("selectedSizes", it.getStringArrayList("sizes"))
                putStringArrayListExtra("selectedStatuses", it.getStringArrayList("statuses"))
                putStringArrayListExtra("selectedTags", it.getStringArrayList("tags"))
            }
        }
        filterActivityLauncher.launch(intent)
    }
}