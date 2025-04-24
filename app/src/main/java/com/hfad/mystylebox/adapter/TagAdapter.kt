package com.hfad.mystylebox.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.hfad.mystylebox.R
import com.hfad.mystylebox.database.dao.ClothingItemTagDao
import com.hfad.mystylebox.database.entity.Tag

class TagAdapter(
    private var tags: List<Tag>,
    private val selectedTagIds: Set<Int>,
    private val tagCountDao: ClothingItemTagDao,
    private val onDelete: (Tag) -> Unit,
    private val onItemClick: (Tag) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    inner class TagViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTagName: TextView = itemView.findViewById(R.id.tvTagName)
        val tvTagCount: TextView = itemView.findViewById(R.id.tvTagCount)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDeleteTag)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tag, parent, false)
        return TagViewHolder(view)
    }

    override fun getItemCount(): Int = tags.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is TagViewHolder && position < tags.size) {
            val tag = tags[position]
            holder.tvTagName.text = tag.name
            val count = tagCountDao.getCountForTag(tag.id)
            holder.tvTagCount.text = "Вещей с тегом: $count"

            holder.itemView.setOnClickListener { onItemClick(tag) }
            holder.btnDelete.setOnClickListener { onDelete(tag) }
        }
    }

    fun updateList(newTags: List<Tag>) {
        tags = newTags
        notifyDataSetChanged()
    }
}