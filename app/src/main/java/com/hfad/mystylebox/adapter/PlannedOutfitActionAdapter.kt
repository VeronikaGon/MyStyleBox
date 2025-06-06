package com.hfad.mystylebox.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.hfad.mystylebox.R
import com.hfad.mystylebox.database.entity.Outfit

class PlannedOutfitActionAdapter(
    private var outfits: List<Outfit>,
    private val onDeleteRequested: (Outfit) -> Unit,
    private val onItemLongClick: (Outfit) -> Unit
) : RecyclerView.Adapter<PlannedOutfitActionAdapter.VH>() {

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val image     = itemView.findViewById<ImageView>(R.id.itemImage)
        private val name      = itemView.findViewById<TextView>(R.id.itemName)
        private val btnDelete = itemView.findViewById<ImageButton>(R.id.ibnotplanned)

        fun bind(outfit: Outfit) {
            name.text = outfit.name
            Glide.with(itemView).load(outfit.imagePath).into(image)

            btnDelete.setOnClickListener {
                onDeleteRequested(outfit)
            }

            itemView.setOnLongClickListener {
                onItemLongClick(outfit)
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_outfit, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(outfits[position])
    }

    override fun getItemCount(): Int = outfits.size

    /** Обновление списка данных */
    fun updateData(newList: List<Outfit>) {
        outfits = newList
        notifyDataSetChanged()
    }
}
