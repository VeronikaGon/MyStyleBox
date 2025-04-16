package com.hfad.mystylebox

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.hfad.mystylebox.adapter.OutfitSelectionAdapter
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.hfad.mystylebox.database.AppDatabase
import com.hfad.mystylebox.database.Outfit
import com.hfad.mystylebox.database.OutfitItemFull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OutfitSelectionActivity : AppCompatActivity() {

    // RecyclerView для отображения комплектов
    private lateinit var recyclerView: RecyclerView
    // Кнопка для подтверждения выбора
    private lateinit var btnConfirm: ImageButton
    // Кнопка возврата
    private lateinit var btnBack: ImageButton
    // TextView для отображения количества выбранных комплектов
    private lateinit var tvSelectedCount: TextView

    // Список всех комплектов из базы данных
    private var fullOutfitItems: List<OutfitItemFull> = emptyList()
    // Хранит выбранные пользователем элементы
    private val selectedOutfits = mutableSetOf<OutfitItemFull>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_outfit_selection)

        // Ищем View по id – убедись, что в activity_outfit_selection.xml заданы соответствующие id
        recyclerView = findViewById(R.id.recyclerView)
        btnConfirm = findViewById(R.id.selectoutfitButton)
        btnBack = findViewById(R.id.imageBack)
        tvSelectedCount = findViewById(R.id.SelectedCount)

        // Настраиваем RecyclerView с двумя колонками
        recyclerView.layoutManager = GridLayoutManager(this, 2)
        recyclerView.adapter = OutfitSelectionAdapter(
            items = fullOutfitItems,
            layoutResId = R.layout.item_clothing_selection,  // Если у тебя отдельный layout для комплекта, используй его
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
            globalSelected = selectedOutfits
        )

        // Обработчик кнопки подтверждения выбора
        btnConfirm.setOnClickListener {
            // Подготавливаем результат: список id выбранных комплектов
            val selectedIds = selectedOutfits.map { it.outfit.id }
            val resultIntent = Intent().apply {
                putIntegerArrayListExtra("selected_outfit_ids", ArrayList(selectedIds))
            }
            setResult(RESULT_OK, resultIntent)
            finish()
        }

        // Кнопка возврата просто закрывает активность
        btnBack.setOnClickListener {
            finish()
        }

        // Загружаем данные из базы данных
        loadAllOutfits()
    }

    /**
     * Обновляет количество выбранных элементов в tvSelectedCount.
     */
    private fun updateSelectedCount() {
        tvSelectedCount.text = "${selectedOutfits.size}"
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