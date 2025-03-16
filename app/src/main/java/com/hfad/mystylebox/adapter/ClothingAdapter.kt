package com.hfad.mystylebox.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.hfad.mystylebox.R
import com.hfad.mystylebox.database.ClothingItemFull

class ClothingAdapter(
    initialItems: List<ClothingItemFull>,
    private val layoutResId: Int
) : RecyclerView.Adapter<ClothingAdapter.ClothingViewHolder>() {

    private var items = mutableListOf<ClothingItemFull>()
    private val allItems = mutableListOf<ClothingItemFull>()

    init {
        items.addAll(initialItems)
        allItems.addAll(initialItems)
    }

    var onItemClick: ((ClothingItemFull) -> Unit)? = null
    var onItemLongClick: ((ClothingItemFull) -> Unit)? = null

    inner class ClothingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.itemImage)
        val nameText: TextView = view.findViewById(R.id.itemName)
        val categoryText: TextView? = view.findViewById(R.id.itemCategory)

        init {
            view.setOnClickListener {
                val pos = adapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    onItemClick?.invoke(items[pos])
                }
            }
            view.setOnLongClickListener {
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
        val itemFull = items[position]
        val item = itemFull.clothingItem
        val maxChars = 15
        holder.nameText.text = if (item.name.length > maxChars) {
            item.name.take(maxChars) + "..."
        } else {
            item.name
        }
        holder.categoryText?.text = "${itemFull.categoryName} > ${itemFull.subcategoryName}"

        Glide.with(holder.imageView.context)
            .load(item.imagePath)
            .into(holder.imageView)
    }

    fun filterByCategory(targetCategory: String) {
        items.clear()
        if (targetCategory.equals("Все", ignoreCase = true)) {
            items.addAll(allItems)
        } else {
            items.addAll(
                allItems.filter { it.categoryName.equals(targetCategory, ignoreCase = true) }
            )
        }
        notifyDataSetChanged()
    }

    fun updateData(newItems: List<ClothingItemFull>) {
        items.clear()
        items.addAll(newItems)
        allItems.clear()
        allItems.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = items.size
}