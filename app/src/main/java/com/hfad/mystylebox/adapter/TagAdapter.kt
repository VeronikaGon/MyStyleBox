package com.hfad.mystylebox.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.hfad.mystylebox.R
import com.hfad.mystylebox.database.dao.ClothingItemTagDao
import com.hfad.mystylebox.database.dao.OutfitTagDao
import com.hfad.mystylebox.database.entity.Tag

class TagAdapter(
    private var tags: List<Tag>,
    private val selectedTagIds: Set<Int>,
    private val itemTagDao: ClothingItemTagDao,
    private val outfitTagDao: OutfitTagDao,
    private val onDelete: (Tag) -> Unit,
    private val onItemClick: (Tag) -> Unit
) : RecyclerView.Adapter<TagAdapter.TagViewHolder>() {

    inner class TagViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTagName: TextView = itemView.findViewById(R.id.tvTagName)
        val tvItemCount: TextView = itemView.findViewById(R.id.tvTagCount)
        val tvOutfitCount: TextView = itemView.findViewById(R.id.tvOutfitCount)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDeleteTag)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tag, parent, false)
        return TagViewHolder(view)
    }

    override fun getItemCount(): Int = tags.size

    override fun onBindViewHolder(holder: TagViewHolder, position: Int) {
        val tag = tags[position]
        holder.tvTagName.text = tag.name

        val itemCount = itemTagDao.getCountForTag(tag.id)
        val outfitCount = outfitTagDao.getCountForTag(tag.id)

        holder.tvItemCount.text = "Вещей с тегом: $itemCount"
        holder.tvOutfitCount.text = "Комплектов с тегом: $outfitCount"

        holder.itemView.isSelected = selectedTagIds.contains(tag.id)
        holder.itemView.setOnClickListener { onItemClick(tag) }
        holder.btnDelete.setOnClickListener { onDelete(tag) }
    }

    fun updateList(newTags: List<Tag>) {
        tags = newTags
        notifyDataSetChanged()
    }
}