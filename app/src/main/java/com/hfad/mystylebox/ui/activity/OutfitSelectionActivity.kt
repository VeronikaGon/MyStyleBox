package com.hfad.mystylebox.ui.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.hfad.mystylebox.adapter.OutfitSelectionAdapter
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.hfad.mystylebox.R
import com.hfad.mystylebox.database.AppDatabase
import com.hfad.mystylebox.database.entity.Outfit
import com.hfad.mystylebox.database.entity.OutfitItemFull
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_outfit_selection)

        recyclerView = findViewById(R.id.recyclerView)
        btnConfirm = findViewById(R.id.selectoutfitButton)
        btnBack = findViewById(R.id.imageBack)
        tvSelectedCount = findViewById(R.id.SelectedCount)
        clayout = findViewById(R.id.clayout)
        clayout.visibility = View.GONE
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
            setResult(RESULT_OK, resultIntent)
            DataProvider.notifyWidgetDataChanged(this)
            finish()
        }

        btnBack.setOnClickListener {
            finish()
        }

        loadAllOutfits()
        updateSelectedCount()
    }

    /**
     * Обновляет количество выбранных элементов в tvSelectedCount.
     */
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
            // Получаем экземпляр базы данных
            val db = AppDatabase.getInstance(this@OutfitSelectionActivity)
            // Получаем список комплектов из таблицы (если в БД данные типа Outfit)
            val outfits: List<Outfit> = db.outfitDao().getAllOutfits()
            // Преобразуем Outfit в OutfitItemFull (если требуется). В простейшем случае можно сделать:
            fullOutfitItems = outfits.map { outfit ->
                OutfitItemFull(outfit)
            }
            withContext(Dispatchers.Main) {
                // Обновляем данные адаптера
                (recyclerView.adapter as? OutfitSelectionAdapter)?.updateData(fullOutfitItems)
                updateSelectedCount()
            }
        }
    }
}