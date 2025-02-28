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

class ClothingAdapter(
    initialItems: List<ClothingItem>,
    private val subcategoryToCategoryMap: Map<Int, String>
) : RecyclerView.Adapter<ClothingAdapter.ClothingViewHolder>() {

    private var items = initialItems.toMutableList()
    private val allItems = initialItems.toList()

    var onItemClick: ((ClothingItem) -> Unit)? = null
    var onItemLongClick: ((ClothingItem) -> Unit)? = null

    inner class ClothingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.itemImage)
        val nameText: TextView = view.findViewById(R.id.itemName)

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
            .inflate(R.layout.item_clothing, parent, false)
        return ClothingViewHolder(view)
    }

    override fun onBindViewHolder(holder: ClothingViewHolder, position: Int) {
        val item = items[position]
        val maxChars = 15
        val displayName = if (item.name.length > maxChars) {
            item.name.take(maxChars) + "..."
        } else {
            item.name
        }
        holder.nameText.text = item.name
        Glide.with(holder.imageView.context).load(item.imagePath).into(holder.imageView)
        holder.itemView.setOnClickListener {
            onItemClick?.invoke(item)
        }
    }

    fun filterByCategory(category: String) {
        items = if (category == "Все") {
            allItems.toMutableList()
        } else {
            allItems.filter { item ->
                subcategoryToCategoryMap[item.subcategoryId] == category
            }.toMutableList()
        }
        notifyDataSetChanged()
    }

    override fun getItemCount() = items.size
}
