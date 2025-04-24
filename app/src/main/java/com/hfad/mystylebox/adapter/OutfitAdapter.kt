package com.hfad.mystylebox.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.hfad.mystylebox.R
import com.hfad.mystylebox.database.entity.Outfit

class OutfitAdapter(
    private var outfits: List<Outfit>,
    private val itemLayout: Int
) : RecyclerView.Adapter<OutfitAdapter.OutfitViewHolder>() {

    var onItemClick: ((Outfit) -> Unit)? = null
    var onItemLongClick: ((Outfit) -> Unit)? = null

    inner class OutfitViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.itemName)
        val ivImage: ImageView = itemView.findViewById(R.id.itemImage)
        val descriptionText: TextView? = itemView.findViewById(R.id.itemCategory)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OutfitViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(itemLayout, parent, false)
        return OutfitViewHolder(view)
    }

    override fun onBindViewHolder(holder: OutfitViewHolder, position: Int) {
        val outfit = outfits[position]
        holder.tvName.text = outfit.name
        Glide.with(holder.itemView.context)
            .load(outfit.imagePath)
            .into(holder.ivImage)
        val maxChars = 53
        holder.descriptionText?.text  = if (outfit.description.length > maxChars) {
            outfit.description.take(maxChars) + "..."
        } else {
            outfit.description
        }
        holder.itemView.setOnClickListener { onItemClick?.invoke(outfit) }
        holder.itemView.setOnLongClickListener {
            onItemLongClick?.invoke(outfit)
            true
        }
    }

    private fun dpToPx(context: Context, dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }

    override fun getItemCount() = outfits.size

    fun updateData(newOutfits: List<Outfit>) {
        outfits = newOutfits
        notifyDataSetChanged()
    }
}