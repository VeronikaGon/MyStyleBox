package com.hfad.mystylebox

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ImageOptionsBottomSheet : BottomSheetDialogFragment() {

    interface ImageOptionsListener {
        fun onEditOptionSelected()
        fun onChangePhotoOptionSelected()
    }

    private var listener: ImageOptionsListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = if (context is ImageOptionsListener) {
            context
        } else {
            throw RuntimeException("$context must implement ImageOptionsListener")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.bottom_sheet_image_options, container, false)

        view.findViewById<Button>(R.id.btnEdit).setOnClickListener {
            listener?.onEditOptionSelected()
            dismiss()
        }
        view.findViewById<Button>(R.id.btnChangePhoto).setOnClickListener {
            listener?.onChangePhotoOptionSelected()
            dismiss()
        }
        view.findViewById<Button>(R.id.btnCancel).setOnClickListener {
            dismiss()
        }
        return view
    }
}