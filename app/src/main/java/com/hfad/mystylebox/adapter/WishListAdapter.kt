package com.hfad.mystylebox.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.hfad.mystylebox.R
import com.hfad.mystylebox.database.entity.WishListItem

class WishListAdapter(
    private var items: List<WishListItem>,
    var layoutRes: Int,
    val onItemClick: (WishListItem) -> Unit
) : RecyclerView.Adapter<WishListAdapter.VH>() {

    inner class VH(val view: View) : RecyclerView.ViewHolder(view) {
        private val ivImage = view.findViewById<ImageView>(R.id.image)
        private val tvName  = view.findViewById<TextView>(R.id.itemName)
        private val tvPrice = view.findViewById<TextView>(R.id.itemCategory)
        fun bind(item: WishListItem) {
            Glide.with(view).load(item.imagePath).into(ivImage)
            tvName.text  = item.name
            tvPrice.text = "Стоимость: ${item.price}"
            view.setOnClickListener { onItemClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(layoutRes, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    fun updateData(newItems: List<WishListItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}
