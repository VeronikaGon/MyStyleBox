package com.hfad.mystylebox.fragment

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import com.hfad.mystylebox.R
import com.hfad.mystylebox.database.Tag

class TagCreationDialogFragment : DialogFragment() {

    private var onTagCreatedListener: ((Tag) -> Unit)? = null

    fun setOnTagCreatedListener(listener: (Tag) -> Unit) {
        onTagCreatedListener = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Создаем билдер диалога
        val builder = AlertDialog.Builder(requireContext())
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_create_tag, null)
        val etTagName = view.findViewById<EditText>(R.id.etTagName)

        builder.setView(view)
            .setTitle("Создать тег")
            .setPositiveButton("Создать") { dialog, _ ->
                val tagName = etTagName.text.toString().trim()
                if (tagName.isNotEmpty()) {
                    // Создаем новый тег (для id можно использовать, например, текущее время)
                    val newTag = Tag(tagName)
                    onTagCreatedListener?.invoke(newTag)
                }
            }
            .setNegativeButton("Отмена") { dialog, _ ->
                dialog.dismiss()
            }
        return builder.create()
    }
}
