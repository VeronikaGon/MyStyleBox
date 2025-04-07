package com.hfad.mystylebox

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class OutfitActionsBottomSheet : BottomSheetDialogFragment() {

    var onDeleteClicked: (() -> Unit)? = null

    companion object {
        fun newInstance(outfitName: String, outfitImagePath: String?): OutfitActionsBottomSheet {
            val args = Bundle().apply {
                putString("outfitName", outfitName)
                putString("outfitImagePath", outfitImagePath)
            }
            val fragment = OutfitActionsBottomSheet()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_outfit_actions, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val tvTitle = view.findViewById<TextView>(R.id.tvTitle)
        val btnDelete = view.findViewById<Button>(R.id.btnDelete)

        val outfitName = arguments?.getString("outfitName") ?: ""
        tvTitle.text = outfitName

        btnDelete.setOnClickListener {
            onDeleteClicked?.invoke()
            dismiss()
        }
    }
}