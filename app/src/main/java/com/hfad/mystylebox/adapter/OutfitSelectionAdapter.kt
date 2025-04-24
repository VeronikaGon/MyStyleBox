package com.hfad.mystylebox.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.hfad.mystylebox.R
import com.hfad.mystylebox.database.entity.OutfitItemFull

class OutfitSelectionAdapter(
    private var items: List<OutfitItemFull>,
    @LayoutRes private val layoutResId: Int,
    private val selectionListener: OnItemSelectionListener,
    private val globalSelected: MutableSet<OutfitItemFull>,
    private val preselectedIds: Set<Long>
) : RecyclerView.Adapter<OutfitSelectionAdapter.OutfitViewHolder>() {

    interface OnItemSelectionListener {
        fun onItemSelectionChanged(item: OutfitItemFull, isSelected: Boolean)
    }

    inner class OutfitViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val container: ConstraintLayout = itemView as ConstraintLayout
        private val itemImage: ImageView = itemView.findViewById(R.id.itemImage)
        private val itemName: TextView = itemView.findViewById(R.id.itemName)

        fun bind(item: OutfitItemFull) {
            Glide.with(itemView).load(item.outfit.imagePath).into(itemImage)
            itemName.text = item.outfit.name



            // Меняем фон в зависимости от текущего состояния выбора
            val bgRes = if (globalSelected.contains(item))
                R.drawable.item_background_active
            else
                R.drawable.item_background
            container.setBackgroundResource(bgRes)

            itemView.isEnabled = true
            itemView.setOnClickListener { toggleSelection(item) }
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
        val view = LayoutInflater.from(parent.context)
            .inflate(layoutResId, parent, false)
        return OutfitViewHolder(view)
    }

    override fun onBindViewHolder(holder: OutfitViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    /**
     * Обновление данных адаптера новым списком
     */
    fun updateData(newItems: List<OutfitItemFull>) {
        items = newItems
        notifyDataSetChanged()
    }
}