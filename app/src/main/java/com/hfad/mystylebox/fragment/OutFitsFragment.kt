package com.hfad.mystylebox.fragment

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import android.widget.ImageButton
import android.widget.TextView
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
    private lateinit var emptyTextView: TextView

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
        emptyTextView = view.findViewById(R.id.emptyTextView)
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

        val imageSearch = view.findViewById<ImageButton>(R.id.imageSearch)
        imageSearch.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                val db = Room.databaseBuilder(
                    requireContext(),
                    AppDatabase::class.java,
                    "wardrobe_db"
                ).build()
                val alloutfits = db.outfitDao().getAllOutfits()
                withContext(Dispatchers.Main) {
                    if (alloutfits.size <= 4) {
                        Toast.makeText(
                            requireContext(),
                            "Добавьте ещё комплектов для поиска",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        startSearchClothingActivity()
                    }
                }
            }
        }

        imageFilter = view.findViewById(R.id.imageFilter)
        imageFilter.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                val db = Room.databaseBuilder(
                    requireContext(),
                    AppDatabase::class.java,
                    "wardrobe_db"
                ).build()
                val alloutfits = db.outfitDao().getAllOutfits()
                withContext(Dispatchers.Main) {
                    if (alloutfits.size <= 4) {
                        Toast.makeText(
                            requireContext(),
                            "Добавьте ещё комплектов для фильтрации",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        startFilterActivity()
                    }
                }
            }
        }
        loadOutfits()
    }

    override fun onResume() {
        super.onResume()
        if (!isFilterActive) {
            loadOutfits()
        } else {
            currentFilters?.let { loadFilteredOutfits(it) }
        }
    }

    private fun updateEmptyView() {
        val itemCount = (recyclerView.adapter as? OutfitAdapter)?.itemCount ?: 0
        if (itemCount == 0) {
            recyclerView.visibility = View.GONE
            emptyTextView.visibility = View.VISIBLE
        } else {
            emptyTextView.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
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
                putStringArrayListExtra("selectedTempLabels", it.getStringArrayList("tempLabels"))
                putExtra("selectedNotTemperature", it.getBoolean("notTemperature", false))
            }
        }
        filterActivityLauncher.launch(intent)
    }

    // Получаем результат из FilterOutfitActivity
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
                putStringArrayList("seasons", ArrayList(selectedSeasons))
                putStringArrayList("tags", ArrayList(selectedTags))
                putStringArrayList("tempLabels", ArrayList(selectedTempLabels))
                putBoolean("notTemperature", selectedNotTemperature)
            }

            val hasFilters = selectedSeasons.isNotEmpty()
                    || selectedTags.isNotEmpty()
                    || selectedTempLabels.isNotEmpty()
                    || selectedNotTemperature

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
        CoroutineScope(Dispatchers.IO).launch {
            val db = Room.databaseBuilder(
                requireContext(), AppDatabase::class.java, "wardrobe_db"
            ).build()
            val itemCount = db.clothingItemDao().getCount()

            withContext(Dispatchers.Main) {
                if (itemCount < 2) {
                    Toast.makeText(
                        requireContext(),
                        "Добавьте хотя бы 2 вещи, чтобы создать комплект",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    val intent = Intent(requireContext(), ClothingSelectionActivity::class.java)
                    startActivity(intent)
                }
            }
        }
    }

    // Загрузка всех комплектов (без фильтра)
    private fun loadOutfits() {
        CoroutineScope(Dispatchers.IO).launch {
            val db = Room.databaseBuilder(
                requireContext(), AppDatabase::class.java, "wardrobe_db"
            ).build()
            val outfits = db.outfitDao().getAllOutfits()
            withContext(Dispatchers.Main) {
                outfitAdapter.updateData(outfits)
                updateEmptyView()
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
            val tempLabels = filters.getStringArrayList("tempLabels") ?: arrayListOf()
            val notTemperature = filters.getBoolean("notTemperature", false)

            Log.d("OutFitsFragment", "Фильтры – сезоны: $seasons, теги: $tags, tempLabels: $tempLabels, без температуры: $notTemperature")

            val labelToRange = mapOf(
                "Heat"  to Pair(35, 100),
                "Hot"   to Pair(27, 34),
                "Warm"  to Pair(20, 26),
                "Cool"  to Pair(10, 19),
                "Cold"  to Pair(-5, 9),
                "Frost" to Pair(-50, -6)
            )

            val filteredOutfits = allOutfitsWithTags.filter { outfitWithTags ->
                val outfit = outfitWithTags.outfit
                val outfitSeasons = outfit.seasons?.map { it.trim().lowercase() } ?: emptyList()
                val seasonMatch = if (seasons.isEmpty()) {
                    true
                } else {
                    seasons.any { season ->
                        if (season == "Без сезона") outfitSeasons.isEmpty()
                        else outfitSeasons.contains(season.trim().lowercase())
                    }
                }

                val temperatureMatch = when {
                    notTemperature -> (outfit.minTemp == NOT_TEMPERATURE && outfit.maxTemp == NOT_TEMPERATURE)
                    tempLabels.isEmpty() -> true
                    else -> tempLabels.any { label ->
                        val range = labelToRange[label]
                        if (range == null) {
                            false
                        } else {
                            val (rMin, rMax) = range
                            outfit.maxTemp >= rMin && outfit.minTemp <= rMax
                        }
                    }
                }

                val outfitTagNames = outfitWithTags.tags.map { it.name.trim().lowercase() }
                val tagMatch = when {
                    tags.contains("Без тегов") -> outfitTagNames.isEmpty()
                    tags.isNotEmpty() -> tags
                        .filter { it != "Без тегов" }
                        .map { it.trim().lowercase() }
                        .all { tag -> outfitTagNames.contains(tag) }
                    else -> true
                }

                seasonMatch && temperatureMatch && tagMatch
            }

            withContext(Dispatchers.Main) {
                val resultList = filteredOutfits.map { it.outfit }
                outfitAdapter.updateData(resultList)
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