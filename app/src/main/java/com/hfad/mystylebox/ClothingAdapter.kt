package com.hfad.mystylebox

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.hfad.mystylebox.database.ClothingItem

class ClothingAdapter(
    private var items: List<ClothingItem>,
    private val subcategoryToCategoryMap: Map<Int, String>
) : RecyclerView.Adapter<ClothingAdapter.ClothingViewHolder>() {

    // Сохраняем полный список для возможности сброса фильтра
    private val allItems: List<ClothingItem> = items.toList()

    var onItemClick: ((ClothingItem) -> Unit)? = null

    class ClothingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.itemImage)
        val nameText: TextView = view.findViewById(R.id.itemName)
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

    override fun getItemCount() = items.size

    // Метод фильтрации по категории
    fun filterByCategory(category: String) {
        items = if (category.equals("Все", ignoreCase = true)) {
            allItems
        } else {
            allItems.filter { clothingItem ->
                subcategoryToCategoryMap[clothingItem.subcategoryId]?.equals(category, ignoreCase = true) ?: false
            }
        }
        notifyDataSetChanged()
    }
}
