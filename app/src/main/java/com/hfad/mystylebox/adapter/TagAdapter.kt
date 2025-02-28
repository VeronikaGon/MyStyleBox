package com.hfad.mystylebox.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.hfad.mystylebox.R
import com.hfad.mystylebox.TagEditingActivity
import com.hfad.mystylebox.database.Tag

class TagAdapter(
    private val tags: List<Tag>,
    private val selectedTagIds: Set<Int>,
    private val onDelete: (Tag) -> Unit,
    private val onItemClick: (Tag) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_TAG = 0
        private const val VIEW_TYPE_FOOTER = 1
    }

    // ViewHolder для тега
    inner class TagViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTagName: TextView = itemView.findViewById(R.id.tvTagName)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDeleteTag)
        val checkbox: CheckBox = itemView.findViewById(R.id.cbSelectTag)
    }

    // ViewHolder для футера (кнопка "Создать тег")
    inner class FooterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val btnCreateTag: Button = itemView.findViewById(R.id.btnFooterCreateTag)
    }

    override fun getItemViewType(position: Int): Int {
        return if (position < tags.size) VIEW_TYPE_TAG else VIEW_TYPE_FOOTER
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_TAG) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_tag, parent, false)
            TagViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_tag_footer, parent, false)
            FooterViewHolder(view)
        }
    }

    override fun getItemCount(): Int {
        // +1 для футера
        return tags.size + 1
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is TagViewHolder && position < tags.size) {
            val tag = tags[position]
            holder.tvTagName.text = tag.name
            holder.checkbox.isChecked = selectedTagIds.contains(tag.id)

            // При клике по элементу переключаем выбор
            holder.itemView.setOnClickListener { onItemClick(tag) }
            // При клике по кнопке удаления вызываем подтверждение
            holder.btnDelete.setOnClickListener { onDelete(tag) }
        } else if (holder is FooterViewHolder) {
            // Кнопка для создания нового тега
            holder.btnCreateTag.setOnClickListener {
                // Так как адаптер не знает об активности,
                // можно передать событие через контекст, если он TagEditingActivity
                if (holder.itemView.context is TagEditingActivity) {
                    (holder.itemView.context as TagEditingActivity).showTagCreationDialog()
                }
            }
        }
    }
}
