package com.hfad.mystylebox

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.hfad.mystylebox.adapter.TagAdapter
import com.hfad.mystylebox.database.AppDatabase
import com.hfad.mystylebox.database.Tag
import com.hfad.mystylebox.database.TagDao
import com.hfad.mystylebox.fragment.TagCreationDialogFragment

class TagEditingActivity : AppCompatActivity() {

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
        tags.addAll(tagDao.getAllTags())

        recyclerView = findViewById(R.id.recyclerViewTags)
        adapter = TagAdapter(tags, selectedTagIds,
            onDelete = { tag -> confirmDeleteTag(tag) },
            onItemClick = { tag -> toggleSelection(tag) }
        )
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Если хотите добавить кнопку "Готово" в AppBar, можно переопределить меню
        // Например, при выборе тега возвращать результат:
        supportActionBar?.apply {
            title = "Редактирование тегов"
            setDisplayHomeAsUpEnabled(true)
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
                // Удаляем из базы
                tagDao.delete(tag)
                // Удаляем из списка и обновляем адаптер
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
                finishWithResult()
                true
            }
            R.id.action_done -> {
                finishWithResult()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun finishWithResult() {
        // Собираем выбранные теги (можно передавать список объектов или только их имена/идентификаторы)
        val selectedTags = tags.filter { selectedTagIds.contains(it.id) }
        val intent = Intent().apply {
            putParcelableArrayListExtra("selected_tags", ArrayList(selectedTags))
        }
        setResult(RESULT_OK, intent)
        finish()
    }

    fun showTagCreationDialog() {
        val dialog = TagCreationDialogFragment()
        dialog.setOnTagCreatedListener { newTag ->
            val newId = tagDao.insert(newTag).toInt()
            val tagWithId = Tag(newTag.name).apply { id = newId }
            tags.add(tagWithId)
            adapter.notifyDataSetChanged()
        }
        dialog.show(supportFragmentManager, "TagCreationDialog")
    }
}
