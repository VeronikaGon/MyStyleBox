package com.hfad.mystylebox.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.hfad.mystylebox.R
import com.hfad.mystylebox.database.ClothingItemFull

class ClothingSelectionAdapter(
    private var items: List<ClothingItemFull>,
    private val layoutResId: Int,
    private val selectionListener: OnItemSelectionListener,
    private val globalSelected: MutableSet<ClothingItemFull>,
    private val lockedPaths: Set<String>
) : RecyclerView.Adapter<ClothingSelectionAdapter.ClothingViewHolder>() {

    interface OnItemSelectionListener {
        fun onItemSelectionChanged(item: ClothingItemFull, isSelected: Boolean)
    }

    inner class ClothingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val container: ConstraintLayout = itemView as ConstraintLayout
        val itemImage: ImageView = itemView.findViewById(R.id.itemImage)
        val itemName: TextView = itemView.findViewById(R.id.itemName)

        fun bind(item: ClothingItemFull) {
            Glide.with(itemView.context)
                .load(item.clothingItem.imagePath)
                .override(200, 200)
                .into(itemImage)

            itemName.text = item.clothingItem.name

            if (lockedPaths.contains(item.clothingItem.imagePath)) {
                container.setBackgroundColor(Color.parseColor("#FCD5CE"))
                itemView.setOnClickListener(null)
            } else {
                if (globalSelected.contains(item)) {
                    container.setBackgroundResource(R.drawable.item_background_active)
                } else {
                    container.setBackgroundResource(R.drawable.item_background)
                }
                itemView.setOnClickListener {
                    toggleSelection(item)
                }
            }
        }

        private fun toggleSelection(item: ClothingItemFull) {
            if (globalSelected.contains(item)) {
                globalSelected.remove(item)
                container.setBackgroundResource(R.drawable.item_background)
                selectionListener.onItemSelectionChanged(item, false)
            } else {
                globalSelected.add(item)
                container.setBackgroundResource(R.drawable.item_background_active)
                selectionListener.onItemSelectionChanged(item, true)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClothingViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(layoutResId, parent, false)
        return ClothingViewHolder(view)
    }

    override fun onBindViewHolder(holder: ClothingViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    // Метод для обновления данных адаптера
    fun updateData(newItems: List<ClothingItemFull>) {
        items = newItems
        notifyDataSetChanged()
    }
}