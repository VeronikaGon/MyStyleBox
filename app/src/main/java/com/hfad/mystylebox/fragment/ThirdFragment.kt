package com.hfad.mystylebox.fragment
import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import androidx.appcompat.widget.SearchView
import android.widget.Switch
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.google.android.material.tabs.TabLayout
import com.hfad.mystylebox.R
import com.hfad.mystylebox.adapter.WishListAdapter
import com.hfad.mystylebox.database.AppDatabase
import com.hfad.mystylebox.database.entity.Subcategory
import com.hfad.mystylebox.database.entity.WishListItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ThirdFragment : Fragment() {

    private lateinit var searchView: SearchView
    private lateinit var switchView: Switch
    private lateinit var tabLayout: TabLayout
    private lateinit var rv: RecyclerView
    private lateinit var adapter: WishListAdapter

    private var allItems = listOf<WishListItem>()
    private var filtered = listOf<WishListItem>()
    private var subcategories = listOf<Subcategory>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_third, container, false)

    override fun onViewCreated(view: View, saved: Bundle?) {
        super.onViewCreated(view, saved)

        searchView   = view.findViewById(R.id.searchView)
        switchView   = view.findViewById(R.id.switchView)
        tabLayout    = view.findViewById(R.id.tabLayout)
        rv           = view.findViewById(R.id.recyclerView)

        rv.layoutManager = GridLayoutManager(context, 2)
        adapter = WishListAdapter(emptyList(), R.layout.item_clothing1) { item ->
        }
        rv.adapter = adapter

        loadFromDbAndSetupTabs()

        searchView.setOnQueryTextListener(object: SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(q: String?) = false
            override fun onQueryTextChange(q: String?): Boolean {
                applyAllFilters(q ?: "")
                return true
            }
        })

        switchView.setOnCheckedChangeListener { _, isList ->
            if (isList) {
                rv.layoutManager = LinearLayoutManager(context)
                adapter.layoutRes = R.layout.item_clothing
            } else {
                rv.layoutManager = GridLayoutManager(context, 2)
                adapter.layoutRes = R.layout.item_clothing1
            }
            adapter.notifyDataSetChanged()
        }

        view.findViewById<ImageButton>(R.id.selectPhotoButton)
            .setOnClickListener { showAddByUrlDialog() }
    }

    private fun loadFromDbAndSetupTabs() {
        CoroutineScope(Dispatchers.IO).launch {
            // Загружаем БД (без именованных аргументов!)
            val db = Room.databaseBuilder(
                requireContext(),
                AppDatabase::class.java,
                "wardrobe_db"
            ).build()

            allItems = db.wishListItemDao().getAll()
            filtered = allItems
            subcategories = db.subcategoryDao().getAllSubcategories()  // сохраняем в поле

            // Формируем список названий вкладок
            val names = mutableListOf<String>("Все")
            subcategories.forEach { names.add(it.name) }

            withContext(Dispatchers.Main) {
                tabLayout.removeAllTabs()
                names.forEach { title ->
                    tabLayout.addTab(tabLayout.newTab().setText(title))
                }
                tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                    override fun onTabSelected(tab: TabLayout.Tab) {
                        applyAllFilters(searchView.query.toString(), tab.text.toString())
                    }
                    override fun onTabUnselected(tab: TabLayout.Tab) = Unit
                    override fun onTabReselected(tab: TabLayout.Tab) = Unit
                })
                adapter.updateData(filtered)
            }
        }
    }

    /** Основная фильтрация: текст + категория */
    private fun applyAllFilters(text: String, category: String = "Все") {
        // 1) текстовый поиск
        var tmp = allItems.filter {
            it.name.contains(text, ignoreCase = true)
        }
        // 2) фильтр по категории (ищем id в списке subcategories)
        if (category != "Все") {
            val id = subcategories.find { it.name == category }?.id ?: -1
            tmp = tmp.filter { it.subcategoryId == id }
        }
        filtered = tmp
        adapter.updateData(filtered)
    }

    /** Окно «Добавить по URL» */
    private fun showAddByUrlDialog() {
        val et = EditText(requireContext()).apply {
            hint = "Вставьте ссылку на изображение"
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Добавить одежду по URL")
            .setView(et)
            .setPositiveButton("Добавить") { _, _ ->
                val url = et.text.toString()
                if (url.isNotBlank()) insertNewItem(url)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    /** Вставляем в БД и обновляем список */
    private fun insertNewItem(imageUrl: String) {
        val newItem = WishListItem(imageUrl, "Новая вещь", 0.0, "", "", subcategories.firstOrNull()?.id ?: 1, "")
        CoroutineScope(Dispatchers.IO).launch {
            val db = Room.databaseBuilder(
                requireContext(),
                AppDatabase::class.java,
                "wardrobe_db"
            ).build()
            db.wishListItemDao().insert(newItem)
            allItems = db.wishListItemDao().getAll()
            withContext(Dispatchers.Main) {
                applyAllFilters(searchView.query.toString())
            }
        }
    }
}
