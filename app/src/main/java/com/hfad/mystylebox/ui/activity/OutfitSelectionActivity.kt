package com.hfad.mystylebox.ui.activity

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.hfad.mystylebox.adapter.OutfitSelectionAdapter
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.RecyclerView
import com.hfad.mystylebox.R
import com.hfad.mystylebox.database.AppDatabase
import com.hfad.mystylebox.database.entity.Outfit
import com.hfad.mystylebox.database.entity.OutfitItemFull
import com.hfad.mystylebox.database.entity.OutfitWithTags
import com.hfad.mystylebox.ui.widget.DataProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OutfitSelectionActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var btnConfirm: ImageButton
    private lateinit var btnBack: ImageButton
    private lateinit var tvSelectedCount: TextView
    private lateinit var clayout: ViewGroup

    private var fullOutfitItems: List<OutfitItemFull> = emptyList()
    private val selectedOutfits = mutableSetOf<OutfitItemFull>()


    private lateinit var btnFilter: ImageButton
    private var currentOutfitItems: List<OutfitItemFull> = emptyList()
    private var isFilterActive: Boolean = false
    private var currentFilters: Bundle? = null

    private val filterActivityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data ?: return@registerForActivityResult
            val selectedSeasons = data.getStringArrayListExtra("selectedSeasons") ?: arrayListOf()
            val selectedTags = data.getStringArrayListExtra("selectedTags") ?: arrayListOf()
            val selectedTempLabels = data.getStringArrayListExtra("selectedTempLabels") ?: arrayListOf()
            val selectedNotTemperature = data.getBooleanExtra("selectedNotTemperature", false)

            currentFilters = Bundle().apply {
                putStringArrayList("seasons", selectedSeasons)
                putStringArrayList("tags", selectedTags)
                putStringArrayList("tempLabels", selectedTempLabels)
                putBoolean("notTemperature", selectedNotTemperature)
            }

            val filteredIds = data.getStringArrayListExtra("filtered_outfit_ids") ?: arrayListOf()

            if (filteredIds.isEmpty()) {
                updateAdapterWithList(fullOutfitItems)
                isFilterActive = false
            } else {
                val filteredList = fullOutfitItems.filter { itemFull ->
                    filteredIds.contains(itemFull.outfit.id.toString())
                }
                currentOutfitItems = filteredList
                isFilterActive = true
                updateAdapterWithList(currentOutfitItems)
            }

            btnFilter.setColorFilter(
                if (isFilterActive) Color.parseColor("#FFB5A7")
                else Color.parseColor("#000000")
            )
        }
    }

    private fun updateAdapterWithList(list: List<OutfitItemFull>) {
        (recyclerView.adapter as? OutfitSelectionAdapter)?.updateData(list)
        updateSelectedCount()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_outfit_selection)

        recyclerView = findViewById(R.id.recyclerView)
        btnConfirm = findViewById(R.id.selectoutfitButton)
        btnBack = findViewById(R.id.imageBack)
        tvSelectedCount = findViewById(R.id.SelectedCount)
        clayout = findViewById(R.id.clayout)
        clayout.visibility = View.GONE
        btnFilter = findViewById(R.id.imageFilter)
        btnFilter.setOnClickListener {
            startFilterActivity()
        }
        val preselected = intent
            .getStringArrayListExtra("EXTRA_SELECTED_IDS")
            ?.mapNotNull { it.toLongOrNull() }
            ?.toSet()
            ?: emptySet()

        recyclerView.adapter = OutfitSelectionAdapter(
            items           = fullOutfitItems,
            layoutResId     = R.layout.item_clothing_selection,
            selectionListener = object : OutfitSelectionAdapter.OnItemSelectionListener {
                override fun onItemSelectionChanged(item: OutfitItemFull, isSelected: Boolean) {
                    if (isSelected) {
                        selectedOutfits.add(item)
                    } else {
                        selectedOutfits.remove(item)
                    }
                    updateSelectedCount()
                }
            },
            globalSelected  = selectedOutfits,
            preselectedIds = preselected
        )

        recyclerView.layoutManager = GridLayoutManager(this, 2)

        btnConfirm.setOnClickListener {
            val selectedIds = selectedOutfits.map { it.outfit.id.toString() }
            val resultIntent = Intent().apply {
                putStringArrayListExtra("EXTRA_SELECTED_IDS", ArrayList(selectedIds))
            }
            setResult(Activity.RESULT_OK, resultIntent)
            DataProvider.notifyWidgetDataChanged(this)
            finish()
        }

        btnBack.setOnClickListener {
            finish()
        }

        loadAllOutfits()
        updateSelectedCount()
    }

    override fun onResume() {
        super.onResume()
        if (!isFilterActive && currentFilters == null) {
            loadAllOutfits()
        }
    }

    //Обновляет количество выбранных элементов в tvSelectedCount.
    private fun updateSelectedCount() {
        tvSelectedCount.text = "${selectedOutfits.size}"
        clayout.visibility = if (selectedOutfits.isEmpty()) View.GONE else View.VISIBLE
    }

    /**
     * Загружает список всех комплектов из базы данных.
     * В данном примере используется метод getAllOutfits() из OutfitDao.
     * При необходимости можно изменить на getAllOutfitsWithTags() и затем преобразовать данные.
     */
    private fun loadAllOutfits() {
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getInstance(this@OutfitSelectionActivity)
            val outfitsWithTags: List<OutfitWithTags> = db.outfitDao().getAllOutfitsWithTags()
            fullOutfitItems = outfitsWithTags.map { outfitWithTags ->
                OutfitItemFull(outfitWithTags.outfit, outfitWithTags.tags)
            }
            currentOutfitItems = fullOutfitItems

            withContext(Dispatchers.Main) {
                (recyclerView.adapter as? OutfitSelectionAdapter)?.updateData(currentOutfitItems)
                isFilterActive = false
                currentFilters = null
                btnFilter.setColorFilter(Color.parseColor("#000000"))
                updateSelectedCount()
            }
        }
    }
    private fun startFilterActivity() {
        val intent = Intent(this, FilterOutfitActivity::class.java).apply {
            currentFilters?.let {
                putStringArrayListExtra("selectedSeasons", it.getStringArrayList("seasons"))
                putStringArrayListExtra("selectedTags", it.getStringArrayList("tags"))
                putStringArrayListExtra("selectedTempLabels", it.getStringArrayList("tempLabels"))
                putExtra("selectedNotTemperature", it.getBoolean("notTemperature", false))
            }
        }
        filterActivityLauncher.launch(intent)
    }

    /**
     * Применяет фильтрацию к fullOutfitItems по спискам сезонов, тегов и параметрам по температуре.
     */
    private fun updateFilteredItems(
        selectedSeasons: List<String>,
        selectedTags: List<String>,
        selectedTemperature: Int,
        selectedNotTemperature: Boolean
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val noSeason = selectedSeasons.isEmpty()
            val noTags = selectedTags.isEmpty()
            val noTemp = (selectedTemperature == Int.MIN_VALUE && !selectedNotTemperature)

            if (noSeason && noTags && noTemp) {
                withContext(Dispatchers.Main) {
                    currentOutfitItems = fullOutfitItems
                    isFilterActive = false
                    (recyclerView.adapter as? OutfitSelectionAdapter)?.updateData(currentOutfitItems)
                    updateSelectedCount()
                }
                return@launch
            }

            val filtered = fullOutfitItems.filter { itemFull ->
                val outfit = itemFull.outfit
                val outfitSeasons = outfit.seasons?.map { it.trim().lowercase() } ?: emptyList()
                val seasonMatch = if (selectedSeasons.isEmpty()) {
                    true
                } else {
                    selectedSeasons.any { season ->
                        if (season.equals("Без сезона", ignoreCase = true)) {
                            outfitSeasons.isEmpty()
                        } else {
                            outfitSeasons.contains(season.trim().lowercase())
                        }
                    }
                }

                val temperatureMatch = when {
                    selectedNotTemperature -> (outfit.minTemp == -99 && outfit.maxTemp == -99)
                    selectedTemperature == Int.MIN_VALUE -> true
                    else -> {
                        val minO = outfit.minTemp
                        val maxO = outfit.maxTemp
                        (selectedTemperature in minO..maxO)
                    }
                }

                val outfitTagNames = itemFull.tags.map { it.name.trim().lowercase() }
                val tagMatch = when {
                    selectedTags.contains("Без тегов") -> outfitTagNames.isEmpty()
                    selectedTags.isNotEmpty() -> {
                        selectedTags
                            .filter { it != "Без тегов" }
                            .map { it.trim().lowercase() }
                            .all { tag -> outfitTagNames.contains(tag) }
                    }
                    else -> true
                }

                seasonMatch && temperatureMatch && tagMatch
            }

            withContext(Dispatchers.Main) {
                currentOutfitItems = filtered
                isFilterActive = true
                (recyclerView.adapter as? OutfitSelectionAdapter)?.updateData(currentOutfitItems)
                updateSelectedCount()
            }
        }
    }
}