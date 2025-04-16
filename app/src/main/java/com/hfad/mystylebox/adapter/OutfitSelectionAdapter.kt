package com.hfad.mystylebox.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.hfad.mystylebox.R
import com.hfad.mystylebox.database.OutfitItemFull

class OutfitSelectionAdapter(
    private var items: List<OutfitItemFull>,
    private val layoutResId: Int,
    private val selectionListener: OnItemSelectionListener,
    private val globalSelected: MutableSet<OutfitItemFull>
) : RecyclerView.Adapter<OutfitSelectionAdapter.OutfitViewHolder>() {

    interface OnItemSelectionListener {
        fun onItemSelectionChanged(item: OutfitItemFull, isSelected: Boolean)
    }

    inner class OutfitViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Корневой контейнер элемента (например, ConstraintLayout)
        val container: ConstraintLayout = itemView as ConstraintLayout
        val itemImage: ImageView = itemView.findViewById(R.id.itemImage)
        val itemName: TextView = itemView.findViewById(R.id.itemName)

        fun bind(item: OutfitItemFull) {
            // Загрузка изображения с помощью Glide
            Glide.with(itemView.context)
                .load(item.outfit.imagePath)
                .into(itemImage)

            itemName.text = item.outfit.name

            // Обновляем фон контейнера в зависимости от выбранности
            if (globalSelected.contains(item)) {
                container.setBackgroundResource(R.drawable.item_background_active)
            } else {
                container.setBackgroundResource(R.drawable.item_background)
            }

            // Обработчик клика по элементу
            itemView.setOnClickListener {
                toggleSelection(item)
            }
        }

        private fun toggleSelection(item: OutfitItemFull) {
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OutfitViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(layoutResId, parent, false)
        return OutfitViewHolder(view)
    }

    override fun onBindViewHolder(holder: OutfitViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    /**
     * Метод для обновления данных адаптера.
     */
    fun updateData(newItems: List<OutfitItemFull>) {
        items = newItems
        notifyDataSetChanged()
    }
}