package com.hfad.mystylebox.ui.bottomsheet

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.hfad.mystylebox.R
import java.io.File


class ItemActionsBottomSheet : BottomSheetDialogFragment() {
    var onDeleteClicked: (() -> Unit)? = null
    var onEditClicked: (() -> Unit)? = null
    var onCreateOutfitClicked: (() -> Unit)? = null

    companion object {
        private const val ARG_ITEM_NAME = "arg_item_name"
        private const val ARG_IMAGE_URI = "arg_image_uri"

        fun newInstance(itemName: String, imageUri: String): ItemActionsBottomSheet {
            return ItemActionsBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_ITEM_NAME, itemName)
                    putString(ARG_IMAGE_URI, imageUri)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_bottom_sheet_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<TextView>(R.id.tvItemName).text = arguments?.getString(ARG_ITEM_NAME) ?: "Вещь"
        view.findViewById<Button>(R.id.btnDelete).setOnClickListener {
            onDeleteClicked?.invoke()
            dismiss()
        }
        view.findViewById<Button>(R.id.btnEdit).setOnClickListener {
            onEditClicked?.invoke()
            dismiss()
        }
        view.findViewById<Button>(R.id.btnCreateOutfit).setOnClickListener {
            onCreateOutfitClicked?.invoke()
            dismiss()
        }
        view.findViewById<Button>(R.id.btnCancel).setOnClickListener {
            dismiss()
        }
        view.findViewById<Button>(R.id.btnShare).setOnClickListener {
            val imageUriString = arguments?.getString(ARG_IMAGE_URI) ?: run {
                Toast.makeText(context, "Изображение не найдено", Toast.LENGTH_SHORT).show()
                dismiss()
                return@setOnClickListener
            }
            try {
                val tempFile = File(requireContext().cacheDir, "share_temp_${System.currentTimeMillis()}.jpg")
                requireContext().contentResolver.openInputStream(Uri.parse(imageUriString))?.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                val contentUri = FileProvider.getUriForFile(
                    requireContext(),
                    "${requireContext().packageName}.fileprovider",
                    tempFile
                )
                Intent(Intent.ACTION_SEND).apply {
                    type = "image/*"
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    putExtra(Intent.EXTRA_TEXT, "Посмотрите на эту вещь!")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }.also { intent ->
                    if (intent.resolveActivity(requireContext().packageManager) != null) {
                        startActivity(Intent.createChooser(intent, "Поделиться"))
                    } else {
                        Toast.makeText(context, "Нет приложений для обмена", Toast.LENGTH_SHORT).show()
                    }
                }
                tempFile.deleteOnExit()
            } catch (e: Exception) {
                Toast.makeText(context, "Ошибка при отправке: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                Log.e("ShareError", "Ошибка шаринга", e)
            }
            dismiss()
        }
    }
}