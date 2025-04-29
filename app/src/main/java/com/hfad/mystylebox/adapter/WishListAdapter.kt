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
        val onClick: (WishListItem) -> Unit,
        val onLongClick: (WishListItem) -> Unit
    ) : RecyclerView.Adapter<WishListAdapter.VH>() {

        inner class VH(val view: View) : RecyclerView.ViewHolder(view) {
            private val ivImage = view.findViewById<ImageView>(R.id.itemImage)
            private val tvName  = view.findViewById<TextView>(R.id.itemName)
            private val tvPrice: TextView? = view.findViewById(R.id.itemCategory)

            fun bind(item: WishListItem) {
                val path = item.imagePath
                if (!path.isNullOrBlank()) {
                    Glide.with(view)
                        .load(path)

                        .into(ivImage)
                } else {
                }
                tvName.text  = item.name
                tvPrice?.text = "Стоимость: ${item.price}"
                view.setOnClickListener { onClick(item) }
                view.setOnLongClickListener {
                    onLongClick(item)
                    true
                }
            }
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            holder.bind(items[position])
        }
        override fun getItemViewType(position: Int): Int = layoutRes

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context)
                .inflate(viewType, parent, false)
            return VH(v)
        }

        override fun getItemCount() = items.size

        fun updateData(newItems: List<WishListItem>) {
            items = newItems
            notifyDataSetChanged()
        }
    }