package com.hfad.mystylebox.ui.bottomsheet

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Bundle
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

class OutfitActionsBottomSheet : BottomSheetDialogFragment() {

    var onDeleteClicked: (() -> Unit)? = null
    var onEditClicked: (() -> Unit)? = null

    companion object {
        fun newInstance(outfitName: String, outfitImagePath: String?): OutfitActionsBottomSheet {
            val args = Bundle().apply {
                putString("outfitName", outfitName)
                putString("outfitImagePath", outfitImagePath)
            }
            return OutfitActionsBottomSheet().apply { arguments = args }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.bottom_sheet_outfit_actions, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val tvTitle = view.findViewById<TextView>(R.id.tvTitle)
        val btnDelete = view.findViewById<Button>(R.id.btnDelete)
        val btnShare = view.findViewById<Button>(R.id.btnShare)
        val btnCancel = view.findViewById<Button>(R.id.btnCancel)

        val outfitName = arguments?.getString("outfitName") ?: ""
        val outfitImagePath = arguments?.getString("outfitImagePath")
        tvTitle.text = outfitName

        btnDelete.setOnClickListener {
            onDeleteClicked?.invoke()
            dismiss()
        }
        view.findViewById<Button>(R.id.btnEdit).setOnClickListener {
            onEditClicked?.invoke()
            dismiss()
        }
        btnCancel.setOnClickListener {
            dismiss()
        }

        btnShare.setOnClickListener {
            val rawPath = arguments?.getString("outfitImagePath")
            if (rawPath.isNullOrBlank()) {
                Toast.makeText(requireContext(), "Изображение не найдено", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            try {
                val parsedUri = Uri.parse(rawPath)
                val contentUri: Uri = when (parsedUri.scheme) {
                    ContentResolver.SCHEME_CONTENT -> parsedUri
                    ContentResolver.SCHEME_FILE -> {
                        val file = File(parsedUri.path!!)
                        FileProvider.getUriForFile(
                            requireContext(),
                            "${requireContext().packageName}.fileprovider",
                            file
                        )
                    }
                    else -> {
                        val file = File(rawPath)
                        FileProvider.getUriForFile(
                            requireContext(),
                            "${requireContext().packageName}.fileprovider",
                            file
                        )
                    }
                }

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/*"
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    putExtra(Intent.EXTRA_TEXT, "Посмотри какой стильный комплект я создала!")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(shareIntent, "Поделиться комплектом"))
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Не удалось поделиться: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }

            dismiss()
        }
    }
}