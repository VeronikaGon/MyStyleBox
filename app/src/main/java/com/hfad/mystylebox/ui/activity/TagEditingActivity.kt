package com.hfad.mystylebox.ui.activity

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.hfad.mystylebox.R
import com.hfad.mystylebox.adapter.TagAdapter
import com.hfad.mystylebox.database.AppDatabase
import com.hfad.mystylebox.database.dao.ClothingItemTagDao
import com.hfad.mystylebox.database.entity.Tag
import com.hfad.mystylebox.database.dao.TagDao
import com.hfad.mystylebox.fragment.TagCreationDialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TagEditingActivity : AppCompatActivity() {
    private lateinit var clothingItemTagDao: ClothingItemTagDao
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TagAdapter
    private lateinit var db: AppDatabase
    private lateinit var tagDao: TagDao
    private val tags = mutableListOf<Tag>()
    private val selectedTagIds = mutableSetOf<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tag_editing)

        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "wardrobe_db"
        )
            .allowMainThreadQueries()
            .build()
        tagDao = db.tagDao()
        clothingItemTagDao = db.clothingItemTagDao()
        tags.addAll(tagDao.getAllTags())

        val initialSelectedIds = intent.getIntegerArrayListExtra("selected_ids")
        initialSelectedIds?.let { selectedTagIds.addAll(it) }
        recyclerView = findViewById(R.id.recyclerViewTags)
        adapter = TagAdapter(
            tags,
            selectedTagIds,
            clothingItemTagDao,
            onDelete = { tag -> confirmDeleteTag(tag) },
            onItemClick = { tag -> toggleSelection(tag) }
        )
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        supportActionBar?.apply {
            title = "Редактирование тегов"
            setDisplayHomeAsUpEnabled(true)
        }
        val searchView = findViewById<SearchView>(R.id.searchView)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                val filteredTags = if (newText.isNullOrEmpty()) {
                    tagDao.getAllTags()
                } else {
                    tagDao.getAllTags().filter { it.name.contains(newText, ignoreCase = true) }
                }
                adapter.updateList(filteredTags)
                return true
            }
        })

        val fabAddTag = findViewById<FloatingActionButton>(R.id.fabAddTag)
        fabAddTag.setOnClickListener {
            showTagCreationDialog()
        }

    }

    // Метод для переключения состояния выбора
    private fun toggleSelection(tag: Tag) {
        if (selectedTagIds.contains(tag.id)) {
            selectedTagIds.remove(tag.id)
        } else {
            selectedTagIds.add(tag.id)
        }
        adapter.notifyDataSetChanged()
    }

    // Метод для подтверждения удаления тега
    private fun confirmDeleteTag(tag: Tag) {
        AlertDialog.Builder(this)
            .setTitle("Удалить тег")
            .setMessage("Вы действительно хотите удалить тег \"${tag.name}\"?")
            .setPositiveButton("Удалить") { dialog, _ ->
                tagDao.delete(tag)
                tags.remove(tag)
                selectedTagIds.remove(tag.id)
                adapter.notifyDataSetChanged()
                dialog.dismiss()
            }
            .setNegativeButton("Отмена") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    // Если пользователь нажимает кнопку "Готово" (например, в меню), возвращаем выбранные теги:
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finishWithResult(false)  // Не сохраняем при нажатии назад
                true
            }
            R.id.action_done -> {
                finishWithResult(true)  // Сохраняем только при явном подтверждении
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun finishWithResult(saveSelection: Boolean) {
        if (saveSelection) {
            val selectedTags = tags.filter { selectedTagIds.contains(it.id) }
            val intent = Intent().apply {
                putParcelableArrayListExtra("selected_tags", ArrayList(selectedTags))
            }
            setResult(RESULT_OK, intent)
        } else {
            setResult(RESULT_CANCELED)
        }
        finish()
    }

    fun showTagCreationDialog() {
        val dialog = TagCreationDialogFragment()
        dialog.setOnTagCreatedListener { newTagName ->
            lifecycleScope.launch(Dispatchers.IO) {
                val existingTag = tagDao.getTagByName(newTagName.toString())

                if (existingTag == null) {
                    val newTag =
                        Tag(newTagName.toString())
                    val newId = tagDao.insert(newTag)
                    val updatedTags = tagDao.getAllTags()

                    withContext(Dispatchers.Main) {
                        tags.clear()
                        tags.addAll(updatedTags)
                        adapter.notifyDataSetChanged()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@TagEditingActivity,
                            "Тег '${newTagName}' уже существует",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
        dialog.show(supportFragmentManager, "TagCreationDialog")
    }
}
