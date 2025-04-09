package com.hfad.mystylebox

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.FileProvider
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.io.File

class OutfitActionsBottomSheet : BottomSheetDialogFragment() {

    var onDeleteClicked: (() -> Unit)? = null

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

        btnCancel.setOnClickListener {
            dismiss()
        }

        btnShare.setOnClickListener {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/*"
                outfitImagePath?.let { path ->
                    val imageFile = File(path)
                    if (imageFile.exists()) {
                        val uri = FileProvider.getUriForFile(
                            requireContext(),
                            "${requireContext().packageName}.fileprovider",
                            imageFile
                        )
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                }
                putExtra(Intent.EXTRA_TEXT, "Посмотри какой стильный комплект я создала!")
            }
            startActivity(Intent.createChooser(shareIntent, "Поделиться комплектом"))
            dismiss()
        }
    }
}