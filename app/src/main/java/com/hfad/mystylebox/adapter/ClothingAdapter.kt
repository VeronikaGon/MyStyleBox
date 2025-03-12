package com.hfad.mystylebox.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.hfad.mystylebox.R
import com.hfad.mystylebox.database.ClothingItem
import com.hfad.mystylebox.database.ClothingItemWithCategory
import com.hfad.mystylebox.database.Subcategory

class ClothingAdapter(
    private val initialItems: List<ClothingItemWithCategory>,
    private val layoutResId: Int
) : RecyclerView.Adapter<ClothingAdapter.ClothingViewHolder>() {

    private var items = initialItems.toMutableList()
    private val allItems = initialItems.toList()

    var onItemClick: ((ClothingItemWithCategory) -> Unit)? = null
    var onItemLongClick: ((ClothingItemWithCategory) -> Unit)? = null

    inner class ClothingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.itemImage)
        val nameText: TextView = view.findViewById(R.id.itemName)
        val categoryText: TextView? = view.findViewById(R.id.itemCategory)

        init {
            itemView.setOnLongClickListener {
                val pos = adapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    onItemLongClick?.invoke(items[pos])
                }
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClothingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(layoutResId, parent, false)
        return ClothingViewHolder(view)
    }

    override fun onBindViewHolder(holder: ClothingViewHolder, position: Int) {
        val item = items[position]
        val maxChars = 15
        holder.nameText.text = if (item.clothingItem.name.length > maxChars) {
            item.clothingItem.name.take(maxChars) + "..."
        } else {
            item.clothingItem.name
        }

        holder.categoryText?.text = "${item.categoryName} > ${item.subcategoryName}"

        Glide.with(holder.imageView.context)
            .load(item.clothingItem.imagePath)
            .into(holder.imageView)

        holder.itemView.setOnClickListener {
            onItemClick?.invoke(item)
        }
    }

    fun filterByCategory(targetCategory: String) {
        items = if (targetCategory == "Все") {
            allItems.toMutableList()
        } else {
            allItems.filter {
                it.categoryName == targetCategory
            }.toMutableList()
        }
        notifyDataSetChanged()
    }

    fun updateData(newItems: List<ClothingItemWithCategory>) { // Изменен тип
        this.items = newItems.toMutableList()
        notifyDataSetChanged()
    }

    override fun getItemCount() = items.size
}