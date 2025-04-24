package com.hfad.mystylebox.fragment

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.GridLayoutManager
import androidx.room.Room
import com.hfad.mystylebox.ui.activity.ClothingSelectionActivity
import com.hfad.mystylebox.ui.activity.EditoutfitActivity
import com.hfad.mystylebox.ui.activity.FilterOutfitActivity
import com.hfad.mystylebox.ui.bottomsheet.OutfitActionsBottomSheet
import com.hfad.mystylebox.R
import com.hfad.mystylebox.ui.activity.SearchOutfitActivity
import com.hfad.mystylebox.adapter.OutfitAdapter
import com.hfad.mystylebox.database.AppDatabase
import com.hfad.mystylebox.database.entity.Outfit
import com.hfad.mystylebox.database.entity.OutfitWithTags
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OutFitsFragment : Fragment() {

    private lateinit var recyclerView: androidx.recyclerview.widget.RecyclerView
    private lateinit var outfitAdapter: OutfitAdapter
    private lateinit var imageFilter: ImageButton
    private var isFilterActive: Boolean = false
    private var currentFilters: Bundle? = null
    private val NOT_TEMPERATURE = -99

    override fun onCreateView(
        inflater: android.view.LayoutInflater, container: android.view.ViewGroup?,
        savedInstanceState: Bundle?
    ): android.view.View? {
        return inflater.inflate(R.layout.fragment_out_fits, container, false)
    }

    override fun onViewCreated(view: android.view.View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val addOutfitButton = view.findViewById<ImageButton>(R.id.addoutfit)
        addOutfitButton.setOnClickListener { startOutfitActivity() }

        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = GridLayoutManager(context, 2)
        outfitAdapter = OutfitAdapter(emptyList(), R.layout.item_clothing)
        recyclerView.adapter = outfitAdapter

        outfitAdapter.onItemClick = { outfit ->
            val intent = Intent(requireContext(), EditoutfitActivity::class.java).apply {
                putExtra("outfit", outfit)
                putExtra("image_uri", outfit.imagePath)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(intent)
        }

        outfitAdapter.onItemLongClick = { outfit ->
            val bottomSheet = OutfitActionsBottomSheet.newInstance(outfit.name, outfit.imagePath)
            bottomSheet.show(childFragmentManager, bottomSheet.tag)
            bottomSheet.onEditClicked = {
                val intent = Intent(requireContext(), EditoutfitActivity::class.java).apply {
                    putExtra("outfit", outfit)
                    putExtra("image_uri", outfit.imagePath)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(intent)
            }
            bottomSheet.onDeleteClicked = {
                deleteOutfit(outfit)
            }
        }

        // Загружаем все комплекты по умолчанию
        loadOutfits()

        val imageSearch = view.findViewById<ImageButton>(R.id.imageSearch)
        imageSearch.setOnClickListener { startSearchClothingActivity() }
        imageFilter = view.findViewById(R.id.imageFilter)
        imageFilter.setOnClickListener { startFilterActivity() }
    }

    private fun startSearchClothingActivity() {
        val intent = Intent(requireContext(), SearchOutfitActivity::class.java)
        startActivity(intent)
    }

    // Запускаем FilterOutfitActivity и передаем текущие фильтры (если имеются)
    private fun startFilterActivity() {
        val intent = Intent(requireContext(), FilterOutfitActivity::class.java).apply {
            currentFilters?.let {
                putStringArrayListExtra("selectedSeasons", it.getStringArrayList("seasons"))
                putStringArrayListExtra("selectedTags", it.getStringArrayList("tags"))
                if (it.containsKey("selectedTemperature")) {
                    putExtra("selectedTemperature", it.getInt("selectedTemperature"))
                }
                if (it.containsKey("selectedNotTemperature")) {
                    putExtra("selectedNotTemperature", it.getBoolean("selectedNotTemperature"))
                }   }
        }
        filterActivityLauncher.launch(intent)
    }

    // Получаем результат из FilterOutfitActivity
    private val filterActivityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val selectedSeasons = result.data?.getStringArrayListExtra("selectedSeasons") ?: arrayListOf()
            val selectedTags = result.data?.getStringArrayListExtra("selectedTags") ?: arrayListOf()
            val selectedTemperature = result.data?.getIntExtra("selectedTemperature", Int.MIN_VALUE) ?: Int.MIN_VALUE
            val selectedNotTemperature = result.data?.getBooleanExtra("selectedNotTemperature", false) ?: false

            currentFilters = Bundle().apply {
                putStringArrayList("seasons", selectedSeasons)
                putStringArrayList("tags", selectedTags)
                if (selectedTemperature != Int.MIN_VALUE) putInt("selectedTemperature", selectedTemperature)
                   putBoolean("selectedNotTemperature", selectedNotTemperature)
            }

            val hasFilters = listOf(selectedSeasons, selectedTags).any { it.isNotEmpty() } ||
                    (selectedTemperature != Int.MIN_VALUE) || selectedNotTemperature

            imageFilter.setColorFilter(
                if (hasFilters) Color.parseColor("#FFB5A7")
                else Color.parseColor("#000000")
            )
            isFilterActive = hasFilters

            if (isFilterActive && currentFilters != null) {
                loadFilteredOutfits(currentFilters!!)
            } else {
                loadOutfits()
            }
        }
    }

    private fun startOutfitActivity() {
        val intent = Intent(requireContext(), ClothingSelectionActivity::class.java)
        startActivity(intent)
    }

    // Загрузка всех комплектов (без фильтра)
    private fun loadOutfits() {
        CoroutineScope(Dispatchers.IO).launch {
            val db = Room.databaseBuilder(
                requireContext(), AppDatabase::class.java, "wardrobe_db"
            ).build()
            // Если возможно, используйте метод, возвращающий OutfitWithTags:
            val outfits = db.outfitDao().getAllOutfits()  // или getAllOutfitsWithTags(), если хотите работать с тегами
            withContext(Dispatchers.Main) {
                outfitAdapter.updateData(outfits)
            }
        }
    }

    // Загрузка комплектов с фильтрацией по сезонам, температуре и тегам.
    private fun loadFilteredOutfits(filters: Bundle) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = Room.databaseBuilder(
                requireContext(), AppDatabase::class.java, "wardrobe_db"
            ).build()
            val allOutfitsWithTags: List<OutfitWithTags> = db.outfitDao().getAllOutfitsWithTags()

            val seasons = filters.getStringArrayList("seasons") ?: arrayListOf()
            val tags = filters.getStringArrayList("tags") ?: arrayListOf()
            val temperature = filters.getInt("selectedTemperature", Int.MIN_VALUE)
            val notTemperature = filters.getBoolean("selectedNotTemperature", false)
            Log.d("OutFitsFragment", "Фильтры - сезоны: $seasons, теги: $tags, температура: $temperature, без температуры: $notTemperature")

            val filteredOutfits = allOutfitsWithTags.filter { outfitWithTags ->
                val outfit = outfitWithTags.outfit
                val outfitSeasons = outfit.seasons?.map { it.trim().lowercase() } ?: emptyList()
                val seasonMatch = if (seasons.isEmpty()) true
                else seasons.any { season ->
                    if (season == "Без сезона") outfitSeasons.isEmpty()
                    else outfitSeasons.contains(season.trim().lowercase())
                }

                Log.d("OutFitsFragment", "Outfit ${outfit.id} - minTemp: ${outfit.minTemp}, maxTemp: ${outfit.maxTemp}")

                val temperatureMatch = when {
                    notTemperature -> outfit.minTemp == NOT_TEMPERATURE && outfit.maxTemp == NOT_TEMPERATURE
                    temperature != Int.MIN_VALUE -> outfit.minTemp <= temperature && outfit.maxTemp >= temperature
                    else -> true
                }


                val outfitTagNames = outfitWithTags.tags.map { it.name.trim().lowercase() }
                val tagMatch = when {
                    tags.contains("Без тегов") -> outfitTagNames.isEmpty()
                    tags.isNotEmpty() -> tags.filter { it != "Без тегов" }
                        .map { it.trim().lowercase() }
                        .all { tag -> outfitTagNames.contains(tag) }
                    else -> true
                }

                seasonMatch && temperatureMatch && tagMatch
            }
            withContext(Dispatchers.Main) {
                // Здесь обновляем адаптер, например, отображая только сами объекты outfit
                outfitAdapter.updateData(filteredOutfits.map { it.outfit })
            }
        }
    }

    // Метод для удаления комплекта с подтверждением
    private fun deleteOutfit(outfit: Outfit) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = Room.databaseBuilder(
                requireContext(), AppDatabase::class.java, "wardrobe_db"
            ).build()
            db.outfitClothingItemDao().deleteForOutfit(outfit.id)
            db.outfitTagDao().deleteTagsForOutfit(outfit.id)
            db.outfitDao().delete(outfit)
            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), "Комплект удалён", Toast.LENGTH_SHORT).show()
                if (isFilterActive && currentFilters != null) {
                    loadFilteredOutfits(currentFilters!!)
                } else {
                    loadOutfits()
                }
            }
        }
    }
}