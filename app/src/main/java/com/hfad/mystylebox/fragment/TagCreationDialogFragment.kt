package com.hfad.mystylebox.fragment

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.hfad.mystylebox.R
import com.hfad.mystylebox.database.Tag

class TagCreationDialogFragment : DialogFragment() {

    private var onTagCreatedListener: ((String) -> Unit)? = null

    fun setOnTagCreatedListener(listener: (String) -> Unit) {
        onTagCreatedListener = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())
        val view = requireActivity().layoutInflater.inflate(R.layout.dialog_create_tag, null)

        val editTextTag = view.findViewById<EditText>(R.id.enterTagName)
        val imageSave = view.findViewById<ImageButton>(R.id.imageSave)
        val buttonCancel = view.findViewById<Button>(R.id.ButtonCancel)
        imageSave.setOnClickListener {
            val tagName = editTextTag.text.toString().trim()
            if (tagName.isNotEmpty()) {
                Toast.makeText(requireContext(), "Тег '$tagName' сохранён", Toast.LENGTH_SHORT).show()
                onTagCreatedListener?.invoke(tagName)
                editTextTag.text.clear()
            } else {
                Toast.makeText(requireContext(), "Введите название тега", Toast.LENGTH_SHORT).show()
            }
        }
        buttonCancel.setOnClickListener {
            dismiss()
        }

        builder.setView(view)
        return builder.create()
    }
}
